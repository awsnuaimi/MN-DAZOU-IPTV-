package com.dazou.iptvplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.dazou.iptvplayer.model.XtreamChannel

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val channels = MutableLiveData<List<XtreamChannel>>()

    val movies = MutableLiveData<List<XtreamChannel>>()

    val series = MutableLiveData<List<XtreamChannel>>()


    fun setChannels(list: List<XtreamChannel>) {
        channels.value = list
    }


    fun setMovies(list: List<XtreamChannel>) {
        movies.value = list
    }


    fun setSeries(list: List<XtreamChannel>) {
        series.value = list
    }

}