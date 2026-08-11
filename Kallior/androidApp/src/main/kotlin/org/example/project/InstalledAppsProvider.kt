package org.example.project

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import javax.inject.Inject

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val category: String,
)

/**
 * Queries the device for user-launchable apps, excluding the host app and the
 * default launcher, and groups them into UI-friendly categories.
 *
 * Icons are intentionally NOT loaded during the initial fetch (see [getApplicationIcon],
 * which is called lazily per selected category to keep scrolling smooth).
 */
class InstalledAppsProvider @Inject constructor(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val iconCache = mutableMapOf<String, Drawable>()

    private val cachedApps: List<InstalledAppInfo> by lazy { computeInstalledApps() }

    fun getInstalledApps(): List<InstalledAppInfo> = cachedApps

    /** Distinct categories present on the device, in a stable preferred order. */
    fun getCategories(): List<String> {
        val present = cachedApps.map { it.category }.toSet()
        return CATEGORY_ORDER.filter { it in present } +
            (present - CATEGORY_ORDER.toSet()).sorted()
    }

    /** Lazily loads and caches the launcher icon for a package. */
    fun getApplicationIcon(packageName: String): Drawable? {
        iconCache[packageName]?.let { return it }
        return try {
            pm.getApplicationIcon(packageName).also { iconCache[packageName] = it }
        } catch (_: Exception) {
            null
        }
    }

    private fun computeInstalledApps(): List<InstalledAppInfo> {
        val ownPackage = context.packageName
        val defaultLauncher = getDefaultLauncherPackage()

        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                pm.getLaunchIntentForPackage(appInfo.packageName) != null &&
                    appInfo.packageName != ownPackage &&
                    appInfo.packageName != defaultLauncher
            }
            .mapNotNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString()
                if (label.isBlank()) return@mapNotNull null
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    appName = label,
                    category = resolveAppCategory(appInfo, pm),
                )
            }
    }

    private fun getDefaultLauncherPackage(): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        return try {
            pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves a UI category for an app. Uses the official Android category first
     * (API 26+), then falls back to keyword matching on the package name + label
     * for apps that don't declare a category (most apps return CATEGORY_UNDEFINED).
     */
    private fun resolveAppCategory(appInfo: ApplicationInfo, pm: PackageManager): String {
        val official = mapCategory(appInfo.category)
        if (official != "Other") return official

        val pkg = appInfo.packageName.lowercase()
        val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault("").lowercase()
        val combined = "$pkg $label"

        if (listOf("instagram", "facebook", "twitter", "tiktok", "snapchat", "reddit", "linkedin", "discord", "x.com").any { combined.contains(it) }) return "Social"
        if (listOf("netflix", "spotify", "youtube", "twitch", "hulu", "primevideo", "disney", "music").any { combined.contains(it) }) return "Entertainment"
        if (listOf("game", "supercell", "unity", "unreal", "roblox", "minecraft", "chess").any { combined.contains(it) }) return "Games"
        if (listOf("amazon", "ebay", "shop", "aliexpress", "walmart").any { combined.contains(it) }) return "Shopping"

        return "Other"
    }

    private fun mapCategory(category: Int): String = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "Games"
        ApplicationInfo.CATEGORY_VIDEO -> "Entertainment"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_AUDIO -> "Music & Audio"
        ApplicationInfo.CATEGORY_IMAGE -> "Photography"
        ApplicationInfo.CATEGORY_NEWS -> "News & Magazines"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
        ApplicationInfo.CATEGORY_MAPS -> "Maps & Navigation"
        ApplicationInfo.CATEGORY_UNDEFINED -> "Other"
        else -> "Other"
    }

    private companion object {
        val CATEGORY_ORDER = listOf(
            "Social",
            "Games",
            "Entertainment",
            "Music & Audio",
            "Photography",
            "News & Magazines",
            "Productivity",
            "Maps & Navigation",
            "Shopping",
            "Other",
        )
    }
}
