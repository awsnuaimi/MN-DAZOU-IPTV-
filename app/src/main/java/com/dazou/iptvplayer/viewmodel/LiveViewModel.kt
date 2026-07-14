package com.dazou.iptvplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dazou.iptvplayer.data.XtreamRepository
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.model.XtreamServer

class LiveViewModel : ViewModel() {

    private var repository: XtreamRepository? = null
    private val _channels = MutableLiveData<List<XtreamChannel>>()
    val channels: LiveData<List<XtreamChannel>> = _channels

    fun setServer(server: XtreamServer) {
        repository = XtreamRepository(server)
    }

    fun loadAllChannels() {
        repository?.loadLiveStreams()
        repository?.liveChannels?.observeForever { _channels.postValue(it) }
    }

    fun getServer(): XtreamServer {
        // TODO: استرجاع السيرفر من SharedPreferences
        return XtreamServer("", "", "")
    }
}