package com.uzuns.uzunsiptv

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val dark = prefs.getBoolean("DARK_THEME", true)
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
