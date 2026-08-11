package kallos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kallos.ui.HomeScreen
import kallos.ui.theme.KalliorTheme
import kallos.viewmodel.GameViewModel

@Composable
fun App() {
    val viewModel = remember { GameViewModel() }

    KalliorTheme {
        HomeScreen(viewModel = viewModel)
    }
}
