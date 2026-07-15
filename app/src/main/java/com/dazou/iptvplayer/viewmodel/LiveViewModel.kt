package com.dazou.iptvplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.dazou.iptvplayer.data.AccountManager
import com.dazou.iptvplayer.data.XtreamRepository
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.model.XtreamServer


class LiveViewModel(
    application: Application
) : AndroidViewModel(application) {


    private var repository: XtreamRepository? = null


    private val accountManager =
        AccountManager(application)



    private val _channels =
        MutableLiveData<List<XtreamChannel>>()


    val channels: LiveData<List<XtreamChannel>>
        get() = _channels



    private var channelsObserver:
            Observer<List<XtreamChannel>>? = null





    fun loadAllChannels() {


        val server =
            accountManager.getActiveAccount()



        if (server == null) {


            _channels.value =
                emptyList()


            return

        }



        repository =
            XtreamRepository(server)




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
            .loadLiveStreams()


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



        channelsObserver?.let {


            repository
                ?.liveChannels
                ?.removeObserver(it)


        }


    }


}