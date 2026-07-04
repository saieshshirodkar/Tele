package com.saiesh.tele.data.source.tdlib

import com.saiesh.tele.core.tdlib.client.TdLibClient
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

class TdLibMessageSource(
    private val client: Client = TdLibClient.client
) {
    private val pendingFastLinks = ConcurrentHashMap<Long, Boolean>()

    fun deleteMessages(chatId: Long, messageIds: LongArray, onResult: (String?) -> Unit) {
        client.send(TdApi.DeleteMessages(chatId, messageIds, true)) { result ->
            when (result) {
                is TdApi.Ok -> onResult(null)
                is TdApi.Error -> onResult(result.message)
                else -> onResult("Failed to delete message")
            }
        }
    }

    fun requestFastLink(
        botUsername: String,
        chatId: Long,
        messageId: Long,
        handlerScheduler: java.util.concurrent.ScheduledExecutorService,
        timeoutSeconds: Long,
        onResult: (String?, String?) -> Unit
    ) {
        if (pendingFastLinks.putIfAbsent(messageId, true) != null) {
            onResult(null, "Already requesting link for this item")
            return
        }

        client.send(TdApi.SearchPublicChat(botUsername)) { chatResult ->
            val botChat = chatResult as? TdApi.Chat
            if (botChat == null) {
                pendingFastLinks.remove(messageId)
                val error = (chatResult as? TdApi.Error)?.message ?: "Bot chat not found"
                onResult(null, error)
                return@send
            }

            val forwardRequest = TdApi.ForwardMessages(
                botChat.id, null, chatId, longArrayOf(messageId), null, true, true
            )
            client.send(forwardRequest) { forwardResult ->
                val forwarded = (forwardResult as? TdApi.Messages)?.messages?.firstOrNull()
                if (forwarded == null) {
                    pendingFastLinks.remove(messageId)
                    val error = (forwardResult as? TdApi.Error)?.message ?: "Failed to forward"
                    onResult(null, error)
                    return@send
                }

                val completed = java.util.concurrent.atomic.AtomicBoolean(false)
                lateinit var updateHandler: (TdApi.Object?) -> Unit

                val timeoutFuture = handlerScheduler.schedule({
                    if (completed.compareAndSet(false, true)) {
                        pendingFastLinks.remove(messageId)
                        TdLibClient.removeUpdateHandler(updateHandler)
                        onResult(null, "Bot did not respond in time")
                    }
                }, timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)

                updateHandler = updateHandler@{ update ->
                    if (completed.get()) return@updateHandler
                    val newMsg = (update as? TdApi.UpdateNewMessage)?.message ?: return@updateHandler
                    if (newMsg.chatId != botChat.id) return@updateHandler
                    val text = (newMsg.content as? TdApi.MessageText)?.text?.text ?: return@updateHandler
                    val replyTo = newMsg.replyTo as? TdApi.MessageReplyToMessage
                    val isDirectReply = replyTo?.messageId == forwarded.id
                    val containsLink = text.contains("https://", ignoreCase = true)

                    if (isDirectReply || containsLink) {
                        val link = extractFastLink(text)
                        if (link != null && completed.compareAndSet(false, true)) {
                            pendingFastLinks.remove(messageId)
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

    private fun extractFastLink(text: String): String? {
        val regex = "https?://\\S+".toRegex()
        val fastLine = text.lineSequence()
            .firstOrNull { it.contains("Download Link:", ignoreCase = true) }
        if (fastLine != null) {
            regex.find(fastLine)?.value?.trimEnd('.', ',', ')', ']', '>')?.let { return it }
        }
        val downloadLine = text.lineSequence()
            .firstOrNull { it.contains("Download", ignoreCase = true) && it.contains("http") }
        if (downloadLine != null) {
            regex.find(downloadLine)?.value?.trimEnd('.', ',', ')', ']', '>')?.let { return it }
        }
        return regex.find(text)?.value?.trimEnd('.', ',', ')', ']', '>')
    }

    fun clearPendingFastLinks() {
        pendingFastLinks.clear()
    }
}
