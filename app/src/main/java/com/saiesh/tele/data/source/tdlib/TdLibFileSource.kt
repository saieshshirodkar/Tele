package com.saiesh.tele.data.source.tdlib

import com.saiesh.tele.core.tdlib.client.TdLibClient
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class TdLibFileSource(
    private val client: Client = TdLibClient.client
) {
    private val pendingDownloads = ConcurrentHashMap<Int, MutableList<(String?) -> Unit>>()

    fun downloadThumbnail(fileId: Int, onResult: (String?) -> Unit) {
        val callbacks = mutableListOf(onResult)
        val existing = pendingDownloads.putIfAbsent(fileId, callbacks)
        if (existing != null) {
            synchronized(existing) { existing.add(onResult) }
            return
        }

        fun dispatch(path: String?) {
            val cbs = pendingDownloads.remove(fileId) ?: return
            synchronized(cbs) {
                for (cb in cbs) cb(path)
            }
        }

        client.send(TdApi.GetFile(fileId)) { result ->
            if (result is TdApi.Error) { dispatch(null); return@send }
            val file = result as? TdApi.File ?: run { dispatch(null); return@send }
            val local = file.local
            val existing = local?.path?.takeIf { it.isNotBlank() && local.isDownloadingCompleted }
            if (existing != null) { dispatch(existing); return@send }

            val completed = AtomicBoolean(false)
            lateinit var updateHandler: (TdApi.Object?) -> Unit

            fun cleanup(path: String?) {
                pendingDownloads.remove(fileId)
                TdLibClient.removeUpdateHandler(updateHandler)
                dispatch(path)
            }

            updateHandler = updateHandler@{ update ->
                if (completed.get()) return@updateHandler
                val updateFile = update as? TdApi.UpdateFile ?: return@updateHandler
                if (updateFile.file.id != fileId) return@updateHandler
                val path = updateFile.file.local?.path?.takeIf { it.isNotBlank() }
                val isReady = updateFile.file.local?.isDownloadingCompleted == true
                if (path != null && isReady && completed.compareAndSet(false, true)) {
                    cleanup(path)
                }
            }

            TdLibClient.addUpdateHandler(updateHandler)
            client.send(TdApi.DownloadFile(fileId, 32, 0, 0, true)) { downloadResult ->
                when (downloadResult) {
                    is TdApi.File -> {
                        val path = downloadResult.local?.path?.takeIf { it.isNotBlank() }
                        val ready = downloadResult.local?.isDownloadingCompleted == true
                        if (ready && path != null && completed.compareAndSet(false, true)) {
                            cleanup(path)
                        }
                    }
                    is TdApi.Error -> {
                        if (completed.compareAndSet(false, true)) {
                            cleanup(null)
                        }
                    }
                }
            }
        }
    }

    fun clearPending() {
        pendingDownloads.clear()
    }
}
