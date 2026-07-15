package com.dazou.iptvplayer

import android.app.Application
import com.dazou.iptvplayer.data.AppContainer

class App : Application() {
    val container by lazy { AppContainer(this) }
}