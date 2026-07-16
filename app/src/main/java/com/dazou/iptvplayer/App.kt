package com.dazou.iptvplayer

import android.app.Application
import com.dazou.iptvplayer.data.AppContainer
import com.dazou.iptvplayer.utils.CrashHandler

class App : Application() {

    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}