package com.lomen.tv.di

import com.lomen.tv.data.remote.api.QuarkApi
import com.lomen.tv.data.remote.api.TmdbApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** 播放专用 OkHttp：无日志拦截，连接池 + 多路并发，供 ExoPlayer 缓冲复用 */
    @Provides
    @Singleton
    @Named("playback")
    fun providePlaybackOkHttpClient(): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequests = 8
            maxRequestsPerHost = 4
        }
        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .dispatcher(dispatcher)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTmdbRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TmdbApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("quark")
    fun provideQuarkRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(QuarkApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(@Named("tmdb") retrofit: Retrofit): TmdbApi {
        return retrofit.create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideQuarkApi(@Named("quark") retrofit: Retrofit): QuarkApi {
        return retrofit.create(QuarkApi::class.java)
    }
}
