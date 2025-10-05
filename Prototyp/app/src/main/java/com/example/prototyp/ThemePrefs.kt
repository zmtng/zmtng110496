package com.example.prototyp.prefs

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Top-level: DataStore-Property am Context
val Context.themeDataStore by preferencesDataStore(name = "settings_theme")

private val THEME_MODE = intPreferencesKey("theme_mode")

object ThemePrefs {

    /**
     * Speichert den Modus im DataStore und setzt ihn sofort für die App.
     * Darf von jedem Coroutine-Context aus aufgerufen werden.
     */
    suspend fun setMode(context: Context, mode: Int) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    /**
     * Liefert den gespeicherten Modus als Flow<Int>.
     * Fallback: MODE_NIGHT_FOLLOW_SYSTEM
     */
    fun modeFlow(context: Context): Flow<Int> =
        context.themeDataStore.data.map { prefs ->
            prefs[THEME_MODE] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
}
