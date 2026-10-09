package com.app.kallior

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/** Small, pass-through countdown for a temporarily allowed foreground app. Main-thread only. */
class TimerOverlayManager(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val allowances = mutableMapOf<String, Allowance>()
    private var foregroundPackage: String? = null
    private var openedAt = 0L
    private var overlayView: TextView? = null

    private data class Allowance(val expiry: Long, val midpoint: Long?)

    fun registerAllowances(current: Map<String, Long>) {
        val now = System.currentTimeMillis()
        allowances.keys.retainAll(current.keys)
        current.forEach { (packageName, expiry) ->
            if (allowances[packageName]?.expiry != expiry) {
                allowances[packageName] = Allowance(expiry, inferMidpoint(expiry - now, expiry))
            }
        }
    }

    fun update(packageName: String?, expiry: Long?) {
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        if (packageName != foregroundPackage) {
            foregroundPackage = packageName
            openedAt = elapsed
            hide()
        }
        if (packageName == null || expiry == null || expiry <= now) {
            hide()
            return
        }

        val allowance = allowances[packageName]?.takeIf { it.expiry == expiry }
            ?: Allowance(expiry, inferMidpoint(expiry - now, expiry)).also {
                allowances[packageName] = it
                // An allowance first detected while the app was already foregrounded
                // is still an app opening for the purpose of the opening reminder.
                openedAt = elapsed
            }
        val remaining = expiry - now
        val show = elapsed - openedAt < OPENING_MS ||
            (allowance.midpoint?.let { now >= it && now < it + MIDPOINT_MS } == true) ||
            remaining <= FINAL_MS
        if (!show || !canDrawOverlays()) {
            hide()
            return
        }

        val seconds = (remaining + 999L) / 1000L
        val text = "Time left  %02d:%02d".format(seconds / 60, seconds % 60)
        if (overlayView == null) show(text) else overlayView?.text = text
    }

    // Only infer the original duration immediately after creation. An expiry
    // observed later cannot distinguish a new 5-minute allowance from an older
    // 10- or 15-minute one; showing a false midpoint would be misleading.
    private fun inferMidpoint(remaining: Long, expiry: Long): Long? {
        val duration = listOf(5L, 10L, 15L)
            .map { it * 60_000L }
            .firstOrNull { remaining in (it - 3_000L)..it }
        return duration?.let { expiry - it / 2 }
    }

    private fun show(text: String) {
        val density = appContext.resources.displayMetrics.density
        val background = GradientDrawable().apply {
            setColor(Color.rgb(30, 32, 40))
            cornerRadius = 24 * density
            setStroke((1 * density).toInt().coerceAtLeast(1), Color.rgb(120, 123, 138))
        }
        val view = TextView(appContext).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding((18 * density).toInt(), (10 * density).toInt(),
                (18 * density).toInt(), (10 * density).toInt())
            this.background = background
            elevation = 6 * density
        }
        val statusBarHeight = appContext.resources.getIdentifier("status_bar_height", "dimen", "android")
            .takeIf { it != 0 }?.let { appContext.resources.getDimensionPixelSize(it) } ?: 0
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight + (12 * density).toInt()
            // Android 12+ rejects touches passing through opaque, untrusted overlays.
            alpha = 0.75f
        }
        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            Log.w("TimerOverlay", "Could not show countdown", e)
        }
    }

    fun hide() {
        overlayView?.let { view ->
            overlayView = null
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.w("TimerOverlay", "Could not remove countdown", e)
            }
        }
    }

    fun destroy() {
        hide()
        allowances.clear()
        foregroundPackage = null
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(appContext)

    private companion object {
        const val OPENING_MS = 5_000L
        const val MIDPOINT_MS = 5_000L
        const val FINAL_MS = 15_000L
    }
}
