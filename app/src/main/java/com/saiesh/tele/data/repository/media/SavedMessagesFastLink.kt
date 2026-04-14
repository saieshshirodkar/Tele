package com.saiesh.tele.data.repository.media

import com.saiesh.tele.core.tdlib.client.TdLibClient
import com.saiesh.tele.domain.model.media.MediaItem
import org.drinkless.tdlib.TdApi
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal fun SavedMessagesRepository.requestFastLinkInternal(
    item: MediaItem,
    onResult: (String?, String?) -> Unit
) {
    client.send(TdApi.SearchPublicChat(BOT_USERNAME)) searchBot@{ chatResult ->
        val botChat = chatResult as? TdApi.Chat
        if (botChat == null) {
            val error = (chatResult as? TdApi.Error)?.message ?: "Bot chat not found"
            onResult(null, error)
            return@searchBot
        }
        val forwardRequest = TdApi.ForwardMessages(
            botChat.id,
            null,
            item.chatId,
            longArrayOf(item.messageId),
            null,
            true,
            true
        )
        client.send(forwardRequest) forwardMessage@{ forwardResult ->
            val forwardedMessage = (forwardResult as? TdApi.Messages)
                ?.messages
                ?.firstOrNull()
            if (forwardedMessage == null) {
                val error = (forwardResult as? TdApi.Error)?.message ?: "Failed to forward message"
                onResult(null, error)
                return@forwardMessage
            }

            val completed = AtomicBoolean(false)
            lateinit var updateHandler: (TdApi.Object?) -> Unit

            val timeoutFuture = handlerScheduler.schedule({
                if (completed.compareAndSet(false, true)) {
                    TdLibClient.removeUpdateHandler(updateHandler)
                    onResult(null, "Bot did not respond in time")
                }
            }, HANDLER_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            updateHandler = updateHandler@{ update ->
                if (completed.get()) return@updateHandler
                val newMessage = (update as? TdApi.UpdateNewMessage)?.message ?: return@updateHandler
                if (newMessage.chatId != botChat.id) return@updateHandler

                val content = newMessage.content as? TdApi.MessageText ?: return@updateHandler
                val text = content.text.text

                val replyTo = newMessage.replyTo as? TdApi.MessageReplyToMessage
                val isDirectReply = replyTo?.messageId == forwardedMessage.id
                val containsLink = text.contains("https://", ignoreCase = true)

                if (isDirectReply || containsLink) {
                    val link = extractFastDownloadLinkInternal(text)
                    if (link != null && completed.compareAndSet(false, true)) {
                        timeoutFuture.cancel(false)
                        TdLibClient.removeUpdateHandler(updateHandler)
                        onResult(link, null)
                    }
                }
            }

            TdLibClient.addUpdateHandler(updateHandler)
        }
    }
}

private fun extractFastDownloadLinkInternal(text: String): String? {
    val lines = text.lineSequence().toList()

    val fastLine = lines.firstOrNull { it.contains("Download Link:", ignoreCase = true) }
    if (fastLine != null) {
        val urlRegex = "https?://\\S+".toRegex()
        urlRegex.find(fastLine)?.value?.trimEnd('.', ',', ')', ']', '>')?.let { return it }
    }

    val downloadLine = lines.firstOrNull {
        it.contains("Download", ignoreCase = true) && it.contains("http")
    }
    if (downloadLine != null) {
        val urlRegex = "https?://\\S+".toRegex()
        urlRegex.find(downloadLine)?.value?.trimEnd('.', ',', ')', ']', '>')?.let { return it }
    }

    val urlRegex = "https?://\\S+".toRegex()
    return urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ']', '>')
}
