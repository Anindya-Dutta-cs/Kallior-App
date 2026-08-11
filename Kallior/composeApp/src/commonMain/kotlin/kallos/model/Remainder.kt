package kallos.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class Remainder(
    val id: String,
    var title: String,
    var time: Instant,
    var description: String? = null,
    val frequencyMinutes: Long = 0L, // 0 = Only once
) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun newId(): String = Uuid.random().toString()

        fun create(
            title: String,
            time: Instant,
            description: String? = null,
            frequencyMinutes: Long = 0L,
        ): Remainder = Remainder(
            id = newId(),
            title = title,
            time = time,
            description = description,
            frequencyMinutes = frequencyMinutes,
        )
    }
}
