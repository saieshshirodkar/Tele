package com.saiesh.tele.data.repository

import com.saiesh.tele.core.tdlib.client.TdLibClient
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicBoolean

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

internal fun SavedMessagesRepository.fetchThumbnailPathInternal(
    fileId: Int,
    onResult: (String?) -> Unit
) {
    client.send(TdApi.GetFile(fileId)) { result ->
        val file = result as? TdApi.File
        val local = file?.local
        val existingPath = local?.path?.takeIf { it.isNotBlank() && local.isDownloadingCompleted }
        if (existingPath != null) {
            onResult(existingPath)
            return@send
        }
        val completed = AtomicBoolean(false)
        lateinit var updateHandler: (TdApi.Object?) -> Unit

        updateHandler = updateHandler@{ update ->
            if (completed.get()) return@updateHandler
            val updateFile = update as? TdApi.UpdateFile ?: return@updateHandler
            if (updateFile.file.id != fileId) return@updateHandler
            val updatedLocal = updateFile.file.local
            val path = updatedLocal?.path?.takeIf { it.isNotBlank() }
            val isReady = updatedLocal?.isDownloadingCompleted == true
            if (path != null && isReady && completed.compareAndSet(false, true)) {
                TdLibClient.removeUpdateHandler(updateHandler)
                onResult(path)
            }
        }
        TdLibClient.addUpdateHandler(updateHandler)
        client.send(TdApi.DownloadFile(fileId, 32, 0, 0, true)) { downloadResult ->
            when (downloadResult) {
                is TdApi.File -> {
                    val downloadedLocal = downloadResult.local
                    val downloadedPath = downloadedLocal?.path?.takeIf { it.isNotBlank() }
                    if (downloadedLocal?.isDownloadingCompleted == true && downloadedPath != null && completed.compareAndSet(false, true)) {
                        TdLibClient.removeUpdateHandler(updateHandler)
                        onResult(downloadedPath)
                    }
                }
                is TdApi.Error -> {
                    if (completed.compareAndSet(false, true)) {
                        TdLibClient.removeUpdateHandler(updateHandler)
                        onResult(null)
                    }
                }
            }
        }
    }
}
