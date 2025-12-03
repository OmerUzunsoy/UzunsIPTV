package com.uzuns.uzunsiptv

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {
    @GET("player_api.php")
    fun login(
        @Query("username") u: String,  // Tırnak içi "username" OLMALI
        @Query("password") p: String   // Tırnak içi "password" OLMALI
    ): Call<LoginResponse>
    @GET("player_api.php")
    fun getLiveCategories(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_live_categories"): Call<List<LiveCategory>>

    @GET("player_api.php")
    fun getLiveStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_live_streams", @Query("category_id") c: String? = null): Call<List<LiveStream>>

    @GET("player_api.php")
    fun getVodCategories(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_vod_categories"): Call<List<VodCategory>>

    @GET("player_api.php")
    fun getVodStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_vod_streams", @Query("category_id") c: String? = null): Call<List<VodStream>>

    @GET("player_api.php")
    fun getVodInfo(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_vod_info", @Query("vod_id") v: Int): Call<VodInfoResponse>

    @GET("player_api.php")
    fun getSeriesCategories(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_series_categories"): Call<List<SeriesCategory>>

    @GET("player_api.php")
    fun getSeries(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_series", @Query("category_id") c: String? = null): Call<List<SeriesStream>>

    @GET("player_api.php")
    fun getSeriesInfo(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_series_info", @Query("series_id") s: Int): Call<SeriesInfoResponse>
}