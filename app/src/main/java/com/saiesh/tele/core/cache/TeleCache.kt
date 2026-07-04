package com.saiesh.tele.core.cache

import android.content.Context
import com.saiesh.tele.domain.model.MediaItem
import com.saiesh.tele.domain.model.VideoChatItem
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object TeleCache {
    private var cacheDir: File? = null

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "tele_cache").also { it.mkdirs() }
        cacheDir?.listFiles()
            ?.filter { it.name.startsWith("media_") && System.currentTimeMillis() - it.lastModified() > 7 * 24 * 60 * 60 * 1000L }
            ?.forEach { it.delete() }
    }

    fun saveVideoChats(chats: List<VideoChatItem>) {
        val dir = cacheDir ?: return
        File(dir, "sidebar.dat").writeBytes(serialize(chats))
    }

    @Suppress("UNCHECKED_CAST")
    fun loadVideoChats(): List<VideoChatItem>? {
        val dir = cacheDir ?: return null
        val file = File(dir, "sidebar.dat")
        if (!file.exists()) return null
        return try {
            deserialize<List<VideoChatItem>>(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    fun saveMediaItems(items: List<MediaItem>, chatKey: String) {
        val dir = cacheDir ?: return
        File(dir, "media_$chatKey.dat").writeBytes(serialize(items))
    }

    @Suppress("UNCHECKED_CAST")
    fun loadMediaItems(chatKey: String): List<MediaItem>? {
        val dir = cacheDir ?: return null
        val file = File(dir, "media_$chatKey.dat")
        if (!file.exists()) return null
        return try {
            deserialize<List<MediaItem>>(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    fun saveChatTitle(chatId: Long, title: String) {
        val dir = cacheDir ?: return
        val titles = loadAllChatTitles().toMutableMap()
        titles[chatId] = title
        File(dir, "chat_titles.dat").writeBytes(serialize(titles))
    }

    fun loadChatTitle(chatId: Long): String? {
        return loadAllChatTitles()[chatId]
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadAllChatTitles(): Map<Long, String> {
        val dir = cacheDir ?: return emptyMap()
        val file = File(dir, "chat_titles.dat")
        if (!file.exists()) return emptyMap()
        return try {
            deserialize<Map<Long, String>>(file.readBytes())
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveChatOrder(chatId: Long, order: Long) {
        val dir = cacheDir ?: return
        val orders = loadAllChatOrders().toMutableMap()
        orders[chatId] = order
        File(dir, "chat_orders.dat").writeBytes(serialize(orders))
    }

    fun loadChatOrder(chatId: Long): Long? {
        return loadAllChatOrders()[chatId]
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadAllChatOrders(): Map<Long, Long> {
        val dir = cacheDir ?: return emptyMap()
        val file = File(dir, "chat_orders.dat")
        if (!file.exists()) return emptyMap()
        return try {
            deserialize<Map<Long, Long>>(file.readBytes())
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveSavedMessagesChatId(chatId: Long) {
        val dir = cacheDir ?: return
        File(dir, "saved_chat_id.dat").writeBytes(serialize(chatId))
    }

    fun loadSavedMessagesChatId(): Long? {
        val dir = cacheDir ?: return null
        val file = File(dir, "saved_chat_id.dat")
        if (!file.exists()) return null
        return try {
            deserialize<Long>(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    fun saveMeId(userId: Long) {
        val dir = cacheDir ?: return
        File(dir, "me_id.dat").writeBytes(serialize(userId))
    }

    fun loadMeId(): Long? {
        val dir = cacheDir ?: return null
        val file = File(dir, "me_id.dat")
        if (!file.exists()) return null
        return try {
            deserialize<Long>(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    private fun serialize(obj: Any): ByteArray {
        ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeObject(obj)
            }
            return baos.toByteArray()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> deserialize(bytes: ByteArray): T {
        ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois ->
                return ois.readObject() as T
            }
        }
    }
}
