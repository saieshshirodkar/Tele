package com.saiesh.tele.presentation.media.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saiesh.tele.core.cache.TeleCache
import com.saiesh.tele.core.tdlib.client.TdLibClient
import com.saiesh.tele.data.repository.TeleRepositoryImpl
import com.saiesh.tele.data.mapper.MediaMapper
import com.saiesh.tele.domain.model.MediaItem
import com.saiesh.tele.domain.model.MediaUiState
import com.saiesh.tele.domain.model.SAVED_MESSAGES_TITLE
import com.saiesh.tele.domain.repository.TeleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class MediaViewModel(
    private val repository: TeleRepository = TeleRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState
    private val pageSize = 30
    private var isInitialized = false

    private fun isMessageInSelectedChat(chatId: Long): Boolean {
        val state = _uiState.value
        return if (state.isSavedMessagesSelected) {
            repository.getCachedSavedMessagesChatId() == chatId
        } else {
            chatId == state.selectedChatId
        }
    }

    private var sidebarRefreshJob: Job? = null

    private val newMessageHandler: (TdApi.UpdateNewMessage) -> Unit = { update ->
        val message = update.message
        if (MediaMapper.isMessageVideoType(message)) {
            val chatInSidebar = _uiState.value.videoChats.any { it.chatId == message.chatId }
            if (!chatInSidebar) {
                triggerSidebarRefresh()
            }
            if (isMessageInSelectedChat(message.chatId)) {
                val item = MediaMapper.mapMessage(message)
                item?.let { newItem ->
                    _uiState.update { current ->
                        val exists = current.items.any { it.messageId == newItem.messageId }
                        if (exists) current
                        else current.copy(
                            items = listOf(newItem) + current.items,
                            focusVersion = current.focusVersion + 1
                        )
                    }
                    fetchThumbnails(listOf(item))
                }
            }
        }
    }

    private val deleteMessageHandler: (TdApi.UpdateDeleteMessages) -> Unit = { update ->
        if (update.isPermanent) {
            val inSelected = isMessageInSelectedChat(update.chatId)
            if (inSelected) {
                _uiState.update { current ->
                    current.copy(
                        items = current.items.filterNot { update.messageIds.contains(it.messageId) },
                        focusVersion = current.focusVersion + 1
                    )
                }
            }
            triggerSidebarRefresh()
        }
    }

    private var chatUpdateJob: Job? = null

    private val chatUpdateHandler: (TdApi.Object?) -> Unit = { update ->
        when (update) {
            is TdApi.UpdateNewChat,
            is TdApi.UpdateChatAddedToList,
            is TdApi.UpdateChatRemovedFromList,
            is TdApi.UpdateChatPosition -> triggerSidebarRefresh()
        }
    }

    private fun triggerSidebarRefresh() {
        sidebarRefreshJob?.cancel()
        sidebarRefreshJob = viewModelScope.launch {
            delay(500)
            loadVideoChats()
        }
    }

    init {
        TdLibClient.addNewMessageHandler(newMessageHandler)
        TdLibClient.addDeleteMessageHandler(deleteMessageHandler)
        TdLibClient.addUpdateHandler(chatUpdateHandler)
    }

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        loadCachedData()
        loadVideoChats()
        refresh()
    }

    private fun loadCachedData() {
        val cachedChats = TeleCache.loadVideoChats()
        val cachedItems = TeleCache.loadMediaItems("saved")
        if (cachedChats != null || cachedItems != null) {
            _uiState.update {
                it.copy(
                    videoChats = cachedChats ?: it.videoChats,
                    items = cachedItems ?: it.items
                )
            }
        }
    }

    override fun onCleared() {
        TdLibClient.removeNewMessageHandler(newMessageHandler)
        TdLibClient.removeDeleteMessageHandler(deleteMessageHandler)
        TdLibClient.removeUpdateHandler(chatUpdateHandler)
        repository.shutdown()
        super.onCleared()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                selectedChatTitle = SAVED_MESSAGES_TITLE,
                selectedChatId = null,
                isSavedMessagesSelected = true,
                hasMore = true,
                nextFromMessageId = 0L
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
            if (error == null) {
                TeleCache.saveMediaItems(items, "saved")
            }
            fetchThumbnails(items)
        }
    }

    fun loadIfNeeded() {
        if (_uiState.value.isLoading) return
        if (_uiState.value.items.isEmpty()) {
            refresh()
        }
    }

    fun load() {
        _uiState.update {
            it.copy(
                isSavedMessagesSelected = true,
                selectedChatId = null,
                selectedChatTitle = SAVED_MESSAGES_TITLE
            )
        }
        updateSidebarSelection()
        refresh()
    }

    fun loadChat(chatId: Long, title: String) {
        val chatKey = chatId.toString()
        val cachedItems = TeleCache.loadMediaItems(chatKey)
        if (cachedItems != null) {
            _uiState.update {
                it.copy(items = cachedItems)
            }
        }
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                selectedChatTitle = title,
                selectedChatId = chatId,
                isSavedMessagesSelected = false,
                hasMore = true,
                nextFromMessageId = 0L
            )
        }
        updateSidebarSelection()
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
            if (error == null) {
                TeleCache.saveMediaItems(items, chatKey)
            }
            fetchThumbnails(items)
        }
    }

    fun loadMoreIfNeeded() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.items.isEmpty()) return
        val nextFromMessageId = state.nextFromMessageId
        if (nextFromMessageId == 0L) {
            _uiState.update { it.copy(hasMore = false, isLoadingMore = false) }
            return
        }
        _uiState.update { it.copy(isLoadingMore = true) }
        if (state.isSavedMessagesSelected) {
            repository.loadLatestMediaPaged(pageSize, nextFromMessageId) { items, nextFromId, error ->
                val current = _uiState.value
                if (!current.isLoadingMore) return@loadLatestMediaPaged
                handleLoadMore(items, nextFromId, error)
            }
        } else {
            val chatId = state.selectedChatId ?: return
            repository.loadChatMediaPaged(chatId, pageSize, nextFromMessageId) { items, nextFromId, error ->
                val current = _uiState.value
                if (!current.isLoadingMore) return@loadChatMediaPaged
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
        }
    }

    private fun fetchThumbnails(items: List<MediaItem>) {
        items
            .asSequence()
            .filter { it.thumbnailFileId != null && it.thumbnailPath.isNullOrBlank() }
            .forEach { item ->
                repository.fetchThumbnailPath(item.thumbnailFileId!!) { path ->
                    if (!path.isNullOrBlank()) {
                        updateThumbnailPath(item.chatId, item.messageId, path)
                    }
                }
            }
    }

    fun loadVideoChatsIfNeeded() {
        if (_uiState.value.isSidebarLoading) return
        loadVideoChats()
    }

    fun refreshVideoChats() {
        loadVideoChats()
    }

    private fun updateSidebarSelection() {
        val selectedId = if (_uiState.value.isSavedMessagesSelected) null else _uiState.value.selectedChatId
        _uiState.update { current ->
            val updated = current.videoChats.map { chat ->
                val isSelected = if (selectedId != null) chat.chatId == selectedId else chat.isSavedMessages
                chat.copy(isSelected = isSelected)
            }
            current.copy(videoChats = updated)
        }
    }

    private fun loadVideoChats() {
        val selectedId = if (_uiState.value.isSavedMessagesSelected) null else _uiState.value.selectedChatId
        _uiState.update { current ->
            val updated = current.videoChats.map { chat ->
                val isSelected = if (selectedId != null) chat.chatId == selectedId else chat.isSavedMessages
                chat.copy(isSelected = isSelected)
            }
            current.copy(videoChats = updated, isSidebarLoading = true, sidebarError = null)
        }
        repository.loadChatsWithVideos(15, selectedId) { chats, error ->
            _uiState.update { current ->
                current.copy(
                    videoChats = chats,
                    isSidebarLoading = false,
                    sidebarError = error
                )
            }
            if (error == null) {
                TeleCache.saveVideoChats(chats)
            }
        }
    }

    fun requestFastLink(item: MediaItem, onResult: (String?, String?) -> Unit) {
        repository.requestFastLink(item, onResult)
    }

    fun deleteMediaItem(item: MediaItem, onResult: (String?) -> Unit) {
        _uiState.update { it.copy(error = null) }
        repository.deleteMessage(item.chatId, item.messageId) { error ->
            if (error != null) {
                _uiState.update { current -> current.copy(error = error) }
                onResult(error)
            } else {
                _uiState.update { current ->
                    current.copy(items = current.items.filterNot { it.messageId == item.messageId })
                }
                onResult(null)
            }
        }
    }

    fun onItemFocused(item: MediaItem) {
        val fileId = item.thumbnailFileId
        if (fileId != null && item.thumbnailPath.isNullOrBlank()) {
            repository.fetchThumbnailPath(fileId) { path ->
                if (!path.isNullOrBlank()) {
                    updateThumbnailPath(item.chatId, item.messageId, path)
                }
            }
        }
    }

    private fun updateThumbnailPath(chatId: Long, messageId: Long, path: String) {
        _uiState.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.chatId == chatId && item.messageId == messageId) item.copy(thumbnailPath = path) else item
                }
            )
        }
    }
}
