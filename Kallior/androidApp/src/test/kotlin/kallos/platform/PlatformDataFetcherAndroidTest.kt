package kallos.platform

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [PlatformDataFetcher.isEntertainmentPackage] using a hand-rolled
 * [PackageManager] test double. These run on the JVM against the Android mockable
 * jar and do not require an emulator.
 */
class PlatformDataFetcherAndroidTest {

    private val fetcher = PlatformDataFetcher()

    @Test
    fun entertainmentTokenClassifiedEvenWhenGetApplicationInfoThrows() {
        val pm = ThrowingPackageManager()
        assertTrue(fetcher.isEntertainmentPackage("com.youtube.app", pm))
        assertTrue(fetcher.isEntertainmentPackage("com.example.tiktok.client", pm))
        assertTrue(fetcher.isEntertainmentPackage("com.example.myGame", pm))
    }

    @Test
    fun gameCategoryIsEntertainment() {
        val info = ApplicationInfo().apply { category = ApplicationInfo.CATEGORY_GAME }
        val pm = StubPackageManager(info)
        assertTrue(fetcher.isEntertainmentPackage("com.example.something", pm))
    }

    @Test
    fun videoCategoryIsEntertainment() {
        val info = ApplicationInfo().apply { category = ApplicationInfo.CATEGORY_VIDEO }
        val pm = StubPackageManager(info)
        assertTrue(fetcher.isEntertainmentPackage("com.example.something", pm))
    }

    @Test
    fun socialCategoryIsEntertainment() {
        val info = ApplicationInfo().apply { category = ApplicationInfo.CATEGORY_SOCIAL }
        val pm = StubPackageManager(info)
        assertTrue(fetcher.isEntertainmentPackage("com.example.something", pm))
    }

    @Test
    fun productivityCategoryIsNotEntertainment() {
        val info = ApplicationInfo().apply { category = ApplicationInfo.CATEGORY_PRODUCTIVITY }
        val pm = StubPackageManager(info)
        assertFalse(fetcher.isEntertainmentPackage("com.example.something", pm))
    }

    @Test
    fun audioCategoryIsNotEntertainment() {
        val info = ApplicationInfo().apply { category = ApplicationInfo.CATEGORY_AUDIO }
        val pm = StubPackageManager(info)
        assertFalse(fetcher.isEntertainmentPackage("com.example.something", pm))
    }

    @Test
    fun packageManagerNameNotFoundExceptionIsHandled() {
        val pm = ThrowingPackageManager()
        // No entertainment token, so result must be false even though the
        // PackageManager throws NameNotFoundException.
        assertFalse(fetcher.isEntertainmentPackage("com.example.workapp", pm))
    }

    /** Returns a fixed [ApplicationInfo] for any package. */
    private class StubPackageManager(private val info: ApplicationInfo) : PackageManager() {
        override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo = info
        override fun addPackageToPreferred(packageName: String) {
            throw UnsupportedOperationException()
        }

        override fun addPermission(info: android.content.pm.PermissionInfo): Boolean {
            throw UnsupportedOperationException()
        }

        override fun addPermissionAsync(info: android.content.pm.PermissionInfo): Boolean {
            throw UnsupportedOperationException()
        }

        override fun addPreferredActivity(p0: android.content.IntentFilter, p1: Int, p2: Array<out android.content.ComponentName>?, p3: android.content.ComponentName) {
            throw UnsupportedOperationException()
        }

