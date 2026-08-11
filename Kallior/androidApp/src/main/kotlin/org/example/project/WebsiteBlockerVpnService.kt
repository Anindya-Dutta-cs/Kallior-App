package org.example.project

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
class WebsiteBlockerVpnService : VpnService() {

    private val channelId = "WebsiteBlockerVpnChannel"
    private val notificationId = 2
    private val actionStop = "org.example.project.action.STOP_VPN"

    private val tunAddress = "10.0.0.2"
    private val dnsAddress = "10.0.0.1"

    private val dnsDispatcher = Dispatchers.IO.limitedParallelism(24)
    private val outputWriteMutex = Mutex()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var readJob: Job? = null
    private var processJob: Job? = null
    private var recoveryJob: Job? = null
    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
    private var currentNetwork: Network? = null

    // True only when the user explicitly stops the VPN (or it is revoked); tells
    // the recovery loop not to bring the tunnel back up.
    private var isShuttingDown = false

    // True when we are intentionally tearing down the tunnel for a network
    // change (handoff), so the recovery loop knows not to log an "unexpected"
    // warning and to reset backoff.
    private var isRestarting = false

    // Handoff of raw packets from the reader to the processor. Recreated per
    // tunnel so it can be closed (draining the processor) when the reader ends.
    private var packetChannel: Channel<PacketData> = Channel(Channel.UNLIMITED)

