package kallos.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
enum class TaskStatus {
    Shelved,
    Pending,
    Completed,
    ShadowClaimed,
}

@Serializable
enum class Category {
    Exercise,
    Work,
    Meditation,
    Diet,
    Other;

    val displayName: String
        get() = when (this) {
            Exercise -> "Exercise"
            Work -> "Work"
            Meditation -> "Meditation"
            Diet -> "Diet"
            Other -> "Other"
        }
}

@Serializable
data class Task(
    val id: String,
    val title: String,
    val category: Category,
    val description: String = "",
    val status: TaskStatus,
    val estimateMinutes: Int,
    val shelvedAt: Instant? = null,
    val activatedAt: Instant? = null,
    val completedAt: Instant? = null,
) {
    companion object {

        @OptIn(ExperimentalUuidApi::class)
        fun newId(): String = Uuid.random().toString()

        /**
         * A task starts [Shelved]. Its [title] is the chosen [category] unless the
         * user picked [Category.Other] and supplied a [customTitle]. [description]
         * is capped at [MAX_DESCRIPTION_LENGTH] characters.
         */
        fun create(
            category: Category,
            description: String = "",
            estimateMinutes: Int = GameConstants.DEFAULT_ESTIMATE_MINUTES,
            customTitle: String? = null,
            shelvedAt: Instant? = null,
        ): Task {
            val title = when {
                category == Category.Other && !customTitle.isNullOrBlank() -> customTitle.trim()
                else -> category.displayName
            }
            return Task(
                id = newId(),
                title = title,
                category = category,
                description = description,
                status = TaskStatus.Shelved,
                estimateMinutes = estimateMinutes.coerceAtLeast(1),
                shelvedAt = shelvedAt,
            )
        }
    }
}
