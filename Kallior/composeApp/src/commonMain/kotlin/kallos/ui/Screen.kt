package kallos.ui

/**
 * Represents the top-level navigation destinations in the sidebar.
 */
sealed class Screen(val label: String) {
    data object Home : Screen("Home")
    data object Shop : Screen("Shop")
    data object Settings : Screen("Settings")
}
