package com.dazou.iptvplayer

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// هيكل بيانات تسجيل الدخول
data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: UserInfo?
)

data class UserInfo(
    @SerializedName("username") val username: String?,
    @SerializedName("status") val status: String?
)

// هيكل بيانات باقات القنوات
data class XtreamCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String
)

// هيكل بيانات القنوات (الجديد)
data class XtreamStream(
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("stream_icon") val streamIcon: String?
)

// واجهة الاتصال بالسيرفر
interface XtreamApiService {
    @GET("player_api.php")
    fun authenticate(
        @Query("username") user: String,
        @Query("password") pass: String
    ): Call<XtreamAuthResponse>

    @GET("player_api.php")
    fun getLiveCategories(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_live_categories"
    ): Call<List<XtreamCategory>>

    // دالة جلب القنوات الخاصة بالباقة (الجديدة)
    @GET("player_api.php")
    fun getLiveStreams(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String
    ): Call<List<XtreamStream>>
}
