package org.example.project

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live counters the [WebsiteBlockerVpnService] publishes so the UI can show what
 * the tunnel is doing. The service is a long-lived [android.net.VpnService] that
 * runs in the app process, so a process-wide singleton is the simplest bridge
 * between it and Compose.
 */
data class VpnDiagnostics(
    val queriesProcessed: Int = 0,
    val queriesBlocked: Int = 0,
    val upstreamErrors: Int = 0,
    val tunnelRestarts: Int = 0,
    val privateDnsDetected: Boolean = false,
)

object VpnDiagnosticsStore {
    private val _state = MutableStateFlow(VpnDiagnostics())
    val state: StateFlow<VpnDiagnostics> = _state.asStateFlow()

    /** Apply a mutation to the current snapshot. */
    fun update(block: (VpnDiagnostics) -> VpnDiagnostics) {
        _state.value = block(_state.value)
    }

    fun reset() {
        _state.value = VpnDiagnostics()
    }
}
