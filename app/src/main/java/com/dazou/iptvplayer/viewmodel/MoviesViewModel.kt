package com.dazou.iptvplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dazou.iptvplayer.data.XtreamRepository
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.model.XtreamMovie
import com.dazou.iptvplayer.model.XtreamServer

class MoviesViewModel(private val repository: XtreamRepository?) : ViewModel() {

    private val _categories = MutableLiveData<List<XtreamCategory>>()
    val categories: LiveData<List<XtreamCategory>> get() = _categories

    private val _movies = MutableLiveData<List<XtreamMovie>>()
    val movies: LiveData<List<XtreamMovie>> get() = _movies

    fun loadCategories() {
        val repo = repository
        if (repo == null) {
            _categories.value = emptyList()
            return
        }
        repo.vodCategories.observeForever { cats -> _categories.postValue(cats) }
        repo.loadVodCategories()
    }

    fun loadMovies(categoryId: String? = null) {
        val repo = repository
        if (repo == null) {
            _movies.value = emptyList()
            return
        }
        repo.vodMovies.observeForever { list -> _movies.postValue(list) }
        repo.loadMovies(categoryId)
    }

    fun getServer(): XtreamServer? = repository?.server
}