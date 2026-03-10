package com.lomen.tv.data.remote.api

import com.lomen.tv.data.remote.model.TmdbMovieResponse
import com.lomen.tv.data.remote.model.TmdbSearchResponse
import com.lomen.tv.data.remote.model.TmdbTvResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val BASE_URL_LEGACY = "https://api.tmdb.org/3/"  // 旧版API，访问更稳定
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        const val POSTER_SIZE_W500 = "w500"
        const val POSTER_SIZE_W342 = "w342"
        const val BACKDROP_SIZE_W780 = "w780"
        const val BACKDROP_SIZE_W1280 = "w1280"
    }

    // 搜索电影
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1,
        @Query("year") year: Int? = null
    ): Response<TmdbSearchResponse<TmdbMovieResponse>>

    // 搜索电视剧
    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1,
        @Query("first_air_date_year") year: Int? = null
    ): Response<TmdbSearchResponse<TmdbTvResponse>>

    // 获取电影详情
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("append_to_response") append: String = "credits"
    ): Response<TmdbMovieResponse>

    // 获取电视剧详情
    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("append_to_response") append: String = "credits"
    ): Response<TmdbTvResponse>

    // 获取电视剧季详情
    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN"
    ): Response<Map<String, Any>>
    
    // 获取电视剧演职人员
    @GET("tv/{tv_id}/credits")
    suspend fun getTvCredits(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN"
    ): Response<Map<String, Any>>
}
