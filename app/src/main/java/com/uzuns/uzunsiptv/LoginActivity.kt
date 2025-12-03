package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginActivity : AppCompatActivity() {

    private lateinit var etProfileName: TextInputEditText
    private lateinit var etServerUrl: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Elemanları bağla
        etProfileName = findViewById(R.id.etProfileName)
        etServerUrl = findViewById(R.id.etServerUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnBack = findViewById(R.id.btnBack)

        // Geri Dön Butonu
        btnBack.setOnClickListener {
            finish() // Bu ekranı kapatır, SelectionActivity'e döner
        }

        // Giriş Butonu
        btnLogin.setOnClickListener {
            val profileName = etProfileName.text.toString().trim()
            val inputUrl = etServerUrl.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (inputUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen zorunlu alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(this, "İnternet bağlantısı yok. Lütfen kontrol edin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val normalizedUrl = try {
                ApiClient.sanitizeBaseUrl(inputUrl)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.message ?: "Sunucu adresi hatalı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(normalizedUrl, username, password, profileName)
        }
    }

    private fun doLogin(url: String, user: String, pass: String, profileName: String) {
        btnLogin.isEnabled = false
        btnLogin.text = "..."

        try {
            val apiService = ApiClient.getClient(url).create(XtreamApi::class.java)
            apiService.login(user, pass).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "GİRİŞ YAP"

                    if (response.isSuccessful && response.body() != null) {
                        val loginData = response.body()!!
                        if (loginData.userInfo?.auth == 1) {
                            // Başarılı
                            val apiUsername = loginData.userInfo.username ?: user
                            val expDate = loginData.userInfo.expDate ?: ""

                            // KAYDET
                            val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("SERVER_URL", url)
                                putString("USERNAME", user)
                                putString("PASSWORD", pass)
                                putString("PROFILE_NAME", profileName)
                                putString("EXP_DATE", expDate)
                                apply()
                            }

                            Toast.makeText(applicationContext, "Giriş Başarılı!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                            val finalName = if (profileName.isNotEmpty()) profileName else apiUsername
                            intent.putExtra("DISPLAY_NAME", finalName)
                            intent.putExtra("EXP_DATE", expDate)

                            // Geri tuşuna basıldığında tekrar login ekranına dönmemek için geçmişi temizle
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)

                        } else {
                            Toast.makeText(applicationContext, "Hatalı Giriş (auth başarısız)", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val code = response.code()
                        val msg = "Sunucu Hatası (HTTP $code)"
                        Log.e(TAG, msg + " url=$url")
                        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "GİRİŞ YAP"
                    handleRequestFailure(t, url)
                }
            })
        } catch (e: Exception) {
            btnLogin.isEnabled = true
            btnLogin.text = "GİRİŞ YAP"
            Log.e(TAG, "Retrofit kurulurken hata", e)
            Toast.makeText(this, "URL Hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleRequestFailure(error: Throwable, url: String) {
        val message = when (error) {
            is UnknownHostException -> {
                val host = Uri.parse(url).host ?: url
                "Sunucuya ulaşılamadı: $host"
            }
            is SocketTimeoutException -> "Sunucu yanıt vermedi (zaman aşımı)"
            else -> "Hata: ${error.localizedMessage ?: error.javaClass.simpleName}"
        }
        Log.e(TAG, "Login isteği başarısız url=$url", error)
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
