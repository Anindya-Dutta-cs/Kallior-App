package org.example.project

import android.app.PictureInPictureParams
import android.content.res.Configuration
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
import org.example.project.health.AccelerometerStepForegroundService
import org.example.project.ui.ExitOverlay

class KalliorMainActivity : ComponentActivity() {
    private var showPictureInPictureExitAnimation by mutableStateOf(false)

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
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                if (showPictureInPictureExitAnimation) {
                    ExitOverlay()
                } else {
                    KalliorNavGraph(navController)
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        InstalledAppsProvider.trimMemory(level)
    }
}
