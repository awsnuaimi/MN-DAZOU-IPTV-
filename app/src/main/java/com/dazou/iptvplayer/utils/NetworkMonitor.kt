package com.dazou.iptvplayer.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build

/** مسؤول فقط عن مراقبة حالة الاتصال بالإنترنت، مستقل تمامًا عن أي واجهة */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun isConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    /** يبدأ المراقبة. onChange بينفذ على Thread غير رئيسي لما يجي من onAvailable/onLost — لف الاستدعاء بـrunOnUiThread بمكان الاستخدام. */
    fun start(onChange: (Boolean) -> Unit) {
        onChange(isConnected())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { onChange(true) }
                override fun onLost(network: Network) { onChange(false) }
            }
            callback = cb
            try {
                connectivityManager.registerDefaultNetworkCallback(cb)
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        callback?.let {
            try { connectivityManager.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        callback = null
    }
}