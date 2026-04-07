package com.uzuns.uzunsiptv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class M3uActivity : AppCompatActivity() {

    private lateinit var tvLastImport: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var etUrl: TextInputEditText
    private lateinit var etUser: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var btnImport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m3u_form)

        etName = findViewById(R.id.etPlaylistName)
        etUrl = findViewById(R.id.etPlaylistUrl)
        etUser = findViewById(R.id.etPlaylistUser)
        etPass = findViewById(R.id.etPlaylistPass)
        tvLastImport = findViewById(R.id.tvLastImport)
        btnImport = findViewById<Button>(R.id.btnImportM3u)
        val btnBack = findViewById<Button>(R.id.btnBack)

        etName.hint = "Playlist Adı (Örn: Benim Listem)"
        etUrl.setText("https://")
        etUrl.hint = "M3U URL veya dosya yolu"
        etUser.hint = "Kullanıcı Adı (İsteğe bağlı)"
        etPass.hint = "Şifre (İsteğe bağlı)"
        btnImport.text = "PLAYLİSTİ KAYDET"
        btnBack.setOnClickListener { finish() }


        val info = Prefs.m3u(this).getString("LAST_IMPORT", "")
        btnImport.setOnClickListener { importPlaylist() }
        if (!info.isNullOrEmpty()) {
            tvLastImport.text = info
            tvLastImport.visibility = TextView.VISIBLE
        }
    }

    private fun importPlaylist() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val rawUrl = etUrl.text?.toString()?.trim().orEmpty()
        if (name.isEmpty() || rawUrl.isEmpty()) {
            Toast.makeText(this, "Playlist adı ve URL zorunludur.", Toast.LENGTH_SHORT).show()
            return
        }
        val url = if (
            rawUrl.startsWith("http://", true) ||
            rawUrl.startsWith("https://", true) ||
            rawUrl.startsWith("file://", true) ||
            rawUrl.startsWith("content://", true)
        ) {
            rawUrl
        } else {
            "http://$rawUrl"
        }
        val user = etUser.text?.toString()?.trim().orEmpty()
        val pass = etPass.text?.toString()?.trim().orEmpty()
        btnImport.isEnabled = false
        btnImport.text = "KAYDEDİLİYOR..."

        lifecycleScope.launch {
            try {
                M3uRepository.loadOrFetch(this@M3uActivity, url, forceRefresh = true)
                val accountName = name.ifEmpty { "M3U Playlist" }
                val account = StoredAccount(
                    id = buildAccountId(TYPE_M3U, url, user, accountName),
                    type = TYPE_M3U,
                    name = accountName,
                    url = url,
                    username = user.ifEmpty { null },
                    password = pass.ifEmpty { null }
                )
                AccountsStore.save(this@M3uActivity, account)
                AccountsStore.setActive(this@M3uActivity, account)

                val info = "$accountName\n${url.take(60)}"
                Prefs.m3u(this@M3uActivity).edit().putString("LAST_IMPORT", info).apply()
                tvLastImport.text = info
                Toast.makeText(this@M3uActivity, "Playlist kaydedildi", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@M3uActivity, DashboardActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@M3uActivity, "M3U yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                btnImport.isEnabled = true
                btnImport.text = "PLAYLİSTİ KAYDET"
            }
        }
    }
}
