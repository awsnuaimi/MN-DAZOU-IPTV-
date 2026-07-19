package com.dazou.iptvplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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

    private val categoriesObserver = Observer<List<XtreamCategory>> { cats -> _categories.postValue(cats) }
    private val moviesObserver = Observer<List<XtreamMovie>> { list -> _movies.postValue(list) }

    init {
        repository?.vodCategories?.observeForever(categoriesObserver)
        repository?.vodMovies?.observeForever(moviesObserver)
    }

    fun loadCategories() {
        val repo = repository
        if (repo == null) {
            _categories.value = emptyList()
            return
        }
        repo.loadVodCategories()
    }

    fun loadMovies(categoryId: String? = null) {
        val repo = repository
        if (repo == null) {
            _movies.value = emptyList()
            return
        }
        repo.loadMovies(categoryId)
    }

    fun getServer(): XtreamServer? = repository?.server

    override fun onCleared() {
        super.onCleared()
        repository?.vodCategories?.removeObserver(categoriesObserver)
        repository?.vodMovies?.removeObserver(moviesObserver)
    }
}