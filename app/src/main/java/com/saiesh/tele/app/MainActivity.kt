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
import com.saiesh.tele.domain.model.auth.AuthStep
import com.saiesh.tele.presentation.auth.vm.AuthViewModel
import com.saiesh.tele.presentation.media.ui.BrowseFragment
import com.saiesh.tele.presentation.search.ui.SearchFragment
import kotlinx.coroutines.launch

/**
 * Loads [AuthFragment] or [BrowseFragment] based on auth state.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupExitHandler()
        val authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiState.collect { state ->
                    val current = supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                    if (state.step == AuthStep.Authorized) {
                        if (current !is BrowseFragment && current !is SearchFragment) {
                            showBrowse()
                        }
                    } else if (current !is AuthFragment) {
                        supportFragmentManager.popBackStack()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.main_browse_fragment, AuthFragment())
                            .commit()
                    }
                }
            }
        }
    }

    fun showSearch() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_browse_fragment, SearchFragment())
            .addToBackStack("search")
            .commit()
    }

    fun showBrowse() {
        supportFragmentManager.popBackStack("search", 0)
        val current = supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
        if (current !is BrowseFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, BrowseFragment())
                .commit()
        }
    }

    private fun setupExitHandler() {
        var waitingForExit = false
        val handler = Handler(Looper.getMainLooper())
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
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
        )
    }
}