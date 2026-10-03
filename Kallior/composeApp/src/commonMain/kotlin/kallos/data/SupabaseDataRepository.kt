package kallos.data

import io.github.jan.supabase.encodeToJsonElement
import io.github.jan.supabase.postgrest.from
import kallos.domain.DailyStats
import kallos.domain.ShadowStats
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock

class SupabaseDataRepository : DataRepository {
    private var cachedUserId: String? = null
    private var dailyStatsCache: List<DailyStats>? = null
    private var dailyStatsCacheTimestamp: Long = 0L
    private var shadowStatsCache: List<ShadowStats>? = null
    private var shadowStatsCacheTimestamp: Long = 0L

    private val cacheTtlMs: Long = 5 * 60 * 1000L

    private fun isCacheValid(timestamp: Long): Boolean =
        (Clock.System.now().toEpochMilliseconds() - timestamp) < cacheTtlMs

    private fun clearCaches() {
        dailyStatsCache = null
        shadowStatsCache = null
        dailyStatsCacheTimestamp = 0L
        shadowStatsCacheTimestamp = 0L
    }

    /** Never reuse cached records after the authenticated account changes. */
    private fun requireUserId(): String {
        val userId = SupabaseManager.requireCurrentUserId()
        if (cachedUserId != userId) {
            clearCaches()
            cachedUserId = userId
        }
        return userId
    }

    private fun userScopedPayload(value: DailyStats, userId: String): JsonObject =
        JsonObject(
            SupabaseManager.client.defaultSerializer.encodeToJsonElement(value).jsonObject +
                    ("user_id" to JsonPrimitive(userId)),
        )

    private fun userScopedPayload(value: ShadowStats, userId: String): JsonObject =
        JsonObject(
            SupabaseManager.client.defaultSerializer.encodeToJsonElement(value).jsonObject +
                    ("user_id" to JsonPrimitive(userId)),
        )

    override suspend fun saveDailyStats(stats: DailyStats) {
        val userId = requireUserId()
        SupabaseManager.client.from("daily_stats").upsert(userScopedPayload(stats, userId)) {
            onConflict = "user_id,date"
        }
        dailyStatsCache = null
    }

    override suspend fun fetchDailyStats(): List<DailyStats> {
        val userId = requireUserId()
        dailyStatsCache?.takeIf { isCacheValid(dailyStatsCacheTimestamp) }?.let { return it }

        return SupabaseManager.client.from("daily_stats")
            .select { filter { eq("user_id", userId) } }
            .decodeList<DailyStats>()
            .also {
                dailyStatsCache = it
                dailyStatsCacheTimestamp = Clock.System.now().toEpochMilliseconds()
            }
    }

    override suspend fun saveShadowStats(stats: ShadowStats) {
        val userId = requireUserId()
        SupabaseManager.client.from("shadow_stats").upsert(userScopedPayload(stats, userId)) {
            onConflict = "user_id,date"
        }
        shadowStatsCache = null
    }

    override suspend fun fetchShadowStats(): List<ShadowStats> {
        val userId = requireUserId()
        shadowStatsCache?.takeIf { isCacheValid(shadowStatsCacheTimestamp) }?.let { return it }

        return SupabaseManager.client.from("shadow_stats")
            .select { filter { eq("user_id", userId) } }
            .decodeList<ShadowStats>()
            .also {
                shadowStatsCache = it
                shadowStatsCacheTimestamp = Clock.System.now().toEpochMilliseconds()
            }
    }
}
