package com.app.kallior

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.navigation.compose.rememberNavController
import com.app.kallior.health.AccelerometerStepForegroundService
import com.app.kallior.ui.ExitOverlay
import io.github.jan.supabase.auth.handleDeeplinks
import kallos.data.SupabaseManager

data class UnblockRequest(
    val packageName: String,
    val label: String,
    val domain: String,
    val isWebsite: Boolean,
    val id: Long,
)

class KalliorMainActivity : ComponentActivity() {
    private var showPictureInPictureExitAnimation by mutableStateOf(false)
    private var unblockRequest by mutableStateOf<UnblockRequest?>(null)
    private var nextUnblockId = 0L

    private fun handleUnblockIntent(intent: Intent?) {
        if (intent?.getStringExtra("navigate_to") != "unblock") return
        val isWebsite = intent.getBooleanExtra("is_website", false)
        val pkg = intent.getStringExtra("blocked_package").orEmpty()
        val domain = intent.getStringExtra("blocked_domain").orEmpty()
        if (if (isWebsite) domain.isBlank() else pkg.isBlank()) return
        unblockRequest = UnblockRequest(
            packageName = pkg,
            label = intent.getStringExtra("blocked_label")?.takeIf { it.isNotBlank() }
                ?: if (isWebsite) domain else pkg,
            domain = domain,
            isWebsite = isWebsite,
            id = ++nextUnblockId,
        )
    }

    override fun onStart() {
        super.onStart()
        AccelerometerStepForegroundService.startIfNeeded(this)
    }

    override fun onResume() {
        super.onResume()
        showPictureInPictureExitAnimation = false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (isChangingConfigurations || isFinishing) return

        val app = application as KalliorApplication
        if (!app.shouldShowExitAnimationForCurrentExit()) return

        if (Settings.canDrawOverlays(this)) {
            // This starts before the lifecycle watcher fires, improving the chance
            // that the animation is visible during the Home transition.
            app.exitOverlay.showIfAllowed()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode) {
            showPictureInPictureExitAnimation = true
            val enteredPictureInPicture = enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    // Android limits PiP to roughly 2.39:1; 4:1 can be rejected.
                    .setAspectRatio(Rational(239, 100))
                    .build(),
            )
            if (!enteredPictureInPicture) showPictureInPictureExitAnimation = false
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) showPictureInPictureExitAnimation = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(0xFF161616.toInt())
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
        if (SupabaseManager.isConfigured) SupabaseManager.client.handleDeeplinks(intent)
        handleUnblockIntent(intent)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                if (showPictureInPictureExitAnimation) {
                    ExitOverlay()
                } else {
                    KalliorNavGraph(
                        navController = navController,
                        unblockRequest = unblockRequest,
                        onUnblockRequestHandled = { unblockRequest = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (SupabaseManager.isConfigured) SupabaseManager.client.handleDeeplinks(intent)
        handleUnblockIntent(intent)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        InstalledAppsProvider.trimMemory(level)
    }
}
