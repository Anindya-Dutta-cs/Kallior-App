package com.app.kallior

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager

data class ForegroundInfo(
    val packageName: String,
    val className: String?,
    val appName: String,
    val isLauncher: Boolean,
    val isStandaloneBrowser: Boolean,
    val isCustomTab: Boolean,
    val isInAppBrowser: Boolean,
)

/**
 * Detects the currently active foreground app and classifies it
 * (launcher, standalone browser, custom tab, in-app browser, or regular app).
 */
class ForegroundAppDetector(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private var cachedLauncherPackages: Set<String> = emptySet()
    private var lastLauncherCheckTime: Long = 0

    private var cachedBrowserPackages: Set<String> = emptySet()
    private var lastBrowserCheckTime: Long = 0


    private val knownBrowserPackages = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.gx",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.kiwibrowser.browser",
        "com.android.browser",
        "com.huawei.browser",
        "com.mi.globalbrowser",
        "com.ucmobile.intl"
    )

    fun isScreenInteractive(): Boolean {
        return powerManager?.isInteractive ?: true
    }

    /**
     * Determines the current foreground app using UsageStatsManager.
     */
    fun getForegroundInfo(): ForegroundInfo? {
        if (!isScreenInteractive()) return null
        val usm = usageStatsManager ?: return null

        val endTime = System.currentTimeMillis()
        var latestPackage: String? = null
        var latestClass: String? = null

        val event = UsageEvents.Event()
        // Check recent 15-second window
        val shortEvents = usm.queryEvents(endTime - 15_000L, endTime)
        while (shortEvents.hasNextEvent()) {
            shortEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestPackage = event.packageName
                latestClass = event.className
            }
        }

        // If no resume event in 15 seconds (e.g. user was idle inside the app), check last 10 minutes
        if (latestPackage == null) {
            val longEvents = usm.queryEvents(endTime - 600_000L, endTime)
            while (longEvents.hasNextEvent()) {
                longEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    latestPackage = event.packageName
                    latestClass = event.className
                }
            }
        }

        val pkg = latestPackage ?: return null
        val cls = latestClass

        val isLauncher = isLauncher(pkg)
        val isCustomTab = isCustomTab(cls)
        val isBrowserPkg = isBrowser(pkg)
        val isStandalone = isBrowserPkg && !isCustomTab
        val isInApp = isCustomTab || (!isBrowserPkg && isLikelyInAppBrowser(cls))

        val appName = getAppLabel(pkg)

        return ForegroundInfo(
            packageName = pkg,
            className = cls,
            appName = appName,
            isLauncher = isLauncher,
            isStandaloneBrowser = isStandalone,
            isCustomTab = isCustomTab,
            isInAppBrowser = isInApp,
        )
    }

    fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    fun isLauncher(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastLauncherCheckTime > 60_000L || cachedLauncherPackages.isEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                cachedLauncherPackages = resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
                lastLauncherCheckTime = now
            } catch (_: Exception) {
            }
        }
        return cachedLauncherPackages.contains(packageName)
    }

    fun isBrowser(packageName: String): Boolean {
        if (knownBrowserPackages.contains(packageName)) return true
        val now = System.currentTimeMillis()
        if (now - lastBrowserCheckTime > 60_000L || cachedBrowserPackages.isEmpty()) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                val resolveInfos = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL)
                val browsers = mutableSetOf<String>()
                for (info in resolveInfos) {
                    val pkg = info.activityInfo?.packageName ?: continue
                    val filter = info.filter
                    if (filter != null && filter.countDataAuthorities() == 0) {
                        browsers.add(pkg)
                    }
                }
                cachedBrowserPackages = browsers
                lastBrowserCheckTime = now
            } catch (_: Exception) {
            }
        }
        return cachedBrowserPackages.contains(packageName)
    }

    private fun isCustomTab(className: String?): Boolean {
        if (className == null) return false
        return className.contains("customtab", ignoreCase = true)
    }

    private fun isLikelyInAppBrowser(className: String?): Boolean {
        if (className == null) return false
        val lower = className.lowercase()
        return lower.contains("webview") ||
            lower.contains("inappbrowser") ||
            lower.contains("browseractivity") ||
            lower.contains("webactivity")
    }

    /**
     * Checks if the currently blocked domain is relevant to the foreground app.
     * Prevents background processes (e.g. Reddit app syncing in background) from
     * triggering overlays while the user is on the home screen or using another app.
     */
    fun isDomainRelatedToApp(domain: String, foregroundInfo: ForegroundInfo): Boolean {
        return isDomainRelevant(
            domain = domain,
            foregroundPackage = foregroundInfo.packageName,
            appName = foregroundInfo.appName,
            isLauncher = foregroundInfo.isLauncher,
            isStandaloneBrowser = foregroundInfo.isStandaloneBrowser,
            isCustomTab = foregroundInfo.isCustomTab,
            isInAppBrowser = foregroundInfo.isInAppBrowser,
            hostAppPackage = context.packageName
        )
    }

    companion object {
        private val commonTlds = setOf(
            "com", "org", "net", "edu", "gov", "mil", "io", "app", "co", "uk",
            "ca", "de", "fr", "jp", "au", "us", "info", "biz", "me", "tv", "ai"
        )

        fun isDomainRelevant(
            domain: String,
            foregroundPackage: String,
            appName: String,
            isLauncher: Boolean,
            isStandaloneBrowser: Boolean,
            isCustomTab: Boolean,
            isInAppBrowser: Boolean,
            hostAppPackage: String,
        ): Boolean {
            // Never trigger on launcher (home screen)
            if (isLauncher) return false

            // Never trigger inside host app itself
            if (foregroundPackage == hostAppPackage) return false

            // Standalone browsers, custom tabs, or in-app web views can navigate to ANY website
            if (isStandaloneBrowser || isCustomTab || isInAppBrowser) {
                return true
            }

            // For regular apps, check if the app's package or name corresponds to the blocked domain
            val cleanDomain = domain.lowercase().removePrefix("www.")
            val domainParts = cleanDomain.split('.')
            val domainKeywords = domainParts.filter { it.length >= 3 && it !in commonTlds }

            val pkgLower = foregroundPackage.lowercase()
            val appNameLower = appName.lowercase()

            for (keyword in domainKeywords) {
                if (pkgLower.contains(keyword) || appNameLower.contains(keyword)) {
                    return true
                }
            }

            // Special mappings for well-known aliases (e.g. x.com / twitter)
            if (cleanDomain == "x.com" && (pkgLower.contains("twitter") || appNameLower.contains("twitter"))) {
                return true
            }

            return false
        }
    }
}
