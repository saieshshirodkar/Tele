package com.saiesh.tele.core.tdlib.client

import android.util.Log
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.CopyOnWriteArrayList

object TdLibClient {
    private const val TAG = "TdLibClient"

    private class HandlerRegistry<T>(
        private val name: String
    ) {
        private val handlers = CopyOnWriteArrayList<(T) -> Unit>()

        fun add(handler: (T) -> Unit) {
            handlers.add(handler)
        }

        fun remove(handler: (T) -> Unit) {
            handlers.remove(handler)
        }

        fun dispatch(value: T) {
            handlers.forEach { handler ->
                try { handler(value) } catch (e: Exception) {
                    Log.e(TAG, "Error in $name handler", e)
                }
            }
        }
    }

    private val updateHandlers = HandlerRegistry<TdApi.Object?>("update")
    private val errorHandlers = HandlerRegistry<Throwable?>("error")
    private val newMessageHandlers = HandlerRegistry<TdApi.UpdateNewMessage>("newMessage")
    private val deleteMessageHandlers = HandlerRegistry<TdApi.UpdateDeleteMessages>("deleteMessage")

    val client: Client by lazy {
        Client.create(
            { update ->
                try {
                    updateHandlers.dispatch(update)
                    when (update) {
                        is TdApi.UpdateNewMessage -> newMessageHandlers.dispatch(update)
                        is TdApi.UpdateDeleteMessages -> deleteMessageHandlers.dispatch(update)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error dispatching update", e)
                }
            },
            { error ->
                try {
                    errorHandlers.dispatch(error)
                } catch (e: Exception) {
                    Log.e(TAG, "Error dispatching error", e)
                }
            },
            null
        )
    }

    fun addUpdateHandler(handler: (TdApi.Object?) -> Unit) = updateHandlers.add(handler)
    fun removeUpdateHandler(handler: (TdApi.Object?) -> Unit) = updateHandlers.remove(handler)

    fun addErrorHandler(handler: (Throwable?) -> Unit) = errorHandlers.add(handler)
    fun removeErrorHandler(handler: (Throwable?) -> Unit) = errorHandlers.remove(handler)

    fun addNewMessageHandler(handler: (TdApi.UpdateNewMessage) -> Unit) = newMessageHandlers.add(handler)
    fun removeNewMessageHandler(handler: (TdApi.UpdateNewMessage) -> Unit) = newMessageHandlers.remove(handler)

    fun addDeleteMessageHandler(handler: (TdApi.UpdateDeleteMessages) -> Unit) = deleteMessageHandlers.add(handler)
    fun removeDeleteMessageHandler(handler: (TdApi.UpdateDeleteMessages) -> Unit) = deleteMessageHandlers.remove(handler)
}
