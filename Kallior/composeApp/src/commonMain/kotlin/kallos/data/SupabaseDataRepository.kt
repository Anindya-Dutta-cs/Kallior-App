package kallos.data


import io.github.jan.supabase.postgrest.from
import kallos.domain.DailyStats
import kallos.domain.ShadowStats

class SupabaseDataRepository : DataRepository {
    override suspend fun saveDailyStats(stats: DailyStats) {
        SupabaseManager.client.from("daily_stats").upsert(stats) {
            onConflict = "date"
        }
    }

    override suspend fun fetchDailyStats(): List<DailyStats> {
        return SupabaseManager.client.from("daily_stats")
            .select()
            .decodeList<DailyStats>()
    }

    override suspend fun saveShadowStats(stats: ShadowStats) {
        SupabaseManager.client.from("shadow_stats").upsert(stats) {
            onConflict = "id"
        }
    }

    override suspend fun fetchShadowStats(): List<ShadowStats> {
        return SupabaseManager.client.from("shadow_stats")
            .select()
            .decodeList<ShadowStats>()
    }
}
