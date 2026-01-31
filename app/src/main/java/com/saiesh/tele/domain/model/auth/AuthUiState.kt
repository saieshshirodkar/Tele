package com.saiesh.tele.domain.model.auth

enum class AuthStep {
    Loading,
    EnterApiKeys,
    EnterPhone,
    EnterCode,
    EnterPassword,
    Authorized
}

data class AuthUiState(
    val apiId: String = "",
    val apiHash: String = "",
    val phone: String = "",
    val code: String = "",
    val password: String = "",
    val step: AuthStep = AuthStep.Loading,
    val message: String? = null,
    val isLoading: Boolean = false
)
