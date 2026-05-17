package com.saiesh.tele.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.saiesh.tele.R
import com.saiesh.tele.presentation.auth.ui.AuthFragment
import com.saiesh.tele.domain.model.AuthStep
import com.saiesh.tele.presentation.auth.vm.AuthViewModel
import com.saiesh.tele.presentation.media.ui.BrowseFragment
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupExitHandler()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, BrowseFragment())
                .commitNow()
        }
        val authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiState.collect { state ->
                    val current = supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                    if (state.step == AuthStep.Authorized) {
                        if (current !is BrowseFragment) {
                            showBrowse()
                        }
                    } else if (state.step != AuthStep.Loading && current !is AuthFragment) {
                        supportFragmentManager.popBackStack()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.main_browse_fragment, AuthFragment())
                            .commit()
                    }
                }
            }
        }
    }

    fun showBrowse() {
        val current = supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
        if (current !is BrowseFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, BrowseFragment())
                .commit()
        }
    }

    private var exitCallback: OnBackPressedCallback? = null

    private fun setupExitHandler() {
        if (exitCallback != null) return
        var waitingForExit = false
        val handler = Handler(Looper.getMainLooper())
        exitCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                if (fragment is com.saiesh.tele.presentation.media.ui.BrowseFragment) {
                    if (fragment.isShowingHeaders()) {
                        if (waitingForExit) {
                            finish()
                            return
                        }
                        waitingForExit = true
                        Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT)
                            .show()
                        handler.removeCallbacksAndMessages(null)
                        handler.postDelayed({ waitingForExit = false }, 2000)
                        return
                    }
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
        onBackPressedDispatcher.addCallback(this, exitCallback!!)
    }
}