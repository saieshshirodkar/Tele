package com.saiesh.tele.data.cache.image

import android.graphics.Bitmap
import android.util.LruCache

object ImageCache {
    private const val DEFAULT_CACHE_SIZE = 50
    private val cache = LruCache<String, Bitmap>(DEFAULT_CACHE_SIZE)
    private val miniCache = LruCache<Long, Bitmap>(100)

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
