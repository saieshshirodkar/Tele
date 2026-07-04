package com.saiesh.tele.domain.repository

import com.saiesh.tele.domain.model.MediaItem
import com.saiesh.tele.domain.model.VideoChatItem

interface TeleRepository {
    fun getCachedSavedMessagesChatId(): Long?

    fun loadChatsWithVideos(
        limit: Int,
        selectedChatId: Long?,
        onResult: (List<VideoChatItem>, String?) -> Unit
    )

    fun loadLatestMediaPaged(
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    )

    fun loadChatMediaPaged(
        chatId: Long,
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    )

    fun getMessage(
        chatId: Long,
        messageId: Long,
        onResult: (MediaItem?, String?) -> Unit
    )

    fun deleteMessage(
        chatId: Long,
        messageId: Long,
        onResult: (String?) -> Unit
    )

    fun fetchThumbnailPath(
        fileId: Int,
        onResult: (String?) -> Unit
    )

    fun requestFastLink(
        item: MediaItem,
        onResult: (String?, String?) -> Unit
    )

    fun shutdown()
}
