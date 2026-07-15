package com.dazou.iptvplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.dazou.iptvplayer.data.AccountManager
import com.dazou.iptvplayer.data.XtreamRepository
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.model.XtreamServer


class LiveViewModel(
    application: Application
) : AndroidViewModel(application) {


    private var repository: XtreamRepository? = null


    private val accountManager =
    (application as com.dazou.iptvplayer.App).accountManager



    private val _categories =
        MutableLiveData<List<XtreamCategory>>()

    val categories: LiveData<List<XtreamCategory>>
        get() = _categories



    private val _channels =
        MutableLiveData<List<XtreamChannel>>()


    val channels: LiveData<List<XtreamChannel>>
        get() = _channels



    private var categoriesObserver:
            Observer<List<XtreamCategory>>? = null


    private var channelsObserver:
            Observer<List<XtreamChannel>>? = null


    var currentCategoryName: String = ""





    fun loadCategories() {


        val server =
            accountManager.getActiveAccount()


        if (server == null) {

            _categories.value =
                emptyList()

            return

        }


        if (repository == null) {

            repository =
                XtreamRepository(server)

        }


        categoriesObserver =
            Observer {

                _categories.postValue(it)

            }


        repository!!
            .liveCategories
            .observeForever(
                categoriesObserver!!
            )


        repository!!
            .loadLiveCategories()

    }





    fun loadChannels(
        categoryId: String? = null
    ) {


        val server =
            accountManager.getActiveAccount()



        if (server == null) {


            _channels.value =
                emptyList()


            return

        }


        if (repository == null) {

            repository =
                XtreamRepository(server)

        }




        channelsObserver =
            Observer {


                _channels.postValue(it)


            }




        repository!!
            .liveChannels
            .observeForever(
                channelsObserver!!
            )



        repository!!
            .loadLiveStreams(categoryId)


    }





    fun loadAllChannels() {

        loadChannels(null)

    }







    fun getServer(): XtreamServer? {


        return accountManager
            .getActiveAccount()


    }







    fun setServer(
        server: XtreamServer
    ) {


        repository =
            XtreamRepository(server)


    }








    override fun onCleared() {


        super.onCleared()


        categoriesObserver?.let {

            repository
                ?.liveCategories
                ?.removeObserver(it)

        }


        channelsObserver?.let {


            repository
                ?.liveChannels
                ?.removeObserver(it)


        }


    }


}