package com.lomen.tv.data.preferences

import com.lomen.tv.domain.model.MediaClassificationStrategy

/**
 * 供 [com.lomen.tv.data.scraper.MediaInfoExtractor] 等同步代码读取当前策略。
 * 由 [MediaClassificationPreferences] 与 [com.lomen.tv.LomenTVApplication] 在启动/设置变更时更新。
 */
object MediaClassificationStrategyHolder {

    @Volatile
    var strategy: MediaClassificationStrategy = MediaClassificationStrategy.STRUCTURE_FIRST
        private set

    fun update(newStrategy: MediaClassificationStrategy) {
        strategy = newStrategy
    }
}
