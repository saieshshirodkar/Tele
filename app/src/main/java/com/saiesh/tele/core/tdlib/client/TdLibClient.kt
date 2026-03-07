package com.saiesh.tele.core.tdlib.client

import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.CopyOnWriteArrayList

object TdLibClient {
    private val updateHandlers = CopyOnWriteArrayList<(TdApi.Object?) -> Unit>()
    private val errorHandlers = CopyOnWriteArrayList<(Throwable?) -> Unit>()
    private val newMessageHandlers = CopyOnWriteArrayList<(TdApi.UpdateNewMessage) -> Unit>()
    private val deleteMessageHandlers = CopyOnWriteArrayList<(TdApi.UpdateDeleteMessages) -> Unit>()

    val client: Client by lazy {
        Client.create(
            { update -> 
                updateHandlers.forEach { it(update) }
                when (update) {
                    is TdApi.UpdateNewMessage -> {
                        newMessageHandlers.forEach { handler ->
                            try { handler(update) } catch (_: Exception) {}
                        }
                    }
                    is TdApi.UpdateDeleteMessages -> {
                        deleteMessageHandlers.forEach { handler ->
                            try { handler(update) } catch (_: Exception) {}
                        }
                    }
                }
            },
            { error -> errorHandlers.forEach { it(error) } },
            null
        )
    }

    fun addUpdateHandler(handler: (TdApi.Object?) -> Unit) {
        updateHandlers.add(handler)
    }

    fun removeUpdateHandler(handler: (TdApi.Object?) -> Unit) {
        updateHandlers.remove(handler)
    }

    fun addErrorHandler(handler: (Throwable?) -> Unit) {
        errorHandlers.add(handler)
    }

    fun removeErrorHandler(handler: (Throwable?) -> Unit) {
        errorHandlers.remove(handler)
    }

    fun addNewMessageHandler(handler: (TdApi.UpdateNewMessage) -> Unit) {
        newMessageHandlers.add(handler)
    }

    fun removeNewMessageHandler(handler: (TdApi.UpdateNewMessage) -> Unit) {
        newMessageHandlers.remove(handler)
    }

    fun addDeleteMessageHandler(handler: (TdApi.UpdateDeleteMessages) -> Unit) {
        deleteMessageHandlers.add(handler)
    }

    fun removeDeleteMessageHandler(handler: (TdApi.UpdateDeleteMessages) -> Unit) {
        deleteMessageHandlers.remove(handler)
    }
}
