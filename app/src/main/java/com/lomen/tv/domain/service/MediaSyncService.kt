package com.lomen.tv.domain.service

import com.lomen.tv.data.local.database.dao.EpisodeDao
import com.lomen.tv.data.local.database.dao.MovieDao
import com.lomen.tv.data.local.database.entity.EpisodeEntity
import com.lomen.tv.data.local.database.entity.MovieEntity
import com.lomen.tv.data.remote.model.QuarkFileItem
import com.lomen.tv.domain.model.MediaType
import com.lomen.tv.utils.FileNameParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSyncService @Inject constructor(
    private val quarkService: QuarkService,
    private val metadataService: MetadataService,
    private val movieDao: MovieDao,
    private val episodeDao: EpisodeDao
) {
    // 同步状态
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    // 同步进度
    private val _syncProgress = MutableStateFlow(0)
    val syncProgress: StateFlow<Int> = _syncProgress

    private val _totalFiles = MutableStateFlow(0)
    val totalFiles: StateFlow<Int> = _totalFiles

    /**
     * 同步状态密封类
     */
    sealed class SyncState {
        object Idle : SyncState()
        object Scanning : SyncState()
        object FetchingMetadata : SyncState()
        object Saving : SyncState()
        data class Error(val message: String) : SyncState()
        object Complete : SyncState()
    }

    /**
     * 同步夸克网盘视频文件
     * 从配置的根目录读取
     */
    suspend fun syncFromQuarkCloud(): Result<SyncResult> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Scanning
                _syncProgress.value = 0

                // 1. 获取配置的根目录下的视频文件
                val videoFilesResult = quarkService.getVideoFilesInFolder(
                    folderId = QuarkService.ROOT_FOLDER_ID
                ) { count ->
                    _syncProgress.value = count
                }

                if (videoFilesResult.isFailure) {
                    _syncState.value = SyncState.Error("获取文件列表失败")
                    return@withContext Result.failure(
                        videoFilesResult.exceptionOrNull() ?: Exception("Unknown error")
                    )
                }

                val videoFiles = videoFilesResult.getOrDefault(emptyList())
                _totalFiles.value = videoFiles.size

                if (videoFiles.isEmpty()) {
                    _syncState.value = SyncState.Complete
                    return@withContext Result.success(SyncResult(0, 0, 0))
                }

                // 2. 按系列分组（剧集）
                val groupedFiles = groupFilesBySeries(videoFiles)

                // 3. 获取元数据并保存
                _syncState.value = SyncState.FetchingMetadata
                var successCount = 0
                var failCount = 0

                groupedFiles.forEach { (seriesName, files) ->
                    try {
                        processSeries(seriesName, files)
                        successCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failCount++
                    }
                    _syncProgress.value = successCount + failCount
                }

                _syncState.value = SyncState.Complete
                Result.success(SyncResult(videoFiles.size, successCount, failCount))

            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }

    /**
     * 按系列分组文件
     */
    private fun groupFilesBySeries(files: List<QuarkFileItem>): Map<String, List<QuarkFileItem>> {
        val groups = mutableMapOf<String, MutableList<QuarkFileItem>>()

        files.forEach { file ->
            val parseResult = FileNameParser.parse(file.fileName)
            val seriesName = if (parseResult.type == MediaType.TV_SHOW) {
                parseResult.title
            } else {
                // 电影按文件名分组
                file.fileName
            }

            groups.getOrPut(seriesName) { mutableListOf() }.add(file)
        }

        return groups
    }

    /**
     * 处理系列（电影或剧集）
     */
    private suspend fun processSeries(seriesName: String, files: List<QuarkFileItem>) {
        // 获取第一个文件用于搜索元数据
        val firstFile = files.first()
        val parseResult = FileNameParser.parse(firstFile.fileName)

        // 搜索元数据
        val metadataResult = metadataService.searchByFileName(firstFile.fileName)

        val mediaItem = if (metadataResult.isSuccess) {
            metadataResult.getOrThrow()
        } else {
            // 创建本地条目
            com.lomen.tv.domain.model.MediaItem(
                id = "local_${firstFile.fileId}",
                title = parseResult.title,
                type = parseResult.type,
                releaseDate = parseResult.year?.toString()
            )
        }

        // 保存到数据库
        if (mediaItem.type == MediaType.TV_SHOW) {
            saveTvShow(mediaItem, files)
        } else {
            saveMovie(mediaItem, files.first())
        }
    }

    /**
     * 保存电影
     */
    private suspend fun saveMovie(mediaItem: com.lomen.tv.domain.model.MediaItem, file: QuarkFileItem) {
        val movieEntity = MovieEntity(
            id = mediaItem.id,
            title = mediaItem.title,
            originalTitle = mediaItem.originalTitle,
            overview = mediaItem.overview,
            posterUrl = mediaItem.posterUrl,
            backdropUrl = mediaItem.backdropUrl,
            releaseDate = mediaItem.releaseDate,
            rating = mediaItem.rating,
            genre = mediaItem.genres.joinToString(","),
            type = MediaType.MOVIE,
            quarkFileId = file.fileId,
            quarkPath = file.fileName,
            tmdbId = mediaItem.tmdbId
        )

        movieDao.insertMovie(movieEntity)
    }

    /**
     * 保存电视剧
     */
    private suspend fun saveTvShow(
        mediaItem: com.lomen.tv.domain.model.MediaItem,
        files: List<QuarkFileItem>
    ) {
        // 1. 保存剧集基础信息
        val movieEntity = MovieEntity(
            id = mediaItem.id,
            title = mediaItem.title,
            originalTitle = mediaItem.originalTitle,
            overview = mediaItem.overview,
            posterUrl = mediaItem.posterUrl,
            backdropUrl = mediaItem.backdropUrl,
            releaseDate = mediaItem.releaseDate,
            rating = mediaItem.rating,
            genre = mediaItem.genres.joinToString(","),
            type = MediaType.TV_SHOW,
            tmdbId = mediaItem.tmdbId,
            seasonCount = mediaItem.seasonCount,
            episodeCount = files.size
        )

        movieDao.insertMovie(movieEntity)

        // 2. 保存每集信息
        val episodes = files.map { file ->
            val parseResult = FileNameParser.parse(file.fileName)
            EpisodeEntity(
                id = "${mediaItem.id}_ep_${file.fileId}",
                movieId = mediaItem.id,
                seasonNumber = parseResult.season ?: 1,
                episodeNumber = parseResult.episode ?: 1,
                title = parseResult.title,
                quarkFileId = file.fileId,
                quarkPath = file.fileName
            )
        }

        episodeDao.insertEpisodes(episodes)
    }

    /**
     * 获取所有本地媒体
     */
    fun getAllMovies(): Flow<List<MovieEntity>> = movieDao.getAllMovies()

    /**
     * 获取电影详情
     */
    suspend fun getMovieById(id: String): MovieEntity? = movieDao.getMovieById(id)

    /**
     * 获取剧集列表
     */
    fun getEpisodesByMovieId(movieId: String): Flow<List<EpisodeEntity>> =
        episodeDao.getEpisodesByMovieId(movieId)

    /**
     * 删除媒体
     */
    suspend fun deleteMedia(movieId: String) {
        movieDao.deleteMovie(movieId)
        episodeDao.deleteEpisodesByMovieId(movieId)
    }

    /**
     * 搜索本地媒体
     */
    fun searchLocalMedia(query: String): Flow<List<MovieEntity>> =
        movieDao.searchMovies(query)

    /**
     * 同步结果数据类
     */
    data class SyncResult(
        val totalFiles: Int,
        val successCount: Int,
        val failCount: Int
    )
}
