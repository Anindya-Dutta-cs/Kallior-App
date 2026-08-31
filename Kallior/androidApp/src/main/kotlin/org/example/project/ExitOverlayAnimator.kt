package org.example.project

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory

/** Displays the exit animation over the launcher without accepting any input. */
class ExitOverlayAnimator(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var overlayView: LottieAnimationView? = null
    private var lastShownAt = 0L

    private val removeRunnable = Runnable { removeNow() }
    private val loadTimeoutRunnable = Runnable { removeNow() }

    fun preload() {
        LottieCompositionFactory.fromRawRes(appContext, R.raw.capybara_exit)
    }

    fun showIfAllowed() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { showIfAllowed() }
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastShownAt < COOLDOWN_MS || overlayView != null) return
        if (!isScreenInteractive() || !hasOverlayPermission()) return

        lastShownAt = now
        show()
    }

    fun cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { cancel() }
            return
        }

        handler.removeCallbacks(removeRunnable)
        handler.removeCallbacks(loadTimeoutRunnable)
        removeNow()
    }

    private fun show() {
        val lottieView = LottieAnimationView(appContext).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            rotation = 180f
            setBackgroundColor(Color.TRANSPARENT)
        }

        try {
            windowManager.addView(lottieView, createTopQuarterLayoutParams())
            overlayView = lottieView
            handler.postDelayed(loadTimeoutRunnable, LOAD_TIMEOUT_MS)

            LottieCompositionFactory.fromRawRes(appContext, R.raw.capybara_exit)
                .addListener { composition ->
                    handler.removeCallbacks(loadTimeoutRunnable)
                    if (overlayView !== lottieView) return@addListener

                    lottieView.setComposition(composition)
                    if (composition.duration > 0) {
                        lottieView.speed = composition.duration / DISPLAY_DURATION_MS.toFloat()
                    }
                    lottieView.repeatCount = 0
                    lottieView.playAnimation()
                    handler.postDelayed(removeRunnable, DISPLAY_DURATION_MS + REMOVAL_BUFFER_MS)
                }
                .addFailureListener {
                    handler.removeCallbacks(loadTimeoutRunnable)
                    removeNow()
                }
        } catch (_: Throwable) {
            overlayView = null
        }
    }

    @Suppress("DEPRECATION")
    private fun createTopQuarterLayoutParams(): WindowManager.LayoutParams {
        val heightPx = (getDisplayHeight() * BOTTOM_SCREEN_FRACTION).toInt()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            heightPx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            dimAmount = 0f
            windowAnimations = 0
        }
    }

    private fun getDisplayHeight(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            appContext.resources.displayMetrics.heightPixels
        }

    private fun hasOverlayPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(appContext)

    private fun isScreenInteractive(): Boolean {
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    private fun removeNow() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Throwable) {
            }
        }
        overlayView = null
    }

    private companion object {
        const val BOTTOM_SCREEN_FRACTION = 0.25f
        const val DISPLAY_DURATION_MS = 2_000L
        const val REMOVAL_BUFFER_MS = 100L
        const val LOAD_TIMEOUT_MS = 800L
        const val COOLDOWN_MS = 2_600L
    }
}
