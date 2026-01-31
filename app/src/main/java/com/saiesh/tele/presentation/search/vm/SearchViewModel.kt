package com.saiesh.tele.presentation.search.vm

import androidx.lifecycle.ViewModel
import com.saiesh.tele.data.repository.media.SavedMessagesRepository
import com.saiesh.tele.domain.model.media.MediaItem
import com.saiesh.tele.domain.model.search.SearchBotResponse
import com.saiesh.tele.domain.model.search.SearchQueryResult
import com.saiesh.tele.domain.model.search.SearchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SearchViewModel(
    private val repository: SavedMessagesRepository = SavedMessagesRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    fun openOverlay() {
        _uiState.update { it.copy(isOverlayVisible = true, error = null) }
    }

    fun closeOverlay() {
        _uiState.update {
            it.copy(
                isOverlayVisible = false,
                error = null,
                results = emptyList(),
                query = "",
                hasSearched = false,
                focusFirstResult = false
            )
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
    }

    fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    error = "Enter at least 2 characters",
                    hasSearched = true,
                    results = emptyList()
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                isSearching = true,
                error = null,
                results = emptyList(),
                query = trimmed,
                hasSearched = true
            )
        }
        repository.searchProBot(trimmed) { response ->
            handleSearchResponse(response)
        }
    }

    fun selectResult(result: SearchQueryResult) {
        if (_uiState.value.isSearching) return
        _uiState.update {
            it.copy(
                isSearching = true,
                error = null,
                focusFirstResult = result.isPagination
            )
        }
        repository.submitProBotSelection(result) { response ->
            handleSearchResponse(response)
        }
    }

    fun consumeFocusFirstResult(): Boolean {
        val shouldFocus = _uiState.value.focusFirstResult
        if (shouldFocus) {
            _uiState.update { it.copy(focusFirstResult = false) }
        }
        return shouldFocus
    }

    fun consumeRefreshMedia(): Boolean {
        val shouldRefresh = _uiState.value.refreshMedia
        if (shouldRefresh) {
            _uiState.update { it.copy(refreshMedia = false) }
        }
        return shouldRefresh
    }

    private fun handleSearchResponse(response: SearchBotResponse) {
        when (response) {
            is SearchBotResponse.Results -> {
                val filteredResults = response.results.filterNot { result ->
                    result.title.contains("srt", ignoreCase = true)
                }
                _uiState.update { current ->
                    current.copy(
                        isSearching = false,
                        results = filteredResults,
                        error = null,
                        hasSearched = true
                    )
                }
            }
            is SearchBotResponse.Error -> {
                _uiState.update {
                    current -> current.copy(isSearching = false, error = response.message, hasSearched = true)
                }
            }
            is SearchBotResponse.Media -> saveToSavedMessages(response.item)
        }
    }

    private fun saveToSavedMessages(item: MediaItem) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        repository.saveSearchMediaToSavedMessages(item) { error ->
            _uiState.update { current ->
                if (error != null) {
                    current.copy(isSearching = false, error = error)
                } else {
                    current.copy(
                        isSearching = false,
                        isOverlayVisible = false,
                        results = emptyList(),
                        query = "",
                        hasSearched = false,
                        focusFirstResult = false,
                        refreshMedia = true
                    )
                }
            }
        }
    }
}
