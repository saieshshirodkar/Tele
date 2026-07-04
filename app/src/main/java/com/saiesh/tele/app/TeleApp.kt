package com.saiesh.tele.app

import android.app.Application
import com.saiesh.tele.core.cache.TeleCache

class TeleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("tdjni")
        TeleCache.init(this)
    }
}
