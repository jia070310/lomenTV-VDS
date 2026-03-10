package com.lomen.tv.utils

import android.util.Log
import com.lomen.tv.domain.model.MediaType

/**
 * 媒体类型智能调整器
 * 根据TMDB的genre_ids进行二次分类优化
 */
object MediaTypeAdjuster {
    private const val TAG = "MediaTypeAdjuster"
    
    /**
     * TMDB Genre ID 映射表
     * 参考：https://developers.themoviedb.org/3/genres/get-movie-list
     * 
     * 电影类型：
     * 10402 - Music (音乐)
     * 99 - Documentary (纪录片)
     * 
     * 电视剧类型：
     * 10764 - Reality (真人秀)
     * 10767 - Talk (脱口秀)
     * 99 - Documentary (纪录片)
     */
    
    // 音乐类型ID（可能是演唱会）
    private val MUSIC_GENRE_IDS = setOf(10402)
    
    // 纪录片类型ID
    private val DOCUMENTARY_GENRE_IDS = setOf(99)
    
    // 综艺/真人秀类型ID
    private val VARIETY_GENRE_IDS = setOf(10764, 10767)
    
    /**
     * 根据TMDB的genre_ids和基础类型，智能调整最终的MediaType
     * 
     * @param baseType 基于文件名关键词判断的基础类型
     * @param genreIds TMDB返回的genre_ids列表
     * @param hasSeasonInfo 是否有季集信息
     * @return 调整后的最终类型
     */
    fun adjustType(
        baseType: MediaType, 
        genreIds: List<Int>, 
        hasSeasonInfo: Boolean
    ): MediaType {
        if (genreIds.isEmpty()) {
            Log.d(TAG, "No genre_ids provided, keeping base type: $baseType")
            return baseType
        }
        
        Log.d(TAG, "Adjusting type - baseType: $baseType, genreIds: $genreIds, hasSeasonInfo: $hasSeasonInfo")
        
        // 优先级1: 如果基础类型已经是特定类型（演唱会/综艺/纪录片），保持不变
        // 因为文件名关键词更准确
        if (baseType in setOf(MediaType.CONCERT, MediaType.VARIETY, MediaType.DOCUMENTARY)) {
            Log.d(TAG, "Base type is already specific, keeping: $baseType")
            return baseType
        }
        
        // 优先级2: 检查是否是纪录片
        if (genreIds.any { it in DOCUMENTARY_GENRE_IDS }) {
            Log.d(TAG, "Detected as Documentary by genre_ids")
            return MediaType.DOCUMENTARY
        }
        
        // 优先级3: 检查是否是综艺（仅针对电视剧类型）
        if (hasSeasonInfo && genreIds.any { it in VARIETY_GENRE_IDS }) {
            Log.d(TAG, "Detected as Variety Show by genre_ids (has season info)")
            return MediaType.VARIETY
        }
        
        // 优先级4: 检查是否是演唱会（Music类型 + 无季集信息）
        if (!hasSeasonInfo && genreIds.any { it in MUSIC_GENRE_IDS }) {
            Log.d(TAG, "Detected as Concert by genre_ids (Music + no season info)")
            return MediaType.CONCERT
        }
        
        // 保持原类型
        Log.d(TAG, "No adjustment needed, keeping base type: $baseType")
        return baseType
    }
    
    /**
     * 便捷方法：直接根据genre_ids和季集信息判断类型
     */
    fun detectFromGenres(genreIds: List<Int>, hasSeasonInfo: Boolean): MediaType? {
        if (genreIds.isEmpty()) return null
        
        // 纪录片优先
        if (genreIds.any { it in DOCUMENTARY_GENRE_IDS }) {
            return MediaType.DOCUMENTARY
        }
        
        // 综艺（有季集信息）
        if (hasSeasonInfo && genreIds.any { it in VARIETY_GENRE_IDS }) {
            return MediaType.VARIETY
        }
        
        // 演唱会（音乐 + 无季集）
        if (!hasSeasonInfo && genreIds.any { it in MUSIC_GENRE_IDS }) {
            return MediaType.CONCERT
        }
        
        return null
    }
    
    /**
     * 获取genre_ids的可读描述（用于调试）
     */
    fun getGenreDescription(genreIds: List<Int>): String {
        val descriptions = mutableListOf<String>()
        
        genreIds.forEach { id ->
            when {
                id in MUSIC_GENRE_IDS -> descriptions.add("Music($id)")
                id in DOCUMENTARY_GENRE_IDS -> descriptions.add("Documentary($id)")
                id in VARIETY_GENRE_IDS -> descriptions.add("Variety($id)")
                else -> descriptions.add("$id")
            }
        }
        
        return descriptions.joinToString(", ")
    }
}
