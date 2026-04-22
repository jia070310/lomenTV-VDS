package com.lomen.tv.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lomen.tv.data.local.database.dao.EpisodeDao
import com.lomen.tv.data.local.database.dao.MovieDao
import com.lomen.tv.data.local.database.dao.SkipConfigDao
import com.lomen.tv.data.local.database.dao.TmdbEpisodeDao
import com.lomen.tv.data.local.database.dao.TmdbMediaDao
import com.lomen.tv.data.local.database.dao.TmdbSyncQueueDao
import com.lomen.tv.data.local.database.dao.WatchHistoryDao
import com.lomen.tv.data.local.database.dao.WebDavMediaDao
import com.lomen.tv.data.local.database.entity.EpisodeEntity
import com.lomen.tv.data.local.database.entity.MovieEntity
import com.lomen.tv.data.local.database.entity.SkipConfigEntity
import com.lomen.tv.data.local.database.entity.TmdbEpisodeEntity
import com.lomen.tv.data.local.database.entity.TmdbMediaEntity
import com.lomen.tv.data.local.database.entity.TmdbSyncQueueEntity
import com.lomen.tv.data.local.database.entity.WatchHistoryEntity
import com.lomen.tv.data.local.database.entity.WebDavMediaEntity

@Database(
    entities = [
        MovieEntity::class,
        EpisodeEntity::class,
        WatchHistoryEntity::class,
        WebDavMediaEntity::class,
        SkipConfigEntity::class,
        TmdbMediaEntity::class,
        TmdbEpisodeEntity::class,
        TmdbSyncQueueEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LomenDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun webDavMediaDao(): WebDavMediaDao
    abstract fun skipConfigDao(): SkipConfigDao
    abstract fun tmdbMediaDao(): TmdbMediaDao
    abstract fun tmdbEpisodeDao(): TmdbEpisodeDao
    abstract fun tmdbSyncQueueDao(): TmdbSyncQueueDao
}
