package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    // Görsel elemanları tanımlıyoruz
    private lateinit var tvLastUpdate: TextView
    private lateinit var btnRefresh: LinearLayout
    private lateinit var ivRefreshIcon: ImageView
    private lateinit var pbLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // --- 1. KULLANICI İSMİNİ AL VE YAZ ---
        val displayName = intent.getStringExtra("DISPLAY_NAME") ?: "Kullanıcı"
        findViewById<TextView>(R.id.tvUsername).text = "Hoşgeldin, $displayName"

        // --- 2. GÖRSEL ELEMANLARI BAĞLA ---
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        btnRefresh = findViewById(R.id.btnRefresh)
        ivRefreshIcon = findViewById(R.id.ivRefreshIcon)
        pbLoading = findViewById(R.id.pbLoading)

        // --- 3. SON GÜNCELLEME TARİHİNİ GETİR ---
        val savedDate = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("LAST_UPDATE", "Veri yok")
        tvLastUpdate.text = "Son Güncelleme: $savedDate"

        // Uygulama açılınca otomatik güncelleme simülasyonu yap
        updatePlaylistData()

        // --- 4. BUTON TIKLAMA OLAYLARI ---

        // Güncelle Butonu (Sağ Alt)
        btnRefresh.setOnClickListener {
            updatePlaylistData()
        }

        // Hesaplar Butonu (Üst)
        findViewById<LinearLayout>(R.id.btnAccounts).setOnClickListener {
            startActivity(Intent(this, AccountsActivity::class.java))
        }

        // Ayarlar Butonu (Üst)
        findViewById<LinearLayout>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Yakında", Toast.LENGTH_SHORT).show()
        }

        // ÇIKIŞ YAP BUTONU (Üst) - Hafızayı siler ve atar
        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply() // Beni hatırla verisini sil

            Toast.makeText(this, "Çıkış yapıldı.", Toast.LENGTH_SHORT).show()

            // Giriş ekranına (LoginActivity) geri dön
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Bu sayfayı kapat
        }

        // --- 5. KART TIKLAMALARI (ANA MENÜ) ---

        // CANLI TV KARTI -> LiveTvActivity'i Açar
        findViewById<CardView>(R.id.cardLive).setOnClickListener {
            // Burası önemli: LiveTvActivity'ye geçiş yapıyoruz
            val intent = Intent(this, LiveTvActivity::class.java)
            startActivity(intent)
        }

        // FİLMLER KARTI
        findViewById<CardView>(R.id.cardMovies).setOnClickListener {
            // Toast mesajını siliyoruz, gerçek sayfayı açıyoruz
            startActivity(Intent(this, VodActivity::class.java))
        }

        // DİZİLER KARTI
        findViewById<CardView>(R.id.cardSeries).setOnClickListener {
            startActivity(Intent(this, SeriesActivity::class.java))
        }
    }

    // --- AKILLI GÜNCELLEME SİMÜLASYONU ---
    private fun updatePlaylistData() {
        // Animasyonu başlat
        ivRefreshIcon.visibility = View.GONE
        pbLoading.visibility = View.VISIBLE
        btnRefresh.isEnabled = false // Tıklamayı engelle
        tvLastUpdate.text = "Veriler güncelleniyor..."

        // 2.5 saniye sonra işlemi bitir (Simülasyon)
        Handler(Looper.getMainLooper()).postDelayed({
            val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
            val currentDate = sdf.format(Date())

            // Tarihi kaydet
            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit().putString("LAST_UPDATE", currentDate).apply()

            // Görünümü eski haline getir
            pbLoading.visibility = View.GONE
            ivRefreshIcon.visibility = View.VISIBLE
            btnRefresh.isEnabled = true
            tvLastUpdate.text = "Son Güncelleme: $currentDate"

            Toast.makeText(this, "Tüm içerik başarıyla güncellendi!", Toast.LENGTH_SHORT).show()
        }, 2500)
    }
}
