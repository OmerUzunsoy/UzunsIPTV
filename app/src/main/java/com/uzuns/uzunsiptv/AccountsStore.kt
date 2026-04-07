package com.uzuns.uzunsiptv

import android.content.Context
import org.json.JSONObject

private const val KEY_ACCOUNTS_LIST = "ACCOUNTS_LIST"
private const val KEY_ACTIVE_ID = "ACTIVE_ACCOUNT_ID"
private const val KEY_ACTIVE_TYPE = "ACTIVE_TYPE"
private const val KEY_MIGRATED = "__accounts_migrated"

const val TYPE_XTREAM = "XTREAM"
const val TYPE_M3U = "M3U"

const val KEY_SERVER_URL = "SERVER_URL"
const val KEY_USERNAME = "USERNAME"
const val KEY_PASSWORD = "PASSWORD"
const val KEY_PROFILE_NAME = "PROFILE_NAME"
const val KEY_EXP_DATE = "EXP_DATE"
const val KEY_M3U_URL = "M3U_URL"
const val KEY_M3U_NAME = "M3U_NAME"
const val KEY_M3U_USER = "M3U_USER"
const val KEY_M3U_PASS = "M3U_PASS"

data class StoredAccount(
    val id: String,
    val type: String,
    val name: String,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val expDate: String? = null
)

object AccountsStore {
    fun getAll(context: Context): List<StoredAccount> {
        val prefs = Prefs.accounts(context)
        migrateLegacyIfNeeded(context, prefs)
        val set = prefs.getStringSet(KEY_ACCOUNTS_LIST, emptySet()) ?: emptySet()
        return set.mapNotNull { decode(it) }.sortedBy { it.name.lowercase() }
    }

    fun save(context: Context, account: StoredAccount) {
        val prefs = Prefs.accounts(context)
        val set = prefs.getStringSet(KEY_ACCOUNTS_LIST, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.removeAll { raw -> decode(raw)?.id == account.id }
        set.add(encode(account))
        prefs.edit().putStringSet(KEY_ACCOUNTS_LIST, set).apply()
    }

    fun remove(context: Context, accountId: String) {
        val prefs = Prefs.accounts(context)
        val set = prefs.getStringSet(KEY_ACCOUNTS_LIST, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.removeAll { raw -> decode(raw)?.id == accountId }
        prefs.edit().putStringSet(KEY_ACCOUNTS_LIST, set).apply()
        if (prefs.getString(KEY_ACTIVE_ID, null) == accountId) {
            clearActive(context)
        }
    }

    fun setActive(context: Context, account: StoredAccount) {
        val prefs = Prefs.accounts(context)
        prefs.edit().putString(KEY_ACTIVE_ID, account.id).apply()

        val userPrefs = Prefs.user(context)
        val editor = userPrefs.edit()
        editor.putString(KEY_ACTIVE_TYPE, account.type)

        when (account.type) {
            TYPE_XTREAM -> {
                editor.putString(KEY_SERVER_URL, account.url)
                editor.putString(KEY_USERNAME, account.username)
                editor.putString(KEY_PASSWORD, account.password)
                editor.putString(KEY_PROFILE_NAME, account.name)
                editor.putString(KEY_EXP_DATE, account.expDate)
            }
            TYPE_M3U -> {
                editor.putString(KEY_M3U_URL, account.url)
                editor.putString(KEY_M3U_NAME, account.name)
                editor.putString(KEY_M3U_USER, account.username)
                editor.putString(KEY_M3U_PASS, account.password)
            }
        }
        editor.apply()
    }

    fun getActive(context: Context): StoredAccount? {
        val prefs = Prefs.accounts(context)
        val activeId = prefs.getString(KEY_ACTIVE_ID, null) ?: return null
        return getAll(context).firstOrNull { it.id == activeId }
    }

    fun getActiveType(context: Context): String? {
        return Prefs.user(context).getString(KEY_ACTIVE_TYPE, null)
    }

    fun clearActive(context: Context) {
        Prefs.accounts(context).edit().remove(KEY_ACTIVE_ID).apply()
        Prefs.user(context).edit().clear().apply()
    }

    private fun encode(account: StoredAccount): String {
        return JSONObject()
            .put("type", account.type)
            .put("id", account.id)
            .put("name", account.name)
            .put("url", account.url)
            .put("username", account.username)
            .put("password", account.password)
            .put("expDate", account.expDate)
            .toString()
    }

    private fun decode(raw: String): StoredAccount? {
        if (raw.startsWith("{")) {
            return try {
                val json = JSONObject(raw)
                StoredAccount(
                    id = json.optString("id"),
                    type = json.optString("type"),
                    name = json.optString("name"),
                    url = json.optString("url"),
                    username = json.optString("username").ifEmpty { null },
                    password = json.optString("password").ifEmpty { null },
                    expDate = json.optString("expDate").ifEmpty { null }
                ).takeIf { it.id.isNotBlank() && it.type.isNotBlank() && it.name.isNotBlank() && it.url.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
        val parts = raw.split("|")
        if (parts.size < 4) return null
        val type = parts[0]
        val id = parts[1]
        val name = parts.getOrNull(2) ?: return null
        val url = parts.getOrNull(3) ?: return null
        val user = parts.getOrNull(4)?.ifEmpty { null }
        val pass = parts.getOrNull(5)?.ifEmpty { null }
        val exp = parts.getOrNull(6)?.ifEmpty { null }
        return StoredAccount(id = id, type = type, name = name, url = url, username = user, password = pass, expDate = exp)
    }

    private fun migrateLegacyIfNeeded(context: Context, prefs: android.content.SharedPreferences) {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        val list = prefs.getStringSet(KEY_ACCOUNTS_LIST, emptySet())?.toMutableSet() ?: mutableSetOf()

        val legacyM3u = prefs.getStringSet("M3U_LIST", emptySet()) ?: emptySet()
        legacyM3u.forEach { raw ->
            val parts = raw.split("|")
            val name = parts.getOrNull(0) ?: "Playlist"
            val url = parts.getOrNull(1) ?: return@forEach
            val account = StoredAccount(
                id = buildAccountId(TYPE_M3U, url, null, name),
                type = TYPE_M3U,
                name = name,
                url = url
            )
            list.add(encode(account))
        }

        val userPrefs = Prefs.user(context)
        val url = userPrefs.getString(KEY_SERVER_URL, null)
        val user = userPrefs.getString(KEY_USERNAME, null)
        val pass = userPrefs.getString(KEY_PASSWORD, null)
        val name = userPrefs.getString(KEY_PROFILE_NAME, null)
        val exp = userPrefs.getString(KEY_EXP_DATE, null)
        if (!url.isNullOrBlank() && !user.isNullOrBlank() && !pass.isNullOrBlank()) {
            val display = if (!name.isNullOrBlank()) name else user
            val account = StoredAccount(
                id = buildAccountId(TYPE_XTREAM, url, user, display),
                type = TYPE_XTREAM,
                name = display,
                url = url,
                username = user,
                password = pass,
                expDate = exp
            )
            list.add(encode(account))
        }

        prefs.edit()
            .putStringSet(KEY_ACCOUNTS_LIST, list)
            .remove("M3U_LIST")
            .putBoolean(KEY_MIGRATED, true)
            .apply()
    }
}

fun buildAccountId(type: String, url: String, username: String? = null, name: String? = null): String {
    val base = listOf(type, url.trim(), username ?: "", name ?: "").joinToString(":")
    return base.lowercase()
}
