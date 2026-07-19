package com.dazou.iptvplayer.data

import android.content.Context
import com.dazou.iptvplayer.model.FavoriteItem
import org.json.JSONArray
import org.json.JSONObject

class FavoritesManager(context: Context) {

    private val prefs = context.getSharedPreferences("iptv_favorites", Context.MODE_PRIVATE)

    fun getFavorites(): List<FavoriteItem> {
        val result = mutableListOf<FavoriteItem>()
        val json = prefs.getString("items", "[]")
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                result.add(
                    FavoriteItem(
                        type = o.getString("type"),
                        id = o.getInt("id"),
                        name = o.getString("name"),
                        icon = o.optString("icon", ""),
                        containerExtension = o.optString("containerExtension", "ts")
                    )
                )
            }
        } catch (_: Exception) { }
        return result
    }

    fun isFavorite(type: String, id: Int): Boolean =
        getFavorites().any { it.type == type && it.id == id }

    /** يضيف العنصر لو مو موجود، أو يحذفه لو موجود أصلاً. يرجع true لو صار مضافًا، false لو انحذف. */
    fun toggleFavorite(item: FavoriteItem): Boolean {
        val current = getFavorites().toMutableList()
        val existingIndex = current.indexOfFirst { it.type == item.type && it.id == item.id }
        return if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            saveFavorites(current)
            false
        } else {
            current.add(item)
            saveFavorites(current)
            true
        }
    }

    fun removeFavorite(type: String, id: Int) {
        val current = getFavorites().toMutableList()
        current.removeAll { it.type == type && it.id == id }
        saveFavorites(current)
    }

    private fun saveFavorites(items: List<FavoriteItem>) {
        val array = JSONArray()
        items.forEach {
            val o = JSONObject()
            o.put("type", it.type)
            o.put("id", it.id)
            o.put("name", it.name)
            o.put("icon", it.icon)
            o.put("containerExtension", it.containerExtension)
            array.put(o)
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}