        override fun canRequestPackageInstalls(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun canonicalToCurrentPackageNames(packageNames: Array<String>): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun checkPermission(permName: String, packageName: String): Int {
            throw UnsupportedOperationException()
        }

        override fun checkSignatures(uid1: Int, uid2: Int): Int {
            throw UnsupportedOperationException()
        }

        override fun checkSignatures(packageName1: String, packageName2: String): Int {
            throw UnsupportedOperationException()
        }

        override fun clearInstantAppCookie() {
            throw UnsupportedOperationException()
        }

        override fun clearPackagePreferredActivities(packageName: String) {
            throw UnsupportedOperationException()
        }

        override fun currentToCanonicalPackageNames(packageNames: Array<String>): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun extendVerificationTimeout(id: Int, verificationCodeAtTimeout: Int, millisecondsToDelay: Long) {
            throw UnsupportedOperationException()
        }

        override fun getActivityBanner(activityName: android.content.ComponentName): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityBanner(intent: android.content.Intent): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityIcon(activityName: android.content.ComponentName): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityIcon(intent: android.content.Intent): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityInfo(component: android.content.ComponentName, flags: Int): android.content.pm.ActivityInfo {
            throw UnsupportedOperationException()
        }

        override fun getActivityLogo(activityName: android.content.ComponentName): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityLogo(intent: android.content.Intent): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getAllPermissionGroups(flags: Int): List<android.content.pm.PermissionGroupInfo> {
            throw UnsupportedOperationException()
        }

        override fun getApplicationBanner(info: android.content.pm.ApplicationInfo): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationBanner(packageName: String): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationEnabledSetting(packageName: String): Int {
            throw UnsupportedOperationException()
        }

        override fun getApplicationIcon(info: android.content.pm.ApplicationInfo): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationIcon(packageName: String): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationLabel(info: android.content.pm.ApplicationInfo): CharSequence {
            throw UnsupportedOperationException()
        }

        override fun getApplicationLogo(info: android.content.pm.ApplicationInfo): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationLogo(packageName: String): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getChangedPackages(sequenceNumber: Int): android.content.pm.ChangedPackages {
            throw UnsupportedOperationException()
        }

        override fun getComponentEnabledSetting(componentName: android.content.ComponentName): Int {
            throw UnsupportedOperationException()
        }

        override fun getDefaultActivityIcon(): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getDrawable(p0: String, p1: Int, p2: android.content.pm.ApplicationInfo?): android.graphics.drawable.Drawable? {
            throw UnsupportedOperationException()
        }

        override fun getInstalledApplications(flags: Int): List<android.content.pm.ApplicationInfo> {
            throw UnsupportedOperationException()
        }

        override fun getInstalledPackages(flags: Int): List<android.content.pm.PackageInfo> {
            throw UnsupportedOperationException()
        }

        override fun getInstallerPackageName(packageName: String): String {
            throw UnsupportedOperationException()
        }

        override fun getInstantAppCookie(): ByteArray {
            throw UnsupportedOperationException()
        }

        override fun getInstantAppCookieMaxBytes(): Int {
            throw UnsupportedOperationException()
        }

        override fun getInstrumentationInfo(className: android.content.ComponentName, flags: Int): android.content.pm.InstrumentationInfo {
            throw UnsupportedOperationException()
        }

        override fun getLaunchIntentForPackage(packageName: String): android.content.Intent {
            throw UnsupportedOperationException()
        }

        override fun getLeanbackLaunchIntentForPackage(packageName: String): android.content.Intent {
            throw UnsupportedOperationException()
        }

        override fun getNameForUid(uid: Int): String {
            throw UnsupportedOperationException()
        }

        override fun getPackageGids(packageName: String): IntArray {
            throw UnsupportedOperationException()
        }

        override fun getPackageGids(packageName: String, flags: Int): IntArray {
            throw UnsupportedOperationException()
        }

        override fun getPackageInfo(versionedPackage: android.content.pm.VersionedPackage, flags: Int): android.content.pm.PackageInfo {
            throw UnsupportedOperationException()
        }

        override fun getPackageInfo(packageName: String, flags: Int): android.content.pm.PackageInfo {
            throw UnsupportedOperationException()
        }

        override fun getPackageInstaller(): android.content.pm.PackageInstaller {
            throw UnsupportedOperationException()
        }

        override fun getPackageUid(packageName: String, flags: Int): Int {
            throw UnsupportedOperationException()
        }

        override fun getPackagesForUid(uid: Int): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun getPackagesHoldingPermissions(permissions: Array<String>, flags: Int): List<android.content.pm.PackageInfo> {
            throw UnsupportedOperationException()
        }

        override fun getPermissionGroupInfo(groupName: String, flags: Int): android.content.pm.PermissionGroupInfo {
            throw UnsupportedOperationException()
        }

        override fun getPermissionInfo(permName: String, flags: Int): android.content.pm.PermissionInfo {
            throw UnsupportedOperationException()
        }

        override fun getPreferredActivities(p0: MutableList<android.content.IntentFilter>, p1: MutableList<android.content.ComponentName>, p2: String?): Int {
            throw UnsupportedOperationException()
        }

        override fun getPreferredPackages(flags: Int): List<android.content.pm.PackageInfo> {
            throw UnsupportedOperationException()
        }

        override fun getProviderInfo(component: android.content.ComponentName, flags: Int): android.content.pm.ProviderInfo {
            throw UnsupportedOperationException()
        }

        override fun getReceiverInfo(component: android.content.ComponentName, flags: Int): android.content.pm.ActivityInfo {
            throw UnsupportedOperationException()
        }

        override fun getResourcesForActivity(activityName: android.content.ComponentName): android.content.res.Resources {
            throw UnsupportedOperationException()
        }

        override fun getResourcesForApplication(app: android.content.pm.ApplicationInfo): android.content.res.Resources {
            throw UnsupportedOperationException()
        }

        override fun getResourcesForApplication(packageName: String): android.content.res.Resources {
            throw UnsupportedOperationException()
        }

        override fun getServiceInfo(component: android.content.ComponentName, flags: Int): android.content.pm.ServiceInfo {
            throw UnsupportedOperationException()
        }

        override fun getSharedLibraries(flags: Int): List<android.content.pm.SharedLibraryInfo> {
            throw UnsupportedOperationException()
        }

        override fun getSystemAvailableFeatures(): Array<android.content.pm.FeatureInfo> {
            throw UnsupportedOperationException()
        }

        override fun getSystemSharedLibraryNames(): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun getText(p0: String, p1: Int, p2: android.content.pm.ApplicationInfo?): CharSequence? {
            throw UnsupportedOperationException()
        }

        override fun getUserBadgedDrawableForDensity(p0: android.graphics.drawable.Drawable, p1: android.os.UserHandle, p2: android.graphics.Rect?, p3: Int): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getUserBadgedIcon(drawable: android.graphics.drawable.Drawable, user: android.os.UserHandle): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getUserBadgedLabel(label: CharSequence, user: android.os.UserHandle): CharSequence {
            throw UnsupportedOperationException()
        }

        override fun getXml(p0: String, p1: Int, p2: android.content.pm.ApplicationInfo?): android.content.res.XmlResourceParser? {
            throw UnsupportedOperationException()
        }

        override fun hasSystemFeature(featureName: String): Boolean {
            throw UnsupportedOperationException()
        }

        override fun hasSystemFeature(featureName: String, version: Int): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isInstantApp(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isInstantApp(packageName: String): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isPermissionRevokedByPolicy(permName: String, packageName: String): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isSafeMode(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun queryBroadcastReceivers(intent: android.content.Intent, flags: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryContentProviders(p0: String?, p1: Int, p2: Int): List<android.content.pm.ProviderInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryContentProviders(p0: String?, p1: Int, p2: android.content.pm.PackageManager.ComponentInfoFlags): List<android.content.pm.ProviderInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryInstrumentation(targetPackage: String, flags: Int): List<android.content.pm.InstrumentationInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentActivities(intent: android.content.Intent, flags: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentActivityOptions(p0: android.content.ComponentName?, p1: Array<out android.content.Intent>?, p2: android.content.Intent, p3: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentActivityOptions(p0: android.content.ComponentName?, p1: MutableList<android.content.Intent>?, p2: android.content.Intent, p3: android.content.pm.PackageManager.ResolveInfoFlags): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentContentProviders(intent: android.content.Intent, flags: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentServices(intent: android.content.Intent, flags: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryPermissionsByGroup(p0: String?, p1: Int): List<android.content.pm.PermissionInfo> {
            throw UnsupportedOperationException()
        }

        override fun removePackageFromPreferred(packageName: String) {
            throw UnsupportedOperationException()
        }

        override fun removePermission(permName: String) {
            throw UnsupportedOperationException()
        }

        override fun resolveActivity(intent: android.content.Intent, flags: Int): android.content.pm.ResolveInfo {
            throw UnsupportedOperationException()
        }

        override fun resolveContentProvider(authority: String, flags: Int): android.content.pm.ProviderInfo {
            throw UnsupportedOperationException()
        }

        override fun resolveService(intent: android.content.Intent, flags: Int): android.content.pm.ResolveInfo {
            throw UnsupportedOperationException()
        }

        override fun setApplicationCategoryHint(packageName: String, categoryHint: Int) {
            throw UnsupportedOperationException()
        }

        override fun setApplicationEnabledSetting(packageName: String, newState: Int, flags: Int) {
            throw UnsupportedOperationException()
        }

        override fun setComponentEnabledSetting(componentName: android.content.ComponentName, newState: Int, flags: Int) {
            throw UnsupportedOperationException()
        }

        override fun setInstallerPackageName(p0: String, p1: String?) {
            throw UnsupportedOperationException()
        }

        override fun updateInstantAppCookie(p0: ByteArray?) {
            throw UnsupportedOperationException()
        }

        override fun verifyPendingInstall(id: Int, verificationCode: Int) {
            throw UnsupportedOperationException()
        }
    }

    /** Always throws [PackageManager.NameNotFoundException]. */
    private class ThrowingPackageManager : PackageManager() {
        override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo {
            throw PackageManager.NameNotFoundException(packageName)
        }
        override fun addPackageToPreferred(packageName: String) {
            throw UnsupportedOperationException()
        }

        override fun addPermission(info: android.content.pm.PermissionInfo): Boolean {
            throw UnsupportedOperationException()
        }

        override fun addPermissionAsync(info: android.content.pm.PermissionInfo): Boolean {
            throw UnsupportedOperationException()
        }

        override fun addPreferredActivity(p0: android.content.IntentFilter, p1: Int, p2: Array<out android.content.ComponentName>?, p3: android.content.ComponentName) {
            throw UnsupportedOperationException()
        }

        override fun canRequestPackageInstalls(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun canonicalToCurrentPackageNames(packageNames: Array<String>): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun checkPermission(permName: String, packageName: String): Int {
            throw UnsupportedOperationException()
        }

        override fun checkSignatures(uid1: Int, uid2: Int): Int {
            throw UnsupportedOperationException()
        }

        override fun checkSignatures(packageName1: String, packageName2: String): Int {
            throw UnsupportedOperationException()
        }

        override fun clearInstantAppCookie() {
            throw UnsupportedOperationException()
        }

        override fun clearPackagePreferredActivities(packageName: String) {
            throw UnsupportedOperationException()
        }

        override fun currentToCanonicalPackageNames(packageNames: Array<String>): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun extendVerificationTimeout(id: Int, verificationCodeAtTimeout: Int, millisecondsToDelay: Long) {
            throw UnsupportedOperationException()
        }

        override fun getActivityBanner(activityName: android.content.ComponentName): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityBanner(intent: android.content.Intent): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityIcon(activityName: android.content.ComponentName): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityIcon(intent: android.content.Intent): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityInfo(component: android.content.ComponentName, flags: Int): android.content.pm.ActivityInfo {
            throw UnsupportedOperationException()
        }

        override fun getActivityLogo(activityName: android.content.ComponentName): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getActivityLogo(intent: android.content.Intent): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getAllPermissionGroups(flags: Int): List<android.content.pm.PermissionGroupInfo> {
            throw UnsupportedOperationException()
        }

        override fun getApplicationBanner(info: android.content.pm.ApplicationInfo): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationBanner(packageName: String): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationEnabledSetting(packageName: String): Int {
            throw UnsupportedOperationException()
        }

        override fun getApplicationIcon(info: android.content.pm.ApplicationInfo): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationIcon(packageName: String): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationLabel(info: android.content.pm.ApplicationInfo): CharSequence {
            throw UnsupportedOperationException()
        }

        override fun getApplicationLogo(info: android.content.pm.ApplicationInfo): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getApplicationLogo(packageName: String): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getChangedPackages(sequenceNumber: Int): android.content.pm.ChangedPackages {
            throw UnsupportedOperationException()
        }

        override fun getComponentEnabledSetting(componentName: android.content.ComponentName): Int {
            throw UnsupportedOperationException()
        }

        override fun getDefaultActivityIcon(): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getDrawable(p0: String, p1: Int, p2: android.content.pm.ApplicationInfo?): android.graphics.drawable.Drawable? {
            throw UnsupportedOperationException()
        }

        override fun getInstalledApplications(flags: Int): List<android.content.pm.ApplicationInfo> {
            throw UnsupportedOperationException()
        }

        override fun getInstalledPackages(flags: Int): List<android.content.pm.PackageInfo> {
            throw UnsupportedOperationException()
        }

        override fun getInstallerPackageName(packageName: String): String {
            throw UnsupportedOperationException()
        }

        override fun getInstantAppCookie(): ByteArray {
            throw UnsupportedOperationException()
        }

        override fun getInstantAppCookieMaxBytes(): Int {
            throw UnsupportedOperationException()
        }

        override fun getInstrumentationInfo(className: android.content.ComponentName, flags: Int): android.content.pm.InstrumentationInfo {
            throw UnsupportedOperationException()
        }

        override fun getLaunchIntentForPackage(packageName: String): android.content.Intent {
            throw UnsupportedOperationException()
        }

        override fun getLeanbackLaunchIntentForPackage(packageName: String): android.content.Intent {
            throw UnsupportedOperationException()
        }

        override fun getNameForUid(uid: Int): String {
            throw UnsupportedOperationException()
        }

        override fun getPackageGids(packageName: String): IntArray {
            throw UnsupportedOperationException()
        }

        override fun getPackageGids(packageName: String, flags: Int): IntArray {
            throw UnsupportedOperationException()
        }

        override fun getPackageInfo(versionedPackage: android.content.pm.VersionedPackage, flags: Int): android.content.pm.PackageInfo {
            throw UnsupportedOperationException()
        }

        override fun getPackageInfo(packageName: String, flags: Int): android.content.pm.PackageInfo {
            throw UnsupportedOperationException()
        }

        override fun getPackageInstaller(): android.content.pm.PackageInstaller {
            throw UnsupportedOperationException()
        }

        override fun getPackageUid(packageName: String, flags: Int): Int {
            throw UnsupportedOperationException()
        }

        override fun getPackagesForUid(uid: Int): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun getPackagesHoldingPermissions(permissions: Array<String>, flags: Int): List<android.content.pm.PackageInfo> {
            throw UnsupportedOperationException()
        }

        override fun getPermissionGroupInfo(groupName: String, flags: Int): android.content.pm.PermissionGroupInfo {
            throw UnsupportedOperationException()
        }

        override fun getPermissionInfo(permName: String, flags: Int): android.content.pm.PermissionInfo {
            throw UnsupportedOperationException()
        }

        override fun getPreferredActivities(p0: MutableList<android.content.IntentFilter>, p1: MutableList<android.content.ComponentName>, p2: String?): Int {
            throw UnsupportedOperationException()
        }

        override fun getPreferredPackages(flags: Int): List<android.content.pm.PackageInfo> {
            throw UnsupportedOperationException()
        }

        override fun getProviderInfo(component: android.content.ComponentName, flags: Int): android.content.pm.ProviderInfo {
            throw UnsupportedOperationException()
        }

        override fun getReceiverInfo(component: android.content.ComponentName, flags: Int): android.content.pm.ActivityInfo {
            throw UnsupportedOperationException()
        }

        override fun getResourcesForActivity(activityName: android.content.ComponentName): android.content.res.Resources {
            throw UnsupportedOperationException()
        }

        override fun getResourcesForApplication(app: android.content.pm.ApplicationInfo): android.content.res.Resources {
            throw UnsupportedOperationException()
        }

        override fun getResourcesForApplication(packageName: String): android.content.res.Resources {
            throw UnsupportedOperationException()
        }

        override fun getServiceInfo(component: android.content.ComponentName, flags: Int): android.content.pm.ServiceInfo {
            throw UnsupportedOperationException()
        }

        override fun getSharedLibraries(flags: Int): List<android.content.pm.SharedLibraryInfo> {
            throw UnsupportedOperationException()
        }

        override fun getSystemAvailableFeatures(): Array<android.content.pm.FeatureInfo> {
            throw UnsupportedOperationException()
        }

        override fun getSystemSharedLibraryNames(): Array<String> {
            throw UnsupportedOperationException()
        }

        override fun getText(p0: String, p1: Int, p2: android.content.pm.ApplicationInfo?): CharSequence? {
            throw UnsupportedOperationException()
        }

        override fun getUserBadgedDrawableForDensity(p0: android.graphics.drawable.Drawable, p1: android.os.UserHandle, p2: android.graphics.Rect?, p3: Int): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getUserBadgedIcon(drawable: android.graphics.drawable.Drawable, user: android.os.UserHandle): android.graphics.drawable.Drawable {
            throw UnsupportedOperationException()
        }

        override fun getUserBadgedLabel(label: CharSequence, user: android.os.UserHandle): CharSequence {
            throw UnsupportedOperationException()
        }

        override fun getXml(p0: String, p1: Int, p2: android.content.pm.ApplicationInfo?): android.content.res.XmlResourceParser? {
            throw UnsupportedOperationException()
        }

        override fun hasSystemFeature(featureName: String): Boolean {
            throw UnsupportedOperationException()
        }

        override fun hasSystemFeature(featureName: String, version: Int): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isInstantApp(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isInstantApp(packageName: String): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isPermissionRevokedByPolicy(permName: String, packageName: String): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isSafeMode(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun queryBroadcastReceivers(intent: android.content.Intent, flags: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryContentProviders(p0: String?, p1: Int, p2: Int): List<android.content.pm.ProviderInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryContentProviders(p0: String?, p1: Int, p2: android.content.pm.PackageManager.ComponentInfoFlags): List<android.content.pm.ProviderInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryInstrumentation(targetPackage: String, flags: Int): List<android.content.pm.InstrumentationInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentActivities(intent: android.content.Intent, flags: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentActivityOptions(p0: android.content.ComponentName?, p1: Array<out android.content.Intent>?, p2: android.content.Intent, p3: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentActivityOptions(p0: android.content.ComponentName?, p1: MutableList<android.content.Intent>?, p2: android.content.Intent, p3: android.content.pm.PackageManager.ResolveInfoFlags): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentContentProviders(intent: android.content.Intent, flags: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryIntentServices(intent: android.content.Intent, flags: Int): List<android.content.pm.ResolveInfo> {
            throw UnsupportedOperationException()
        }

        override fun queryPermissionsByGroup(p0: String?, p1: Int): List<android.content.pm.PermissionInfo> {
            throw UnsupportedOperationException()
        }

        override fun removePackageFromPreferred(packageName: String) {
            throw UnsupportedOperationException()
        }

        override fun removePermission(permName: String) {
            throw UnsupportedOperationException()
        }

        override fun resolveActivity(intent: android.content.Intent, flags: Int): android.content.pm.ResolveInfo {
            throw UnsupportedOperationException()
        }

        override fun resolveContentProvider(authority: String, flags: Int): android.content.pm.ProviderInfo {
            throw UnsupportedOperationException()
        }

        override fun resolveService(intent: android.content.Intent, flags: Int): android.content.pm.ResolveInfo {
            throw UnsupportedOperationException()
        }

        override fun setApplicationCategoryHint(packageName: String, categoryHint: Int) {
            throw UnsupportedOperationException()
        }

        override fun setApplicationEnabledSetting(packageName: String, newState: Int, flags: Int) {
            throw UnsupportedOperationException()
        }

        override fun setComponentEnabledSetting(componentName: android.content.ComponentName, newState: Int, flags: Int) {
            throw UnsupportedOperationException()
        }

        override fun setInstallerPackageName(p0: String, p1: String?) {
            throw UnsupportedOperationException()
        }

        override fun updateInstantAppCookie(p0: ByteArray?) {
            throw UnsupportedOperationException()
        }

        override fun verifyPendingInstall(id: Int, verificationCode: Int) {
            throw UnsupportedOperationException()
        }
    }
}
