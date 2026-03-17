package com.lomen.tv.data.model.live

import androidx.compose.runtime.Immutable
import com.lomen.tv.data.model.live.EpgProgramme.Companion.isLive

/**
 * 频道节目单
 */
@Immutable
data class ChannelEpg(
    /**
     * 频道名称
     */
    val channel: String = "",

    /**
     * 节目列表
     */
    val programmes: EpgProgrammeList = EpgProgrammeList(),
) {
    companion object {
        /**
         * 获取当前节目和下一个节目
         */
        fun ChannelEpg.currentProgrammes(): CurrentProgramme? {
            val currentProgramme = programmes.firstOrNull { it.isLive() } ?: return null

            return CurrentProgramme(
                now = currentProgramme,
                next = programmes.indexOf(currentProgramme).let { index ->
                    if (index + 1 < programmes.size) programmes[index + 1]
                    else null
                },
            )
        }
    }
}
