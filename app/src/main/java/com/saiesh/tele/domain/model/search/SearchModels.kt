package com.saiesh.tele.domain.model.search

import com.saiesh.tele.domain.model.media.MediaItem

data class SearchQueryResult(
    val title: String,
    val callbackData: ByteArray,
    val chatId: Long,
    val messageId: Long,
    val isPagination: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchQueryResult> = emptyList(),
    val error: String? = null,
    val isOverlayVisible: Boolean = false,
    val refreshMedia: Boolean = false,
    val hasSearched: Boolean = false,
    val focusFirstResult: Boolean = false
)

sealed class SearchBotResponse {
    data class Results(val results: List<SearchQueryResult>) : SearchBotResponse()
    data class Media(val item: MediaItem) : SearchBotResponse()
    data class Error(val message: String) : SearchBotResponse()
}
