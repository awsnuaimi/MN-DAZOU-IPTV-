package com.dazou.iptvplayer

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {
    @GET("player_api.php")
    fun getLiveStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_live_streams"): Call<List<StreamModel>>

    @GET("player_api.php")
    fun getVodStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_vod_streams"): Call<List<StreamModel>>

    @GET("player_api.php")
    fun getSeriesStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_series"): Call<List<StreamModel>>
}

data class StreamModel(val stream_id: String?, val series_id: String?, val name: String)
