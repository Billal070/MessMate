package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.messMateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "messmate_preferences"
)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val language: AppLanguage = AppLanguage.EN,
    val setupCompleted: Boolean = false,
    val notificationPermissionPrompted: Boolean = false
)

class PreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val NOTIFICATION_PROMPTED = booleanPreferencesKey("notification_prompted")
    }

    val preferencesFlow: Flow<UserPreferences> = context.messMateDataStore.data.map { prefs ->
        UserPreferences(
            themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE]),
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            setupCompleted = prefs[Keys.SETUP_COMPLETED] ?: false,
            notificationPermissionPrompted = prefs[Keys.NOTIFICATION_PROMPTED] ?: false
        )
    }

    suspend fun getPreferencesOnce(): UserPreferences = preferencesFlow.first()

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.messMateDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        LocaleHelper.saveLanguageSync(context, language)
        context.messMateDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.messMateDataStore.edit { prefs ->
            prefs[Keys.SETUP_COMPLETED] = completed
        }
    }

    suspend fun setNotificationPermissionPrompted(prompted: Boolean) {
        context.messMateDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_PROMPTED] = prompted
        }
    }
}
