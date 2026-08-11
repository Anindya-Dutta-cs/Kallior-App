package kallos.domain

data class ShadowGap(
    val resilienceGap: Double,
    val shadowAhead: Boolean,
    val tasksMissed: Int,
    val divergenceMoment: String?,
) {
    companion object {
        fun compute(
            userScores: RadarScores,
            shadowScores: RadarScores,
            tasksMissed: Int,
            divergenceMoment: String?,
        ): ShadowGap {
            val resilienceGap = shadowScores.resilience - userScores.resilience
            val shadowAhead = resilienceGap > 0.0
            return ShadowGap(
                resilienceGap = resilienceGap,
                shadowAhead = shadowAhead,
                tasksMissed = tasksMissed,
                divergenceMoment = divergenceMoment,
            )
        }
    }
}
