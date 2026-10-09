package com.app.kallior

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView

import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.app.kallior.R

class OverlayManager(
    private val context: Context,
    private val blockerRepository: BlockerRepository,
    private val appBlockerController: AppBlockerControllerImpl,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val foregroundDetector = ForegroundAppDetector(context)
    private var overlayView: View? = null
    private var overlayOwner: ComposeOverlayOwner? = null
    private var currentPackageName: String? = null
    private var dismissWatcherJob: Job? = null
    private val watcherScope = CoroutineScope(Dispatchers.Main + Job())

    fun isOverlayShowing(): Boolean = overlayView != null

    /**
     * Shows the blocking overlay for [packageName]. Returns false if the view could
     * not be added (e.g. SYSTEM_ALERT_WINDOW was revoked) so the caller can degrade
     * gracefully.
     */
    fun showOverlay(packageName: String): Boolean {
        if (overlayView != null) return true
        currentPackageName = packageName

        val appName = foregroundDetector.getAppLabel(packageName)
        val owner = ComposeOverlayOwner().apply {
            onCreate()
        }
        overlayOwner = owner

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            owner.attachToView(this)

            setContent {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides owner,
                    LocalViewModelStoreOwner provides owner,
                    LocalSavedStateRegistryOwner provides owner,
                    LocalOnBackPressedDispatcherOwner provides owner
                ) {
                    MaterialTheme(colorScheme = darkColorScheme()) {
                        BlockingOverlay(
                            blockedTitle = "$appName is Blocked",
                            onOpenKallior = {
                                openUnblockScreen(context, packageName, appName)
                                hideOverlay()
                            },
                            onExit = {
                                hideOverlay()
                                goToHome()
                            },
                            onDismiss = { /* system handles dismiss; nothing to do */ }
                        )
                    }
                }
            }
        }

        // NOTE: Do NOT use FLAG_NOT_FOCUSABLE. Without focus the overlay cannot
        // receive touches, so the Open Kallior / Exit buttons would never fire and
        // blocking would feel like "nothing happens". Keep the overlay touch-modal.
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = composeView
        return try {
            windowManager.addView(overlayView, layoutParams)
            owner.onResume()
            // A blocked app was opened and the block actually engaged — count it as
            // an attempt. (If the overlay was already up we returned true earlier,
            // so this only fires once per genuinely new blocked-app open.)
            BlockerStatsTracker.recordAttempt()
            startDismissWatcher(packageName)
            true
        } catch (e: Exception) {
            val tag = when (e) {
                is WindowManager.BadTokenException -> "bad token"
                is SecurityException -> "security"
                else -> "unknown error"
            }
            Log.e("OverlayManager", "Failed to add overlay ($tag)", e)
            overlayView = null
            overlayOwner?.apply {
                onPause()
                onDestroy()
            }
            overlayOwner = null
            false
        }
    }

    private fun startDismissWatcher(blockedPackage: String) {
        dismissWatcherJob?.cancel()
        dismissWatcherJob = watcherScope.launch {
            while (overlayView != null && isActive) {
                delay(250)
                val fgInfo = foregroundDetector.getForegroundInfo()
                val shouldDismiss = fgInfo == null ||
                    fgInfo.isLauncher ||
                    fgInfo.packageName != blockedPackage ||
                    !foregroundDetector.isScreenInteractive()
                if (shouldDismiss) {
                    hideOverlay()
                    break
                }
            }
        }
    }

    fun hideOverlay() {
        dismissWatcherJob?.cancel()
        dismissWatcherJob = null
        overlayView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            overlayView = null
            overlayOwner?.apply {
                onPause()
                onDestroy()
            }
            overlayOwner = null
            currentPackageName = null
        }
    }

    /**
     * Called when the foreground app changes. If the overlay is up and the user
     * moved to a different app, take it down so it can't linger over the launcher.
     */
    fun onAppSwitched(newPackageName: String) {
        if (currentPackageName != null && newPackageName != currentPackageName) {
            hideOverlay()
        }
    }

    private fun goToHome() {
        goToHome(context)
    }
}

private fun openUnblockScreen(context: Context, packageName: String, label: String, domain: String? = null) {
    context.startActivity(Intent(context, KalliorMainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("navigate_to", "unblock")
        putExtra("blocked_package", packageName)
        putExtra("blocked_label", label)
        putExtra("is_website", domain != null)
        domain?.let { putExtra("blocked_domain", it) }
    })
}

/** Shared utility to navigate to the Android home screen. */
private fun goToHome(context: Context) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

/** Shared utility to redirect the browser to google.com when a website block overlay is exited. */
private fun redirectToGoogle(context: Context, browserPackage: String? = null) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (!browserPackage.isNullOrBlank()) {
            setPackage(browserPackage)
        }
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
            goToHome(context)
        }
    }
}

