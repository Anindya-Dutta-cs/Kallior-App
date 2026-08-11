package kallos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kallos.ui.theme.KalliorBackground
import kallos.ui.theme.KalliorMuted
import kallos.ui.theme.KalliorOnSurface
import kallos.ui.theme.KalliorSurface
import kallos.ui.theme.KalliorSurfaceVariant
import kallos.ui.theme.KalliorTertiary

/**
 * Permanent left-hand sidebar (500 dp).
 *
 * Contains the brand name, a welcome card, navigation items,
 * and a user chip at the bottom.
 */
@Composable
fun Sidebar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screens = listOf(Screen.Home, Screen.Shop, Screen.Settings)

    Column(
        modifier = modifier
            .width(500.dp)
            .fillMaxHeight()
            .background(KalliorBackground)
            .padding(24.dp),
    ) {
        // ── Brand name ───────────────────────────────────────
        Text(
            text = "Kallior",
            style = MaterialTheme.typography.displaySmall,
            color = KalliorTertiary,
        )

        Spacer(Modifier.height(28.dp))

        // ── Welcome card ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(KalliorSurfaceVariant)
                .padding(20.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(KalliorMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    // Small dot highlight (decorative)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(KalliorOnSurface)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Welcome back User!",
                    style = MaterialTheme.typography.titleSmall,
                    color = KalliorOnSurface,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Navigation items ─────────────────────────────────
        screens.forEach { screen ->
            val isSelected = screen == currentScreen
            Text(
                text = screen.label,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isSelected) KalliorOnSurface else KalliorMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onScreenSelected(screen) }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        // ── User chip ────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(KalliorSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            var toggled by remember { mutableStateOf(true) }
            Switch(
                checked = toggled,
                onCheckedChange = { toggled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = KalliorOnSurface,
                    checkedTrackColor = KalliorTertiary,
                    uncheckedThumbColor = KalliorMuted,
                    uncheckedTrackColor = KalliorSurfaceVariant,
                ),
                modifier = Modifier.size(width = 42.dp, height = 24.dp),
            )
            Text(
                text = "User",
                style = MaterialTheme.typography.titleSmall,
                color = KalliorTertiary,
            )
        }
    }
}
