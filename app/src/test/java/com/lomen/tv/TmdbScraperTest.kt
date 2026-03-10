package com.lomen.tv

import android.util.Log
import com.lomen.tv.data.scraper.TmdbScraper
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TmdbScraperTest {

    @Test
    fun testSearchTv() = runBlocking {
        val scraper = TmdbScraper()

        println("=== 测试TMDB刮削: 太平年 ===")

        try {
            val result = scraper.searchTv("太平年")

            if (result != null) {
                println("✓ 搜索成功!")
                println("  标题: ${result.title}")
                println("  原始标题: ${result.originalTitle}")
                println("  年份: ${result.year}")
                println("  评分: ${result.rating}")
                println("  海报: ${result.posterUrl}")
                println("  背景图: ${result.backdropUrl}")
                println("  简介: ${result.overview?.take(100)}...")
            } else {
                println("✗ 搜索失败: 返回null")
            }
        } catch (e: Exception) {
            println("✗ 搜索异常: ${e.message}")
            e.printStackTrace()
        }

        println("\n=== 测试完成 ===")
    }

    @Test
    fun testSearchMovie() = runBlocking {
        val scraper = TmdbScraper()

        println("=== 测试TMDB刮削: 流浪地球 ===")

        try {
            val result = scraper.searchMovie("流浪地球")

            if (result != null) {
                println("✓ 搜索成功!")
                println("  标题: ${result.title}")
                println("  原始标题: ${result.originalTitle}")
                println("  年份: ${result.year}")
                println("  评分: ${result.rating}")
                println("  海报: ${result.posterUrl}")
                println("  背景图: ${result.backdropUrl}")
                println("  简介: ${result.overview?.take(100)}...")
            } else {
                println("✗ 搜索失败: 返回null")
            }
        } catch (e: Exception) {
            println("✗ 搜索异常: ${e.message}")
            e.printStackTrace()
        }

        println("\n=== 测试完成 ===")
    }
}
