package com.lomen.tv.domain.service

import android.util.Log
import com.lomen.tv.data.local.database.dao.TmdbEpisodeDao
import com.lomen.tv.data.local.database.dao.TmdbMediaDao
import com.lomen.tv.data.local.database.dao.TmdbSyncQueueDao
import com.lomen.tv.data.local.database.entity.TmdbEpisodeEntity
import com.lomen.tv.data.local.database.entity.TmdbMediaEntity
import com.lomen.tv.data.local.database.entity.TmdbSyncQueueEntity
import com.lomen.tv.data.scraper.TmdbScraper
import com.lomen.tv.domain.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbMetadataSyncManager @Inject constructor(
    private val queueDao: TmdbSyncQueueDao,
    private val tmdbMediaDao: TmdbMediaDao,
    private val tmdbEpisodeDao: TmdbEpisodeDao,
    private val metadataService: MetadataService
) {
    companion object {
        private const val TAG = "TmdbMetaSync"
        private const val IDLE_DELAY_MS = 1_000L
        private const val FAILURE_DELAY_MS = 2_000L
        private const val MAX_ATTEMPTS = 5
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(1) // 低并发：后台慢慢补全，避免抢网影响播放/浏览
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            Log.d(TAG, "Tmdb metadata sync loop started")
            while (isActive) {
                val next = runCatching { queueDao.nextByState(TmdbSyncQueueEntity.State.PENDING) }
                    .getOrNull()
                if (next == null) {
                    delay(IDLE_DELAY_MS)
                    continue
                }
                semaphore.withPermit {
                    process(next)
                }
            }
        }
    }

    suspend fun enqueueMedia(tmdbId: Int, priority: Int = 0) {
        queueDao.upsert(
            TmdbSyncQueueEntity(
                key = TmdbSyncQueueEntity.mediaKey(tmdbId),
                tmdbId = tmdbId,
                taskType = TmdbSyncQueueEntity.TaskType.MEDIA,
                seasonNumber = null,
                priority = priority,
                state = TmdbSyncQueueEntity.State.PENDING,
                attemptCount = 0,
                lastError = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun enqueueSeasonEpisodes(tmdbId: Int, seasonNumber: Int, priority: Int = 0) {
        queueDao.upsert(
            TmdbSyncQueueEntity(
                key = TmdbSyncQueueEntity.seasonKey(tmdbId, seasonNumber),
                tmdbId = tmdbId,
                taskType = TmdbSyncQueueEntity.TaskType.SEASON_EPISODES,
                seasonNumber = seasonNumber,
                priority = priority,
                state = TmdbSyncQueueEntity.State.PENDING,
                attemptCount = 0,
                lastError = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun process(task: TmdbSyncQueueEntity) {
        val running = task.copy(
            state = TmdbSyncQueueEntity.State.RUNNING,
            updatedAt = System.currentTimeMillis()
        )
        queueDao.update(running)

        val result = runCatching {
            when (task.taskType) {
                TmdbSyncQueueEntity.TaskType.MEDIA -> syncMedia(task.tmdbId)
                TmdbSyncQueueEntity.TaskType.SEASON_EPISODES -> {
                    val season = task.seasonNumber ?: return@runCatching
                    syncSeasonEpisodes(task.tmdbId, season)
                }
            }
        }

        if (result.isSuccess) {
            queueDao.update(
                running.copy(
                    state = TmdbSyncQueueEntity.State.DONE,
                    updatedAt = System.currentTimeMillis(),
                    lastError = null
                )
            )
        } else {
            val msg = result.exceptionOrNull()?.message ?: result.exceptionOrNull()?.javaClass?.simpleName ?: "unknown"
            val attempts = task.attemptCount + 1
            val finalState = if (attempts >= MAX_ATTEMPTS) TmdbSyncQueueEntity.State.FAILED else TmdbSyncQueueEntity.State.PENDING
            queueDao.update(
                running.copy(
                    state = finalState,
                    attemptCount = attempts,
                    lastError = msg,
                    updatedAt = System.currentTimeMillis()
                )
            )
            delay(FAILURE_DELAY_MS)
        }
    }

    private suspend fun syncMedia(tmdbId: Int) {
        val existing = tmdbMediaDao.getByTmdbId(tmdbId)
        if (existing != null && System.currentTimeMillis() - existing.updatedAt < 7L * 24 * 60 * 60 * 1000) {
            // 一周内更新过就不频繁刷新
            return
        }

        val tv = metadataService.getDetailsByTmdbId(tmdbId, MediaType.TV_SHOW).getOrNull()
        val movie = metadataService.getDetailsByTmdbId(tmdbId, MediaType.MOVIE).getOrNull()
        val best = tv ?: movie ?: return

        val entity = TmdbMediaEntity(
            tmdbId = tmdbId,
            type = best.type,
            title = best.title,
            originalTitle = best.originalTitle,
            overview = best.overview,
            posterUrl = best.posterUrl,
            backdropUrl = best.backdropUrl,
            rating = best.rating,
            year = best.releaseDate?.take(4)?.toIntOrNull(),
            genres = best.genres.joinToString(","),
            seasonCount = best.seasonCount.takeIf { it > 0 },
            episodeCount = best.episodeCount.takeIf { it > 0 },
            updatedAt = System.currentTimeMillis()
        )
        tmdbMediaDao.upsert(entity)
    }

    private suspend fun syncSeasonEpisodes(tmdbId: Int, seasonNumber: Int) {
        // 直接复用 TmdbScraper 的 season 解析（包含 still/runtime 等）
        val scraper = TmdbScraper.getInstance()
        val eps = scraper.getTvSeasonEpisodes(tmdbId.toString(), seasonNumber).orEmpty()
        if (eps.isEmpty()) return
        val entities = eps.map { ep ->
            TmdbEpisodeEntity(
                tmdbId = tmdbId,
                seasonNumber = seasonNumber,
                episodeNumber = ep.episodeNumber,
                name = ep.name.takeIf { it.isNotBlank() },
                overview = ep.overview?.takeIf { it.isNotBlank() },
                stillUrl = ep.stillUrl,
                airDate = ep.airDate,
                runtimeMinutes = ep.runtime.takeIf { it > 0 },
                updatedAt = System.currentTimeMillis()
            )
        }
        tmdbEpisodeDao.upsertAll(entities)
    }
}

