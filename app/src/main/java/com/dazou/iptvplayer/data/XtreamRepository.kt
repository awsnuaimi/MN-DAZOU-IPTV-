package com.dazou.iptvplayer.data

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.model.*

class XtreamRepository(
    private val server: XtreamServer
) {


    val liveCategories =
        MutableLiveData<List<XtreamCategory>>()

    val liveChannels =
        MutableLiveData<List<XtreamChannel>>()

    val vodCategories =
        MutableLiveData<List<XtreamCategory>>()

    val vodMovies =
        MutableLiveData<List<XtreamMovie>>()

    val seriesCategories =
        MutableLiveData<List<XtreamCategory>>()

    val series =
        MutableLiveData<List<XtreamSeries>>()

    val episodes =
        MutableLiveData<List<XtreamEpisode>>()





    fun loadLiveCategories() {


        Log.d(
            "XtreamRepository",
            "Loading live categories from ${server.url}"
        )


        XtreamAPI.getLiveCategories(server) {


            Log.d(
                "XtreamRepository",
                "Live categories count = ${it.size}"
            )


            liveCategories.postValue(it)

        }

    }





    fun loadLiveStreams(
        categoryId:String? = null
    ) {


        Log.d(
            "XtreamRepository",
            "Loading live streams"
        )


        XtreamAPI.getLiveStreams(
            server,
            categoryId
        ){


            Log.d(
                "XtreamRepository",
                "Live channels count = ${it.size}"
            )


            liveChannels.postValue(it)

        }

    }






    fun loadVodCategories() {


        XtreamAPI.getVodCategories(server){


            Log.d(
                "XtreamRepository",
                "VOD categories = ${it.size}"
            )


            vodCategories.postValue(it)


        }


    }







    fun loadMovies(
        categoryId:String? = null
    ) {


        XtreamAPI.getVodStreams(
            server,
            categoryId
        ){


            Log.d(
                "XtreamRepository",
                "Movies count = ${it.size}"
            )


            vodMovies.postValue(it)


        }


    }







    fun loadSeriesCategories() {


        XtreamAPI.getVodCategories(server){


            seriesCategories.postValue(it)


        }


    }







    fun loadSeries(
        categoryId:String? = null
    ) {


        XtreamAPI.getSeries(
            server,
            categoryId
        ){


            Log.d(
                "XtreamRepository",
                "Series count = ${it.size}"
            )


            series.postValue(it)


        }


    }







    fun loadEpisodes(
        seriesId:Int
    ) {


        XtreamAPI.getSeriesInfo(
            server,
            seriesId
        ){


            episodes.postValue(it)


        }


    }


}