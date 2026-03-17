package com.lomen.tv.data.model.live

import android.util.Log
import androidx.compose.runtime.Immutable

/**
 * 频道节目单列表
 */
@Immutable
data class ChannelEpgList(
    val value: List<ChannelEpg> = emptyList(),
) : List<ChannelEpg> by value {
    companion object {
        val EXAMPLE = ChannelEpgList()
        private const val TAG = "ChannelEpgList"
    }

    /**
     * 获取指定频道的当前节目
     */
    fun currentProgrammes(channel: LiveChannel): CurrentProgramme? {
        Log.d(TAG, "查找频道: '${channel.channelName}', EPG列表大小: ${size}")
        
        // 打印前5个EPG频道名称用于调试
        if (size > 0) {
            Log.d(TAG, "EPG频道列表(前5个): ${take(5).map { it.channel }}")
        }
        
        // 首先尝试精确匹配
        var epg = firstOrNull { it.channel == channel.channelName }
        
        // 如果精确匹配失败，尝试忽略大小写和空格匹配
        if (epg == null) {
            val normalizedTarget = channel.channelName.normalizeForMatch()
            Log.d(TAG, "精确匹配失败，尝试模糊匹配: 目标='${channel.channelName}' -> 标准化='$normalizedTarget'")
            epg = firstOrNull { 
                val normalizedEpg = it.channel.normalizeForMatch()
                val match = normalizedEpg == normalizedTarget
                if (match) {
                    Log.d(TAG, "模糊匹配成功: EPG='${it.channel}' -> 标准化='$normalizedEpg'")
                }
                match
            }
        }
        
        if (epg == null) {
            Log.d(TAG, "未找到频道 '${channel.channelName}' 的EPG数据")
            return null
        }
        
        Log.d(TAG, "找到频道 '${channel.channelName}' 的EPG数据 (匹配: '${epg.channel}'), 节目数量: ${epg.programmes.size}")
        
        val now = System.currentTimeMillis()
        Log.d(TAG, "当前时间: $now")
        
        // 打印前几个节目的时间范围用于调试
        epg.programmes.take(3).forEachIndexed { index, programme ->
            Log.d(TAG, "节目$index: ${programme.title}, 开始: ${programme.startAt}, 结束: ${programme.endAt}")
        }
        
        val currentProgramme = epg.programmes.firstOrNull { p -> 
            val isCurrent = now >= p.startAt && now <= p.endAt
            if (isCurrent) {
                Log.d(TAG, "找到当前节目: ${p.title}")
            }
            isCurrent
        }
        
        // 如果找不到当前时间段的节目，使用第一个节目作为默认显示
        // （EPG数据可能不是实时的，但至少显示节目信息）
        val actualCurrentProgramme = currentProgramme ?: epg.programmes.firstOrNull()
        
        if (actualCurrentProgramme == null) {
            Log.d(TAG, "未找到任何节目")
            return null
        }
        
        if (currentProgramme == null) {
            Log.d(TAG, "未找到当前时间段的节目，使用第一个节目作为默认显示: ${actualCurrentProgramme.title}")
        }
        
        val currentIndex = epg.programmes.indexOf(actualCurrentProgramme)
        val nextProgramme = if (currentIndex + 1 < epg.programmes.size) {
            epg.programmes[currentIndex + 1]
        } else null
        
        return CurrentProgramme(
            now = actualCurrentProgramme,
            next = nextProgramme,
        )
    }
    
    /**
     * 标准化频道名称用于匹配（移除空格、转为小写）
     */
    private fun String.normalizeForMatch(): String {
        return this.replace(" ", "").replace("-", "").lowercase()
    }
}
