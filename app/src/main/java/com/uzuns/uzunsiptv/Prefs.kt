package com.uzuns.uzunsiptv

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object Prefs {
    private const val USER_PREFS = "user_prefs"
    private const val ACCOUNTS_PREFS = "accounts_prefs"
    private const val M3U_PREFS = "m3u_prefs"
    private const val MIGRATED_FLAG = "__migrated_to_encrypted"

    fun user(context: Context): SharedPreferences =
        encrypted(context, USER_PREFS, legacyName = "UserPrefs")

    fun accounts(context: Context): SharedPreferences =
        encrypted(context, ACCOUNTS_PREFS, legacyName = "AccountsPrefs")

    fun m3u(context: Context): SharedPreferences =
        encrypted(context, M3U_PREFS, legacyName = "M3UPrefs")

    fun settings(context: Context): SharedPreferences =
        context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

    fun app(context: Context): SharedPreferences =
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    private fun encrypted(
        context: Context,
        fileName: String,
        legacyName: String? = null
    ): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val securePrefs = EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        if (legacyName != null) {
            migrateLegacyIfNeeded(context, legacyName, securePrefs)
        }
        return securePrefs
    }

    private fun migrateLegacyIfNeeded(
        context: Context,
        legacyName: String,
        target: SharedPreferences
    ) {
        if (target.getBoolean(MIGRATED_FLAG, false)) return
        val legacy = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        val legacyData = legacy.all
        if (legacyData.isNotEmpty()) {
            val editor = target.edit()
            for ((key, value) in legacyData) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Long -> editor.putLong(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
            }
            editor.putBoolean(MIGRATED_FLAG, true).apply()
            legacy.edit().clear().apply()
        } else {
            target.edit().putBoolean(MIGRATED_FLAG, true).apply()
        }
    }
}
