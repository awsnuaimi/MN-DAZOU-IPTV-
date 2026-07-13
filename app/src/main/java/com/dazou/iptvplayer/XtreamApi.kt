package com.dazou.iptvplayer

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class XtreamServer(
    val url: String,
    val username: String,
    val password: String
)

data class XtreamCategory(
    val categoryId: String,
    val categoryName: String,
    val parentId: Int
)

data class XtreamChannel(
    val streamId: Int,
    val name: String,
    val streamType: String, // live, movie
    val streamIcon: String,
    val epgChannelId: String,
    val added: String,
    val categoryId: String,
    val containerExtension: String
)

data class XtreamMovie(
    val streamId: Int,
    val name: String,
    val containerExtension: String,
    val streamIcon: String,
    val plot: String,
    val cast: String,
    val director: String,
    val genre: String,
    val rating: String,
    val year: String
)

data class XtreamSeries(
    val seriesId: Int,
    val name: String,
    val cover: String,
    val plot: String,
    val cast: String,
    val director: String,
    val genre: String,
    val rating: String,
    val year: String
)

data class XtreamEpisode(
    val id: Int,
    val episodeNum: Int,
    val seasonNum: Int,
    val title: String,
    val containerExtension: String,
    val info: String
)

object XtreamAPI {
    
    fun getLiveCategories(server: XtreamServer, callback: (List<XtreamCategory>) -> Unit) {
        thread {
            try {
                val url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_live_categories"
                val json = fetchJson(url)
                val categories = parseCategories(json)
                runOnUiThread { callback(categories) }
            } catch (e: Exception) {
                runOnUiThread { callback(emptyList()) }
            }
        }
    }
    
    fun getLiveStreams(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamChannel>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_live_streams"
                if (categoryId != null) {
                    url += "&category_id=$categoryId"
                }
                val json = fetchJson(url)
                val channels = parseChannels(json)
                runOnUiThread { callback(channels) }
            } catch (e: Exception) {
                runOnUiThread { callback(emptyList()) }
            }
        }
    }
    
    fun getVodCategories(server: XtreamServer, callback: (List<XtreamCategory>) -> Unit) {
        thread {
            try {
                val url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_vod_categories"
                val json = fetchJson(url)
                val categories = parseCategories(json)
                runOnUiThread { callback(categories) }
            } catch (e: Exception) {
                runOnUiThread { callback(emptyList()) }
            }
        }
    }
    
    fun getVodStreams(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamMovie>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_vod_streams"
                if (categoryId != null) {
                    url += "&category_id=$categoryId"
                }
                val json = fetchJson(url)
                val movies = parseMovies(json)
                runOnUiThread { callback(movies) }
            } catch (e: Exception) {
                runOnUiThread { callback(emptyList()) }
            }
        }
    }
    
    fun getSeries(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamSeries>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_series"
                if (categoryId != null) {
                    url += "&category_id=$categoryId"
                }
                val json = fetchJson(url)
                val series = parseSeries(json)
                runOnUiThread { callback(series) }
            } catch (e: Exception) {
                runOnUiThread { callback(emptyList()) }
            }
        }
    }
    
    fun getSeriesInfo(server: XtreamServer, seriesId: Int, callback: (List<XtreamEpisode>) -> Unit) {
        thread {
            try {
                val url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_series_info&series_id=$seriesId"
                val json = fetchJson(url)
                val episodes = parseEpisodes(json)
                runOnUiThread { callback(episodes) }
            } catch (e: Exception) {
                runOnUiThread { callback(emptyList()) }
            }
        }
    }
    
    fun getStreamUrl(server: XtreamServer, streamId: Int, extension: String = "ts", type: String = "live"): String {
        return "${server.url}/$type/${server.username}/${server.password}/$streamId.$extension"
    }
    
    fun getMovieUrl(server: XtreamServer, streamId: Int, extension: String = "mp4"): String {
        return "${server.url}/movie/${server.username}/${server.password}/$streamId.$extension"
    }
    
    fun getSeriesEpisodeUrl(server: XtreamServer, episodeId: Int, extension: String = "mp4"): String {
        return "${server.url}/series/${server.username}/${server.password}/$episodeId.$extension"
    }
    
    // دوال مساعدة
    private fun fetchJson(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        conn.disconnect()
        return response.toString()
    }
    
    private fun parseCategories(json: String): List<XtreamCategory> {
        val categories = mutableListOf<XtreamCategory>()
        val jsonArray = org.json.JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            categories.add(XtreamCategory(
                categoryId = obj.optString("category_id", ""),
                categoryName = obj.optString("category_name", "غير معروف"),
                parentId = obj.optInt("parent_id", 0)
            ))
        }
        return categories
    }
    
    private fun parseChannels(json: String): List<XtreamChannel> {
        val channels = mutableListOf<XtreamChannel>()
        val jsonArray = org.json.JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            channels.add(XtreamChannel(
                streamId = obj.optInt("stream_id", 0),
                name = obj.optString("name", "قناة ${i + 1}"),
                streamType = obj.optString("stream_type", "live"),
                streamIcon = obj.optString("stream_icon", ""),
                epgChannelId = obj.optString("epg_channel_id", ""),
                added = obj.optString("added", ""),
                categoryId = obj.optString("category_id", ""),
                containerExtension = obj.optString("container_extension", "ts")
            ))
        }
        return channels
    }
    
    private fun parseMovies(json: String): List<XtreamMovie> {
        val movies = mutableListOf<XtreamMovie>()
        val jsonArray = org.json.JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            movies.add(XtreamMovie(
                streamId = obj.optInt("stream_id", 0),
                name = obj.optString("name", "فيلم ${i + 1}"),
                containerExtension = obj.optString("container_extension", "mp4"),
                streamIcon = obj.optString("stream_icon", ""),
                plot = obj.optString("plot", ""),
                cast = obj.optString("cast", ""),
                director = obj.optString("director", ""),
                genre = obj.optString("genre", ""),
                rating = obj.optString("rating", ""),
                year = obj.optString("year", "")
            ))
        }
        return movies
    }
    
    private fun parseSeries(json: String): List<XtreamSeries> {
        val series = mutableListOf<XtreamSeries>()
        val jsonArray = org.json.JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            series.add(XtreamSeries(
                seriesId = obj.optInt("series_id", 0),
                name = obj.optString("name", "مسلسل ${i + 1}"),
                cover = obj.optString("cover", ""),
                plot = obj.optString("plot", ""),
                cast = obj.optString("cast", ""),
                director = obj.optString("director", ""),
                genre = obj.optString("genre", ""),
                rating = obj.optString("rating", ""),
                year = obj.optString("year", "")
            ))
        }
        return series
    }
    
    private fun parseEpisodes(json: String): List<XtreamEpisode> {
        val episodes = mutableListOf<XtreamEpisode>()
        try {
            val jsonObj = org.json.JSONObject(json)
            val episodesArray = jsonObj.getJSONObject("episodes").getJSONArray("1") // Season 1
            for (i in 0 until episodesArray.length()) {
                val obj = episodesArray.getJSONObject(i)
                episodes.add(XtreamEpisode(
                    id = obj.optInt("id", 0),
                    episodeNum = obj.optInt("episode_num", 0),
                    seasonNum = obj.optInt("season_num", 1),
                    title = obj.optString("title", "حلقة ${i + 1}"),
                    containerExtension = obj.optString("container_extension", "mp4"),
                    info = obj.optString("info", "")
                ))
            }
        } catch (e: Exception) {
            // لو في مواسم متعددة، نتعامل معها لاحقاً
        }
        return episodes
    }
    
    private fun runOnUiThread(action: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }
}