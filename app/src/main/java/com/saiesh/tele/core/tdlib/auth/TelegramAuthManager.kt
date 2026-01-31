package com.saiesh.tele.core.tdlib.auth

import android.content.Context
import android.os.Build
import com.saiesh.tele.BuildConfig
import com.saiesh.tele.core.tdlib.client.TdLibClient
import com.saiesh.tele.data.store.ApiCredentialsStore
import com.saiesh.tele.domain.model.auth.AuthStep
import com.saiesh.tele.domain.model.auth.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.drinkless.tdlib.TdApi
import java.io.File

class TelegramAuthManager(private val context: Context) {
    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState

    private val client = TdLibClient.client
    private val credentialsStore = ApiCredentialsStore(context)

    init {
        TdLibClient.addUpdateHandler(::handleUpdate)
        TdLibClient.addErrorHandler(::handleError)
        client.send(TdApi.GetAuthorizationState()) { result ->
            when (result) {
                is TdApi.AuthorizationState -> handleAuthState(result)
                else -> handleResult(result)
            }
        }
    }

    fun submitPhone(phone: String) {
        _uiState.update { it.copy(isLoading = true, message = null) }
        client.send(TdApi.SetAuthenticationPhoneNumber(phone, null)) { result ->
            handleResult(result)
        }
    }

    fun submitCode(code: String) {
        _uiState.update { it.copy(isLoading = true, message = null) }
        client.send(TdApi.CheckAuthenticationCode(code)) { result ->
            handleResult(result)
        }
    }

    fun submitPassword(password: String) {
        _uiState.update { it.copy(isLoading = true, message = null) }
        client.send(TdApi.CheckAuthenticationPassword(password)) { result ->
            handleResult(result)
        }
    }

    fun submitApiKeys(apiId: String, apiHash: String) {
        credentialsStore.save(apiId, apiHash)
        _uiState.update { it.copy(apiId = apiId, apiHash = apiHash, isLoading = true, message = null) }
        client.send(TdApi.GetAuthorizationState()) { result ->
            when (result) {
                is TdApi.AuthorizationState -> handleAuthState(result)
                else -> handleResult(result)
            }
        }
    }

    private fun handleUpdate(update: TdApi.Object?) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> handleAuthState(update.authorizationState)
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState?) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> setTdlibParameters()
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                _uiState.update { it.copy(step = AuthStep.EnterPhone, isLoading = false) }
            }
            is TdApi.AuthorizationStateWaitCode -> {
                _uiState.update { it.copy(step = AuthStep.EnterCode, isLoading = false) }
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                _uiState.update { it.copy(step = AuthStep.EnterPassword, isLoading = false) }
            }
            is TdApi.AuthorizationStateReady -> {
                _uiState.update { it.copy(step = AuthStep.Authorized, isLoading = false) }
            }
            is TdApi.AuthorizationStateLoggingOut -> {
                _uiState.update { it.copy(message = "Logging out…", isLoading = true) }
            }
            is TdApi.AuthorizationStateClosing -> {
                _uiState.update { it.copy(message = "Closing session…", isLoading = true) }
            }
            is TdApi.AuthorizationStateClosed -> {
                _uiState.update { it.copy(message = "Session closed", isLoading = false) }
            }
            else -> Unit
        }
    }

    private fun handleResult(result: TdApi.Object?) {
        if (result is TdApi.Error) {
            _uiState.update { state ->
                state.copy(message = result.message, isLoading = false)
            }
        }
    }

    private fun handleError(t: Throwable?) {
        _uiState.update { state ->
            state.copy(message = t?.message ?: "TDLib error", isLoading = false)
        }
    }

    private fun setTdlibParameters() {
        val storedApiId = credentialsStore.getApiId()
        val storedApiHash = credentialsStore.getApiHash()
        val buildApiId = BuildConfig.TELEGRAM_API_ID.toIntOrNull() ?: 0
        val buildApiHash = BuildConfig.TELEGRAM_API_HASH
        val apiId = when {
            !storedApiId.isNullOrBlank() -> storedApiId.toIntOrNull() ?: 0
            else -> buildApiId
        }
        val apiHash = if (!storedApiHash.isNullOrBlank()) storedApiHash else buildApiHash
        if (apiId == 0 || apiHash.isBlank()) {
            _uiState.update {
                it.copy(
                    step = AuthStep.EnterApiKeys,
                    apiId = storedApiId.orEmpty(),
                    apiHash = storedApiHash.orEmpty(),
                    message = "Enter your Telegram API ID and Hash",
                    isLoading = false
                )
            }
            return
        }
        val databaseDir = File(context.filesDir, "tdlib").absolutePath
        val filesDir = File(context.filesDir, "tdlib_files").absolutePath

        val parameters = TdApi.SetTdlibParameters(
            false,
            databaseDir,
            filesDir,
            byteArrayOf(),
            true,
            true,
            true,
            false,
            apiId,
            apiHash,
            "en",
            "Android TV",
            Build.VERSION.RELEASE ?: "Android",
            BuildConfig.VERSION_NAME
        )
        client.send(parameters) { result ->
            handleResult(result)
        }
    }
}
