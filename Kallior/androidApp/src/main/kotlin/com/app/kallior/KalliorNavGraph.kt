package com.app.kallior

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kallos.viewmodel.GameViewModel
import com.app.kallior.ui.AddAppScreen
import com.app.kallior.ui.AddWebsitesScreen
import com.app.kallior.ui.AriaAlarmScreen
import com.app.kallior.ui.BadgesScreen
import com.app.kallior.ui.FocusFortressScreen
import com.app.kallior.ui.HomeScreen
import com.app.kallior.ui.UnblockScreen
import com.app.kallior.ui.KalliorColors
import com.app.kallior.ui.ProfileScreen
import com.app.kallior.ui.ProgressionFeedbackHost
import com.app.kallior.ui.SettingsScreen
import com.app.kallior.ui.FieldScoreScreen
import com.app.kallior.ui.PlaceholderScreen
import com.app.kallior.ui.PermissionManagerScreen
import com.app.kallior.ui.AuthenticationGate
import io.github.jan.supabase.auth.auth
import kallos.data.SupabaseManager

/** CompositionLocal to track if the navigation bar should transition to a sheet. */
val LocalNavBarTransition = compositionLocalOf { mutableStateOf(false) }

/** Navigation graph for the Android app shell. */
@Composable
fun KalliorNavGraph(
    navController: NavHostController,
    unblockRequest: UnblockRequest? = null,
    onUnblockRequestHandled: () -> Unit = {},
) {
    AuthenticationGate {
        AuthenticatedKalliorNavGraph(navController, unblockRequest, onUnblockRequestHandled)
    }
}

