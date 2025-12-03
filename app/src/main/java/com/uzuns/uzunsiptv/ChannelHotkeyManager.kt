package com.uzuns.uzunsiptv

import android.content.Context
import com.uzuns.uzunsiptv.LiveStream
import org.json.JSONObject

object ChannelHotkeyManager {
    private const val PREFS = "ChannelHotkeys"
    private const val KEY_MAP = "HOTKEY_MAP"

    fun assignHotkey(context: Context, number: String, streamId: Int) {
        val normalized = normalize(number)
        if (normalized.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONObject(prefs.getString(KEY_MAP, "{}") ?: "{}")
        json.put(normalized, streamId)
        prefs.edit().putString(KEY_MAP, json.toString()).apply()
    }

    fun getStreamIdForNumber(context: Context, number: String): Int? {
        val normalized = normalize(number)
        if (normalized.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONObject(prefs.getString(KEY_MAP, "{}") ?: "{}")
        return if (json.has(normalized)) json.getInt(normalized) else null
    }

    fun getNumberForStream(context: Context, streamId: Int): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONObject(prefs.getString(KEY_MAP, "{}") ?: "{}")
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (json.optInt(k, -1) == streamId) return k
        }
        return null
    }

    fun firstAvailableNumber(context: Context, start: Int = 1): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONObject(prefs.getString(KEY_MAP, "{}") ?: "{}")
        val used = mutableSetOf<Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            json.optInt(k, -1).takeIf { it >= 0 }?.let { used.add(it) }
            k.toIntOrNull()?.let { used.add(it) }
        }
        var candidate = start
        while (used.contains(candidate)) {
            candidate++
        }
        return candidate.toString()
    }

    fun assignSequential(context: Context, streams: List<LiveStream>, start: Int = 1) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONObject(prefs.getString(KEY_MAP, "{}") ?: "{}")
        var current = start
        streams.forEach { stream ->
            json.put(current.toString(), stream.streamId)
            current++
        }
        prefs.edit().putString(KEY_MAP, json.toString()).apply()
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_MAP).apply()
    }

    private fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val withoutLeadingZeros = trimmed.trimStart('0')
        return if (withoutLeadingZeros.isEmpty()) "0" else withoutLeadingZeros
    }
}
