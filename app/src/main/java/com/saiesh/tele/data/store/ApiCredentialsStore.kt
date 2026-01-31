package com.saiesh.tele.data.store

import android.content.Context

class ApiCredentialsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiId(): String? = prefs.getString(KEY_API_ID, null)

    fun getApiHash(): String? = prefs.getString(KEY_API_HASH, null)

    fun save(apiId: String, apiHash: String) {
        prefs.edit()
            .putString(KEY_API_ID, apiId.trim())
            .putString(KEY_API_HASH, apiHash.trim())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "telegram_api_credentials"
        private const val KEY_API_ID = "api_id"
        private const val KEY_API_HASH = "api_hash"
    }
}
