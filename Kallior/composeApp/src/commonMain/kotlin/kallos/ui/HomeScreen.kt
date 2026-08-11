package kallos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kallos.ui.theme.KalliorSurface
import kallos.viewmodel.GameViewModel

/**
 * Root composable for the desktop layout.
 *
 * Hosts a permanent [Sidebar] (500 dp) on the left and switches the
 * main content pane between [DashboardScreen], [ShopScreen], and
 * [SettingsScreen] based on sidebar navigation.
 */
@Composable
fun HomeScreen(viewModel: GameViewModel) {
    var currentScreen: Screen by remember { mutableStateOf(Screen.Home) }

    Row(modifier = Modifier.fillMaxSize().background(KalliorSurface)) {
        // ── Permanent sidebar ────────────────────────────────
        Sidebar(
            currentScreen = currentScreen,
            onScreenSelected = { currentScreen = it },
        )

        // ── Main content area ────────────────────────────────
        when (currentScreen) {
            Screen.Home -> DashboardScreen(
                viewModel = viewModel,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Screen.Shop -> ShopScreen(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Screen.Settings -> SettingsScreen(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}
