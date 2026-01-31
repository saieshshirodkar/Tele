package com.saiesh.tele.data.repository.media

import org.drinkless.tdlib.TdApi

internal fun SavedMessagesRepository.deleteMessageInternal(
    chatId: Long,
    messageId: Long,
    onResult: (String?) -> Unit
) {
    client.send(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true)) { result ->
        when (result) {
            is TdApi.Ok -> onResult(null)
            is TdApi.Error -> onResult(result.message)
            else -> onResult("Failed to delete message")
        }
    }
}
