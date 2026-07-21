package com.dazou.iptvplayer.data

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.model.*

class XtreamRepository(
    val server: XtreamServer
) {

    // Live Data for Live TV
    val liveCategories = MutableLiveData<List<XtreamCategory>>()
    val liveChannels = MutableLiveData<List<XtreamChannel>>()

    // Live Data for VOD (Movies)
    val vodCategories = MutableLiveData<List<XtreamCategory>>()
    val vodMovies = MutableLiveData<List<XtreamMovie>>()

    // Live Data for Series
    val seriesCategories = MutableLiveData<List<XtreamCategory>>()
    val series = MutableLiveData<List<XtreamSeries>>()
    val episodes = MutableLiveData<List<XtreamEpisode>>()

    // ========== Live TV Functions ==========
    fun loadLiveCategories() {
        Log.d("XtreamRepository", "Loading live categories")
        XtreamAPI.getLiveCategories(server) {
            Log.d("XtreamRepository", "Live categories count = ${it.size}")
            liveCategories.postValue(it)
        }
    }

    fun loadLiveStreams(categoryId: String? = null) {
        Log.d("XtreamRepository", "Loading live streams")
        XtreamAPI.getLiveStreams(server, categoryId) {
            Log.d("XtreamRepository", "Live channels count = ${it.size}")
            liveChannels.postValue(it)
        }
    }

    // ========== VOD (Movies) Functions ==========
    fun loadVodCategories() {
        Log.d("XtreamRepository", "Loading VOD categories")
        XtreamAPI.getVodCategories(server) {
            Log.d("XtreamRepository", "VOD categories = ${it.size}")
            vodCategories.postValue(it)
        }
    }

    fun loadMovies(categoryId: String? = null) {
        Log.d("XtreamRepository", "Loading movies")
        XtreamAPI.getVodStreams(server, categoryId) {
            Log.d("XtreamRepository", "Movies count = ${it.size}")
            vodMovies.postValue(it)
        }
    }

    // ========== Series Functions ==========
    fun loadSeriesCategories() {
        Log.d("XtreamRepository", "Loading series categories")
        XtreamAPI.getSeriesCategories(server) {  // ✅ تم التصحيح
            Log.d("XtreamRepository", "Series categories = ${it.size}")
            seriesCategories.postValue(it)
        }
    }

    fun loadSeries(categoryId: String? = null) {
        Log.d("XtreamRepository", "Loading series")
        XtreamAPI.getSeries(server, categoryId) {
            Log.d("XtreamRepository", "Series count = ${it.size}")
            series.postValue(it)
        }
    }

    fun loadEpisodes(seriesId: Int) {
        Log.d("XtreamRepository", "Loading episodes for series ID: $seriesId")
        XtreamAPI.getSeriesInfo(server, seriesId) {
            Log.d("XtreamRepository", "Episodes count = ${it.size}")
            episodes.postValue(it)
        }
    }
}