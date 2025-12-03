package com.uzuns.uzunsiptv

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("user_info") val userInfo: UserInfo?,
    @SerializedName("server_info") val serverInfo: ServerInfo?
)

data class UserInfo(
    val username: String?,
    val auth: Int?, // 1 ise başarılı
    val status: String?, // "Active"
    @SerializedName("exp_date") val expDate: String? // Bitiş tarihi
)

data class ServerInfo(
    val url: String?,
    val port: String?,
    @SerializedName("https_port") val httpsPort: String?,
    @SerializedName("server_protocol") val protocol: String?
)