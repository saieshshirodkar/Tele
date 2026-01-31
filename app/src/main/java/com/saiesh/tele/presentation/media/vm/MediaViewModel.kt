package com.saiesh.tele.presentation.media.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saiesh.tele.data.repository.media.SavedMessagesRepository
import com.saiesh.tele.domain.model.media.MediaItem
import com.saiesh.tele.domain.model.media.MediaUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaViewModel(
    private val repository: SavedMessagesRepository = SavedMessagesRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState
    private val pageSize = 30

    fun loadIfNeeded() {
        if (_uiState.value.isLoading || _uiState.value.items.isNotEmpty()) {
            return
        }
        load()
    }

    fun load() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                selectedChatTitle = "Saved Messages",
                selectedChatId = null,
                isSavedMessagesSelected = true,
                hasMore = true
            )
        }
        repository.loadLatestMediaPaged(pageSize, null) { items, nextFromMessageId, error ->
            _uiState.update { current ->
                current.copy(
                    items = items,
                    isLoading = false,
                    error = error,
                    hasMore = nextFromMessageId != 0L,
                    nextFromMessageId = nextFromMessageId
                )
            }
            fetchThumbnails(items)
            retryMissingThumbnails()
        }
    }

    fun loadChat(chatId: Long, title: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                selectedChatTitle = title,
                selectedChatId = chatId,
                isSavedMessagesSelected = false,
                hasMore = true
            )
        }
        repository.loadChatMediaPaged(chatId, pageSize, null) { items, nextFromMessageId, error ->
            _uiState.update { current ->
                current.copy(
                    items = items,
                    isLoading = false,
                    error = error,
                    hasMore = nextFromMessageId != 0L,
                    nextFromMessageId = nextFromMessageId
                )
            }
            fetchThumbnails(items)
            retryMissingThumbnails()
        }
    }

    fun loadMoreIfNeeded() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.items.isEmpty()) {
            return
        }
        val nextFromMessageId = state.nextFromMessageId
        if (nextFromMessageId == 0L) {
            _uiState.update { it.copy(hasMore = false, isLoadingMore = false) }
            return
        }
        _uiState.update { it.copy(isLoadingMore = true) }
        if (state.isSavedMessagesSelected) {
            repository.loadLatestMediaPaged(pageSize, nextFromMessageId) { items, nextFromId, error ->
                handleLoadMore(items, nextFromId, error)
            }
        } else {
            val chatId = state.selectedChatId ?: return
            repository.loadChatMediaPaged(chatId, pageSize, nextFromMessageId) { items, nextFromId, error ->
                handleLoadMore(items, nextFromId, error)
            }
        }
    }

    private fun handleLoadMore(items: List<MediaItem>, nextFromMessageId: Long, error: String?) {
        _uiState.update { current ->
            val merged = (current.items + items).distinctBy { it.messageId }
            current.copy(
                items = merged,
                isLoadingMore = false,
                error = error,
                hasMore = nextFromMessageId != 0L,
                nextFromMessageId = nextFromMessageId
            )
        }
        if (items.isNotEmpty()) {
            fetchThumbnails(items)
            retryMissingThumbnails()
        }
    }

    private fun fetchThumbnails(items: List<MediaItem>) {
        items
            .asSequence()
            .filter { it.thumbnailFileId != null && it.thumbnailPath.isNullOrBlank() }
            .forEach { item ->
                repository.fetchThumbnailPath(item.thumbnailFileId!!) { path ->
                    if (!path.isNullOrBlank()) {
                        updateThumbnailPath(item.messageId, path)
                    }
                }
            }
    }

    private fun retryMissingThumbnails() {
        viewModelScope.launch {
            delay(1500)
            val items = _uiState.value.items
            items
                .asSequence()
                .filter { it.thumbnailFileId != null && it.thumbnailPath.isNullOrBlank() }
                .forEach { item ->
                    repository.fetchThumbnailPath(item.thumbnailFileId!!) { path ->
                        if (!path.isNullOrBlank()) {
                            updateThumbnailPath(item.messageId, path)
                        }
                    }
                }
        }
    }

    fun loadVideoChatsIfNeeded() {
        if (_uiState.value.isSidebarLoading || _uiState.value.videoChats.isNotEmpty()) {
            return
        }
        loadVideoChats()
    }

    fun loadVideoChats() {
        _uiState.update { it.copy(isSidebarLoading = true, sidebarError = null) }
        repository.loadVideoChats(40) { chats, error ->
            _uiState.update { current ->
                current.copy(
                    videoChats = chats,
                    isSidebarLoading = false,
                    sidebarError = error
                )
            }
        }
    }

    fun requestFastLink(item: MediaItem, onResult: (String?, String?) -> Unit) {
        repository.requestFastLink(item, onResult)
    }

    fun onItemFocused(item: MediaItem) {
        val fileId = item.thumbnailFileId
        if (fileId != null && item.thumbnailPath.isNullOrBlank()) {
            repository.fetchThumbnailPath(fileId) { path ->
                if (!path.isNullOrBlank()) {
                    updateThumbnailPath(item.messageId, path)
                }
            }
        }
    }

    private fun updateThumbnailPath(messageId: Long, path: String) {
        _uiState.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.messageId == messageId) item.copy(thumbnailPath = path) else item
                }
            )
        }
    }
}
