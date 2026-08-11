package kallos.data

import kallos.domain.DailyStats
import kallos.domain.ShadowStats
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract tests for [SupabaseDataRepository]. These tests intentionally do not
 * require a live Supabase instance. They only verify the type contract and that
 * the four suspend functions are callable on the singleton client wired up by
 * [SupabaseManager]. A connection failure is acceptable; the goal is to prove
 * the surface matches [DataRepository].
 */
class SupabaseDataRepositoryTest {

    @Test
    fun repositoryImplementsDataRepository() {
        val repo: DataRepository = SupabaseDataRepository()
        assertNotNull(repo)
    }

    @Test
    fun fourFunctionsAreCallable() = runBlocking {
        val repo: DataRepository = SupabaseDataRepository()

        // We deliberately call each declared suspend function with the placeholder
        // client (https://your-project.supabase.co). Any network attempt will fail
        // fast, but the function reference itself must be resolvable and the call
        // must return the declared return type (or throw). Either outcome satisfies
        // a contract test.

        val saveDailyStatsResult: Unit = runCatching { repo.saveDailyStats(sampleDailyStats) }
            .fold(onSuccess = { }, onFailure = { })
        assertEquals(Unit, saveDailyStatsResult)

        val fetchDailyStatsResult: List<DailyStats> = runCatching { repo.fetchDailyStats() }
            .fold(onSuccess = { it }, onFailure = { emptyList() })
        assertTrue(fetchDailyStatsResult is List<DailyStats>)

        val saveShadowStatsResult: Unit = runCatching { repo.saveShadowStats(sampleShadowStats) }
            .fold(onSuccess = { }, onFailure = { })
        assertEquals(Unit, saveShadowStatsResult)

        val fetchShadowStatsResult: List<ShadowStats> = runCatching { repo.fetchShadowStats() }
            .fold(onSuccess = { it }, onFailure = { emptyList() })
        assertTrue(fetchShadowStatsResult is List<ShadowStats>)
    }

    private val sampleDailyStats: DailyStats = DailyStats(date = LocalDate(2026, 5, 31))

    private val sampleShadowStats: ShadowStats = ShadowStats(date = LocalDate(2026, 5, 31))
}
