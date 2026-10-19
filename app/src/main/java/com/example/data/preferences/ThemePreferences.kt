package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

enum class AppThemeMode(val title: String, val description: String) {
    SYSTEM("Automático", "Sigue el tema del sistema"),
    LIGHT("Modo Claro", "Fondo luminoso y acentos limpios"),
    DARK("Modo Oscuro", "Fondo oscuro de alto contraste")
}

class ThemePreferences(private val context: Context) {
    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("app_theme_mode")
    }

    val themeModeFlow: Flow<AppThemeMode> = context.themeDataStore.data.map { preferences ->
        val name = preferences[THEME_MODE_KEY] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}
