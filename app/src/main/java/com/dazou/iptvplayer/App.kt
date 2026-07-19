package com.dazou.iptvplayer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.dazou.iptvplayer.data.AppContainer
import com.dazou.iptvplayer.utils.CrashHandler

class App : Application() {

    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        applySavedTheme()
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
        val isLight = prefs.getBoolean("is_light_theme", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isLight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        )
    }
}