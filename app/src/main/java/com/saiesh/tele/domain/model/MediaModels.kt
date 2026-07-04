package com.saiesh.tele.domain.model

import java.io.Serializable

const val SAVED_MESSAGES_TITLE = "Saved Messages"

enum class MediaType {
    Photo,
    Video
}

data class MediaItem(
    val chatId: Long,
    val messageId: Long,
    val date: Int,
    val type: MediaType,
    val title: String,
    val fileId: Int?,
    val thumbnailFileId: Int? = null,
    val thumbnailPath: String? = null,
    val miniThumbnailBytes: ByteArray? = null,
    val thumbnailWidth: Int = 0,
    val thumbnailHeight: Int = 0,
    val durationSeconds: Int = 0,
    val fileSizeBytes: Long = 0
) : Serializable

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
    val focusVersion: Int = 0
)

data class VideoChatItem(
    val chatId: Long,
    val title: String,
    val isSavedMessages: Boolean,
    val isSelected: Boolean = false,
    val order: Long = 0L
) : Serializable
