package com.dazou.iptvplayer

import android.util.Log
import org.json.JSONArray
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
    val streamType: String,
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

    private const val TAG = "XtreamAPI"

    fun getLiveCategories(server: XtreamServer, callback: (List<XtreamCategory>) -> Unit) {
        thread {
            try {
                val url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_live_categories"
                val json = fetchJson(url)
                val categories = parseCategories(json)
                runOnUiThread { callback(categories) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading live categories", e)
                runOnUiThread { callback(emptyList()) }
            }
        }
    }

    fun getLiveStreams(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamChannel>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_live_streams"
                if (categoryId != null) url += "&category_id=$categoryId"
                val json = fetchJson(url)
                Log.d(TAG, "Live streams JSON (first 300): ${json.take(300)}")
                val channels = parseChannels(json)
                Log.d(TAG, "Parsed ${channels.size} live channels")
                runOnUiThread { callback(channels) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading live streams", e)
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
                Log.e(TAG, "Error loading VOD categories", e)
                runOnUiThread { callback(emptyList()) }
            }
        }
    }

    fun getVodStreams(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamMovie>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_vod_streams"
                if (categoryId != null) url += "&category_id=$categoryId"
                val json = fetchJson(url)
                Log.d(TAG, "VOD streams JSON (first 300): ${json.take(300)}")
                val movies = parseMovies(json)
                Log.d(TAG, "Parsed ${movies.size} movies")
                runOnUiThread { callback(movies) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading VOD streams", e)
                runOnUiThread { callback(emptyList()) }
            }
        }
    }

    fun getSeries(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamSeries>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_series"
                if (categoryId != null) url += "&category_id=$categoryId"
                val json = fetchJson(url)
                Log.d(TAG, "Series JSON (first 300): ${json.take(300)}")
                val series = parseSeries(json)
                Log.d(TAG, "Parsed ${series.size} series")
                runOnUiThread { callback(series) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading series", e)
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
                Log.e(TAG, "Error loading series info", e)
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

    private fun fetchJson(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
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
        val jsonArray = extractJsonArray(json)
        if (jsonArray == null) return categories
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
        val jsonArray = extractJsonArray(json)
        if (jsonArray == null) return channels
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
        val jsonArray = extractJsonArray(json)
        if (jsonArray == null) return movies
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
        val jsonArray = extractJsonArray(json)
        if (jsonArray == null) return series
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
            val jsonObj = JSONObject(json)
            // بعض السيرفرات ترجع "episodes" والبعض الآخر "episodes_list" أو "data"
            val episodesObj = jsonObj.optJSONObject("episodes")
                ?: jsonObj.optJSONObject("episodes_list")
                ?: jsonObj.optJSONObject("data")
            if (episodesObj != null) {
                // الحلقات قد تكون كائن فيه مفاتيح seasons
                val keys = episodesObj.keys()
                while (keys.hasNext()) {
                    val seasonKey = keys.next()
                    val seasonArray = episodesObj.optJSONArray(seasonKey) ?: continue
                    for (i in 0 until seasonArray.length()) {
                        val obj = seasonArray.getJSONObject(i)
                        episodes.add(XtreamEpisode(
                            id = obj.optInt("id", 0),
                            episodeNum = obj.optInt("episode_num", 0),
                            seasonNum = obj.optInt("season_num", seasonKey.toIntOrNull() ?: 1),
                            title = obj.optString("title", "حلقة ${i + 1}"),
                            containerExtension = obj.optString("container_extension", "mp4"),
                            info = obj.optString("info", "")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing episodes", e)
        }
        return episodes
    }

    // دالة ذكية لاستخراج JSON array سواء كانت مباشرة أو داخل كائن
    private fun extractJsonArray(json: String): JSONArray? {
        return try {
            val trimmed = json.trim()
            when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val jsonObject = JSONObject(trimmed)
                    // بعض السيرفرات تضع البيانات داخل مفتاح "data" أو "result"
                    jsonObject.optJSONArray("data")
                        ?: jsonObject.optJSONArray("result")
                        ?: jsonObject.optJSONArray("items")
                        ?: jsonObject.names()?.let { names ->
                            // قد يكون المصفوفة تحت أول مفتاح
                            jsonObject.optJSONArray(names.getString(0))
                        }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract JSON array", e)
            null
        }
    }

    private fun runOnUiThread(action: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }
}