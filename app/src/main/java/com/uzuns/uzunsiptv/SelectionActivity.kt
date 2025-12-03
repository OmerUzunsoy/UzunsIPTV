package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        // --- OTO LOGIN KONTROLÜ (Buraya Taşındı) ---
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("USERNAME", null)

        if (savedUser != null) {
            // Daha önce giriş yapılmış, seçime gerek yok -> Direkt Dashboard
            val savedName = prefs.getString("PROFILE_NAME", null)
            val apiUser = prefs.getString("USERNAME", "Kullanıcı")
            val expDate = prefs.getString("EXP_DATE", "")

            val intent = Intent(this, DashboardActivity::class.java)
            val finalName = if (!savedName.isNullOrEmpty()) savedName else apiUser
            intent.putExtra("DISPLAY_NAME", finalName)
            intent.putExtra("EXP_DATE", expDate)

            startActivity(intent)
            finish()
            return
        }
        // -------------------------------------------

        setContentView(R.layout.activity_selection)

        // Xtream Butonu -> LoginActivity'e gider
        findViewById<CardView>(R.id.cardXtream).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // M3U Butonu -> M3U import ekranı
        findViewById<CardView>(R.id.cardM3u).setOnClickListener {
            startActivity(Intent(this, M3uActivity::class.java))
        }
    }
}
