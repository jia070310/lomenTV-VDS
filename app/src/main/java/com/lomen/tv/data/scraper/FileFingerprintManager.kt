package com.lomen.tv.data.scraper

import com.lomen.tv.data.webdav.WebDavFile
import java.security.MessageDigest

/**
 * 文件指纹管理器
 * 用于增量扫描，通过文件指纹判断文件是否发生变化
 */
object FileFingerprintManager {

    /**
     * 生成文件指纹
     * 使用文件路径、大小、修改时间生成MD5哈希
     */
    fun generateFingerprint(file: WebDavFile): String {
        val content = "${file.path}|${file.size}|${file.lastModified}"
        return md5(content)
    }

    /**
     * 生成文件指纹（从组件）
     */
    fun generateFingerprint(path: String, size: Long, lastModified: Long): String {
        val content = "$path|$size|$lastModified"
        return md5(content)
    }

    /**
     * 计算MD5
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
