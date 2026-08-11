package kallos.domain

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ShadowProfile(
    var displayName: String = "Shadow",
    var tasksClaimedCount: Int = 0,
    var divergenceMomentDescription: String? = null,
    var currentShadowTaskId: String? = null,
    var currentShadowTaskStartedAt: Instant? = null,
    var lastShadowTaskTitle: String? = null,
    var shadowCompletedTaskIds: List<String> = emptyList(),
)
