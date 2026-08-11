package org.example.project

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress

/**
 * Handles the "upstream" side of DNS: picking the resolver the OS is actually
 * using and shuttling queries to it.
 *
 * Two improvements over a hardcoded [DatagramSocket] per query (the plan's
 * Phase 3.1):
 *  - The resolver list is pulled from the active network's [LinkProperties] so
 *    we follow carrier/auto DNS instead of always hammering 8.8.8.8.
 *  - Sockets are pooled and reused so we don't allocate a new one (and trigger
 *    GC) for every single query.
 *
 * [protectSocket] must route the socket around the VPN; that can only be done
 * from the [android.net.VpnService] itself, so the service passes its own
 * `protect` in.
 */
class DnsUpstreamManager(
    private val context: Context,
    private val protectSocket: (DatagramSocket) -> Unit,
) {
    /** The real Wi-Fi/cellular network, kept in sync by the service — NEVER
     *  read cm.activeNetwork() directly here once the VPN is up, since the
     *  VPN itself becomes activeNetwork and its DNS config is our own fake
     *  resolver (10.0.0.1), causing every query to be forwarded to itself. */
    var underlyingNetwork: android.net.Network? = null

    private val socketPool = mutableListOf<DatagramSocket>()
    private val poolLock = Any()
    private val maxPoolSize = 16
    private val upstreamTimeoutMs = 3000

    /** Resolvers advertised by the current network, restricted to IPv4, with a sane fallback. */
    fun getUpstreamServers(): List<InetAddress> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = underlyingNetwork ?: cm?.activeNetwork // fallback only if never set yet
        val link: LinkProperties? = cm?.getLinkProperties(network)
        val ipv4Servers = link?.dnsServers?.filterIsInstance<Inet4Address>()
        return if (ipv4Servers.isNullOrEmpty()) {
            listOf(InetAddress.getByName("1.1.1.1"))
        } else {
            ipv4Servers
        }
    }

    /** Sends [payload] to an upstream resolver and returns its raw DNS answer. */
    fun forwardDnsQuery(payload: ByteArray): ByteArray? {
        for (server in getUpstreamServers()) {
            val socket = acquireSocket()
            try {
                protectSocket(socket)
                socket.soTimeout = upstreamTimeoutMs
                socket.send(DatagramPacket(payload, payload.size, server, 53))
                val buf = ByteArray(4096)
                val reply = DatagramPacket(buf, buf.size)
                socket.receive(reply)
                // Success — return the clean socket to the pool.
                releaseSocket(socket)
                return buf.copyOfRange(reply.offset, reply.offset + reply.length)
            } catch (e: Exception) {
                // This server failed/timed out. Close the socket so stale
                // late-arriving packets don't poison future queries.
                Log.e("KalliorDNS", "Upstream $server failed: ${e.javaClass.simpleName}: ${e.message}")
                try { socket.close() } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun acquireSocket(): DatagramSocket = synchronized(poolLock) {
        socketPool.removeFirstOrNull() ?: DatagramSocket()
    }

    private fun releaseSocket(socket: DatagramSocket) = synchronized(poolLock) {
        if (socket.isClosed) return@synchronized
        if (socketPool.size < maxPoolSize) {
            socketPool.add(socket)
        } else {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }
}
