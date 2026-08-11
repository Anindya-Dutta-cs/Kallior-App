package org.example.project

import kotlinx.coroutines.flow.Flow

interface AppBlockerController {
    fun startBlocking()
    fun stopBlocking()
    fun addBlockedApp(packageName: String)
    fun removeBlockedApp(packageName: String)
    fun getBlockedAppsFlow(): Flow<Set<String>>

    /** Temporarily unblock [packageName] for [durationMinutes] from now. */
    fun allowAppTemporarily(packageName: String, durationMinutes: Int)

    /** True when the app is blocked AND not currently allowed via Allow Until. */
    suspend fun isAppBlocked(packageName: String): Boolean

    fun setBlockingEnabled(enabled: Boolean)
    fun isBlockingEnabledFlow(): Flow<Boolean>

    fun startWebsiteBlocking()
    fun stopWebsiteBlocking()
    fun addBlockedWebsite(url: String)
    fun removeBlockedWebsite(url: String)
    fun getBlockedWebsitesFlow(): Flow<List<String>>

    fun isVpnEnabledFlow(): Flow<Boolean>
    fun setVpnEnabled(enabled: Boolean)
}
