import com.dazou.iptvplayer.model.*
data class FavoriteItem(val type: String, val id: Int, val name: String)
data class HistoryItem(val type: String, val id: Int, val name: String, val timestamp: Long)