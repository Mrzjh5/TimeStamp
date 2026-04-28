package com.miui.timesnote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "times_note_settings")

/**
 * 设置数据管理
 */
object SettingsData {
    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    private val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
    private val LOW_POWER_MODE = booleanPreferencesKey("low_power_mode")
    private val WALLPAPER_ADAPTIVE = booleanPreferencesKey("wallpaper_adaptive")

    val darkModeFlow: Flow<Boolean> = ContextProvider.dataStore.data.map { it[DARK_MODE] ?: false }
    val lastResetDateFlow: Flow<String> = ContextProvider.dataStore.data.map { it[LAST_RESET_DATE] ?: "" }
    val lowPowerModeFlow: Flow<Boolean> = ContextProvider.dataStore.data.map { it[LOW_POWER_MODE] ?: false }
    val wallpaperAdaptiveFlow: Flow<Boolean> = ContextProvider.dataStore.data.map { it[WALLPAPER_ADAPTIVE] ?: true }
    val firstLaunchFlow: Flow<Boolean> = ContextProvider.dataStore.data.map { it[FIRST_LAUNCH] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        ContextProvider.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setLastResetDate(date: String) {
        ContextProvider.dataStore.edit { it[LAST_RESET_DATE] = date }
    }

    suspend fun setLowPowerMode(enabled: Boolean) {
        ContextProvider.dataStore.edit { it[LOW_POWER_MODE] = enabled }
    }

    suspend fun setWallpaperAdaptive(enabled: Boolean) {
        ContextProvider.dataStore.edit { it[WALLPAPER_ADAPTIVE] = enabled }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        ContextProvider.dataStore.edit { it[FIRST_LAUNCH] = isFirst }
    }
}

object ContextProvider {
    lateinit var context: Context
        private set
}

fun initContext(context: Context) {
    ContextProvider.context = context.applicationContext
}
