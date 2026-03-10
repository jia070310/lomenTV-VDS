package com.lomen.tv.di

import android.content.Context
import androidx.room.Room
import com.lomen.tv.data.local.database.LomenDatabase
import com.lomen.tv.data.local.database.dao.EpisodeDao
import com.lomen.tv.data.local.database.dao.MovieDao
import com.lomen.tv.data.local.database.dao.WatchHistoryDao
import com.lomen.tv.data.local.database.dao.WebDavMediaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LomenDatabase {
        return Room.databaseBuilder(
            context,
            LomenDatabase::class.java,
            "lomen_tv_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMovieDao(database: LomenDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    fun provideEpisodeDao(database: LomenDatabase): EpisodeDao {
        return database.episodeDao()
    }

    @Provides
    fun provideWatchHistoryDao(database: LomenDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    @Provides
    fun provideWebDavMediaDao(database: LomenDatabase): WebDavMediaDao {
        return database.webDavMediaDao()
    }
}