    // In a real Koin/Hilt setup this would be injected; for the service we build it directly.
    private lateinit var websiteBlockerRepository: WebsiteBlockerRepository
    private lateinit var upstreamManager: DnsUpstreamManager
    private var blockedWebsites = setOf<String>()
    private val lastBlockEmission = ConcurrentHashMap<String, Long>()
    private var websiteOverlayManager: WebsiteBlockOverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        websiteBlockerRepository = WebsiteBlockerRepository(this)
        upstreamManager = DnsUpstreamManager(this) { protect(it) }
        createNotificationChannel()
        VpnDiagnosticsStore.reset()
        websiteOverlayManager = WebsiteBlockOverlayManager(this).also {
            it.initialize(scope)
        }
        scope.launch {
            websiteBlockerRepository.blockedWebsitesFlow.collect { websites ->
                blockedWebsites = websites.toSet()
            }
        }
        scope.launch {
            websiteBlockerRepository.allowUntilFlow.collect { map ->
                // Seed the EventBus so whitelisting survives service restarts
                map.forEach { (domain, expiry) ->
                    BlockEventBus.updateWhitelist(domain, expiry)
                }
            }
        }
        // When a domain is whitelisted, restart the tunnel to flush the OS DNS
        // cache. Without this, Android/Chrome keep using the cached NXDOMAIN
        // and the site never reconnects.
        scope.launch {
            var previousSize = 0
            BlockEventBus.whitelistState.collect { whitelist ->
                if (whitelist.size > previousSize && previousSize > 0) {
                    restartVpn()
                }
                previousSize = whitelist.size
            }
        }
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == actionStop) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }
        // VpnService must present a persistent notification (also required on API 26+).
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(notificationId, notification)
        }
        isShuttingDown = false
        if (recoveryJob == null || recoveryJob?.isActive == false) {
            recoveryJob = scope.launch { runVpnWithRecovery() }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        super.onRevoke()
        // Called when the user disables the VPN in settings or another VPN takes over.
        isShuttingDown = true
        scope.launch { websiteBlockerRepository.setVpnEnabled(false) }
        notifyVpnRevoked()
        stopVpn()
        stopSelf()
    }

    /** One-time, event-driven notification shown when VPN control is revoked. */
    private fun notifyVpnRevoked() {
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Kallior blocking turned off")
            .setContentText("The VPN was disabled by the system or another app. Re-enable Focus Fortress to resume blocking.")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            ?.notify(notificationId + 1, notification)
    }

    // --- Resilience: recovery loop (Plan Phase 1.2) ------------------------

    /**
     * Brings the tunnel up and blocks until it closes. If it closes for any
     * reason other than an explicit shutdown, it is re-established with
     * exponential backoff (capped) so a transient OS/network tear-down does not
     * silently kill blocking.
     */
    private suspend fun runVpnWithRecovery() {
        var backoffDelay = 1000L
        while (scope.isActive && !isShuttingDown) {
            try {
                startTunnel()
                // startTunnel joins the reader/processor jobs, so reaching here
                // means the tunnel closed.
                if (isShuttingDown) break

                if (isRestarting) {
                    backoffDelay = 1000L
                    isRestarting = false
                } else {
                    Log.w("KalliorVPN", "Tunnel closed unexpectedly; restarting in ${backoffDelay}ms")
                }
            } catch (e: Exception) {
                if (isShuttingDown || !scope.isActive) break
                Log.e("KalliorVPN", "Tunnel crashed, restarting in ${backoffDelay}ms", e)
            }
            VpnDiagnosticsStore.update { it.copy(tunnelRestarts = it.tunnelRestarts + 1) }
            delay(backoffDelay.milliseconds)
            if (!isRestarting) {
                backoffDelay = (backoffDelay * 2).coerceAtMost(30000L)
            }
        }
    }

    // --- TUN setup ---------------------------------------------------------

    /**
     * Establishes the interface and spins up the reader + processor coroutines,
     * then blocks until both finish (the tunnel is torn down).
     */
    private suspend fun startTunnel() {
        if (vpnInterface != null) return

        val builder = Builder()
            .setSession("Kallior Focus Fortress")
            .setMtu(1500)
            .addAddress(tunAddress, 32)
            .addRoute(dnsAddress, 32)
            .addDnsServer(dnsAddress)

        try {
            vpnInterface = builder.establish()
        } catch (e: Exception) {
            Log.e("KalliorVPN", "builder.establish() threw", e)
            return
        }

        val vpn = vpnInterface ?: run {
            Log.e("KalliorVPN", "builder.establish() returned null")
            return
        }

        packetChannel = Channel(Channel.UNLIMITED)
        readJob = scope.launch(Dispatchers.IO) { runReader(vpn) }
        processJob = scope.launch(Dispatchers.IO) { runProcessor(vpn) }
        readJob!!.join()
        // Reader is done (EOF or cancelled): close so the processor drains and
        // returns, allowing startTunnel's join below to complete.
        packetChannel.close()
        processJob!!.join()
    }

    /** Tear down the current tunnel; the recovery loop brings it back up. */
    private fun restartVpn() {
        isShuttingDown = false
        isRestarting = true
        readJob?.cancel()
        processJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
    }

    private fun stopVpn() {
        isShuttingDown = true
        recoveryJob?.cancel()
        readJob?.cancel()
        processJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
        runCatching { packetChannel.close() }
    }

    // --- Read loop (Plan Phase 4.1) ----------------------------------------

    /**
     * Reads packets from the TUN into a single, reused buffer (no per-packet
     * allocation) and hands a copy to the processor via [packetChannel]. Returns
     * immediately on EOF (interface closed) so the recovery loop can react.
     */
    private fun runReader(vpn: ParcelFileDescriptor) {
        val inputStream = FileInputStream(vpn.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)

        while (scope.isActive) {
            val length = try {
                inputStream.read(buffer.array())
            } catch (_: Exception) {
                -1
            }
            if (length < 0) break // EOF: the interface was closed
            if (length == 0) continue

            val packetBytes = ByteArray(length)
            buffer.get(packetBytes, 0, length) // dup the slice we need
            packetChannel.trySend(PacketData(packetBytes))
            buffer.clear()
        }
    }

    /**
     * Consumes packets from [packetChannel] and writes any DNS answer back to
     * the TUN. Runs independently of the reader, so a slow upstream query never
     * stalls packet intake.
     */
    private suspend fun runProcessor(vpn: ParcelFileDescriptor) {
        val outputStream = FileOutputStream(vpn.fileDescriptor)
        for (packet in packetChannel) {
            scope.launch(dnsDispatcher) {
                val response = handlePacket(packet.data, packet.data.size)
                if (response != null) {
                    outputWriteMutex.withLock {
                        try {
                            outputStream.write(response)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    /**
     * Inspects one IPv4/UDP packet. Returns the full IP packet to write back to the
     * TUN (a forged or upstream-sourced DNS answer), or null to drop the packet.
     */
    private fun handlePacket(packet: ByteArray, length: Int): ByteArray? {
        if (length < 20) return null
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null // IPv4 only for V1

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl + 8) return null
        if ((packet[9].toInt() and 0xFF) != 17) return null // UDP only

        val clientIp = packet.copyOfRange(12, 16)
        val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        if (dstPort != 53) return null // only DNS reaches the tunnel

        val dnsPayload = packet.copyOfRange(ihl + 8, length)
        val domain = parseDnsQuestionName(dnsPayload) ?: return null

        VpnDiagnosticsStore.update { it.copy(queriesProcessed = it.queriesProcessed + 1) }

        return if (shouldBlock(domain)) {
            VpnDiagnosticsStore.update { it.copy(queriesBlocked = it.queriesBlocked + 1) }
            handleBlockedDomain(domain)
            wrapDnsAnswer(clientIp, srcPort, forgeBlockedDnsAnswer(dnsPayload))
        } else {
            val upstream = forwardToUpstream(dnsPayload)
            if (upstream != null) wrapDnsAnswer(clientIp, srcPort, upstream) else null
        }
    }

    private fun shouldBlock(domain: String): Boolean {
        if (BlockEventBus.isWhitelisted(domain)) return false
        val normalizedDomain = domain.removeSuffix(".").lowercase()
        return blockedWebsites.contains(normalizedDomain) || 
               blockedWebsites.contains(normalizedDomain.removePrefix("www."))
    }

    private fun handleBlockedDomain(domain: String) {
        val now = System.currentTimeMillis()
        val normalized = domain.removeSuffix(".").lowercase()
        val lastEmitted = lastBlockEmission[normalized] ?: 0L
        if (now - lastEmitted > 2000) {
            lastBlockEmission[normalized] = now
            BlockEventBus.emitBlockEvent(normalized)
        }
    }

    /** Extracts the QNAME from a DNS message's question section. */
    private fun parseDnsQuestionName(dns: ByteArray): String? {
        if (dns.size < 12) return null
        var pos = 12
        val sb = StringBuilder()
        while (true) {
            if (pos >= dns.size) return null
            val len = dns[pos].toInt() and 0xFF
            if (len == 0) break
            if (len > 63) return null // compression not expected in a question
            if (pos + 1 + len > dns.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until len) sb.append((dns[pos + 1 + i].toInt() and 0xFF).toChar())
            pos += 1 + len
        }
        return sb.toString()
    }

    private fun forwardToUpstream(query: ByteArray): ByteArray? {
        val response = upstreamManager.forwardDnsQuery(query)
        if (response == null) {
            VpnDiagnosticsStore.update { it.copy(upstreamErrors = it.upstreamErrors + 1) }
        }
        return response
    }

    /**
     * Returns an NXDOMAIN (RCODE 3) response for all query types. NXDOMAIN is
     * universally understood as "this domain does not exist" and, critically,
     * OS/browser resolvers respect the short TTL in the response (unlike a
     * forged 0.0.0.0 A-record which gets negatively cached for 60+ seconds
     * by Android netd and Chrome).
     */
    private fun forgeBlockedDnsAnswer(query: ByteArray): ByteArray {
        var pos = 12
        while (pos < query.size) {
            val len = query[pos].toInt() and 0xFF
            if (len == 0) { pos += 1; break }
            pos += 1 + len
        }
        val questionEnd = minOf(pos + 4, query.size)

        // NXDOMAIN with no answer records, no authority, no additional.
        val answer = ByteArray(questionEnd)
        query.copyInto(answer, 0, 0, questionEnd)
        val rd = query[2].toInt() and 0x01
        answer[2] = (0x81).toByte()                       // QR=1, Opcode=0, AA=0, TC=0, RD=1
        answer[3] = (0x80 or 0x03 or rd).toByte()          // RA=1, RCODE=3 (NXDOMAIN)
        answer[4] = query[4]; answer[5] = query[5]         // QDCOUNT preserved
        answer[6] = 0; answer[7] = 0                       // ANCOUNT = 0
        answer[8] = 0; answer[9] = 0                       // NSCOUNT = 0
        answer[10] = 0; answer[11] = 0                     // ARCOUNT = 0
        return answer
    }

    /** Wraps a DNS message into a complete IPv4/UDP packet destined back to the querying device. */
    private fun wrapDnsAnswer(clientIp: ByteArray, clientPort: Int, dns: ByteArray): ByteArray {
        val serverIp = InetAddress.getByName(dnsAddress).address
        val totalLen = 20 + 8 + dns.size
        val packet = ByteArray(totalLen)

        // IPv4 header (no options).
        packet[0] = 0x45.toByte()
        packet[1] = 0x00.toByte()
        packet[2] = (totalLen ushr 8).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x00.toByte()
        packet[5] = 0x00.toByte()
        packet[6] = 0x40.toByte() // don't fragment
        packet[7] = 0x00.toByte()
        packet[8] = 0x40.toByte() // TTL 64
        packet[9] = 17.toByte()    // protocol UDP
        System.arraycopy(serverIp, 0, packet, 12, 4) // source = fake DNS server (10.0.0.1)
        System.arraycopy(clientIp, 0, packet, 16, 4) // destination = the actual querying device (10.0.0.2)
        val checksum = computeIpChecksum(packet)
        packet[10] = (checksum ushr 8).toByte()
        packet[11] = (checksum and 0xFF).toByte()

        // UDP header.
        val u = 20
        packet[u] = 0x00.toByte(); packet[u + 1] = 53.toByte()        // source port 53
        packet[u + 2] = (clientPort ushr 8).toByte(); packet[u + 3] = (clientPort and 0xFF).toByte()
        val udpLen = 8 + dns.size
        packet[u + 4] = (udpLen ushr 8).toByte(); packet[u + 5] = (udpLen and 0xFF).toByte()
        packet[u + 6] = 0x00.toByte(); packet[u + 7] = 0x00.toByte() // UDP checksum 0 (optional)

        System.arraycopy(dns, 0, packet, u + 8, dns.size)
        return packet
    }

    private fun computeIpChecksum(header: ByteArray): Int {
        var sum = 0
        var i = 0
        while (i < 20) {
            sum += ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum ushr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum.inv() and 0xFFFF
    }

    // --- Phase 2: network changes & Private DNS ----------------------------

    private fun buildNotification(): Notification {
        val privateDns = VpnDiagnosticsStore.state.value.privateDnsDetected
        val contentText = if (privateDns) {
            "Private DNS detected — disable it in Android settings for blocking to work"
        } else {
            "Website blocking is active"
        }
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, WebsiteBlockerVpnService::class.java).setAction(actionStop),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Stop",
            stopIntent
        ).build()
        return Notification.Builder(this, channelId)
            .setContentTitle("Kallior is protecting your focus")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .addAction(stopAction)
            .build()
    }

    private fun refreshNotification() {
        getSystemService(NotificationManager::class.java)
            ?.notify(notificationId, buildNotification())
    }

    /**
     * Detects Android "Private DNS" (DNS-over-TLS). When active the OS encrypts
     * DNS before it reaches our tunnel, so we cannot intercept it. We surface
     * this via diagnostics + the persistent notification rather than silently
     * failing (Plan Phase 2.2).
     */
    private fun detectPrivateDns() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
        val link: LinkProperties? = cm?.getLinkProperties(cm.activeNetwork)
        val detected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            link?.privateDnsServerName != null
        } else {
            false
        }
        VpnDiagnosticsStore.update { it.copy(privateDnsDetected = detected) }
        refreshNotification()
    }

    /** Re-establish the tunnel when the underlying network (WiFi <-> cellular) changes. */
    private fun registerNetworkCallback() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        currentNetwork = cm.activeNetwork
        upstreamManager.underlyingNetwork = currentNetwork // seed it immediately, before establish()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) // never match our own tunnel
            .build()

        connectivityCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (network != currentNetwork) {
                    currentNetwork = network
                    upstreamManager.underlyingNetwork = network
                    restartVpn()
                }
                detectPrivateDns()
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                if (network == currentNetwork) detectPrivateDns()
            }
        }
        try {
            cm.registerNetworkCallback(request, connectivityCallback!!)
        } catch (_: Exception) {
        }
        detectPrivateDns()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Website Blocker VPN",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        websiteOverlayManager?.destroy()
        websiteOverlayManager = null
        stopVpn()
        val cm = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityCallback?.let { cm?.unregisterNetworkCallback(it) }
        connectivityCallback = null
    }
}

/** A raw IP packet handed from the TUN reader to the DNS processor. */
private data class PacketData(val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PacketData
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}
