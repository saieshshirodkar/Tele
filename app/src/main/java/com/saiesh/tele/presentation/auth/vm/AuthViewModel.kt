package com.saiesh.tele.presentation.auth.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saiesh.tele.core.tdlib.auth.TelegramAuthManager
import com.saiesh.tele.domain.model.AuthUiState
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
                        phone = state.phone,
                        code = state.code,
                        password = state.password,
                        message = state.message,
                        isLoading = state.isLoading
                    )
                }
            }
        }
    }

    fun onPhoneChange(value: String) = updateField { it.copy(phone = value) }
    fun onApiIdChange(value: String) = updateField { it.copy(apiId = value) }
    fun onApiHashChange(value: String) = updateField { it.copy(apiHash = value) }
    fun onCodeChange(value: String) = updateField { it.copy(code = value) }
    fun onPasswordChange(value: String) = updateField { it.copy(password = value) }

    private fun updateField(transform: (AuthUiState) -> AuthUiState) {
        _uiState.update { transform(it).copy(message = null) }
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

    fun submitPassword() {
        val password = _uiState.value.password
        if (password.isBlank()) {
            _uiState.update { it.copy(message = "Enter your 2FA password") }
            return
        }
        authManager.submitPassword(password)
    }

    override fun onCleared() {
        authManager.cleanup()
        super.onCleared()
    }
}
