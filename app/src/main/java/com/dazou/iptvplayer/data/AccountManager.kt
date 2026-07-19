package com.dazou.iptvplayer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dazou.iptvplayer.model.XtreamServer
import org.json.JSONArray
import org.json.JSONObject

class AccountManager(private val context: Context) {

    // ✅ تخزين مشفّر لبيانات الحسابات (يشمل كلمات السر) بدل SharedPreferences العادية
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "iptv_accounts_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // احتياط: لو فشل إنشاء التخزين المشفّر لأي سبب (نادر)، نرجع لتخزين عادي
            // بدل ما نكرش التطبيق بالكامل
            context.getSharedPreferences("iptv_accounts", Context.MODE_PRIVATE)
        }
    }

    init {
        migrateFromLegacyPrefsIfNeeded()
    }

    /**
     * لو كان عند المستخدم حسابات محفوظة من قبل بالتخزين القديم غير المشفّر،
     * ننقلها تلقائيًا للتخزين المشفّر الجديد مرة وحدة، ثم نمسح النسخة القديمة.
     */
    private fun migrateFromLegacyPrefsIfNeeded() {
        try {
            val legacyPrefs = context.getSharedPreferences("iptv_accounts", Context.MODE_PRIVATE)
            val legacyJson = legacyPrefs.getString("accounts", null)
            val alreadyMigrated = prefs.contains("accounts")

            if (!legacyJson.isNullOrEmpty() && !alreadyMigrated) {
                prefs.edit()
                    .putString("accounts", legacyJson)
                    .putInt("active_account", legacyPrefs.getInt("active_account", -1))
                    .apply()

                // مسح النسخة القديمة غير المشفّرة بعد نقل البيانات بنجاح
                legacyPrefs.edit().clear().apply()
            }
        } catch (_: Exception) {
            // لو فشل النقل لأي سبب، نتجاهله بهدوء ونكمل بالتخزين الجديد فاضي
        }
    }

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

    fun deleteAccount(position: Int) {
        val accounts = getAccounts().toMutableList()
        if (position < 0 || position >= accounts.size) return

        accounts.removeAt(position)
        saveAccounts(accounts)

        val activeIndex = prefs.getInt("active_account", -1)
        val newActiveIndex = when {
            activeIndex == -1 -> -1
            position == activeIndex -> -1
            position < activeIndex -> activeIndex - 1
            else -> activeIndex
        }
        prefs.edit().putInt("active_account", newActiveIndex).apply()
    }
}