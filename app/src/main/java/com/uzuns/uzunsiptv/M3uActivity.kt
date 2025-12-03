package com.uzuns.uzunsiptv

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class M3uActivity : AppCompatActivity() {

    private lateinit var tvLastImport: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var etUrl: TextInputEditText
    private lateinit var etUser: TextInputEditText
    private lateinit var etPass: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m3u_form)

        etName = findViewById(R.id.etPlaylistName)
        etUrl = findViewById(R.id.etPlaylistUrl)
        etUser = findViewById(R.id.etPlaylistUser)
        etPass = findViewById(R.id.etPlaylistPass)
        tvLastImport = findViewById(R.id.tvLastImport)
        val btnImport = findViewById<Button>(R.id.btnImportM3u)
        val btnBack = findViewById<Button>(R.id.btnBack)

        etName.hint = "Playlist Adı (Örn: Benim Listem)"
        etUrl.setText("https://")
        etUrl.hint = "M3U URL veya dosya yolu"
        etUser.hint = "Kullanıcı Adı (İsteğe bağlı)"
        etPass.hint = "Şifre (İsteğe bağlı)"
        btnImport.text = "PLAYLİSTİ KAYDET"
        btnBack.setOnClickListener { finish() }


        val info = getSharedPreferences("M3UPrefs", MODE_PRIVATE).getString("LAST_IMPORT", "")
        btnImport.setOnClickListener { importPlaylist() }
        if (!info.isNullOrEmpty()) {
            tvLastImport.text = info
            tvLastImport.visibility = TextView.VISIBLE
        }
    }

    private fun importPlaylist() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val url = etUrl.text?.toString()?.trim().orEmpty()
        if (name.isEmpty() || url.isEmpty()) {
            Toast.makeText(this, "Playlist adı ve URL zorunludur.", Toast.LENGTH_SHORT).show()
            return
        }
        val accountsPrefs = getSharedPreferences("AccountsPrefs", MODE_PRIVATE)
        val currentSet = accountsPrefs.getStringSet("M3U_LIST", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentSet.add("$name|$url")
        accountsPrefs.edit().putStringSet("M3U_LIST", currentSet).apply()

        val info = "$name\n${url.take(60)}"
        getSharedPreferences("M3UPrefs", MODE_PRIVATE).edit().putString("LAST_IMPORT", info).apply()
        tvLastImport.text = info
        Toast.makeText(this, "Playlist kaydedildi", Toast.LENGTH_SHORT).show()
    }
}
