package org.example.project

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import javax.inject.Inject

class AppBlockerControllerImpl @Inject constructor(
    private val context: Context,
    private val repository: BlockerRepository,
    private val websiteRepository: WebsiteBlockerRepository
) : AppBlockerController {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startBlocking() {
        scope.launch { repository.setBlockingEnabled(true) }
        val serviceIntent = Intent(context, AppBlockerForegroundService::class.java)
        context.startForegroundService(serviceIntent)
    }

    override fun stopBlocking() {
        scope.launch { repository.setBlockingEnabled(false) }
        val serviceIntent = Intent(context, AppBlockerForegroundService::class.java)
        context.stopService(serviceIntent)
    }

    override fun addBlockedApp(packageName: String) {
        scope.launch {
            repository.addBlockedApp(packageName)
        }
    }

    override fun removeBlockedApp(packageName: String) {
        scope.launch {
            repository.removeBlockedApp(packageName)
        }
    }

    override fun getBlockedAppsFlow(): Flow<Set<String>> {
        return repository.blockedAppsFlow
    }

    override fun allowAppTemporarily(packageName: String, durationMinutes: Int) {
        scope.launch {
            val allowUntil = System.currentTimeMillis() + (durationMinutes * 60_000L)
            repository.setAllowUntil(packageName, allowUntil)
            // The overlay already recorded this app open as an attempt. Count the
            // bypass only once its temporary allow was successfully persisted.
            BlockerStatsTracker.recordBypass()
        }
    }

    override suspend fun isAppBlocked(packageName: String): Boolean {
        val isBlocked = repository.blockedAppsFlow.first().contains(packageName)
        return isBlocked && !repository.isAppAllowed(packageName)
    }

    override fun setBlockingEnabled(enabled: Boolean) {
        scope.launch { repository.setBlockingEnabled(enabled) }
    }

    override fun isBlockingEnabledFlow(): Flow<Boolean> {
        return repository.isBlockingEnabledFlow
    }

    override fun startWebsiteBlocking() {
        scope.launch { websiteRepository.setVpnEnabled(true) }
        val serviceIntent = Intent(context, WebsiteBlockerVpnService::class.java)
        context.startForegroundService(serviceIntent)
    }

    override fun stopWebsiteBlocking() {
        scope.launch { websiteRepository.setVpnEnabled(false) }
        val serviceIntent = Intent(context, WebsiteBlockerVpnService::class.java)
        context.stopService(serviceIntent)
    }

    override fun addBlockedWebsite(url: String) {
        scope.launch {
            websiteRepository.addBlockedWebsite(url)
        }
    }

    override fun removeBlockedWebsite(url: String) {
        scope.launch {
            websiteRepository.removeBlockedWebsite(url)
        }
    }

    override fun getBlockedWebsitesFlow(): Flow<List<String>> {
        return websiteRepository.blockedWebsitesFlow
    }

    override fun isVpnEnabledFlow(): Flow<Boolean> {
        return websiteRepository.isVpnEnabledFlow
    }

    override fun setVpnEnabled(enabled: Boolean) {
        scope.launch { websiteRepository.setVpnEnabled(enabled) }
    }
}
