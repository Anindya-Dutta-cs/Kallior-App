package kallos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kallos.model.Task
import kallos.ui.theme.KalliorMuted
import kallos.ui.theme.KalliorSurfaceVariant
import kallos.ui.theme.KalliorTertiary

/**
 * A card displaying a single [Task] with title, category, estimate,
 * status, and a delete button.
 */
@Composable
fun TaskCard(
    task: Task,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KalliorSurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Left: status indicator bar ───────────────────────
        Spacer(
            Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    when (task.status) {
                        kallos.model.TaskStatus.Shelved -> KalliorMuted
                        kallos.model.TaskStatus.Pending -> KalliorTertiary
                        kallos.model.TaskStatus.Completed -> MaterialTheme.colorScheme.secondary
                        kallos.model.TaskStatus.ShadowClaimed -> MaterialTheme.colorScheme.error
                    }
                )
        )

        Spacer(Modifier.width(12.dp))

        // ── Centre: title + metadata ─────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = KalliorTertiary,
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = KalliorMuted,
                )
                Text(
                    text = "${task.estimateMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = KalliorMuted,
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = KalliorMuted,
                )
                Text(
                    text = task.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = KalliorMuted,
                )
            }
        }

        // ── Right: delete button ─────────────────────────────
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.labelLarge,
                color = KalliorMuted,
            )
        }
    }
}
