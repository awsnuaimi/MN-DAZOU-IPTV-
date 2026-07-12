package com.dazou.iptvplayer

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// واجهة الاتصال بالسيرفر (تعريف المسارات)
interface XtreamApi {

    // جلب القنوات المباشرة
    @GET("player_api.php")
    fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): Call<List<StreamModel>>

    // جلب الأفلام
    @GET("player_api.php")
    fun getVodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): Call<List<StreamModel>>
}

// نموذج البيانات (Data Model) الذي يمثل القناة أو الفيلم
data class StreamModel(
    val stream_id: String,
    val name: String,
    val stream_type: String?,
    val stream_icon: String?,
    val stream_url: String?,
    val rating: String?,
    val plot: String?
)
