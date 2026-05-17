package com.saiesh.tele.data.cache

import android.graphics.Bitmap
import android.util.LruCache

object ImageCache {
    private const val MAX_CACHE_BYTES = 20 * 1024 * 1024
    private const val MAX_MINI_CACHE_BYTES = 5 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
    }

    private val miniCache = object : LruCache<Long, Bitmap>(MAX_MINI_CACHE_BYTES) {
        override fun sizeOf(key: Long, bitmap: Bitmap): Int = bitmap.byteCount
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (cache.get(key) == null) {
            cache.put(key, bitmap)
        }
    }

    fun getMini(key: Long): Bitmap? = miniCache.get(key)

    fun putMini(key: Long, bitmap: Bitmap) {
        if (miniCache.get(key) == null) {
            miniCache.put(key, bitmap)
        }
    }
}
