package com.saiesh.tele.data.repository

import com.saiesh.tele.core.tdlib.client.TdLibClient
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val pendingThumbnailDownloads = ConcurrentHashMap<Int, Boolean>()

fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02dh%02dm%02ds", hours, minutes, seconds)
}

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
    if (pendingThumbnailDownloads.putIfAbsent(fileId, true) != null) return
    client.send(TdApi.GetFile(fileId)) { result ->
        when (result) {
            is TdApi.Error -> {
                pendingThumbnailDownloads.remove(fileId)
                onResult(null)
                return@send
            }
        }
        val file = result as? TdApi.File
        if (file == null) {
            pendingThumbnailDownloads.remove(fileId)
            onResult(null)
            return@send
        }
        val local = file.local
        val existingPath = local?.path?.takeIf { it.isNotBlank() && local.isDownloadingCompleted }
        if (existingPath != null) {
            pendingThumbnailDownloads.remove(fileId)
            onResult(existingPath)
            return@send
        }
        val completed = AtomicBoolean(false)
        lateinit var updateHandler: (TdApi.Object?) -> Unit

        fun cleanup() {
            pendingThumbnailDownloads.remove(fileId)
            TdLibClient.removeUpdateHandler(updateHandler)
        }

        updateHandler = updateHandler@{ update ->
            if (completed.get()) return@updateHandler
            val updateFile = update as? TdApi.UpdateFile ?: return@updateHandler
            if (updateFile.file.id != fileId) return@updateHandler
            val updatedLocal = updateFile.file.local
            val path = updatedLocal?.path?.takeIf { it.isNotBlank() }
            val isReady = updatedLocal?.isDownloadingCompleted == true
            if (path != null && isReady && completed.compareAndSet(false, true)) {
                cleanup()
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
                        cleanup()
                        onResult(downloadedPath)
                    }
                }
                is TdApi.Error -> {
                    if (completed.compareAndSet(false, true)) {
                        cleanup()
                        onResult(null)
                    }
                }
            }
        }
    }
}
