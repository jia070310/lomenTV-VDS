package com.lomen.tv.data.model.live

import androidx.compose.runtime.Immutable

/**
 * 节目单节目信息
 */
@Immutable
data class EpgProgramme(
    /**
     * 开始时间
     */
    val startAt: Long = 0,

    /**
     * 结束时间
     */
    val endAt: Long = 0,

    /**
     * 节目标题
     */
    val title: String = "",
) {
    companion object {
        /**
         * 是否正在播放
         */
        fun EpgProgramme.isLive(): Boolean {
            val now = System.currentTimeMillis()
            return now in startAt..endAt
        }

        /**
         * 节目进度（0-1）
         */
        fun EpgProgramme.progress(): Float {
            val now = System.currentTimeMillis()
            return when {
                now < startAt -> 0f
                now > endAt -> 1f
                else -> (now - startAt).toFloat() / (endAt - startAt).toFloat()
            }
        }
    }
}
