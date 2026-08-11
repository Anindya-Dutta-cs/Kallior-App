package kallos.data

import kallos.domain.DailyStats
import kallos.domain.ShadowStats

interface DataRepository {
    suspend fun saveDailyStats(stats: DailyStats)
    suspend fun fetchDailyStats(): List<DailyStats>
    suspend fun saveShadowStats(stats: ShadowStats)
    suspend fun fetchShadowStats(): List<ShadowStats>
}
