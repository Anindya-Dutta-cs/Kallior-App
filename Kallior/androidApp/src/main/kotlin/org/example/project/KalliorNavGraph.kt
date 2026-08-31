package org.example.project

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
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
import org.example.project.ui.AddWebsitesScreen
import org.example.project.ui.AriaAlarmScreen
import org.example.project.ui.BadgesScreen
import org.example.project.ui.FocusFortressScreen
import org.example.project.ui.HomeScreen
import org.example.project.ui.KalliorColors
import org.example.project.ui.ProfileScreen
import org.example.project.ui.ProgressionFeedbackHost
import org.example.project.ui.SettingsScreen
import org.example.project.ui.PlaceholderScreen

/** CompositionLocal to track if the navigation bar should transition to a sheet. */
val LocalNavBarTransition = compositionLocalOf { mutableStateOf(false) }

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
    val isTransitioning = remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalNavBarTransition provides isTransitioning) {
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
                composable("addWebsites") {
                    AddWebsitesScreen(navController = navController)
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
                isTransitioning = isTransitioning.value,
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

            // More Menu Expansion - Panel is transparent, items are styled like the nav bar
            AnimatedVisibility(
                visible = showMoreMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 68.dp) // Gap between bottom tab and nav bar top will be 8.dp (same as spacedBy)
                    .zIndex(2f)
            ) {
                Column(
                    modifier = Modifier
                        .width(180.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

            ProgressionFeedbackHost(gameViewModel)
        }
    }
}

@Composable
fun KalliorNavigationBar(
    currentRoute: String?,
    isTransitioning: Boolean,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit
) {
    val barHeight = 68.dp
    // 22.5% of height: 68 * 0.225 = 15.3dp
    val cornerRadius = barHeight * 0.225f

    val bgColor by animateColorAsState(
        targetValue = if (isTransitioning) Color.Black else Color(0xFF161616),
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "navBarColor"
    )

    val context = LocalContext.current
    LaunchedEffect(isTransitioning) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        if (isTransitioning) {
            activity.enableEdgeToEdge(
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
            )
        } else {
            activity.enableEdgeToEdge(
                navigationBarStyle = SystemBarStyle.dark(0xFF161616.toInt())
            )
        }
    }

    Surface(
        color = bgColor,
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

            // Define tabs explicitly to handle individual staggered animations
            val tabs = listOf(
                Triple("Home", R.drawable.home, null),
                Triple("Zen Silo", R.drawable.focusfortress, null),
                Triple("More", null, Icons.Default.KeyboardArrowUp),
                Triple("AriaAlarm", R.drawable.ariaalarm, null),
                Triple("Settings", R.drawable.settings, null)
            )

            tabs.forEachIndexed { index, (label, iconRes, iconVector) ->
                val distFromCenter = kotlin.math.abs(index - 2)
                // Stagger: 0ms, 150ms, 300ms
                val staggerDelay = (2 - distFromCenter) * 150

                val tabProgress by animateFloatAsState(
                    targetValue = if (isTransitioning) 0f else 1f,
                    animationSpec = tween(
                        durationMillis = 400,
                        delayMillis = if (isTransitioning) staggerDelay else 0,
                        easing = LinearOutSlowInEasing
                    ),
                    label = "navTabProgress_$index"
                )

                NavTab(
                    modifier = tabModifier.graphicsLayer {
                        alpha = tabProgress
                        scaleX = 0.8f + 0.2f * tabProgress
                        scaleY = 0.8f + 0.2f * tabProgress
                        translationY = 24.dp.toPx() * (1f - tabProgress)
                    },
                    label = label,
                    iconRes = iconRes,
                    iconVector = iconVector,
                    isSelected = when(index) {
                        0 -> currentRoute == "home"
                        1 -> currentRoute == "blocker"
                        3 -> currentRoute == "alarm"
                        4 -> currentRoute == "settings"
                        else -> false
                    },
                    onClick = {
                        val route = when(index) {
                            0 -> "home"
                            1 -> "blocker"
                            2 -> "more"
                            3 -> "alarm"
                            4 -> "settings"
                            else -> "home"
                        }
                        onItemClick(route)
                    }
                )
            }
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
    val itemHeight = 48.dp
    val radius = itemHeight * 0.225f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFF161616))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            color = KalliorColors.NormalText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
