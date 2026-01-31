package com.saiesh.tele.presentation.auth.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.auth.AuthStep
import com.saiesh.tele.presentation.auth.vm.AuthViewModel
import kotlinx.coroutines.launch

class AuthFragment : Fragment(R.layout.fragment_auth) {
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.auth_title)
        val message = view.findViewById<TextView>(R.id.auth_message)
        val progress = view.findViewById<ProgressBar>(R.id.auth_progress)
        val apiIdInput = view.findViewById<EditText>(R.id.auth_api_id_input)
        val apiHashInput = view.findViewById<EditText>(R.id.auth_api_hash_input)
        val phoneInput = view.findViewById<EditText>(R.id.auth_phone_input)
        val codeInput = view.findViewById<EditText>(R.id.auth_code_input)
        val passwordInput = view.findViewById<EditText>(R.id.auth_password_input)
        val submit = view.findViewById<Button>(R.id.auth_submit_button)

        apiIdInput.addTextChangedListener { viewModel.onApiIdChange(it?.toString().orEmpty()) }
        apiHashInput.addTextChangedListener { viewModel.onApiHashChange(it?.toString().orEmpty()) }
        phoneInput.addTextChangedListener { viewModel.onPhoneChange(it?.toString().orEmpty()) }
        codeInput.addTextChangedListener { viewModel.onCodeChange(it?.toString().orEmpty()) }
        passwordInput.addTextChangedListener { viewModel.onPasswordChange(it?.toString().orEmpty()) }

        submit.setOnClickListener {
            when (viewModel.uiState.value.step) {
                AuthStep.EnterApiKeys -> viewModel.submitApiKeys()
                AuthStep.EnterPhone -> viewModel.submitPhone()
                AuthStep.EnterCode -> viewModel.submitCode()
                AuthStep.EnterPassword -> viewModel.submitPassword(passwordInput.text.toString())
                else -> Unit
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    title.text = if (state.step == AuthStep.EnterApiKeys) {
                        getString(R.string.auth_api_title)
                    } else {
                        getString(R.string.auth_title)
                    }
                    message.text = state.message.orEmpty()
                    message.visibility = if (state.message.isNullOrBlank()) View.GONE else View.VISIBLE
                    progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    apiIdInput.visibility = if (state.step == AuthStep.EnterApiKeys) View.VISIBLE else View.GONE
                    apiHashInput.visibility = if (state.step == AuthStep.EnterApiKeys) View.VISIBLE else View.GONE
                    phoneInput.visibility = if (state.step == AuthStep.EnterPhone) View.VISIBLE else View.GONE
                    codeInput.visibility = if (state.step == AuthStep.EnterCode) View.VISIBLE else View.GONE
                    passwordInput.visibility = if (state.step == AuthStep.EnterPassword) View.VISIBLE else View.GONE
                    submit.visibility = if (state.step == AuthStep.Authorized || state.step == AuthStep.Loading) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                    if (state.step == AuthStep.EnterApiKeys && apiIdInput.text.toString() != state.apiId) {
                        apiIdInput.setText(state.apiId)
                    }
                    if (state.step == AuthStep.EnterApiKeys && apiHashInput.text.toString() != state.apiHash) {
                        apiHashInput.setText(state.apiHash)
                    }
                    if (state.step == AuthStep.EnterPhone && phoneInput.text.toString() != state.phone) {
                        phoneInput.setText(state.phone)
                    }
                    if (state.step == AuthStep.EnterCode && codeInput.text.toString() != state.code) {
                        codeInput.setText(state.code)
                    }
                    if (state.step == AuthStep.EnterPassword && passwordInput.text.toString() != state.password) {
                        passwordInput.setText(state.password)
                    }

                    submit.text = when (state.step) {
                        AuthStep.EnterApiKeys -> getString(R.string.auth_submit_api)
                        AuthStep.EnterPhone -> getString(R.string.auth_submit_phone)
                        AuthStep.EnterCode -> getString(R.string.auth_submit_code)
                        AuthStep.EnterPassword -> getString(R.string.auth_submit_password)
                        else -> getString(R.string.auth_submit)
                    }
                }
            }
        }
    }
}
