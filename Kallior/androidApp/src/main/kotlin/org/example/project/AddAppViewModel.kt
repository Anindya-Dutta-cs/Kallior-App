package org.example.project

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddAppUiState(
    val categories: List<String> = emptyList(),
    val apps: List<InstalledAppInfo> = emptyList(),
    val blockedApps: Set<String> = emptySet(),
    val strictlyBlockedApps: Set<String> = emptySet(),
)

class AddAppViewModel(private val context: Context, private val mode: String = "BLOCKER") : ViewModel() {

    private val provider = InstalledAppsProvider(context)
    private val controller = AppBlockerControllerImpl(
        context,
        BlockerRepository(context),
        WebsiteBlockerRepository(context),
    )
    private val timeWastingRepo = TimeWastingAppsRepository(context)
    private val permissionManager = PermissionManager(context)

    private val categories: List<String> = provider.getCategories()

    private val _selectedCategory = MutableStateFlow(categories.firstOrNull().orEmpty())
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _uiState = MutableStateFlow(
        AddAppUiState(
            categories = categories,
            apps = filterFor(_selectedCategory.value),
        ),
    )
    val uiState: StateFlow<AddAppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            controller.getBlockedAppsFlow().collect { strictly ->
                _uiState.update { it.copy(strictlyBlockedApps = strictly) }
            }
        }
        viewModelScope.launch {
            if (mode == "TIME_WASTING") {
                timeWastingRepo.timeWastingAppsFlow.collect { blocked ->
                    _uiState.update { it.copy(blockedApps = blocked) }
                }
            } else {
                controller.getBlockedAppsFlow().collect { blocked ->
                    _uiState.update { it.copy(blockedApps = blocked) }
                }
            }
        }
    }

    fun onCategorySelected(category: String) {
        if (category == _selectedCategory.value) return
        _selectedCategory.value = category
        _uiState.update { it.copy(apps = filterFor(category)) }
        preloadIcons(category)
    }

    private val _permissionError = MutableStateFlow(false)
    val permissionError: StateFlow<Boolean> = _permissionError.asStateFlow()

    fun toggleAppBlock(packageName: String) {
        val isBlocked = _uiState.value.blockedApps.contains(packageName)
        if (mode == "TIME_WASTING") {
            viewModelScope.launch {
                if (isBlocked) {
                    timeWastingRepo.removeTimeWastingApp(packageName)
                } else {
                    timeWastingRepo.addTimeWastingApp(packageName)
                }
            }
            return
        }

        if (isBlocked) {
            controller.removeBlockedApp(packageName)
            return
        }

        controller.addBlockedApp(packageName)

        val hasPermissions = permissionManager.hasUsageStatsPermission() &&
            permissionManager.hasOverlayPermission()

        if (hasPermissions) {
            // CRITICAL: Ensure the service is running when an app is blocked
            controller.startBlocking()
        } else {
            // Don't silently start a service that can't function. Surface this
            // to the UI so the user is sent back to Focus Fortress to grant
            // permissions instead of thinking blocking is active when it isn't.
            _permissionError.update { true }
        }
    }

    fun clearPermissionError() {
        _permissionError.update { false }
    }

    fun loadIcon(packageName: String): Drawable? = provider.getApplicationIcon(packageName)

    private fun preloadIcons(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            filterFor(category).forEach { provider.getApplicationIcon(it.packageName) }
        }
    }

    private fun filterFor(category: String): List<InstalledAppInfo> {
        return provider.getInstalledApps()
            .filter { it.category == category }
            .sortedBy { it.appName.lowercase() }
    }
}
