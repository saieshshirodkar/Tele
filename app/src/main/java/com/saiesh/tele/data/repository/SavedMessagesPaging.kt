package com.saiesh.tele.data.repository

import android.util.Log
import com.saiesh.tele.domain.model.MediaItem
import org.drinkless.tdlib.TdApi

internal fun SavedMessagesRepository.loadLatestMediaInternal(
    limit: Int,
    onResult: (List<MediaItem>, String?) -> Unit
) {
    val cached = cachedMeId
    if (cached != null) {
        loadSavedMessagesChatInternal(cached, limit, onResult)
        return
    }
    client.send(TdApi.GetMe()) { meResult ->
        when (meResult) {
            is TdApi.User -> {
                cachedMeId = meResult.id
                loadSavedMessagesChatInternal(meResult.id, limit, onResult)
            }
            is TdApi.Error -> {
                Log.e(SavedMessagesRepository.TAG, "Failed to get user: ${meResult.message}")
                onResult(emptyList(), meResult.message)
            }
            else -> {
                Log.e(SavedMessagesRepository.TAG, "Unexpected response from GetMe")
                onResult(emptyList(), "Unexpected response from TDLib")
            }
        }
    }
}

internal fun SavedMessagesRepository.loadLatestMediaPagedInternal(
    limit: Int,
    fromMessageId: Long?,
    onResult: (List<MediaItem>, Long, String?) -> Unit
) {
    resolveSavedMessagesChatInternal { savedChatId: Long?, error: String? ->
        if (savedChatId == null) {
            onResult(emptyList(), 0L, error ?: "Saved Messages unavailable")
            return@resolveSavedMessagesChatInternal
        }
        loadMediaFromHistoryInternal(savedChatId, limit, fromMessageId, onResult)
    }
}

internal fun SavedMessagesRepository.loadChatMediaInternal(
    chatId: Long,
    limit: Int,
    onResult: (List<MediaItem>, String?) -> Unit
) {
    searchMediaInternal(chatId, limit, null, onResult)
}

internal fun SavedMessagesRepository.loadChatMediaPagedInternal(
    chatId: Long,
    limit: Int,
    fromMessageId: Long?,
    onResult: (List<MediaItem>, Long, String?) -> Unit
) {
    loadMediaFromHistoryInternal(chatId, limit, fromMessageId, onResult)
}

private fun SavedMessagesRepository.loadMediaFromHistoryInternal(
    chatId: Long,
    limit: Int,
    fromMessageId: Long?,
    onResult: (List<MediaItem>, Long, String?) -> Unit
) {
    val collected = mutableListOf<MediaItem>()

    fun finish(nextFromMessageId: Long, error: String? = null) {
        val ordered = collected
            .distinctBy { it.messageId }
            .sortedByDescending { it.date }
        val page = ordered.take(limit)
        onResult(page, nextFromMessageId, error)
    }

    fun fetch(nextFromId: Long) {
        val query = TdApi.GetChatHistory(chatId, nextFromId, 0, 100, false)
        client.send(query) { result ->
            when (result) {
                is TdApi.Messages -> {
                    val messages = result.messages?.toList().orEmpty()
                    if (messages.isEmpty()) {
                        finish(0L)
                        return@send
                    }
                    val trimmed = if (nextFromId != 0L && messages.first().id == nextFromId) {
                        messages.drop(1)
                    } else {
                        messages
                    }
                    if (trimmed.isEmpty()) {
                        finish(0L)
                        return@send
                    }
                    trimmed.forEach { message ->
                        val media = mapMessageToMediaInternal(message) ?: return@forEach
                        collected.add(media)
                    }
                    if (collected.size >= limit) {
                        val ordered = collected
                            .distinctBy { it.messageId }
                            .sortedByDescending { it.date }
                        val nextFromMessageId = ordered
                            .take(limit)
                            .lastOrNull()
                            ?.messageId
                            ?: 0L
                        finish(nextFromMessageId)
                        return@send
                    }
                    val lastMessageId = messages.last().id
                    if (lastMessageId == nextFromId) {
                        finish(0L)
                        return@send
                    }
                    fetch(lastMessageId)
                }
                is TdApi.Error -> {
                    Log.e(SavedMessagesRepository.TAG, "Failed to load chat history: ${result.message}")
                    finish(0L, result.message)
                }
                else -> {
                    Log.e(SavedMessagesRepository.TAG, "Unexpected response from GetChatHistory")
                    finish(0L, "Failed to load chat history")
                }
            }
        }
    }

    fetch(fromMessageId ?: 0L)
}
