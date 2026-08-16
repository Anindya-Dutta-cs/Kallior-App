package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kallos.viewmodel.GameViewModel
import org.example.project.ui.AddAppScreen
import org.example.project.ui.AriaAlarmScreen
import org.example.project.ui.BadgesScreen
import org.example.project.ui.FocusFortressScreen
import org.example.project.ui.HomeScreen
import org.example.project.ui.KalliorColors
import org.example.project.ui.ProfileScreen
import org.example.project.ui.SettingsScreen
import org.example.project.ui.PlaceholderScreen

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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showMoreMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    navController = navController,
                    gameViewModel = gameViewModel,
                )
            }
            composable("profile") {
                ProfileScreen(
                    gameViewModel = gameViewModel,
                )
            }
            composable("badges") {
                BadgesScreen(navController = navController)
            }
            composable("alarm") {
                AriaAlarmScreen()
            }
            composable("blocker") {
                FocusFortressScreen(
                    navController = navController,
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
                )
            }
            composable("about") {
                PlaceholderScreen("About Us")
            }
        }

        // Floating Navigation Bar - Overlaying content to reveal background through curves
        KalliorNavigationBar(
            currentRoute = currentRoute,
            modifier = Modifier.align(Alignment.BottomCenter),
            onItemClick = { route ->
                if (route == "more") {
                    showMoreMenu = !showMoreMenu
                } else {
                    showMoreMenu = false
                    if (route == "home") {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    } else {
                        navController.navigate(route)
                    }
                }
            }
        )

        // More Menu Expansion - Sitting slightly above the nav bar
        AnimatedVisibility(
            visible = showMoreMenu,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .zIndex(2f)
        ) {
            Column(
                modifier = Modifier
                    .width(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(KalliorColors.PrimaryLayer)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MoreMenuItem("Profile", onClick = {
                    showMoreMenu = false
                    navController.navigate("profile")
                })
                MoreMenuItem("Badges", onClick = {
                    showMoreMenu = false
                    navController.navigate("badges")
                })
            }
        }
    }
}

@Composable
fun KalliorNavigationBar(
    currentRoute: String?,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit
) {
    val barHeight = 68.dp
    // 22.5% of height: 68 * 0.225 = 15.3dp
    val cornerRadius = barHeight * 0.225f

    Surface(
        color = Color(0xFF161616),
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabModifier = Modifier.weight(1f)
            
            NavTab(
                modifier = tabModifier,
                label = "Home",
                iconRes = R.drawable.home,
                isSelected = currentRoute == "home",
                onClick = { onItemClick("home") }
            )
            NavTab(
                modifier = tabModifier,
                label = "Zen Silo",
                iconRes = R.drawable.focusfortress,
                isSelected = currentRoute == "blocker",
                onClick = { onItemClick("blocker") }
            )
            NavTab(
                modifier = tabModifier,
                label = "More",
                iconVector = Icons.Default.KeyboardArrowUp,
                isSelected = false,
                onClick = { onItemClick("more") }
            )
            NavTab(
                modifier = tabModifier,
                label = "AriaAlarm",
                iconRes = R.drawable.ariaalarm,
                isSelected = currentRoute == "alarm",
                onClick = { onItemClick("alarm") }
            )
            NavTab(
                modifier = tabModifier,
                label = "Settings",
                iconRes = R.drawable.settings,
                isSelected = currentRoute == "settings",
                onClick = { onItemClick("settings") }
            )
        }
    }
}

@Composable
fun NavTab(
    modifier: Modifier = Modifier,
    label: String,
    iconRes: Int? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = if (isSelected) KalliorColors.AccentOrange else KalliorColors.MutedText,
                modifier = Modifier.size(20.dp) // Smaller icons
            )
        } else if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = label,
                tint = KalliorColors.MutedText,
                modifier = Modifier.size(20.dp) // Smaller icons
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp, // Smaller text
            color = if (isSelected) KalliorColors.AccentOrange else KalliorColors.MutedText,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun MoreMenuItem(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KalliorColors.ForegroundCard)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = KalliorColors.NormalText,
            fontSize = 16.sp
        )
    }
}
