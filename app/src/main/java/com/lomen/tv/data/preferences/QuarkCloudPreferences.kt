package com.lomen.tv.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 夸克网盘偏好设置
 */
@Singleton
class QuarkCloudPreferences @Inject constructor(
    private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "quark_cloud")
    
    companion object {
        private val COOKIE_KEY = stringPreferencesKey("cookie")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    }
    
    val cookie: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[COOKIE_KEY]
        }
    
    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_LOGGED_IN_KEY] ?: false
        }
    
    suspend fun saveCookie(cookie: String) {
        context.dataStore.edit {
            preferences ->
            preferences[COOKIE_KEY] = cookie
            preferences[IS_LOGGED_IN_KEY] = true
        }
    }
    
    suspend fun clearCookie() {
        context.dataStore.edit {
            preferences ->
            preferences.remove(COOKIE_KEY)
            preferences[IS_LOGGED_IN_KEY] = false
        }
    }
}