package com.saiesh.tele.data.repository

import com.saiesh.tele.core.tdlib.client.TdLibClient
import com.saiesh.tele.domain.model.MediaItem
import com.saiesh.tele.domain.model.VideoChatItem
import org.drinkless.tdlib.TdApi
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

internal const val BOT_USERNAME = "StreamVaultProBot"
internal const val HANDLER_TIMEOUT_SECONDS = 30L

class SavedMessagesRepository {
    companion object {
        const val TAG = "SavedMessagesRepo"
    }

    internal val client = TdLibClient.client
    internal val handlerScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    internal var cachedMeId: Long? = null
    internal var cachedSavedMessagesChatId: Long? = null

    fun shutdown() {
        handlerScheduler.shutdown()
        cachedMeId = null
        cachedSavedMessagesChatId = null
    }

    fun loadLatestMedia(limit: Int, onResult: (List<MediaItem>, String?) -> Unit) =
        loadLatestMediaInternal(limit, onResult)

    fun loadLatestMediaPaged(
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) = loadLatestMediaPagedInternal(limit, fromMessageId, onResult)

    fun getMessage(chatId: Long, messageId: Long, onResult: (MediaItem?, String?) -> Unit) {
        client.send(TdApi.GetMessage(chatId, messageId)) { result ->
            when (result) {
                is TdApi.Message -> {
                    val item = mapMessageToMediaInternal(result)
                    onResult(item, null)
                }
                is TdApi.Error -> onResult(null, result.message)
                else -> onResult(null, "Failed to load message")
            }
        }
    }

    fun loadChatMedia(chatId: Long, limit: Int, onResult: (List<MediaItem>, String?) -> Unit) =
        loadChatMediaInternal(chatId, limit, onResult)

    fun loadChatMediaPaged(
        chatId: Long,
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) = loadChatMediaPagedInternal(chatId, limit, fromMessageId, onResult)

    fun loadVideoChats(limit: Int, selectedChatId: Long?, onResult: (List<VideoChatItem>, String?) -> Unit) =
        loadVideoChatsInternal(limit, selectedChatId, onResult)

    fun fetchThumbnailPath(fileId: Int, onResult: (String?) -> Unit) =
        fetchThumbnailPathInternal(fileId, onResult)

    fun requestFastLink(item: MediaItem, onResult: (String?, String?) -> Unit) =
        requestFastLinkInternal(item, onResult)

    fun deleteMessage(chatId: Long, messageId: Long, onResult: (String?) -> Unit) =
        deleteMessageInternal(chatId, messageId, onResult)
}