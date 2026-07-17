package com.dazou.iptvplayer.api

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import com.dazou.iptvplayer.model.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object XtreamAPI {
    private const val TAG = "XtreamAPI"

    var lastRequestUrl: String = ""
    var lastResponseBody: String = ""
    var lastErrorMessage: String = ""
    var lastItemCount: Int = -1

    fun getLiveCategories(server: XtreamServer, callback: (List<XtreamCategory>) -> Unit) {
        thread {
            try {
                val url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_live_categories"
                val json = fetchJson(url)
                val categories = parseCategories(json)
                runOnUiThread { callback(categories) }
            } catch (e: Exception) {
                Log.e(TAG, "live categories error", e)
                lastErrorMessage = e.message ?: "خطأ غير معروف"
                runOnUiThread { callback(emptyList()) }
            }
        }
    }

    fun getLiveStreams(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamChannel>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_live_streams"
                if (categoryId != null) url += "&category_id=$categoryId"
                lastRequestUrl = url
                val json = fetchJson(url)
                lastResponseBody = json.take(500)
                Log.d(TAG, "Live streams JSON first 500: $lastResponseBody")
                val channels = parseChannels(json)
                lastItemCount = channels.size
                Log.d(TAG, "Parsed ${channels.size} live channels")
                runOnUiThread { callback(channels) }
            } catch (e: Exception) {
                Log.e(TAG, "live streams error", e)
                lastErrorMessage = e.message ?: "خطأ غير معروف"
                lastResponseBody = ""
                lastItemCount = -1
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
                Log.e(TAG, "vod categories error", e)
                lastErrorMessage = e.message ?: "خطأ غير معروف"
                runOnUiThread { callback(emptyList()) }
            }
        }
    }

    fun getVodStreams(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamMovie>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_vod_streams"
                if (categoryId != null) url += "&category_id=$categoryId"
                lastRequestUrl = url
                val json = fetchJson(url)
                lastResponseBody = json.take(500)
                Log.d(TAG, "VOD streams JSON first 500: $lastResponseBody")
                val movies = parseMovies(json)
                lastItemCount = movies.size
                Log.d(TAG, "Parsed ${movies.size} movies")
                runOnUiThread { callback(movies) }
            } catch (e: Exception) {
                Log.e(TAG, "vod streams error", e)
                lastErrorMessage = e.message ?: "خطأ غير معروف"
                lastResponseBody = ""
                lastItemCount = -1
                runOnUiThread { callback(emptyList()) }
            }
        }
    }

    fun getSeries(server: XtreamServer, categoryId: String? = null, callback: (List<XtreamSeries>) -> Unit) {
        thread {
            try {
                var url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_series"
                if (categoryId != null) url += "&category_id=$categoryId"
                lastRequestUrl = url
                val json = fetchJson(url)
                lastResponseBody = json.take(500)
                Log.d(TAG, "Series JSON first 500: $lastResponseBody")
                val series = parseSeries(json)
                lastItemCount = series.size
                Log.d(TAG, "Parsed ${series.size} series")
                runOnUiThread { callback(series) }
            } catch (e: Exception) {
                Log.e(TAG, "series error", e)
                lastErrorMessage = e.message ?: "خطأ غير معروف"
                lastResponseBody = ""
                lastItemCount = -1
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
                Log.e(TAG, "series info error", e)
                runOnUiThread { callback(emptyList()) }
            }
        }
    }

    fun getShortEpg(server: XtreamServer, streamId: Int, callback: (List<XtreamEpgProgram>) -> Unit) {
        thread {
            try {
                val url = "${server.url}/player_api.php?username=${server.username}&password=${server.password}&action=get_short_epg&stream_id=$streamId"
                val json = fetchJson(url)
                val programs = parseEpg(json)
                runOnUiThread { callback(programs) }
            } catch (e: Exception) {
                Log.e(TAG, "epg error", e)
                runOnUiThread { callback(emptyList()) }
            }
        }
    }

    fun getStreamUrl(server: XtreamServer, streamId: Int, extension: String = "ts", type: String = "live") =
        "${server.url}/$type/${server.username}/${server.password}/$streamId.$extension"

    fun getMovieUrl(server: XtreamServer, streamId: Int, extension: String = "mp4") =
        "${server.url}/movie/${server.username}/${server.password}/$streamId.$extension"

    fun getSeriesEpisodeUrl(server: XtreamServer, episodeId: Int, extension: String = "mp4") =
        "${server.url}/series/${server.username}/${server.password}/$episodeId.$extension"

    private fun fetchJson(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) response.append(line)
        reader.close()
        conn.disconnect()
        return response.toString()
    }

    private fun extractJsonArray(json: String): JSONArray? {
        val trimmed = json.trim()
        if (trimmed.startsWith("[")) return try { JSONArray(trimmed) } catch (_: Exception) { null }
        if (trimmed.startsWith("{")) {
            return try {
                val obj = JSONObject(trimmed)
                val possibleKeys = listOf("available_channels", "channels", "streams", "live_streams", "movies", "series", "data")
                for (key in possibleKeys) { if (obj.has(key)) return obj.getJSONArray(key) }
                null
            } catch (_: Exception) { null }
        }
        return null
    }

    private fun parseCategories(json: String): List<XtreamCategory> { val list = mutableListOf<XtreamCategory>(); val arr = extractJsonArray(json) ?: return list; for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); list.add(XtreamCategory(o.optString("category_id"), o.optString("category_name", "?"), o.optInt("parent_id"))) }; return list }
    private fun parseChannels(json: String): List<XtreamChannel> { val list = mutableListOf<XtreamChannel>(); val arr = extractJsonArray(json) ?: return list; for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); list.add(XtreamChannel(o.optInt("stream_id"), o.optString("name", "Ch $i"), o.optString("stream_type", "live"), o.optString("stream_icon"), o.optString("epg_channel_id"), o.optString("added"), o.optString("category_id"), o.optString("container_extension", "ts"))) }; return list }
    private fun parseMovies(json: String): List<XtreamMovie> { val list = mutableListOf<XtreamMovie>(); val arr = extractJsonArray(json) ?: return list; for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); list.add(XtreamMovie(o.optInt("stream_id"), o.optString("name", "Movie $i"), o.optString("container_extension", "mp4"), o.optString("stream_icon"), o.optString("plot"), o.optString("cast"), o.optString("director"), o.optString("genre"), o.optString("rating"), o.optString("year"))) }; return list }
    private fun parseSeries(json: String): List<XtreamSeries> { val list = mutableListOf<XtreamSeries>(); val arr = extractJsonArray(json) ?: return list; for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); list.add(XtreamSeries(o.optInt("series_id"), o.optString("name", "Series $i"), o.optString("cover"), o.optString("plot"), o.optString("cast"), o.optString("director"), o.optString("genre"), o.optString("rating"), o.optString("year"))) }; return list }
    private fun parseEpisodes(json: String): List<XtreamEpisode> { val episodes = mutableListOf<XtreamEpisode>(); try { val root = JSONObject(json); val epsObj = root.optJSONObject("episodes") ?: root.optJSONObject("episodes_list") ?: root.optJSONObject("data"); epsObj?.let { val keys = it.keys(); while (keys.hasNext()) { val seasonKey = keys.next(); val seasonArr = it.optJSONArray(seasonKey) ?: continue; for (i in 0 until seasonArr.length()) { val ep = seasonArr.getJSONObject(i); episodes.add(XtreamEpisode(ep.optInt("id"), ep.optInt("episode_num"), seasonKey.toIntOrNull() ?: 1, ep.optString("title", "Ep $i"), ep.optString("container_extension", "mp4"), ep.optString("info"))) } } } } catch (_: Exception) {}; return episodes }

    private fun parseEpg(json: String): List<XtreamEpgProgram> {
        val list = mutableListOf<XtreamEpgProgram>()
        try {
            val root = JSONObject(json)
            val arr = root.optJSONArray("epg_listings") ?: return list
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val start = o.optLong("start_timestamp", 0L)
                val stop = o.optLong("stop_timestamp", 0L)
                val titleB64 = o.optString("title", "")
                val title = try {
                    String(android.util.Base64.decode(titleB64, android.util.Base64.DEFAULT))
                } catch (_: Exception) { titleB64 }
                list.add(
                    XtreamEpgProgram(
                        title = title,
                        startTimestamp = start,
                        stopTimestamp = stop,
                        nowPlaying = o.optString("now_playing") == "1"
                    )
                )
            }
        } catch (_: Exception) { }
        return list
    }

    private fun runOnUiThread(action: () -> Unit) { android.os.Handler(android.os.Looper.getMainLooper()).post(action) }
}