package org.example.project

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForegroundAppDetectorTest {

    private val hostPackage = "org.example.project"

    @Test
    fun `launcher is never relevant to any domain`() {
        val result = ForegroundAppDetector.isDomainRelevant(
            domain = "reddit.com",
            foregroundPackage = "com.google.android.apps.nexuslauncher",
            appName = "Pixel Launcher",
            isLauncher = true,
            isStandaloneBrowser = false,
            isCustomTab = false,
            isInAppBrowser = false,
            hostAppPackage = hostPackage,
        )
        assertFalse(result, "Overlay should NEVER show when user is on the launcher / home screen")
    }

    @Test
    fun `host app itself is never relevant to website blocking overlay`() {
        val result = ForegroundAppDetector.isDomainRelevant(
            domain = "reddit.com",
            foregroundPackage = hostPackage,
            appName = "Kallior",
            isLauncher = false,
            isStandaloneBrowser = false,
            isCustomTab = false,
            isInAppBrowser = false,
            hostAppPackage = hostPackage,
        )
        assertFalse(result, "Overlay should not show while inside Kallior itself")
    }

    @Test
    fun `standalone browser is always relevant to blocked website query`() {
        val result = ForegroundAppDetector.isDomainRelevant(
            domain = "reddit.com",
            foregroundPackage = "com.android.chrome",
            appName = "Chrome",
            isLauncher = false,
            isStandaloneBrowser = true,
            isCustomTab = false,
            isInAppBrowser = false,
            hostAppPackage = hostPackage,
        )
        assertTrue(result, "Overlay should show when user is in Chrome visiting a blocked site")
    }

    @Test
    fun `custom tab or in-app browser is always relevant to blocked website query`() {
        val customTabResult = ForegroundAppDetector.isDomainRelevant(
            domain = "reddit.com",
            foregroundPackage = "com.android.chrome",
            appName = "Chrome",
            isLauncher = false,
            isStandaloneBrowser = false,
            isCustomTab = true,
            isInAppBrowser = true,
            hostAppPackage = hostPackage,
        )
        assertTrue(customTabResult, "Overlay should show in Custom Tabs")

        val inAppResult = ForegroundAppDetector.isDomainRelevant(
            domain = "reddit.com",
            foregroundPackage = "com.twitter.android",
            appName = "X",
            isLauncher = false,
            isStandaloneBrowser = false,
            isCustomTab = false,
            isInAppBrowser = true,
            hostAppPackage = hostPackage,
        )
        assertTrue(inAppResult, "Overlay should show in In-App Browsers")
    }

    @Test
    fun `app matching domain keyword is relevant`() {
        val result = ForegroundAppDetector.isDomainRelevant(
            domain = "reddit.com",
            foregroundPackage = "com.reddit.frontpage",
            appName = "Reddit",
            isLauncher = false,
            isStandaloneBrowser = false,
            isCustomTab = false,
            isInAppBrowser = false,
            hostAppPackage = hostPackage,
        )
        assertTrue(result, "Reddit app should trigger blocker when reddit.com is blocked")
    }

    @Test
    fun `unrelated app is NOT relevant - prevents background sync bug`() {
        val calculatorResult = ForegroundAppDetector.isDomainRelevant(
            domain = "reddit.com",
            foregroundPackage = "com.google.android.calculator",
            appName = "Calculator",
            isLauncher = false,
            isStandaloneBrowser = false,
            isCustomTab = false,
            isInAppBrowser = false,
            hostAppPackage = hostPackage,
        )
        assertFalse(calculatorResult, "Reddit background query must not trigger overlay over Calculator")

        val settingsResult = ForegroundAppDetector.isDomainRelevant(
            domain = "reddit.com",
            foregroundPackage = "com.android.settings",
            appName = "Settings",
            isLauncher = false,
            isStandaloneBrowser = false,
            isCustomTab = false,
            isInAppBrowser = false,
            hostAppPackage = hostPackage,
        )
        assertFalse(settingsResult, "Reddit background query must not trigger overlay over Settings")
    }

    @Test
    fun `special alias matching works for twitter and x`() {
        val result = ForegroundAppDetector.isDomainRelevant(
            domain = "x.com",
            foregroundPackage = "com.twitter.android",
            appName = "X",
            isLauncher = false,
            isStandaloneBrowser = false,
            isCustomTab = false,
            isInAppBrowser = false,
            hostAppPackage = hostPackage,
        )
        assertTrue(result, "x.com should be relevant to com.twitter.android")
    }
}
