package com.uzuns.uzunsiptv

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // IPTV listeleri büyük olabiliyor, zaman aşımını yükselt
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Kullanıcının girdiği sunucu adresini normalize eder.
     * Boş veya hatalı adreslerde IllegalArgumentException fırlatılır.
     */
    fun sanitizeBaseUrl(rawBaseUrl: String): String {
        var url = rawBaseUrl.trim()
        if (url.isEmpty()) throw IllegalArgumentException("Sunucu adresi boş olamaz.")

        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }

        val normalized = if (url.endsWith("/")) url else "$url/"
        return normalized
    }

    fun getClient(baseUrl: String): Retrofit {
        val normalizedUrl = sanitizeBaseUrl(baseUrl)
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
