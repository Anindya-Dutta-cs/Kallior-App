package kallos.data


import io.github.jan.supabase.postgrest.from
import kallos.domain.DailyStats
import kallos.domain.ShadowStats
import kotlin.time.Clock

class SupabaseDataRepository : DataRepository {

    // ── In-memory cache-aside with TTL ──────────────────────────────
    private var dailyStatsCache: List<DailyStats>? = null
    private var dailyStatsCacheTimestamp: Long = 0L

    private var shadowStatsCache: List<ShadowStats>? = null
    private var shadowStatsCacheTimestamp: Long = 0L

    /** Cache entries older than this are considered stale. */
    private val cacheTtlMs: Long = 5 * 60 * 1000L // 5 minutes

    private fun isCacheValid(timestamp: Long): Boolean =
        (Clock.System.now().toEpochMilliseconds() - timestamp) < cacheTtlMs
    // ────────────────────────────────────────────────────────────────

    override suspend fun saveDailyStats(stats: DailyStats) {
        SupabaseManager.client.from("daily_stats").upsert(stats) {
            onConflict = "date"
        }
        // Invalidate cache on write so next fetch gets fresh data
        dailyStatsCache = null
    }

    override suspend fun fetchDailyStats(): List<DailyStats> {
        dailyStatsCache?.takeIf { isCacheValid(dailyStatsCacheTimestamp) }?.let { return it }

        return SupabaseManager.client.from("daily_stats")
            .select()
            .decodeList<DailyStats>()
            .also {
                dailyStatsCache = it
                dailyStatsCacheTimestamp = Clock.System.now().toEpochMilliseconds()
            }
    }

    override suspend fun saveShadowStats(stats: ShadowStats) {
        SupabaseManager.client.from("shadow_stats").upsert(stats) {
            onConflict = "id"
        }
        // Invalidate cache on write so next fetch gets fresh data
        shadowStatsCache = null
    }

    override suspend fun fetchShadowStats(): List<ShadowStats> {
        shadowStatsCache?.takeIf { isCacheValid(shadowStatsCacheTimestamp) }?.let { return it }

        return SupabaseManager.client.from("shadow_stats")
            .select()
            .decodeList<ShadowStats>()
            .also {
                shadowStatsCache = it
                shadowStatsCacheTimestamp = Clock.System.now().toEpochMilliseconds()
            }
    }
}
