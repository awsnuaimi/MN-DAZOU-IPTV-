package com.dazou.iptvplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.model.XtreamCategory

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _activeChannel = MutableLiveData<XtreamChannel?>()
    val activeChannel: LiveData<XtreamChannel?> = _activeChannel

    private val _isFullscreen = MutableLiveData(false)
    val isFullscreen: LiveData<Boolean> = _isFullscreen

    val categories = MutableLiveData<List<XtreamCategory>>(emptyList())

    val channels = MutableLiveData<List<XtreamChannel>>(emptyList())


    fun playChannel(channel: XtreamChannel) {

        _activeChannel.value = channel

        getApplication<Application>()
            .getSharedPreferences("app_data", 0)
            .edit()
            .putInt("last_channel_id", channel.streamId)
            .apply()
    }


    fun toggleFullscreen() {

        _isFullscreen.value = !(_isFullscreen.value ?: false)

    }
}