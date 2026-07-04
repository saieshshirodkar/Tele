package com.saiesh.tele.data.repository

import android.util.Log
import com.saiesh.tele.core.cache.TeleCache
import com.saiesh.tele.data.mapper.MediaMapper
import com.saiesh.tele.data.source.tdlib.BOT_USERNAME
import com.saiesh.tele.data.source.tdlib.FAST_LINK_TIMEOUT_SECONDS
import com.saiesh.tele.data.source.tdlib.HISTORY_PAGE_SIZE
import com.saiesh.tele.data.source.tdlib.SEARCH_LIMIT_PER_FILTER
import com.saiesh.tele.data.source.tdlib.SIDEBAR_LIMIT
import com.saiesh.tele.data.source.tdlib.TdLibChatSource
import com.saiesh.tele.data.source.tdlib.TdLibFileSource
import com.saiesh.tele.data.source.tdlib.TdLibMediaSource
import com.saiesh.tele.data.source.tdlib.TdLibMessageSource
import com.saiesh.tele.domain.model.MediaItem
import com.saiesh.tele.domain.model.SAVED_MESSAGES_TITLE
import com.saiesh.tele.domain.model.VideoChatItem
import com.saiesh.tele.domain.repository.TeleRepository
import org.drinkless.tdlib.TdApi
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class TeleRepositoryImpl(
    private val chatSource: TdLibChatSource = TdLibChatSource(),
    private val mediaSource: TdLibMediaSource = TdLibMediaSource(),
    private val fileSource: TdLibFileSource = TdLibFileSource(),
    private val messageSource: TdLibMessageSource = TdLibMessageSource()
) : TeleRepository {

    private val handlerScheduler = Executors.newSingleThreadScheduledExecutor()

    override fun getCachedSavedMessagesChatId(): Long? = chatSource.getCachedSavedMessagesChatId()

    override fun loadChatsWithVideos(
        limit: Int,
        selectedChatId: Long?,
        onResult: (List<VideoChatItem>, String?) -> Unit
    ) {
        chatSource.resolveSavedMessagesChat { savedChatId, error ->
            if (savedChatId == null) {
                onResult(emptyList(), error ?: "Saved Messages unavailable")
                return@resolveSavedMessagesChat
            }

            val mainList = TdApi.ChatListMain()
            val filters = listOf(
                TdApi.SearchMessagesFilterVideo(),
                TdApi.SearchMessagesFilterVideoNote(),
                TdApi.SearchMessagesFilterAnimation(),
                TdApi.SearchMessagesFilterDocument()
            )

            val searchPending = AtomicInteger(filters.size)
            val lock = Any()
            val videoChats = mutableSetOf<Long>()

            for (filter in filters) {
                chatSource.searchMessagesAcrossChats(mainList, filter, SEARCH_LIMIT_PER_FILTER) { messages, _ ->
                    synchronized(lock) {
                        for (msg in messages) {
                            if (msg.content is TdApi.MessageDocument) {
                                val doc = (msg.content as TdApi.MessageDocument).document
                                if (doc == null || !doc.mimeType.startsWith("video/", ignoreCase = true)) continue
                            }
                            videoChats.add(msg.chatId)
                        }
                        if (searchPending.decrementAndGet() == 0) {
                            chatSource.getChatsInOrder(SEARCH_LIMIT_PER_FILTER * 4) { orderedIds, _ ->
                                val chatIdsToFetch = mutableListOf<Long>()
                                if (savedChatId !in videoChats) {
                                    chatIdsToFetch.add(savedChatId)
                                }
                                for (id in orderedIds) {
                                    if (id in videoChats) {
                                        chatIdsToFetch.add(id)
                                        if (chatIdsToFetch.size >= limit * 2 + 1) break
                                    }
                                }
                                fetchChatTitles(chatIdsToFetch, savedChatId, selectedChatId, limit, onResult)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchChatTitles(
        chatIds: List<Long>,
        savedChatId: Long,
        selectedChatId: Long?,
        limit: Int,
        onResult: (List<VideoChatItem>, String?) -> Unit
    ) {
        if (chatIds.isEmpty()) {
            onResult(emptyList(), null)
            return
        }

        val pending = AtomicInteger(chatIds.size)
        val lock = Any()
        val chatMap = mutableMapOf<Long, VideoChatItem>()

        fun finish() {
            val ordered = mutableListOf<VideoChatItem>()
            chatMap[savedChatId]?.let { ordered.add(it) }
            for (id in chatIds) {
                if (id == savedChatId) continue
                chatMap[id]?.let { ordered.add(it) }
                if (ordered.size >= limit + 1) break
            }
            onResult(ordered, null)
        }

        for (chatId in chatIds) {
            if (chatId == savedChatId) {
                val isSelected = selectedChatId == null
                synchronized(lock) {
                    chatMap[chatId] = VideoChatItem(
                        chatId = chatId,
                        title = SAVED_MESSAGES_TITLE,
                        isSavedMessages = true,
                        isSelected = isSelected
                    )
                }
                if (pending.decrementAndGet() == 0) finish()
                continue
            }

            val cachedTitle = TeleCache.loadChatTitle(chatId)
            if (cachedTitle != null) {
                val isSelected = selectedChatId != null && chatId == selectedChatId
                synchronized(lock) {
                    chatMap[chatId] = VideoChatItem(
                        chatId = chatId,
                        title = cachedTitle,
                        isSavedMessages = false,
                        isSelected = isSelected
                    )
                }
                if (pending.decrementAndGet() == 0) finish()
                continue
            }

            chatSource.getChat(chatId) { chat, _ ->
                synchronized(lock) {
                    val title = chat?.title ?: run {
                        if (pending.decrementAndGet() == 0) finish()
                        return@getChat
                    }
                    TeleCache.saveChatTitle(chatId, title)
                    val isSelected = selectedChatId != null && chatId == selectedChatId
                    chatMap[chatId] = VideoChatItem(
                        chatId = chatId,
                        title = title,
                        isSavedMessages = false,
                        isSelected = isSelected
                    )
                    if (pending.decrementAndGet() == 0) finish()
                }
            }
        }
    }

    override fun loadLatestMediaPaged(
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) {
        chatSource.resolveSavedMessagesChat { savedChatId, error ->
            if (savedChatId == null) {
                onResult(emptyList(), 0L, error ?: "Saved Messages unavailable")
                return@resolveSavedMessagesChat
            }
            loadMediaInChat(savedChatId, limit, fromMessageId, onResult)
        }
    }

    override fun loadChatMediaPaged(
        chatId: Long,
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) {
        loadMediaInChat(chatId, limit, fromMessageId, onResult)
    }

    private fun loadMediaInChat(
        chatId: Long,
        limit: Int,
        fromMessageId: Long?,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) {
        if (fromMessageId != null && fromMessageId != 0L) {
            loadMediaPageViaHistory(chatId, limit, fromMessageId, onResult)
        } else {
            loadMediaInitial(chatId, limit, onResult)
        }
    }

    private fun loadMediaInitial(
        chatId: Long,
        limit: Int,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) {
        val filters = listOf(
            TdApi.SearchMessagesFilterVideo(),
            TdApi.SearchMessagesFilterVideoNote(),
            TdApi.SearchMessagesFilterAnimation(),
            TdApi.SearchMessagesFilterDocument()
        )

        val pending = AtomicInteger(filters.size)
        val lock = Any()
        val collected = mutableListOf<MediaItem>()

        for (filter in filters) {
            mediaSource.searchChatMessages(chatId, filter, limit * 3, null) { messages, _ ->
                synchronized(lock) {
                    for (msg in messages) {
                        val item = MediaMapper.mapMessage(msg)
                        if (item != null && item.type == com.saiesh.tele.domain.model.MediaType.Video) {
                            collected.add(item)
                        }
                    }
                    if (pending.decrementAndGet() == 0) {
                        val result = collected
                            .distinctBy { it.messageId }
                            .sortedByDescending { it.date }
                        val page = result.take(limit)
                        val nextId = if (result.size > limit) result[limit - 1].messageId else 0L
                        onResult(page, nextId, null)
                    }
                }
            }
        }
    }

    private fun loadMediaPageViaHistory(
        chatId: Long,
        limit: Int,
        fromMessageId: Long,
        onResult: (List<MediaItem>, Long, String?) -> Unit
    ) {
        mediaSource.getChatHistory(chatId, fromMessageId, HISTORY_PAGE_SIZE) { messages, error ->
            if (error != null) {
                onResult(emptyList(), 0L, error)
                return@getChatHistory
            }
            val trimmed = if (messages.firstOrNull()?.id == fromMessageId) {
                messages.drop(1)
            } else {
                messages
            }
            val items = trimmed.mapNotNull(MediaMapper::mapMessage)
                .filter { it.type == com.saiesh.tele.domain.model.MediaType.Video }
            val page = items.take(limit)
            val nextId = if (page.size >= limit) page.last().messageId
                else if (trimmed.size < HISTORY_PAGE_SIZE) 0L
                else trimmed.last().id
            onResult(page, nextId, null)
        }
    }

    override fun getMessage(
        chatId: Long,
        messageId: Long,
        onResult: (MediaItem?, String?) -> Unit
    ) {
        mediaSource.getMessage(chatId, messageId) { message, error ->
            if (message != null) {
                onResult(MediaMapper.mapMessage(message), null)
            } else {
                onResult(null, error)
            }
        }
    }

    override fun deleteMessage(
        chatId: Long,
        messageId: Long,
        onResult: (String?) -> Unit
    ) {
        messageSource.deleteMessages(chatId, longArrayOf(messageId), onResult)
    }

    override fun fetchThumbnailPath(
        fileId: Int,
        onResult: (String?) -> Unit
    ) {
        fileSource.downloadThumbnail(fileId, onResult)
    }

    override fun requestFastLink(
        item: MediaItem,
        onResult: (String?, String?) -> Unit
    ) {
        messageSource.requestFastLink(
            BOT_USERNAME,
            item.chatId,
            item.messageId,
            handlerScheduler,
            FAST_LINK_TIMEOUT_SECONDS,
            onResult
        )
    }

    override fun shutdown() {
        handlerScheduler.shutdown()
        fileSource.clearPending()
        messageSource.clearPendingFastLinks()
        chatSource.clearCache()
    }
}
