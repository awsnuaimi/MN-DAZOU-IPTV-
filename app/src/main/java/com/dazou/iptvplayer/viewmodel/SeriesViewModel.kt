package com.dazou.iptvplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.dazou.iptvplayer.data.XtreamRepository
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.model.XtreamEpisode
import com.dazou.iptvplayer.model.XtreamSeries
import com.dazou.iptvplayer.model.XtreamServer

class SeriesViewModel(private val repository: XtreamRepository?) : ViewModel() {

    private val _categories = MutableLiveData<List<XtreamCategory>>()
    val categories: LiveData<List<XtreamCategory>> get() = _categories

    private val _seriesList = MutableLiveData<List<XtreamSeries>>()
    val seriesList: LiveData<List<XtreamSeries>> get() = _seriesList

    private val _episodes = MutableLiveData<List<XtreamEpisode>>()
    val episodes: LiveData<List<XtreamEpisode>> get() = _episodes

    private val categoriesObserver = Observer<List<XtreamCategory>> { _categories.postValue(it) }
    private val seriesObserver = Observer<List<XtreamSeries>> { _seriesList.postValue(it) }
    private val episodesObserver = Observer<List<XtreamEpisode>> { _episodes.postValue(it) }

    init {
        repository?.seriesCategories?.observeForever(categoriesObserver)
        repository?.series?.observeForever(seriesObserver)
        repository?.episodes?.observeForever(episodesObserver)
    }

    fun loadCategories() {
        val repo = repository
        if (repo == null) {
            _categories.value = emptyList()
            return
        }
        repo.loadSeriesCategories()
    }

    fun loadSeries(categoryId: String? = null) {
        val repo = repository
        if (repo == null) {
            _seriesList.value = emptyList()
            return
        }
        repo.loadSeries(categoryId)
    }

    fun loadEpisodes(seriesId: Int) {
        val repo = repository
        if (repo == null) {
            _episodes.value = emptyList()
            return
        }
        repo.loadEpisodes(seriesId)
    }

    fun getServer(): XtreamServer? = repository?.server

    override fun onCleared() {
        super.onCleared()
        repository?.seriesCategories?.removeObserver(categoriesObserver)
        repository?.series?.removeObserver(seriesObserver)
        repository?.episodes?.removeObserver(episodesObserver)
    }
}