@Composable
private fun AuthenticatedKalliorNavGraph(
    navController: NavHostController,
    unblockRequest: UnblockRequest?,
    onUnblockRequestHandled: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gameViewModel = remember {
        val persistence = AndroidLocalPersistence(context)
        val repo = kallos.repository.GameRepository(localPersistence = persistence)
        val metricsCollector = kallos.platform.AndroidPlatformMetricsCollector()
        GameViewModel(repository = repo, metricsCollector = metricsCollector)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current
    var unblockPopup by remember { mutableStateOf<UnblockSuccessEvent?>(null) }

    LaunchedEffect(Unit) {
        BlockEventBus.unblockSuccess.collectLatest { event ->
            unblockPopup = event
            delay(3500)
            unblockPopup = null
        }
    }
    LaunchedEffect(unblockPopup, currentRoute) {
        if (unblockPopup != null && currentRoute == "home") {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

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
                composable(
                    route = "unblock?pkg={pkg}&label={label}&domain={domain}&isWebsite={isWebsite}",
                    arguments = listOf(
                        androidx.navigation.navArgument("pkg") { defaultValue = "" },
                        androidx.navigation.navArgument("label") { defaultValue = "App" },
                        androidx.navigation.navArgument("domain") { defaultValue = "" },
                        androidx.navigation.navArgument("isWebsite") {
                            type = androidx.navigation.NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { entry ->
                    val packageName = entry.arguments?.getString("pkg").orEmpty()
                    val domain = entry.arguments?.getString("domain").orEmpty()
                    val isWebsite = entry.arguments?.getBoolean("isWebsite") ?: false
                    val cancel = { (context as? ComponentActivity)?.finish(); Unit }
                    BackHandler(onBack = cancel)
                    UnblockScreen(
                        packageName = packageName,
                        blockedLabel = entry.arguments?.getString("label") ?: "App",
                        domain = domain,
                        isWebsite = isWebsite,
                        onUnblocked = {
                            if (openUnblockedTarget(context, packageName, domain, isWebsite)) {
                                (context as? ComponentActivity)?.finish()
                            } else {
                                // If the target was uninstalled or cannot handle the URL,
                                // the allowance remains valid and Kallior stays usable.
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        },
                        onCancel = cancel,
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
                        navController = navController,
                        onLogout = { scope.launch { SupabaseManager.client.auth.signOut() } },
                    )
                }
                composable("about") {
                    PlaceholderScreen("About Us")
                }
                composable("permissions") {
                    PermissionManagerScreen()
                }
                composable(
                    route = "field_score/{fieldIndex}?isShadow={isShadow}",
                    arguments = listOf(
                        androidx.navigation.navArgument("fieldIndex") {
                            type = androidx.navigation.NavType.IntType
                            defaultValue = 0
                        },
                        androidx.navigation.navArgument("isShadow") {
                            type = androidx.navigation.NavType.BoolType
                            defaultValue = false
                        }
                    ),
                    enterTransition = {
                        scaleIn(
                            initialScale = 0.15f,
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        scaleOut(
                            targetScale = 0.15f,
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(250))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        scaleOut(
                            targetScale = 0.15f,
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(250))
                    }
                ) { backStackEntry ->
                    val fieldIndex = backStackEntry.arguments?.getInt("fieldIndex") ?: 0
                    val isShadow = backStackEntry.arguments?.getBoolean("isShadow") ?: false
                    FieldScoreScreen(
                        navController = navController,
                        gameViewModel = gameViewModel,
                        fieldIndex = fieldIndex,
                        isShadow = isShadow,
                    )
                }
            }

            LaunchedEffect(unblockRequest?.id) {
                val request = unblockRequest ?: return@LaunchedEffect
                navController.navigate(
                    "unblock?pkg=${Uri.encode(request.packageName)}&label=${Uri.encode(request.label)}" +
                        "&domain=${Uri.encode(request.domain)}&isWebsite=${request.isWebsite}"
                ) {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
                onUnblockRequestHandled()
            }

            val isScoreScreen = currentRoute?.startsWith("field_score") == true
            val isUnblockScreen = currentRoute?.startsWith("unblock") == true

            // Floating Navigation Bar - Overlaying content to reveal background through curves
            AnimatedVisibility(
                visible = !isScoreScreen && !isUnblockScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                KalliorNavigationBar(
                    currentRoute = currentRoute,
                    isTransitioning = isTransitioning.value,
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
            }

            // More Menu Expansion - Panel is transparent, items are styled like the nav bar
            AnimatedVisibility(
                visible = showMoreMenu && !isUnblockScreen,
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
            UnblockFeedbackPopup(unblockPopup, currentRoute == "home")
        }
    }
}

private fun openUnblockedTarget(context: Context, packageName: String, domain: String, isWebsite: Boolean): Boolean {
    if (!isWebsite) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            Log.w("UnblockScreen", "Could not open allowed app $packageName", e)
            false
        }
    }

    val host = domain.removePrefix("www.").takeIf { it.isNotBlank() } ?: return false
    val url = Uri.parse("https://$host")
    if (url.host.isNullOrBlank()) return false
    val browserIntent = Intent(Intent.ACTION_VIEW, url)
    val intents = if (packageName.isBlank()) listOf(browserIntent) else
        listOf(Intent(browserIntent).setPackage(packageName), browserIntent)
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.w("UnblockScreen", "Could not open $host with ${intent.`package` ?: "default browser"}", e)
        }
    }
    return false
}

@Composable
private fun UnblockFeedbackPopup(event: UnblockSuccessEvent?, isHome: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = event != null && isHome,
            enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp).fillMaxWidth(0.86f),
        ) {
            Surface(
                shape = RoundedCornerShape(maxWidth * 0.1935f),
                color = KalliorColors.PrimaryLayer,
                border = BorderStroke(1.dp, KalliorColors.AccentOrange.copy(alpha = 0.65f)),
                shadowElevation = 10.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KalliorColors.AccentOrange)
                    Text(
                        text = event?.let { "${it.label} unblocked for ${it.durationMinutes} min" }.orEmpty(),
                        color = KalliorColors.NormalText,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
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
                    isSelected = when (index) {
                        0 -> currentRoute == "home"
                        1 -> currentRoute == "blocker"
                        3 -> currentRoute == "alarm"
                        4 -> currentRoute == "settings"
                        else -> false
                    },
                    onClick = {
                        val route = when (index) {
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
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    scale.animateTo(
                        1.2f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    scale.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                onClick()
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = if (isSelected) KalliorColors.AccentOrange else KalliorColors.InactiveNav,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )
        } else if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = label,
                tint = KalliorColors.InactiveNav,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) KalliorColors.AccentOrange else KalliorColors.InactiveNav,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) KalliorColors.AccentOrange else Color.Transparent)
        )
    }
}

@Composable
fun MoreMenuItem(label: String, onClick: () -> Unit) {
    val itemHeight = 48.dp
    val radius = itemHeight * 0.225f
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFF161616))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
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
