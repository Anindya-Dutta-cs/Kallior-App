package org.example.project

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kallos.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import org.example.project.ui.AddAppScreen
import org.example.project.ui.AriaAlarmScreen
import org.example.project.ui.BadgesScreen
import org.example.project.ui.FocusFortressScreen
import org.example.project.ui.HomeScreen
import org.example.project.ui.SettingsScreen
import org.example.project.ui.PlaceholderScreen
import org.example.project.ui.SidebarContent

/** Navigation graph for the Android app shell. */
@Composable
fun KalliorNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val gameViewModel = remember {
        val persistence = AndroidLocalPersistence(context)
        val repo = kallos.repository.GameRepository(localPersistence = persistence)
        val metricsCollector = kallos.platform.AndroidPlatformMetricsCollector()
        GameViewModel(repository = repo, metricsCollector = metricsCollector)
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            SidebarContent(
                currentRoute = currentRoute,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    if (route == "home") {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    } else {
                        navController.navigate(route)
                    }
                }
            )
        }
    ) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    navController = navController,
                    gameViewModel = gameViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable("badges") {
                BadgesScreen(navController = navController)
            }
            composable("alarm") {
                AriaAlarmScreen(onMenuClick = { scope.launch { drawerState.open() } })
            }
            composable("blocker") {
                FocusFortressScreen(
                    navController = navController,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(
                route = "addApp?mode={mode}",
                arguments = listOf(androidx.navigation.navArgument("mode") { defaultValue = "BLOCKER" })
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "BLOCKER"
                AddAppScreen(navController = navController, mode = mode)
            }
            composable("settings") {
                SettingsScreen(
                    gameViewModel = gameViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable("about") {
                PlaceholderScreen("About Us", onMenuClick = { scope.launch { drawerState.open() } })
            }
        }
    }
}
