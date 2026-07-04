package com.saiesh.tele.data.source.tdlib

import com.saiesh.tele.core.cache.TeleCache
import com.saiesh.tele.core.tdlib.client.TdLibClient
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TdLibChatSource(
    private val client: Client = TdLibClient.client
) {
    private var cachedMeId: Long? = null
    private var cachedSavedMessagesChatId: Long? = null

    init {
        cachedSavedMessagesChatId = TeleCache.loadSavedMessagesChatId()
        cachedMeId = TeleCache.loadMeId()
    }

    fun getCachedSavedMessagesChatId(): Long? = cachedSavedMessagesChatId

    fun resolveSavedMessagesChat(onResult: (Long?, String?) -> Unit) {
        val cached = cachedSavedMessagesChatId
        if (cached != null) {
            onResult(cached, null)
            return
        }

        fun createChat(userId: Long) {
            client.send(TdApi.CreatePrivateChat(userId, false)) { result ->
                when (result) {
                    is TdApi.Chat -> {
                        cachedSavedMessagesChatId = result.id
                        TeleCache.saveSavedMessagesChatId(result.id)
                        onResult(result.id, null)
                    }
                    is TdApi.Error -> onResult(null, result.message)
                    else -> onResult(null, "Failed to open Saved Messages")
                }
            }
        }

        val cachedUser = cachedMeId
        if (cachedUser != null) {
            createChat(cachedUser)
            return
        }

        client.send(TdApi.GetMe()) { result ->
            when (result) {
                is TdApi.User -> {
                    cachedMeId = result.id
                    TeleCache.saveMeId(result.id)
                    createChat(result.id)
                }
                is TdApi.Error -> onResult(null, result.message)
                else -> onResult(null, "Failed to load user")
            }
        }
    }

    fun getChat(chatId: Long, onResult: (TdApi.Chat?, String?) -> Unit) {
        client.send(TdApi.GetChat(chatId)) { result ->
            when (result) {
                is TdApi.Chat -> onResult(result, null)
                is TdApi.Error -> onResult(null, result.message)
                else -> onResult(null, "Unexpected response")
            }
        }
    }

    fun getChatsInOrder(
        limit: Int,
        onResult: (List<Long>, String?) -> Unit
    ) {
        client.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { result ->
            when (result) {
                is TdApi.Chats -> onResult(result.chatIds?.toList().orEmpty(), null)
                is TdApi.Error -> onResult(emptyList(), result.message)
                else -> onResult(emptyList(), "Unexpected response")
            }
        }
    }

    fun searchMessagesAcrossChats(
        chatList: TdApi.ChatList?,
        filter: TdApi.SearchMessagesFilter,
        limit: Int,
        onResult: (List<TdApi.Message>, String?) -> Unit
    ) {
        val query = TdApi.SearchMessages(
            chatList ?: TdApi.ChatListMain(),
            "",
            "",
            limit,
            filter,
            null,
            0,
            0
        )
        client.send(query) { result ->
            when (result) {
                is TdApi.FoundMessages -> {
                    onResult(result.messages?.toList().orEmpty(), null)
                }
                is TdApi.Error -> onResult(emptyList(), result.message)
                else -> onResult(emptyList(), "Unexpected response")
            }
        }
    }

    fun clearCache() {
        cachedMeId = null
        cachedSavedMessagesChatId = null
    }
}
