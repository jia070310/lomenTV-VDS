package com.lomen.tv.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lomen.tv.data.local.database.entity.TmdbSyncQueueEntity

@Dao
interface TmdbSyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TmdbSyncQueueEntity)

    @Update
    suspend fun update(entity: TmdbSyncQueueEntity)

    @Query(
        """
        SELECT * FROM tmdb_sync_queue 
        WHERE state = :state 
        ORDER BY priority DESC, updatedAt ASC
        LIMIT 1
        """
    )
    suspend fun nextByState(state: TmdbSyncQueueEntity.State = TmdbSyncQueueEntity.State.PENDING): TmdbSyncQueueEntity?
}

