package com.lomen.tv.data.model.live

import androidx.compose.runtime.Immutable

/**
 * 节目单节目列表
 */
@Immutable
data class EpgProgrammeList(
    val value: List<EpgProgramme> = emptyList(),
) : List<EpgProgramme> by value
