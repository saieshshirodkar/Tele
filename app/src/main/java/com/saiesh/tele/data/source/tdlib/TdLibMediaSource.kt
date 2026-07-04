package com.saiesh.tele.data.source.tdlib

import com.saiesh.tele.core.tdlib.client.TdLibClient
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TdLibMediaSource(
    private val client: Client = TdLibClient.client
) {
    fun searchChatMessages(
        chatId: Long,
        filter: TdApi.SearchMessagesFilter,
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<TdApi.Message>, String?) -> Unit
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
                    onResult(result.messages?.toList().orEmpty(), null)
                }
                is TdApi.Error -> onResult(emptyList(), result.message)
                else -> onResult(emptyList(), "Unexpected response")
            }
        }
    }

    fun getChatHistory(
        chatId: Long,
        fromMessageId: Long,
        limit: Int,
        onResult: (List<TdApi.Message>, String?) -> Unit
    ) {
        val query = TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)
        client.send(query) { result ->
            when (result) {
                is TdApi.Messages -> {
                    val messages = result.messages?.toList().orEmpty()
                    onResult(messages, null)
                }
                is TdApi.Error -> onResult(emptyList(), result.message)
                else -> onResult(emptyList(), "Unexpected response")
            }
        }
    }

    fun getMessage(
        chatId: Long,
        messageId: Long,
        onResult: (TdApi.Message?, String?) -> Unit
    ) {
        client.send(TdApi.GetMessage(chatId, messageId)) { result ->
            when (result) {
                is TdApi.Message -> onResult(result, null)
                is TdApi.Error -> onResult(null, result.message)
                else -> onResult(null, "Failed to load message")
            }
        }
    }
}
