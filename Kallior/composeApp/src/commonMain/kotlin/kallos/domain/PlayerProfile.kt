package kallos.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfile(
    var name: String = "",
    var age: Int = 0,
    var email: String = "",
    var unlockedCosmeticIds: Set<String> = emptySet(),
    var lastActiveDate: LocalDate? = null,
    var kp: Int = 0,
    var avatar: Avatar = Avatar(),
)
