package com.saiesh.tele.data.repository

import android.util.Log
import com.saiesh.tele.domain.model.SAVED_MESSAGES_TITLE
import com.saiesh.tele.domain.model.MediaType
import com.saiesh.tele.domain.model.VideoChatItem
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal fun SavedMessagesRepository.loadVideoChatsInternal(
    limit: Int,
    selectedChatId: Long?,
    onResult: (List<VideoChatItem>, String?) -> Unit
) {
    resolveSavedMessagesChatInternal { savedChatId, error ->
        if (savedChatId == null) {
            onResult(emptyList(), error ?: "Saved Messages unavailable")
            return@resolveSavedMessagesChatInternal
        }
        client.send(TdApi.GetChats(null, limit)) chatsSend@{ chatsResult ->
            val chats = chatsResult as? TdApi.Chats
            val chatIds = chats?.chatIds?.toList().orEmpty()
            if (chatIds.isEmpty()) {
                onResult(emptyList(), null)
                return@chatsSend
            }
            Log.d(SavedMessagesRepository.TAG, "Found ${chatIds.size} chats")
            val pending = AtomicInteger(chatIds.size)
            val lock = Any()
            val chatMap = mutableMapOf<Long, VideoChatItem>()
            fun finish() {
                val ordered = mutableListOf<VideoChatItem>()
                chatMap[savedChatId]?.let { ordered.add(it) }
                chatIds.forEach { chatId ->
                    if (chatId == savedChatId) return@forEach
                    chatMap[chatId]?.let { ordered.add(it) }
                }
                onResult(ordered, null)
            }
            chatIds.forEach { chatId ->
                client.send(TdApi.GetChat(chatId)) chatSend@{ chatResult ->
                    val chat = chatResult as? TdApi.Chat
                    val title = if (chatId == savedChatId) SAVED_MESSAGES_TITLE else chat?.title
                    if (title == null) {
                        if (pending.decrementAndGet() == 0) finish()
                        return@chatSend
                    }
                    hasVideoInChatInternal(chatId) { hasVideo ->
                        if (hasVideo || chatId == savedChatId) {
                            synchronized(lock) {
                                val isSelected = if (selectedChatId != null) chatId == selectedChatId else chatId == savedChatId
                                chatMap[chatId] = VideoChatItem(
                                    chatId = chatId,
                                    title = title,
                                    isSavedMessages = chatId == savedChatId,
                                    isSelected = isSelected
                                )
                            }
                        }
                        if (pending.decrementAndGet() == 0) finish()
                    }
                }
            }
        }
    }
}

internal fun SavedMessagesRepository.resolveSavedMessagesChatInternal(
    onResult: (Long?, String?) -> Unit
) {
    val cachedChatId = cachedSavedMessagesChatId
    if (cachedChatId != null) {
        onResult(cachedChatId, null)
        return
    }

    fun createChat(userId: Long) {
        client.send(TdApi.CreatePrivateChat(userId, false)) { chatResult ->
            when (chatResult) {
                is TdApi.Chat -> {
                    cachedSavedMessagesChatId = chatResult.id
                    onResult(chatResult.id, null)
                }
                is TdApi.Error -> onResult(null, chatResult.message)
                else -> onResult(null, "Failed to open Saved Messages")
            }
        }
    }

    val cachedUserId = cachedMeId
    if (cachedUserId != null) {
        createChat(cachedUserId)
        return
    }

    client.send(TdApi.GetMe()) { meResult ->
        when (meResult) {
            is TdApi.User -> {
                cachedMeId = meResult.id
                createChat(meResult.id)
            }
            is TdApi.Error -> onResult(null, meResult.message)
            else -> onResult(null, "Failed to load user")
        }
    }
}

internal fun SavedMessagesRepository.loadSavedMessagesChatInternal(
    userId: Long,
    limit: Int,
    onResult: (List<com.saiesh.tele.domain.model.MediaItem>, String?) -> Unit
) {
    client.send(TdApi.CreatePrivateChat(userId, false)) { chatResult ->
        when (chatResult) {
            is TdApi.Chat -> searchMediaInternal(chatResult.id, limit, null, onResult)
            is TdApi.Error -> onResult(emptyList(), chatResult.message)
            else -> onResult(emptyList(), "Failed to load Saved Messages chat")
        }
    }
}

private fun SavedMessagesRepository.hasVideoInChatInternal(chatId: Long, onResult: (Boolean) -> Unit) {
    val found = AtomicBoolean(false)
    val pending = AtomicInteger(2)
    val lock = Any()

    fun checkDone() {
        if (pending.decrementAndGet() == 0) {
            onResult(found.get())
        }
    }

    searchWithFilterInternal(chatId, 1, null, TdApi.SearchMessagesFilterPhotoAndVideo()) { items, _ ->
        synchronized(lock) {
            if (items.any { it.type == MediaType.Video }) {
                found.set(true)
            }
        }
        checkDone()
    }

    searchWithFilterInternal(chatId, 1, null, TdApi.SearchMessagesFilterDocument()) { items, _ ->
        synchronized(lock) {
            if (items.any { it.type == MediaType.Video }) {
                found.set(true)
            }
        }
        checkDone()
    }
}
