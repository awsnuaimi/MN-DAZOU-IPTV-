package com.dazou.iptvplayer

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// 1. هيكل البيانات التي سنستلمها من السيرفر
data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: UserInfo?
)

data class UserInfo(
    @SerializedName("username") val username: String?,
    @SerializedName("status") val status: String?
)

// 2. واجهة الأوامر التي سنرسلها للسيرفر
interface XtreamApiService {
    @GET("player_api.php")
    fun authenticate(
        @Query("username") user: String,
        @Query("password") pass: String
    ): Call<XtreamAuthResponse>
}
