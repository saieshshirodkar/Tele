package com.saiesh.tele.domain.model

data class MediaUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedChatTitle: String = "Saved Messages",
    val selectedChatId: Long? = null,
    val isSavedMessagesSelected: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val nextFromMessageId: Long = 0L,
    val videoChats: List<VideoChatItem> = emptyList(),
    val isSidebarLoading: Boolean = false,
    val sidebarError: String? = null,
    val focusVersion: Int = 0,
)
