package com.lomen.tv.data.model.live

import androidx.compose.runtime.Immutable

/**
 * 当前节目和下一个节目
 */
@Immutable
data class CurrentProgramme(
    /**
     * 当前正在播放的节目
     */
    val now: EpgProgramme = EpgProgramme(),

    /**
     * 下一个节目
     */
    val next: EpgProgramme? = null,
) {
    companion object {
        val EXAMPLE = CurrentProgramme(
            now = EpgProgramme(
                startAt = System.currentTimeMillis() - 100000,
                endAt = System.currentTimeMillis() + 200000,
                title = "新闻联播",
            ),
            next = EpgProgramme(
                startAt = System.currentTimeMillis() + 200000,
                endAt = System.currentTimeMillis() + 500000,
                title = "天气预报",
            ),
        )
    }
}
