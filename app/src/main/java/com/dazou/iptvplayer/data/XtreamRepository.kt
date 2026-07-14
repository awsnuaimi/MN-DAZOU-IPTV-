package com.dazou.iptvplayer.data

import androidx.lifecycle.MutableLiveData
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.model.*

class XtreamRepository(private val server: XtreamServer) {

    val liveCategories = MutableLiveData<List<XtreamCategory>>()
    val liveChannels = MutableLiveData<List<XtreamChannel>>()
    val vodCategories = MutableLiveData<List<XtreamCategory>>()
    val vodMovies = MutableLiveData<List<XtreamMovie>>()
    val seriesCategories = MutableLiveData<List<XtreamCategory>>()
    val series = MutableLiveData<List<XtreamSeries>>()
    val episodes = MutableLiveData<List<XtreamEpisode>>()

    fun loadLiveCategories() {
        XtreamAPI.getLiveCategories(server) { liveCategories.postValue(it) }
    }

    fun loadLiveStreams(categoryId: String? = null) {
        XtreamAPI.getLiveStreams(server, categoryId) { liveChannels.postValue(it) }
    }

    fun loadVodCategories() {
        XtreamAPI.getVodCategories(server) { vodCategories.postValue(it) }
    }

    fun loadMovies(categoryId: String? = null) {
        XtreamAPI.getVodStreams(server, categoryId) { vodMovies.postValue(it) }
    }

    fun loadSeriesCategories() {
        XtreamAPI.getLiveCategories(server) { seriesCategories.postValue(it) }
    }

    fun loadSeries(categoryId: String? = null) {
        XtreamAPI.getSeries(server, categoryId) { series.postValue(it) }
    }

    fun loadEpisodes(seriesId: Int) {
        XtreamAPI.getSeriesInfo(server, seriesId) { episodes.postValue(it) }
    }
}