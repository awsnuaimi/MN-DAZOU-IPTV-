package com.dazou.iptvplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dazou.iptvplayer.data.XtreamRepository

class ViewModelFactory(private val repository: XtreamRepository?) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LiveViewModel::class.java) ->
                LiveViewModel(repository) as T
            modelClass.isAssignableFrom(MoviesViewModel::class.java) ->
                MoviesViewModel(repository) as T
            modelClass.isAssignableFrom(SeriesViewModel::class.java) ->
                SeriesViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}