@Composable
private fun BlockingOverlay(
    blockedTitle: String = "App is Blocked",
    onOpenKallior: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 40.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Kallior",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1.5f))

            // Image with concentric rings
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 160.dp)
                    .drawBehind {
                        val baseWidth = size.width
                        val baseHeight = size.height
                        val steps = 8
                        val stepDelta = 12.dp.toPx()
                        val baseRadius = 32.dp.toPx()
                        val radiusDelta = 8.dp.toPx()

                        for (i in steps downTo 1) {
                            val currentDelta = i * stepDelta
                            val rectWidth = baseWidth + 2 * currentDelta
                            val rectHeight = baseHeight + 2 * currentDelta
                            val x = -currentDelta
                            val y = -currentDelta
                            val radius = baseRadius + i * radiusDelta

                            // Smooth alpha decay for concentric gradient rings
                            val alpha = when (i) {
                                1 -> 0.05f
                                2 -> 0.045f
                                3 -> 0.04f
                                4 -> 0.035f
                                5 -> 0.03f
                                6 -> 0.025f
                                7 -> 0.02f
                                else -> 0.015f
                            }

                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(x, y),
                                size = Size(rectWidth, rectHeight),
                                cornerRadius = CornerRadius(radius, radius),
                                alpha = alpha
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.capybara),
                    contentDescription = "Capybara",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                )
            }

            Spacer(modifier = Modifier.weight(1.5f))

            val titleFontSize = when {
                blockedTitle.length > 24 -> 24.sp
                blockedTitle.length > 16 -> 28.sp
                else -> 34.sp
            }
            Text(
                text = blockedTitle,
                fontSize = titleFontSize,
                lineHeight = (titleFontSize.value * 1.2f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(2f))

            OutlinedButton(
                onClick = onOpenKallior,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Open Kallior",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD9D9D9),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Exit",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))
        }


    }
    }
}

/** Shows a Compose overlay when the VPN blocks a website domain. */
class WebsiteBlockOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val foregroundDetector = ForegroundAppDetector(context)
    private var overlayView: View? = null
    private var overlayOwner: ComposeOverlayOwner? = null
    private var listenerJob: Job? = null
    private var dismissWatcherJob: Job? = null
    private var initializeScope: CoroutineScope? = null
    private var isOverlayShowing = false
    private var currentDomain: String? = null
    private var currentBlockedPackage: String? = null

    fun initialize(scope: CoroutineScope) {
        initializeScope = scope
        listenerJob = scope.launch(Dispatchers.Main.immediate) {
            BlockEventBus.blockEvents.collectLatest { event ->
                if (!isOverlayShowing) {
                    val fgInfo = foregroundDetector.getForegroundInfo()
                    if (fgInfo != null && foregroundDetector.isDomainRelatedToApp(event.domain, fgInfo)) {
                        showOverlay(event.domain, fgInfo)
                    }
                }
            }
        }
    }

    fun destroy() {
        listenerJob?.cancel()
        listenerJob = null
        dismissWatcherJob?.cancel()
        dismissWatcherJob = null
        hideOverlay()
    }

    private fun showOverlay(domain: String, foregroundInfo: ForegroundInfo) {
        if (isOverlayShowing) return
        isOverlayShowing = true
        currentDomain = domain
        currentBlockedPackage = foregroundInfo.packageName

        val websiteName = domain.removePrefix("www.").removeSuffix(".")
        val isStandalone = foregroundInfo.isStandaloneBrowser && !foregroundInfo.isInAppBrowser && !foregroundInfo.isCustomTab
        val title = if (!foregroundInfo.isStandaloneBrowser &&
            !foregroundInfo.isInAppBrowser &&
            !foregroundInfo.isCustomTab &&
            foregroundInfo.appName.isNotBlank()
        ) {
            "${foregroundInfo.appName} is Blocked"
        } else {
            "$websiteName is Blocked"
        }

        val owner = ComposeOverlayOwner().apply { onCreate() }
        overlayOwner = owner

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            owner.attachToView(this)
            setContent {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides owner,
                    LocalViewModelStoreOwner provides owner,
                    LocalSavedStateRegistryOwner provides owner,
                    LocalOnBackPressedDispatcherOwner provides owner,
                ) {
                    MaterialTheme(colorScheme = darkColorScheme()) {
                        BlockingOverlay(
                            blockedTitle = title,
                            onOpenKallior = {
                                openUnblockScreen(context, foregroundInfo.packageName, websiteName, domain)
                                hideOverlay()
                            },
                            onExit = {
                                hideOverlay()
                                if (isStandalone) {
                                    redirectToGoogle(context, foregroundInfo.packageName)
                                } else {
                                    goToHome(context)
                                }
                            },
                            onDismiss = { hideOverlay() }
                        )
                    }
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = composeView
        try {
            windowManager.addView(composeView, layoutParams)
            owner.onResume()
            // A blocked website was opened and its overlay successfully engaged.
            BlockerStatsTracker.recordAttempt()
            startDismissWatcher(foregroundInfo.packageName)
        } catch (e: Exception) {
            Log.e("WebsiteBlockOverlay", "Failed to add overlay", e)
            isOverlayShowing = false
            currentDomain = null
            currentBlockedPackage = null
            overlayView = null
            owner.apply {
                onPause()
                onDestroy()
            }
            overlayOwner = null
        }
    }

    private fun startDismissWatcher(blockedPackage: String) {
        dismissWatcherJob?.cancel()
        dismissWatcherJob = initializeScope?.launch(Dispatchers.Main) {
            while (isOverlayShowing && isActive) {
                delay(250)
                val currentFg = foregroundDetector.getForegroundInfo()
                val shouldDismiss = currentFg == null ||
                    currentFg.isLauncher ||
                    !foregroundDetector.isScreenInteractive() ||
                    currentFg.packageName != blockedPackage
                if (shouldDismiss) {
                    hideOverlay()
                    break
                }
            }
        }
    }

    fun hideOverlay() {
        dismissWatcherJob?.cancel()
        dismissWatcherJob = null
        overlayView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
        }
        overlayView = null
        overlayOwner?.apply {
            onPause()
            onDestroy()
        }
        overlayOwner = null
        isOverlayShowing = false
        currentDomain = null
        currentBlockedPackage = null
    }

}
