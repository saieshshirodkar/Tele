package com.saiesh.tele.data.repository.media

import com.saiesh.tele.core.tdlib.client.TdLibClient
import com.saiesh.tele.domain.model.media.MediaItem
import com.saiesh.tele.domain.model.search.SearchBotResponse
import com.saiesh.tele.domain.model.search.SearchQueryResult
import com.saiesh.tele.domain.model.media.VideoChatItem
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

internal const val BOT_USERNAME = "FileToLinkV5Bot"
internal const val PRO_SEARCH_BOT_USERNAME = "ProSearchM11Bot"
internal const val HANDLER_TIMEOUT_SECONDS = 30L
internal const val SEARCH_TIMEOUT_SECONDS = 10L

class SavedMessagesRepository {
    internal val client = TdLibClient.client
    internal val handlerScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    internal var cachedMeId: Long? = null
    internal var cachedProSearchChatId: Long? = null
    internal var cachedSavedMessagesChatId: Long? = null

    fun loadLatestMedia(limit: Int, onResult: (List<MediaItem>, String?) -> Unit) =
        loadLatestMediaInternal(limit, onResult)

    fun loadLatestMediaPaged(
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) = loadLatestMediaPagedInternal(limit, fromMessageId, onResult)

    fun loadChatMedia(chatId: Long, limit: Int, onResult: (List<MediaItem>, String?) -> Unit) =
        loadChatMediaInternal(chatId, limit, onResult)

    fun loadChatMediaPaged(
        chatId: Long,
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) = loadChatMediaPagedInternal(chatId, limit, fromMessageId, onResult)

    fun loadVideoChats(limit: Int, onResult: (List<VideoChatItem>, String?) -> Unit) =
        loadVideoChatsInternal(limit, onResult)

    fun searchProBot(query: String, onResult: (SearchBotResponse) -> Unit) =
        searchProBotInternal(query, onResult)

    fun submitProBotSelection(result: SearchQueryResult, onResult: (SearchBotResponse) -> Unit) =
        submitProBotSelectionInternal(result, onResult)

    fun saveSearchMediaToSavedMessages(item: MediaItem, onResult: (String?) -> Unit) =
        saveSearchMediaInternal(item, onResult)

    fun fetchThumbnailPath(fileId: Int, onResult: (String?) -> Unit) =
        fetchThumbnailPathInternal(fileId, onResult)

    fun requestFastLink(item: MediaItem, onResult: (String?, String?) -> Unit) =
        requestFastLinkInternal(item, onResult)

    fun deleteMessage(chatId: Long, messageId: Long, onResult: (String?) -> Unit) =
        deleteMessageInternal(chatId, messageId, onResult)
}