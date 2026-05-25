package com.hflocal.android.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hflocal.shared.domain.model.UserInfo
import com.hflocal.shared.domain.usecase.LoginWithTokenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val token: String = "",
    val showToken: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: UserInfo? = null,
    val isLoggedIn: Boolean = false,
    val activeTab: Int = 0
)

class AuthViewModel(
    private val loginUseCase: LoginWithTokenUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun updateToken(token: String) {
        _state.value = _state.value.copy(token = token, error = null)
    }

    fun toggleShowToken() {
        _state.value = _state.value.copy(showToken = !_state.value.showToken)
    }

    fun setTab(tab: Int) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    fun login() {
        viewModelScope.launch {
            val current = _state.value
            if (current.token.isBlank()) {
                _state.value = current.copy(error = "Please enter a token")
                return@launch
            }
            if (!current.token.startsWith("hf_")) {
                _state.value = current.copy(error = "Token must start with hf_")
                return@launch
            }
            _state.value = current.copy(isLoading = true, error = null)
            try {
                val user = loginUseCase(current.token)
                _state.value = current.copy(
                    isLoading = false,
                    user = user,
                    isLoggedIn = true
                )
            } catch (e: Exception) {
                _state.value = current.copy(
                    isLoading = false,
                    error = "Invalid token: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
