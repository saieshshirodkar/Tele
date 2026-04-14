package com.saiesh.tele.data.repository

import com.saiesh.tele.domain.model.MediaItem
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicInteger

internal fun SavedMessagesRepository.searchMediaInternal(
    chatId: Long,
    limit: Int,
    fromMessageId: Long?,
    onResult: (List<MediaItem>, String?) -> Unit
) {
    val perFilterLimit = (limit * 3).coerceAtMost(100)
    val results = mutableListOf<MediaItem>()
    val pendingRequests = AtomicInteger(5)
    val lock = Any()
    var error: String? = null

    fun collect(items: List<MediaItem>?, err: String?) {
        synchronized(lock) {
            if (items != null) {
                results.addAll(items)
            }
            if (err != null && error == null) {
                error = err
            }
            if (pendingRequests.decrementAndGet() == 0) {
                val merged = results
                    .distinctBy { it.messageId }
                    .sortedByDescending { it.date }
                onResult(merged.take(limit), error)
            }
        }
    }

    searchWithFilterInternal(chatId, perFilterLimit, fromMessageId, TdApi.SearchMessagesFilterVideo()) { items, err ->
        collect(items, err)
    }
    searchWithFilterInternal(chatId, perFilterLimit, fromMessageId, TdApi.SearchMessagesFilterDocument()) { items, err ->
        collect(items, err)
    }
    searchWithFilterInternal(chatId, perFilterLimit, fromMessageId, TdApi.SearchMessagesFilterVideoNote()) { items, err ->
        collect(items, err)
    }
    searchWithFilterInternal(chatId, perFilterLimit, fromMessageId, TdApi.SearchMessagesFilterAnimation()) { items, err ->
        collect(items, err)
    }
    searchWithFilterInternal(chatId, perFilterLimit, fromMessageId, TdApi.SearchMessagesFilterPhotoAndVideo()) { items, err ->
        collect(items, err)
    }
}

internal fun SavedMessagesRepository.searchWithFilterInternal(
    chatId: Long,
    limit: Int,
    fromMessageId: Long?,
    filter: TdApi.SearchMessagesFilter,
    onItems: (List<MediaItem>, String?) -> Unit
) {
    val offset = if (fromMessageId == null || fromMessageId == 0L) 0 else 1
    val query = TdApi.SearchChatMessages(
        chatId,
        null,
        "",
        null,
        fromMessageId ?: 0,
        offset,
        limit,
        filter
    )
    client.send(query) { result ->
        when (result) {
            is TdApi.FoundChatMessages -> {
                val items = result.messages?.mapNotNull(::mapMessageToMediaInternal).orEmpty()
                onItems(items, null)
            }
            is TdApi.Error -> onItems(emptyList(), result.message)
            else -> onItems(emptyList(), "Unknown response from TDLib")
        }
    }
}
