package kallos.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class IosPlatformMetricsCollector : PlatformMetricsCollector {
    override fun collect(): PlatformMetrics = PlatformMetrics()
    override val updates: Flow<Unit> = emptyFlow()
}
