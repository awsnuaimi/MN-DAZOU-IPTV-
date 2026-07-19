package com.dazou.iptvplayer.model

data class XtreamServer(val url: String, val username: String, val password: String)
data class XtreamCategory(val categoryId: String, val categoryName: String, val parentId: Int)
data class XtreamChannel(val streamId: Int, val name: String, val streamType: String, val streamIcon: String, val epgChannelId: String, val added: String, val categoryId: String, val containerExtension: String)
data class XtreamMovie(val streamId: Int, val name: String, val containerExtension: String, val streamIcon: String, val plot: String, val cast: String, val director: String, val genre: String, val rating: String, val year: String)
data class XtreamSeries(val seriesId: Int, val name: String, val cover: String, val plot: String, val cast: String, val director: String, val genre: String, val rating: String, val year: String)
data class XtreamEpisode(val id: Int, val episodeNum: Int, val seasonNum: Int, val title: String, val containerExtension: String, val info: String)
data class EpgProgram(val channelId: String, val title: String, val startTime: String, val endTime: String, val description: String)
data class FavoriteItem(
    val type: String,
    val id: Int,
    val name: String,
    val icon: String = "",
    val containerExtension: String = "ts"
)
data class HistoryItem(
    val type: String,
    val id: Int,
    val name: String,
    val timestamp: Long,
    val icon: String = "",
    val containerExtension: String = "ts",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)