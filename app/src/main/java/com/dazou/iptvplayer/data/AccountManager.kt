package com.dazou.iptvplayer.data

import android.content.Context
import com.dazou.iptvplayer.model.XtreamServer
import org.json.JSONArray
import org.json.JSONObject

class AccountManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("iptv_accounts", Context.MODE_PRIVATE)

    fun saveAccount(server: XtreamServer) {
        val accounts = getAccounts().toMutableList()
        accounts.add(server)
        saveAccounts(accounts)
    }

    fun getAccounts(): List<XtreamServer> {
        val result = mutableListOf<XtreamServer>()
        val json = prefs.getString("accounts", "[]")
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    XtreamServer(
                        obj.getString("url"),
                        obj.getString("username"),
                        obj.getString("password")
                    )
                )
            }
        } catch (_: Exception) { }
        return result
    }

    private fun saveAccounts(accounts: List<XtreamServer>) {
        val array = JSONArray()
        accounts.forEach {
            val obj = JSONObject()
            obj.put("url", it.url)
            obj.put("username", it.username)
            obj.put("password", it.password)
            array.put(obj)
        }
        prefs.edit().putString("accounts", array.toString()).apply()
    }

    fun setActiveAccount(position: Int) {
        prefs.edit().putInt("active_account", position).apply()
    }

    fun getActiveAccount(): XtreamServer? {
        val position = prefs.getInt("active_account", -1)
        val accounts = getAccounts()
        return if (position >= 0 && position < accounts.size) accounts[position] else null
    }

    /**
     * يحذف الحساب في الموقع المحدد، ويصحح تلقائيًا فهرس "الحساب النشط"
     * حتى لا يتحول الحساب النشط لحساب آخر بالخطأ بعد الحذف.
     */
    fun deleteAccount(position: Int) {
        val accounts = getAccounts().toMutableList()
        if (position < 0 || position >= accounts.size) return

        accounts.removeAt(position)
        saveAccounts(accounts)

        val activeIndex = prefs.getInt("active_account", -1)
        val newActiveIndex = when {
            activeIndex == -1 -> -1
            position == activeIndex -> -1   // الحساب المحذوف كان هو النشط
            position < activeIndex -> activeIndex - 1
            else -> activeIndex
        }
        prefs.edit().putInt("active_account", newActiveIndex).apply()
    }
}