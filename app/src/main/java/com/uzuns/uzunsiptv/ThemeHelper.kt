package com.uzuns.uzunsiptv

import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    fun applyTheme(@Suppress("UNUSED_PARAMETER") context: android.content.Context) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
