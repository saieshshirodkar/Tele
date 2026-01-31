package com.saiesh.tele.presentation.search.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.saiesh.tele.R
import com.saiesh.tele.app.MainActivity
import com.saiesh.tele.presentation.search.adapter.SearchResultsAdapter
import com.saiesh.tele.presentation.search.vm.SearchViewModel
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search) {
    private val viewModel: SearchViewModel by activityViewModels()
    private lateinit var adapter: SearchResultsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val queryInput = view.findViewById<EditText>(R.id.search_query)
        val progress = view.findViewById<ProgressBar>(R.id.search_progress)
        val message = view.findViewById<TextView>(R.id.search_message)
        val results = view.findViewById<RecyclerView>(R.id.search_results)

        adapter = SearchResultsAdapter { result ->
            viewModel.selectResult(result)
        }
        results.layoutManager = LinearLayoutManager(requireContext())
        results.adapter = adapter

        queryInput.post {
            queryInput.requestFocus()
            val inputManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.showSoftInput(queryInput, InputMethodManager.SHOW_IMPLICIT)
        }

        queryInput.addTextChangedListener { viewModel.updateQuery(it?.toString().orEmpty()) }
        queryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.performSearch(queryInput.text.toString())
                val inputManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(queryInput.windowToken, 0)
                true
            } else {
                false
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progress.isVisible = state.isSearching
                    val hasQuery = state.query.isNotBlank()
                    val errorText = when {
                        state.error != null && state.hasSearched -> state.error
                        state.hasSearched && hasQuery && state.results.isEmpty() && !state.isSearching -> getString(R.string.search_empty)
                        else -> null
                    }
                    message.text = errorText.orEmpty()
                    message.isVisible = !errorText.isNullOrBlank()
                    if (queryInput.text.toString() != state.query) {
                        queryInput.setText(state.query)
                    }
                    adapter.submit(if (state.hasSearched) state.results else emptyList())
                    if (!state.isSearching && state.results.isNotEmpty() && viewModel.consumeFocusFirstResult()) {
                        results.post {
                            results.scrollToPosition(0)
                            results.post {
                                results.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                                    ?: results.getChildAt(0)?.requestFocus()
                            }
                        }
                    }
                    if (viewModel.consumeRefreshMedia()) {
                        (activity as? MainActivity)?.showBrowse()
                    }
                }
            }
        }
    }
}
