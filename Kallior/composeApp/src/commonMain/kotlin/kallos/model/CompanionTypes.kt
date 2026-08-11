package kallos.model

import kallos.domain.ShadowGap

data class CompanionContext(
    val shadowGap: ShadowGap,
    val mostSkippedCategory: Category?,
    val avatarVitality: Int,
    val pendingConfirmedTasks: Int,
    val userResilience: Double,
)

enum class CompanionPersonality {
    Mentor,
    Peer,
}
