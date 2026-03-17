package com.lomen.tv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomen.tv.data.preferences.PlayerSettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerSettingsViewModel @Inject constructor(
    private val playerSettingsPreferences: PlayerSettingsPreferences
) : ViewModel() {

    /**
     * 自动跳过片头片尾开关状态
     */
    val autoSkipIntroOutro: Flow<Boolean> = playerSettingsPreferences.autoSkipIntroOutro

    /**
     * 设置自动跳过片头片尾开关状态
     */
    fun setAutoSkipIntroOutro(enabled: Boolean) {
        android.util.Log.d("PlayerSettingsViewModel", "Setting auto skip intro outro to: $enabled")
        viewModelScope.launch {
            playerSettingsPreferences.setAutoSkipIntroOutro(enabled)
        }
    }

    /**
     * 记忆续播功能开关状态
     */
    val rememberPlaybackPosition: Flow<Boolean> = playerSettingsPreferences.rememberPlaybackPosition

    /**
     * 设置记忆续播功能开关状态
     */
    fun setRememberPlaybackPosition(enabled: Boolean) {
        android.util.Log.d("PlayerSettingsViewModel", "Setting remember playback position to: $enabled")
        viewModelScope.launch {
            playerSettingsPreferences.setRememberPlaybackPosition(enabled)
        }
    }
}
