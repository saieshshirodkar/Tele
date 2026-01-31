package com.saiesh.tele.presentation.auth.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saiesh.tele.core.tdlib.auth.TelegramAuthManager
import com.saiesh.tele.domain.model.auth.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = TelegramAuthManager(application)
    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            authManager.uiState.collect { state ->
                _uiState.update { current ->
                    current.copy(
                        step = state.step,
                        apiId = state.apiId,
                        apiHash = state.apiHash,
                        message = state.message,
                        isLoading = state.isLoading
                    )
                }
            }
        }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value, message = null) }
    }

    fun onApiIdChange(value: String) {
        _uiState.update { it.copy(apiId = value, message = null) }
    }

    fun onApiHashChange(value: String) {
        _uiState.update { it.copy(apiHash = value, message = null) }
    }

    fun onCodeChange(value: String) {
        _uiState.update { it.copy(code = value, message = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, message = null) }
    }

    fun submitPhone() {
        val phone = _uiState.value.phone
        if (phone.isBlank()) {
            _uiState.update { it.copy(message = "Enter your phone number") }
            return
        }
        authManager.submitPhone(phone)
    }

    fun submitApiKeys() {
        val apiId = _uiState.value.apiId.trim()
        val apiHash = _uiState.value.apiHash.trim()
        if (apiId.isBlank() || apiId.toIntOrNull() == null) {
            _uiState.update { it.copy(message = "Enter a valid API ID") }
            return
        }
        if (apiHash.isBlank()) {
            _uiState.update { it.copy(message = "Enter a valid API Hash") }
            return
        }
        authManager.submitApiKeys(apiId, apiHash)
    }

    fun submitCode() {
        val code = _uiState.value.code
        if (code.isBlank()) {
            _uiState.update { it.copy(message = "Enter the code from Telegram") }
            return
        }
        authManager.submitCode(code)
    }

    fun submitPassword(password: String) {
        if (password.isBlank()) {
            _uiState.update { it.copy(message = "Enter your 2FA password") }
            return
        }
        authManager.submitPassword(password)
    }
}
