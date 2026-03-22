package com.lomen.tv.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lomen.tv.domain.model.MediaClassificationStrategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mediaClassificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "media_classification_preferences"
)

/**
 * 媒体类型识别策略（结构优先 / 关键词优先）
 */
@Singleton
class MediaClassificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.mediaClassificationDataStore

    companion object {
        private val STRATEGY_KEY = stringPreferencesKey("classification_strategy")
        val DEFAULT_STRATEGY = MediaClassificationStrategy.STRUCTURE_FIRST
    }

    val classificationStrategy: Flow<MediaClassificationStrategy> = dataStore.data.map { preferences ->
        parseStrategy(preferences[STRATEGY_KEY])
    }

    suspend fun saveClassificationStrategy(strategy: MediaClassificationStrategy) {
        dataStore.edit { preferences ->
            preferences[STRATEGY_KEY] = strategy.name
        }
    }

    private fun parseStrategy(raw: String?): MediaClassificationStrategy {
        if (raw.isNullOrBlank()) return DEFAULT_STRATEGY
        return try {
            MediaClassificationStrategy.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            DEFAULT_STRATEGY
        }
    }
}
