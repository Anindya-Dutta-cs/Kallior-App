package kallos.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenTimeData(
    @SerialName("total_minutes") val totalMinutes: Int,
    @SerialName("entertainment_minutes") val entertainmentMinutes: Int,
    @SerialName("ios_discipline_score") val iosDisciplineScore: Int? = null,
    @SerialName("app_usages") val appUsages: List<AppUsageData> = emptyList()
)

@Serializable
data class AppUsageData(
    @SerialName("package_name") val packageName: String,
    @SerialName("app_name") val appName: String,
    @SerialName("time_in_foreground_ms") val timeInForegroundMs: Long,
    @SerialName("last_time_used_ms") val lastTimeUsedMs: Long = 0L,
    @SerialName("icon_res_id") val iconResId: Int? = null
)

@Serializable
data class HealthData(
    @SerialName("value") val value: Double,
    @SerialName("unit") val unit: String
)
