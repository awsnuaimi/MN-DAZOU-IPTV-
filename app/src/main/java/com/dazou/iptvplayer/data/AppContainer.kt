package com.dazou.iptvplayer.data

import android.app.Application
import com.dazou.iptvplayer.api.XtreamAPI

class AppContainer(application: Application) {
    val accountManager = AccountManager(application)

    // خاصية ذكية تعيد Repository مرتبطاً بالحساب النشط، أو null
    val currentRepository: XtreamRepository?
        get() {
            val server = accountManager.getActiveAccount()
            return if (server != null) XtreamRepository(server) else null
        }
}