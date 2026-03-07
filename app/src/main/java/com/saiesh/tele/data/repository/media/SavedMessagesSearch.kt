package com.saiesh.tele.data.repository.media

import com.saiesh.tele.core.tdlib.client.TdLibClient
import com.saiesh.tele.domain.model.media.MediaItem
import com.saiesh.tele.domain.model.search.SearchBotResponse
import com.saiesh.tele.domain.model.search.SearchQueryResult
import org.drinkless.tdlib.TdApi
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal fun SavedMessagesRepository.searchProBotInternal(
    query: String,
    onResult: (SearchBotResponse) -> Unit
) {
    resolveProSearchChatInternal { chatId, error ->
        if (chatId == null) {
            onResult(SearchBotResponse.Error(error ?: "Search bot unavailable"))
            return@resolveProSearchChatInternal
        }

        val completed = AtomicBoolean(false)
        lateinit var updateHandler: (TdApi.Object?) -> Unit
        val timeoutFuture = handlerScheduler.schedule({
            if (completed.compareAndSet(false, true)) {
                TdLibClient.removeUpdateHandler(updateHandler)
                onResult(SearchBotResponse.Error("Bot did not respond in time"))
            }
        }, SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        updateHandler = searchReplyHandler@{ update ->
            val newMessage = (update as? TdApi.UpdateNewMessage)?.message ?: return@searchReplyHandler
            if (newMessage.chatId != chatId || newMessage.isOutgoing) return@searchReplyHandler

            val response = parseSearchBotMessageInternal(newMessage)
            if (response != null && completed.compareAndSet(false, true)) {
                timeoutFuture.cancel(false)
                TdLibClient.removeUpdateHandler(updateHandler)
                onResult(response)
            }
        }
        TdLibClient.addUpdateHandler(updateHandler)

        val input = TdApi.InputMessageText(TdApi.FormattedText(query, null), null, false)
        val send = TdApi.SendMessage(chatId, null, null, null, null, input)
        client.send(send) { result ->
            if (result is TdApi.Error && completed.compareAndSet(false, true)) {
                timeoutFuture.cancel(false)
                TdLibClient.removeUpdateHandler(updateHandler)
                onResult(SearchBotResponse.Error(result.message))
            }
        }
    }
}

internal fun SavedMessagesRepository.submitProBotSelectionInternal(
    result: SearchQueryResult,
    onResult: (SearchBotResponse) -> Unit
) {
    val completed = AtomicBoolean(false)
    lateinit var updateHandler: (TdApi.Object?) -> Unit
    val timeoutFuture = handlerScheduler.schedule({
        if (completed.compareAndSet(false, true)) {
            TdLibClient.removeUpdateHandler(updateHandler)
            onResult(SearchBotResponse.Error("Bot did not respond in time"))
        }
    }, SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    updateHandler = selectionReplyHandler@{ update ->
        when (update) {
            is TdApi.UpdateNewMessage -> {
                val newMessage = update.message
                if (newMessage.chatId != result.chatId || newMessage.isOutgoing) return@selectionReplyHandler

                val response = parseSearchBotMessageInternal(newMessage)
                if (response != null && completed.compareAndSet(false, true)) {
                    timeoutFuture.cancel(false)
                    TdLibClient.removeUpdateHandler(updateHandler)
                    onResult(response)
                }
            }
            is TdApi.UpdateMessageEdited -> {
                if (update.chatId != result.chatId || update.messageId != result.messageId) {
                    return@selectionReplyHandler
                }
                val replyMarkup = update.replyMarkup as? TdApi.ReplyMarkupInlineKeyboard
                    ?: return@selectionReplyHandler
                val results = parseInlineKeyboardInternal(replyMarkup, update.chatId, update.messageId)
                if (results.isNotEmpty() && completed.compareAndSet(false, true)) {
                    timeoutFuture.cancel(false)
                    TdLibClient.removeUpdateHandler(updateHandler)
                    onResult(SearchBotResponse.Results(results))
                }
            }
        }
    }
    TdLibClient.addUpdateHandler(updateHandler)

    val data = result.callbackData
    if (data == null) {
        if (completed.compareAndSet(false, true)) {
            timeoutFuture.cancel(false)
            TdLibClient.removeUpdateHandler(updateHandler)
            onResult(SearchBotResponse.Error("Invalid selection"))
        }
        return
    }
    val payload = TdApi.CallbackQueryPayloadData(data)
    val request = TdApi.GetCallbackQueryAnswer(result.chatId, result.messageId, payload)
    client.send(request) { response ->
        if (response is TdApi.Error && completed.compareAndSet(false, true)) {
            timeoutFuture.cancel(false)
            TdLibClient.removeUpdateHandler(updateHandler)
            onResult(SearchBotResponse.Error(response.message))
        }
    }
}

internal fun SavedMessagesRepository.saveSearchMediaInternal(
    item: MediaItem,
    onResult: (String?) -> Unit
) {
    resolveSavedMessagesChatInternal { savedChatId, error ->
        if (savedChatId == null) {
            onResult(error ?: "Saved Messages unavailable")
            return@resolveSavedMessagesChatInternal
        }
        val request = TdApi.ForwardMessages(
            savedChatId,
            null,
            item.chatId,
            longArrayOf(item.messageId),
            null,
            true,
            false
        )
        client.send(request) { result ->
            when (result) {
                is TdApi.Messages -> onResult(null)
                is TdApi.Error -> onResult(result.message)
                else -> onResult("Failed to save message")
            }
        }
    }
}

private fun SavedMessagesRepository.resolveProSearchChatInternal(
    onResult: (Long?, String?) -> Unit
) {
    val cached = cachedProSearchChatId
    if (cached != null) {
        onResult(cached, null)
        return
    }
    client.send(TdApi.SearchPublicChat(PRO_SEARCH_BOT_USERNAME)) { result ->
        val chat = result as? TdApi.Chat
        if (chat != null) {
            cachedProSearchChatId = chat.id
            onResult(chat.id, null)
        } else {
            val error = (result as? TdApi.Error)?.message ?: "Search bot not found"
            onResult(null, error)
        }
    }
}

internal fun SavedMessagesRepository.autoProcessInviteInternal(
    url: String,
    onResult: (String?) -> Unit
) {
    client.send(TdApi.JoinChatByInviteLink(url)) { result ->
        when (result) {
            is TdApi.Chat -> onResult(null)
            is TdApi.Error -> {
                if (result.message == "INVITE_REQUEST_SENT" || result.message == "USER_ALREADY_PARTICIPANT") {
                    onResult(null)
                } else {
                    onResult(result.message)
                }
            }
            else -> onResult("Failed to process invite link")
        }
    }
}

private fun SavedMessagesRepository.parseSearchBotMessageInternal(message: TdApi.Message): SearchBotResponse? {
    val replyMarkup = message.replyMarkup as? TdApi.ReplyMarkupInlineKeyboard
    if (replyMarkup != null) {
        val results = parseInlineKeyboardInternal(replyMarkup, message.chatId, message.id)
        return if (results.isNotEmpty()) {
            SearchBotResponse.Results(results)
        } else {
            SearchBotResponse.Error("No results found")
        }
    }

    val media = mapMessageToMediaInternal(message)
    if (media != null) {
        return SearchBotResponse.Media(media)
    }

    val text = (message.content as? TdApi.MessageText)?.text?.text
    if (text != null && text.contains("no results", ignoreCase = true)) {
        return SearchBotResponse.Error("No results found")
    }
    return null
}

private fun SavedMessagesRepository.parseInlineKeyboardInternal(
    markup: TdApi.ReplyMarkupInlineKeyboard,
    chatId: Long,
    messageId: Long
): List<SearchQueryResult> {
    val results = mutableListOf<SearchQueryResult>()
    markup.rows?.forEach { row ->
        row?.forEach { button ->
            var callbackData: ByteArray? = null
            var url: String? = null
            
            when (val type = button.type) {
                is TdApi.InlineKeyboardButtonTypeCallback -> callbackData = type.data
                is TdApi.InlineKeyboardButtonTypeCallbackWithPassword -> callbackData = type.data
                is TdApi.InlineKeyboardButtonTypeUrl -> url = type.url
            }

            if (callbackData != null || url != null) {
                results.add(
                    SearchQueryResult(
                        title = button.text,
                        callbackData = callbackData,
                        url = url,
                        chatId = chatId,
                        messageId = messageId,
                        isPagination = isPaginationButtonInternal(button.text)
                    )
                )
            }
        }
    }
    return results
}

private fun isPaginationButtonInternal(text: String): Boolean {
    val normalized = text.trim().lowercase()
    if (
        normalized.contains("next") ||
        normalized.contains("page") ||
        normalized.contains("more") ||
        normalized.contains("prev") ||
        normalized.contains("back")
    ) {
        return true
    }
    return text.contains("»") || text.contains("›") || text.contains(">>") || text.contains("→")
}
