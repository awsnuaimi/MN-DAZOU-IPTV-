package com.dazou.iptvplayer

import android.app.Application
import com.dazou.iptvplayer.data.AccountManager

class App : Application() {
    val accountManager: AccountManager by lazy { AccountManager(this) }
}