package com.lomen.tv.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lomen.tv.data.local.database.dao.EpisodeDao
import com.lomen.tv.data.local.database.dao.MovieDao
import com.lomen.tv.data.local.database.dao.WatchHistoryDao
import com.lomen.tv.data.local.database.dao.WebDavMediaDao
import com.lomen.tv.data.local.database.entity.EpisodeEntity
import com.lomen.tv.data.local.database.entity.MovieEntity
import com.lomen.tv.data.local.database.entity.WatchHistoryEntity
import com.lomen.tv.data.local.database.entity.WebDavMediaEntity

@Database(
    entities = [
        MovieEntity::class,
        EpisodeEntity::class,
        WatchHistoryEntity::class,
        WebDavMediaEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LomenDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun webDavMediaDao(): WebDavMediaDao
}
