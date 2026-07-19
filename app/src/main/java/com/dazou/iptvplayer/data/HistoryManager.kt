package com.dazou.iptvplayer.data

import android.content.Context
import com.dazou.iptvplayer.model.HistoryItem
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("iptv_history", Context.MODE_PRIVATE)
    private val maxItems = 30

    fun getHistory(): List<HistoryItem> {
        val result = mutableListOf<HistoryItem>()
        val json = prefs.getString("items", "[]")
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                result.add(
                    HistoryItem(
                        type = o.getString("type"),
                        id = o.getInt("id"),
                        name = o.getString("name"),
                        timestamp = o.optLong("timestamp", 0L),
                        icon = o.optString("icon", ""),
                        containerExtension = o.optString("containerExtension", "ts"),
                        positionMs = o.optLong("positionMs", 0L),
                        durationMs = o.optLong("durationMs", 0L)
                    )
                )
            }
        } catch (_: Exception) { }
        return result
    }

    /** يضيف عنصر جديد لسجل المشاهدة، أو يحدّث توقيته لو موجود أصلاً وينقله لأول القائمة */
    fun addOrUpdateHistory(item: HistoryItem) {
        val current = getHistory().toMutableList()
        current.removeAll { it.type == item.type && it.id == item.id }
        current.add(0, item.copy(timestamp = System.currentTimeMillis()))
        val trimmed = if (current.size > maxItems) current.take(maxItems) else current
        saveHistory(trimmed)
    }

    /** يحدّث موقع التشغيل الحالي لعنصر موجود بالسجل — يُستخدم لاحقًا لاستكمال المشاهدة */
    fun updateProgress(type: String, id: Int, positionMs: Long, durationMs: Long) {
        val current = getHistory().toMutableList()
        val index = current.indexOfFirst { it.type == type && it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(positionMs = positionMs, durationMs = durationMs)
            saveHistory(current)
        }
    }

    fun getSavedProgress(type: String, id: Int): HistoryItem? =
        getHistory().firstOrNull { it.type == type && it.id == id }

    private fun saveHistory(items: List<HistoryItem>) {
        val array = JSONArray()
        items.forEach {
            val o = JSONObject()
            o.put("type", it.type)
            o.put("id", it.id)
            o.put("name", it.name)
            o.put("timestamp", it.timestamp)
            o.put("icon", it.icon)
            o.put("containerExtension", it.containerExtension)
            o.put("positionMs", it.positionMs)
            o.put("durationMs", it.durationMs)
            array.put(o)
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}