package com.dazou.iptvplayer.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.dazou.iptvplayer.data.XtreamRepository
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.model.XtreamServer

class LiveViewModel(private val repository: XtreamRepository?) : ViewModel() {

    private val _categories = MutableLiveData<List<XtreamCategory>>()
    val categories: LiveData<List<XtreamCategory>> get() = _categories

    private val _channels = MutableLiveData<List<XtreamChannel>>()
    val channels: LiveData<List<XtreamChannel>> get() = _channels

    // مراقِبون ثابتون (نفس الكائن) — يُسجَّلون مرة واحدة فقط ويُزالون بشكل صحيح لاحقًا
    private val categoriesObserver = Observer<List<XtreamCategory>> { cats ->
        Log.d("LiveViewModel", "Categories received: ${cats.size}")
        _categories.postValue(cats)
    }
    private val channelsObserver = Observer<List<XtreamChannel>> { ch ->
        Log.d("LiveViewModel", "Channels received: ${ch.size}")
        _channels.postValue(ch)
    }

    init {
        repository?.liveCategories?.observeForever(categoriesObserver)
        repository?.liveChannels?.observeForever(channelsObserver)
    }

    fun loadCategories() {
        val repo = repository
        if (repo == null) {
            Log.e("LiveViewModel", "Repository is null! No active account.")
            _categories.value = emptyList()
            return
        }
        Log.d("LiveViewModel", "Loading categories")
        repo.loadLiveCategories()
    }

    fun loadChannels(categoryId: String? = null) {
        val repo = repository
        if (repo == null) {
            _channels.value = emptyList()
            return
        }
        repo.loadLiveStreams(categoryId)
    }

    fun getServer(): XtreamServer? = repository?.server

    override fun onCleared() {
        super.onCleared()
        repository?.liveCategories?.removeObserver(categoriesObserver)
        repository?.liveChannels?.removeObserver(channelsObserver)
    }
}