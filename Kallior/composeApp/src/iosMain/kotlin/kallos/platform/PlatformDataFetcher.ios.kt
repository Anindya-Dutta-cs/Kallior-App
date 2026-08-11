package kallos.platform

import kallos.domain.HealthData
import kallos.domain.ScreenTimeData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.HealthKit.HKCategorySample
import platform.HealthKit.HKCategoryType
import platform.HealthKit.HKCategoryTypeIdentifierSleepAnalysis
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleep
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleepCore
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleepDeep
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleepREM
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleepUnspecified
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectQueryNoLimit
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifierStepCount
import platform.HealthKit.HKQuery
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKStatisticsOptionCumulativeSum
import platform.HealthKit.HKStatisticsQuery
import platform.HealthKit.HKUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
class PlatformDataFetcher {

    suspend fun getScreenTimeData(): ScreenTimeData {
        val exact = IosScreenTimeState.exactEndOfDayMinutes
        val selectedMinutes = exact ?: IosScreenTimeState.thresholdEstimateMinutes
        val score = calculateIosDisciplineScore(selectedMinutes)
        return ScreenTimeData(
            totalMinutes = selectedMinutes,
            entertainmentMinutes = selectedMinutes,
            iosDisciplineScore = score
        )
    }

    fun applyThresholdCrossed(thresholdMinutes: Int) {
        IosScreenTimeState.thresholdEstimateMinutes = thresholdMinutes
    }

    fun applyExactEndOfDayMinutes(minutes: Int) {
        IosScreenTimeState.exactEndOfDayMinutes = minutes
    }

    suspend fun getSleepData(): HealthData {
        if (!HKHealthStore.isHealthDataAvailable()) {
            return HealthData(0.0, "minutes")
        }
        val healthStore = HKHealthStore()
        val sleepType = HKCategoryType.categoryTypeForIdentifier(
            HKCategoryTypeIdentifierSleepAnalysis
        ) ?: return HealthData(0.0, "minutes")

        val (startOfDay, endOfDay) = todayRange()
        val predicate = HKQuery.predicateForSamplesWithStartDate(
            startDate = startOfDay,
            endDate = endOfDay,
            options = 0u
        )

        val samples = suspendCoroutine<List<HKCategorySample>> { cont ->
            val query = HKSampleQuery(
                sampleType = sleepType,
                predicate = predicate,
                limit = HKObjectQueryNoLimit,
                sortDescriptors = null
            ) { _, results, error ->
                if (error != null) {
                    cont.resume(emptyList())
                } else {
                    @Suppress("UNCHECKED_CAST")
                    cont.resume((results as? List<HKCategorySample>) ?: emptyList())
                }
            }
            healthStore.executeQuery(query)
        }

        var totalSeconds = 0.0
        for (sample in samples) {
            val value = sample.value
            if (!isAsleepValue(value)) continue
            totalSeconds += sample.endDate.timeIntervalSinceDate(sample.startDate)
        }
        val totalMinutes = (totalSeconds / 60.0).toInt()
        return HealthData(totalMinutes.toDouble(), "minutes")
    }

    suspend fun getStepData(): HealthData {
        if (!HKHealthStore.isHealthDataAvailable()) {
            return HealthData(0.0, "count")
        }
        val healthStore = HKHealthStore()
        val stepType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierStepCount
        ) ?: return HealthData(0.0, "count")

        val (startOfDay, endOfDay) = todayRange()
        val predicate = HKQuery.predicateForSamplesWithStartDate(
            startDate = startOfDay,
            endDate = endOfDay,
            options = 0u
        )

        val totalCount = suspendCoroutine<Double> { cont ->
            val query = HKStatisticsQuery(
                quantityType = stepType,
                quantitySamplePredicate = predicate,
                options = HKStatisticsOptionCumulativeSum
            ) { _, result, error ->
                if (error != null || result == null) {
                    cont.resume(0.0)
                } else {
                    val quantity = result.sumQuantity()
                    if (quantity == null) {
                        cont.resume(0.0)
                    } else {
                        cont.resume(quantity.doubleValueForUnit(HKUnit.countUnit()))
                    }
                }
            }
            healthStore.executeQuery(query)
        }

        return HealthData(totalCount, "count")
    }

    private fun todayRange(): Pair<NSDate, NSDate> {
        val calendar = NSCalendar.currentCalendar
        val now = NSDate()
        val startOfDay = calendar.startOfDayForDate(now)
        return startOfDay to now
    }

    private fun isAsleepValue(value: Long): Boolean {
        return value == HKCategoryValueSleepAnalysisAsleep ||
            value == HKCategoryValueSleepAnalysisAsleepUnspecified ||
            value == HKCategoryValueSleepAnalysisAsleepCore ||
            value == HKCategoryValueSleepAnalysisAsleepDeep ||
            value == HKCategoryValueSleepAnalysisAsleepREM
    }
}
