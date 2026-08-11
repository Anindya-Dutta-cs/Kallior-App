package kallos.platform

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kallos.domain.AppUsageData
import kallos.domain.HealthData
import kallos.domain.ScreenTimeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.example.project.TimeWastingAppsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PlatformDataFetcher {

    suspend fun getScreenTimeData(): ScreenTimeData = withContext(Dispatchers.IO) {
        try {
            val context = ApplicationContextHolder.applicationContext
                ?: return@withContext ScreenTimeData(0, 0, null)
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as? UsageStatsManager
                ?: return@withContext ScreenTimeData(0, 0, null)
            val pm = context.packageManager
            // The user-selected "Apps that waste your time" list is the source of
            // truth for the entertainment/time-sink metric.
            val timeWastingApps = TimeWastingAppsRepository(context).timeWastingAppsFlow.first()
            // Never count this app's own foreground time towards the user's
            // screen-time total.
            val ownPackageName = context.packageName

            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

            // Pair MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND events to measure the
            // exact per-app foreground time for today. This is far more accurate
            // than summing totalTimeInForeground from the daily usage-stats
            // aggregation, which both over-counts and can include neighbouring days.
            val events = usageStatsManager.queryEvents(startOfDay, now)
            val event = UsageEvents.Event()
            // MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND are deprecated since API 34
            // in favour of the per-activity ACTIVITY_RESUMED / ACTIVITY_PAUSED,
            // which carry the identical integer values (1 and 2). Matching on
            // those values therefore covers both old and new Android. Because the
            // new events are per-activity, we keep a foreground reference count
            // per package and only open/close a session when it crosses 0<->1.
            val foregroundStarts = mutableMapOf<String, Long>()
            val foregroundCounts = mutableMapOf<String, Int>()
            val foregroundDurations = mutableMapOf<String, Long>()
            val lastUsed = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                lastUsed[pkg] = maxOf(lastUsed[pkg] ?: 0L, event.timeStamp)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        val count = (foregroundCounts[pkg] ?: 0) + 1
                        if (count == 1) foregroundStarts[pkg] = event.timeStamp
                        foregroundCounts[pkg] = count
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val count = (foregroundCounts[pkg] ?: 0) - 1
                        if (count <= 0) {
                            foregroundCounts[pkg] = 0
                            val start = foregroundStarts.remove(pkg)
                            if (start != null) {
                                val duration = event.timeStamp - start
                                if (duration > 0L) {
                                    foregroundDurations[pkg] =
                                        (foregroundDurations[pkg] ?: 0L) + duration
                                }
                            }
                        } else {
                            foregroundCounts[pkg] = count
                        }
                    }
                }
            }

            // Any package still counted foreground at query time never emitted a
            // closing event; close its current session out against now.
            for ((pkg, start) in foregroundStarts) {
                val duration = now - start
                if (duration > 0L) {
                    foregroundDurations[pkg] =
                        (foregroundDurations[pkg] ?: 0L) + duration
                }
            }

            var totalMillis = 0L
            var entertainmentMillis = 0L
            val appUsages = mutableListOf<AppUsageData>()

            for ((pkg, foregroundMs) in foregroundDurations) {
                if (foregroundMs <= 0L) continue
                if (pkg == ownPackageName) continue
                // The total deliberately includes every tracked app, including apps
                // selected as time sinks. The sink figure is a subset, never an
                // additive adjustment to this total.
                totalMillis += foregroundMs
                if (pkg in timeWastingApps) {
                    entertainmentMillis += foregroundMs
                }
                appUsages += AppUsageData(
                    packageName = pkg,
                    appName = resolveAppName(pkg, pm),
                    timeInForegroundMs = foregroundMs,
                    lastTimeUsedMs = lastUsed[pkg] ?: 0L,
                    iconResId = null
                )
            }

            ScreenTimeData(
                totalMinutes = (totalMillis / MILLIS_PER_MINUTE).toInt(),
                entertainmentMinutes = (entertainmentMillis / MILLIS_PER_MINUTE).toInt(),
                iosDisciplineScore = null,
                appUsages = appUsages
            )
        } catch (_: Throwable) {
            ScreenTimeData(0, 0, null)
        }
    }

    suspend fun getSleepData(): HealthData = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext HealthData(0.0, UNIT_MINUTES)
        }
        return@withContext try {
            val context = ApplicationContextHolder.applicationContext
                ?: return@withContext HealthData(0.0, UNIT_MINUTES)
            val client = HealthConnectClient.getOrCreate(context)

            // Look from previous noon to current noon so we capture sleep
            // sessions that start before midnight (plan §8.4).
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val start = today.minusDays(1)
                .atTime(12, 0)
                .atZone(zone)
                .toInstant()
            val end = today
                .atTime(12, 0)
                .atZone(zone)
                .toInstant()

            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)

            // Pick the longest session ending in our window (ignores naps
            // shorter than 3 hours, per plan §18.6).
            val primaryRecord = response.records
                .filter { it.endTime.isAfter(start) && it.startTime.isBefore(end) }
                .filter {
                    java.time.Duration.between(it.startTime, it.endTime).toMinutes() >= 180
                }
                .maxByOrNull {
                    java.time.Duration.between(it.startTime, it.endTime).toMillis()
                }

            if (primaryRecord == null) {
                return@withContext HealthData(0.0, UNIT_MINUTES)
            }

            // If the record has sleep stages, sum only actual sleep stages
            // (exclude AWAKE). Otherwise use the full session duration.
            val sleepStages = primaryRecord.stages.filter {
                it.stage != SleepSessionRecord.STAGE_TYPE_AWAKE
            }

            val totalMillis = if (sleepStages.isNotEmpty()) {
                sleepStages.sumOf { stage ->
                    stage.endTime.toEpochMilli() - stage.startTime.toEpochMilli()
                }
            } else {
                primaryRecord.endTime.toEpochMilli() - primaryRecord.startTime.toEpochMilli()
            }

            HealthData(totalMillis.toDouble() / MILLIS_PER_MINUTE, UNIT_MINUTES)
        } catch (_: Throwable) {
            HealthData(0.0, UNIT_MINUTES)
        }
    }

    suspend fun getStepData(): HealthData = withContext(Dispatchers.IO) {
        val context = ApplicationContextHolder.applicationContext
            ?: return@withContext HealthData(0.0, UNIT_COUNT)

        // Health Connect's aggregate API is the authoritative daily total. Unlike
        // ReadRecordsRequest it is not capped by a page of individual records.
        val healthConnectSteps = try {
            val client = HealthConnectClient.getOrCreate(context)
            val (start, end) = todayInstantRange()
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (_: Throwable) {
            0L
        }

        if (healthConnectSteps > 0L) {
            return@withContext HealthData(healthConnectSteps.toDouble(), UNIT_COUNT)
        }

        // Fall back to the device's cumulative hardware step counter when no
        // Health Connect source has reported steps for today.
        val sensorSteps = runCatching {
            org.example.project.health.FallbackStepCounter(context).readStepsForToday()
        }.getOrNull()
        HealthData((sensorSteps ?: 0L).toDouble(), UNIT_COUNT)
    }

    internal fun isEntertainmentPackage(packageName: String, pm: PackageManager): Boolean {
        val lower = packageName.lowercase()
        if (ENTERTAINMENT_TOKENS.any { lower.contains(it) }) return true

        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            val category = info.category
            category == ApplicationInfo.CATEGORY_GAME ||
                category == ApplicationInfo.CATEGORY_VIDEO ||
                category == ApplicationInfo.CATEGORY_SOCIAL
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    private fun resolveAppName(packageName: String, pm: PackageManager): String {
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        } catch (_: Throwable) {
            packageName
        }
    }

    private fun todayInstantRange(): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        return start to now
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val UNIT_MINUTES = "minutes"
        const val UNIT_COUNT = "count"
        val ENTERTAINMENT_TOKENS = setOf(
            "game",
            "video",
            "social",
            "entertainment",
            "youtube",
            "tiktok",
            "instagram",
            "netflix"
        )
    }
}
