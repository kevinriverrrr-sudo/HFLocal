package com.hflocal.android.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hflocal.shared.domain.model.*
import com.hflocal.shared.domain.repository.IDeviceRepository
import com.hflocal.shared.domain.repository.ISettingsRepository
import com.hflocal.shared.domain.usecase.GetSettingsUseCase
import com.hflocal.shared.domain.usecase.LogoutUseCase
import com.hflocal.shared.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val deviceProfile: DeviceProfile = DeviceProfile(),
    val hfToken: String? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

class SettingsViewModel(
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val logout: LogoutUseCase,
    private val settingsRepo: ISettingsRepository,
    private val deviceRepo: IDeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Load settings
            getSettings().collect { settings ->
                _state.update { it.copy(settings = settings, isLoading = false) }
            }
        }

        // Load device profile
        viewModelScope.launch {
            deviceRepo.getDeviceProfile().collect { profile ->
                _state.update { it.copy(deviceProfile = profile) }
            }
        }

        // Load HF token
        viewModelScope.launch {
            val token = settingsRepo.getHfToken()
            _state.update { it.copy(hfToken = token, isLoggedIn = !token.isNullOrEmpty()) }
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            val updated = _state.updateAndGet { it.copy(
                settings = it.settings.copy(theme = theme)
            ) }.settings
            updateSettings(updated)
        }
    }

    fun toggleAnimations(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _state.updateAndGet { it.copy(
                settings = it.settings.copy(animationsEnabled = enabled)
            ) }.settings
            updateSettings(updated)
        }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _state.updateAndGet { it.copy(
                settings = it.settings.copy(downloadOnlyOnWifi = enabled)
            ) }.settings
            updateSettings(updated)
        }
    }

    fun toggleGatedModels(show: Boolean) {
        viewModelScope.launch {
            val updated = _state.updateAndGet { it.copy(
                settings = it.settings.copy(showGatedModels = show)
            ) }.settings
            updateSettings(updated)
        }
    }

    fun toggleChatHistory(save: Boolean) {
        viewModelScope.launch {
            val updated = _state.updateAndGet { it.copy(
                settings = it.settings.copy(saveChatHistory = save)
            ) }.settings
            updateSettings(updated)
        }
    }

    fun updateDefaultSort(sort: String) {
        viewModelScope.launch {
            val updated = _state.updateAndGet { it.copy(
                settings = it.settings.copy(defaultSort = sort)
            ) }.settings
            updateSettings(updated)
        }
    }

    fun updateSystemPrompt(prompt: String) {
        viewModelScope.launch {
            val updated = _state.updateAndGet { it.copy(
                settings = it.settings.copy(defaultSystemPrompt = prompt)
            ) }.settings
            updateSettings(updated)
        }
    }

    fun performLogout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                logout()
                _state.update { it.copy(hfToken = null, isLoggedIn = false) }
                onComplete()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Logout failed: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
