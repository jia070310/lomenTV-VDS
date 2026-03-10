package com.lomen.tv.data.scraper

import android.util.Log
import com.lomen.tv.data.webdav.WebDavFile
import com.lomen.tv.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class ScrapedMedia(
    val id: String,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val year: Int?,
    val rating: Float?,
    val genres: List<String>,
    val type: MediaType,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val filePath: String,
    val fileName: String,
    val source: String, // "tmdb" or "douban"
    val tmdbId: String? = null
) {
    // 保留isMovie用于向后兼容
    val isMovie: Boolean
        get() = type == MediaType.MOVIE
}

class MediaScraper {
    companion object {
        private const val TAG = "MediaScraper"
    }

    private val tmdbScraper = TmdbScraper.getInstance()
    private val doubanScraper = DoubanScraper()

    suspend fun scrapeMedia(files: List<WebDavFile>): List<ScrapedMedia> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScrapedMedia>()
        
        for (file in files) {
            val scraped = scrapeSingleFile(file)
            if (scraped != null) {
                results.add(scraped)
            }
        }
        
        results
    }

    suspend fun scrapeSingleFile(file: WebDavFile): ScrapedMedia? {
        // 提取媒体信息
        val parentFolder = file.path.substringBeforeLast("/", "").substringAfterLast("/", "")
        val mediaInfo = MediaInfoExtractor.extract(file.name, parentFolder)
        
        Log.d(TAG, "Scraping: ${mediaInfo.title}, year: ${mediaInfo.year}, type: ${mediaInfo.type}")
        
        // 核心原则：根据类型决定搜索TMDB的哪个表
        // TV_SHOW, VARIETY → 搜索tv表
        // MOVIE, CONCERT, DOCUMENTARY, OTHER → 搜索movie表
        val tmdbResult = when (mediaInfo.type) {
            MediaType.TV_SHOW, MediaType.VARIETY -> {
                tmdbScraper.searchTv(mediaInfo.title, mediaInfo.year)
            }
            MediaType.MOVIE, MediaType.CONCERT, MediaType.DOCUMENTARY, MediaType.OTHER -> {
                tmdbScraper.searchMovie(mediaInfo.title, mediaInfo.year)
            }
        }
        
        if (tmdbResult != null) {
            return ScrapedMedia(
                id = tmdbResult.id,
                title = tmdbResult.title,
                originalTitle = tmdbResult.originalTitle,
                overview = tmdbResult.overview,
                posterUrl = tmdbResult.posterUrl,
                backdropUrl = tmdbResult.backdropUrl,
                year = tmdbResult.year,
                rating = tmdbResult.rating,
                genres = tmdbResult.genres,
                type = mediaInfo.type,  // 使用从文件名提取的类型
                seasonNumber = mediaInfo.season,
                episodeNumber = mediaInfo.episode,
                filePath = file.path,
                fileName = file.name,
                source = "tmdb"
            )
        }
        
        // TMDB失败，尝试豆瓣
        Log.d(TAG, "TMDB failed, trying Douban for: ${mediaInfo.title}")
        val doubanResult = doubanScraper.search(mediaInfo.title, mediaInfo.year)
        
        if (doubanResult != null) {
            return ScrapedMedia(
                id = doubanResult.id,
                title = doubanResult.title,
                originalTitle = doubanResult.originalTitle,
                overview = doubanResult.overview,
                posterUrl = doubanResult.posterUrl,
                backdropUrl = doubanResult.backdropUrl,
                year = doubanResult.year,
                rating = doubanResult.rating,
                genres = doubanResult.genres,
                type = mediaInfo.type,  // 使用从文件名提取的类型
                seasonNumber = mediaInfo.season,
                episodeNumber = mediaInfo.episode,
                filePath = file.path,
                fileName = file.name,
                source = "douban"
            )
        }
        
        // 都失败了，使用文件名作为标题
        Log.w(TAG, "All scrapers failed for: ${file.name}")
        return ScrapedMedia(
            id = file.path.hashCode().toString(),
            title = mediaInfo.title,
            originalTitle = null,
            overview = null,
            posterUrl = null,
            backdropUrl = null,
            year = mediaInfo.year,
            rating = null,
            genres = emptyList(),
            type = mediaInfo.type,
            seasonNumber = mediaInfo.season,
            episodeNumber = mediaInfo.episode,
            filePath = file.path,
            fileName = file.name,
            source = "local"
        )
    }

    suspend fun scrapeBatch(files: List<WebDavFile>, onProgress: (Int, Int) -> Unit): List<ScrapedMedia> = withContext(Dispatchers.IO) {
        val total = files.size
        val completed = java.util.concurrent.atomic.AtomicInteger(0)

        Log.d(TAG, "Starting batch scrape of $total files")

        // 增加并发数到10个，提高刮削速度
        val batchSize = 10
        val results = mutableListOf<ScrapedMedia>()

        files.chunked(batchSize).forEach { batch ->
            val batchResults = batch.map { file ->
                async {
                    val result = scrapeSingleFile(file)
                    val current = completed.incrementAndGet()
                    onProgress(current, total)
                    result
                }
            }.awaitAll().filterNotNull()

            results.addAll(batchResults)
        }

        Log.d(TAG, "Batch scrape completed: ${results.size} / $total")
        results
    }
}
