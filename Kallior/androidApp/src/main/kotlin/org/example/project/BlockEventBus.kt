package org.example.project

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BlockedDomainEvent(val domain: String, val timestamp: Long)

/** Event bus for block events and immediate whitelisting sync between UI and VPN. */
object BlockEventBus {
    private val _blockEvents = MutableSharedFlow<BlockedDomainEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val blockEvents: SharedFlow<BlockedDomainEvent> = _blockEvents.asSharedFlow()

    private val _whitelistState = MutableStateFlow<Map<String, Long>>(emptyMap())
    val whitelistState: StateFlow<Map<String, Long>> = _whitelistState.asStateFlow()

    fun emitBlockEvent(domain: String) {
        _blockEvents.tryEmit(BlockedDomainEvent(domain, System.currentTimeMillis()))
    }

    fun updateWhitelist(domain: String, expiryTimestamp: Long) {
        val normalized = normalizeDomain(domain)
        // Store both the www. and non-www. variants so the whitelist matches
        // regardless of which form the DNS query arrives in.
        val withoutWww = normalized.removePrefix("www.")
        val withWww = "www.$withoutWww"
        _whitelistState.update { current ->
            current + (normalized to expiryTimestamp) +
                    (withoutWww to expiryTimestamp) +
                    (withWww to expiryTimestamp)
        }
    }

    fun isWhitelisted(domain: String): Boolean {
        val now = System.currentTimeMillis()
        var d = normalizeDomain(domain)
        val whitelist = _whitelistState.value

        while (d.contains('.')) {
            if ((whitelist[d] ?: 0L) > now) return true
            d = d.substringAfter('.', "")
            if (d.isEmpty()) break
        }
        return (whitelist[d] ?: 0L) > now
    }

    private fun normalizeDomain(domain: String): String =
        domain.removeSuffix(".").lowercase()
}
