package com.uzuns.uzunsiptv

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

object M3uRepository {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun loadOrFetch(context: Context, url: String, forceRefresh: Boolean): List<LiveStream> {
        return withContext(Dispatchers.IO) {
            val file = cacheFile(context, url)
            if (!forceRefresh && file.exists()) {
                return@withContext M3uParser.parse(file.readText())
            }
            val content = readSource(context, url)
            file.writeText(content)
            M3uParser.parse(content)
        }
    }

    private fun cacheFile(context: Context, url: String): File {
        val safe = url.hashCode().toString()
        return File(context.filesDir, "m3u_$safe.m3u")
    }

    private fun readSource(context: Context, url: String): String {
        return when {
            url.startsWith("content://") -> {
                context.contentResolver.openInputStream(Uri.parse(url))?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IllegalArgumentException("Dosya açılamadı")
            }
            url.startsWith("file://") -> {
                val path = Uri.parse(url).path ?: throw IllegalArgumentException("Dosya yolu geçersiz")
                File(path).readText()
            }
            File(url).exists() -> File(url).readText()
            else -> download(url)
        }
    }

    private fun download(url: String): String {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code()}")
            }
            return response.body()?.string() ?: ""
        }
    }
}

object M3uParser {
    private val attrRegex = Regex("""([A-Za-z0-9_-]+)=\"(.*?)\"""")

    fun parse(content: String): List<LiveStream> {
        val result = mutableListOf<LiveStream>()
        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        var pendingNum: String? = null
        var currentGroup: String? = null
        var id = 1

        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val info = line.substringAfter(":")
                    val parts = info.split(",", limit = 2)
                    val attrs = parts.getOrNull(0).orEmpty()
                    val name = parts.getOrNull(1)?.trim().orEmpty()
                    pendingName = if (name.isNotEmpty()) name else null
                    pendingLogo = null
                    pendingGroup = null
                    pendingNum = null
                    attrRegex.findAll(attrs).forEach { match ->
                        val key = match.groupValues[1]
                        val value = match.groupValues[2]
                        when (key.lowercase()) {
                            "tvg-logo" -> pendingLogo = value
                            "group-title" -> pendingGroup = value
                            "tvg-chno" -> pendingNum = value
                            "tvg-name" -> if (pendingName.isNullOrEmpty()) pendingName = value
                        }
                    }
                }
                line.startsWith("#EXTGRP", ignoreCase = true) -> {
                    currentGroup = line.substringAfter(":").trim()
                }
                line.startsWith("#") -> {
                    // ignore comments
                }
                else -> {
                    val url = line
                    val name = pendingName ?: url.substringAfterLast("/").take(40)
                    val group = pendingGroup?.ifEmpty { null } ?: currentGroup ?: "Diğer"
                    result.add(
                        LiveStream(
                            num = pendingNum,
                            name = name,
                            streamType = "m3u",
                            streamId = id++,
                            streamIcon = pendingLogo,
                            epgChannelId = null,
                            added = null,
                            categoryId = group,
                            customSid = null,
                            tvArchive = null,
                            directSource = url,
                            tvArchiveDuration = null
                        )
                    )
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = null
                    pendingNum = null
                }
            }
        }
        return result
    }
}
