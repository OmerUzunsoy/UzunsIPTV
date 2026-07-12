package com.uzuns.uzunsiptv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import com.uzuns.uzunsiptv.BuildConfig

class DashboardActivity : AppCompatActivity() {

    // Görsel elemanları tanımlıyoruz
    private lateinit var tvLastUpdate: TextView
    private lateinit var btnRefresh: LinearLayout
    private lateinit var ivRefreshIcon: ImageView
    private lateinit var pbLoading: ProgressBar
    private var activeType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // --- 1. KULLANICI İSMİNİ AL VE YAZ ---
        val active = AccountsStore.getActive(this)
        activeType = active?.type
        val displayName = intent.getStringExtra("DISPLAY_NAME") ?: active?.name ?: "Kullanıcı"
        findViewById<TextView>(R.id.tvUsername).text = "Hoşgeldin, $displayName"
        findViewById<TextView>(R.id.tvSignature).text = getString(
            R.string.signature_format,
            getString(R.string.developer_name),
            BuildConfig.VERSION_NAME
        )

        // --- 2. GÖRSEL ELEMANLARI BAĞLA ---
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        btnRefresh = findViewById(R.id.btnRefresh)
        ivRefreshIcon = findViewById(R.id.ivRefreshIcon)
        pbLoading = findViewById(R.id.pbLoading)

        // --- 3. SON GÜNCELLEME TARİHİNİ GETİR ---
        val savedDate = Prefs.app(this).getString("LAST_UPDATE", "Veri yok")
        tvLastUpdate.text = "Son Güncelleme: $savedDate"

        // Only refresh on the first creation. Rotation recreates the Activity and
        // must not start another request or show another success message.
        if (savedInstanceState == null) {
            updatePlaylistData()
        }

        // --- 4. BUTON TIKLAMA OLAYLARI ---

        // Güncelle Butonu (Sağ Alt)
        btnRefresh.setOnClickListener {
            updatePlaylistData()
        }

        // Hesaplar Butonu (Üst)
        findViewById<LinearLayout>(R.id.btnAccounts).setOnClickListener {
            startActivity(Intent(this, AccountsActivity::class.java))
        }

        // Kısayollar
        findViewById<LinearLayout>(R.id.btnShortcuts).setOnClickListener {
            startActivity(Intent(this, ShortcutsActivity::class.java))
        }

        // ÇIKIŞ YAP BUTONU (Üst) - Hafızayı siler ve atar
        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            AccountsStore.clearActive(this)
            Toast.makeText(this, "Çıkış yapıldı.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SelectionActivity::class.java))
            finish()
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
            if (activeType == TYPE_M3U) {
                Toast.makeText(this, "M3U sadece canlı TV destekler.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, VodActivity::class.java))
            }
        }

        // DİZİLER KARTI
        findViewById<CardView>(R.id.cardSeries).setOnClickListener {
            if (activeType == TYPE_M3U) {
                Toast.makeText(this, "M3U sadece canlı TV destekler.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, SeriesActivity::class.java))
            }
        }

        updateContentAvailability()
    }

    override fun onResume() {
        super.onResume()
        val active = AccountsStore.getActive(this)
        activeType = active?.type
        val name = active?.name ?: "Kullanıcı"
        findViewById<TextView>(R.id.tvUsername).text = "Hoşgeldin, $name"
        updateContentAvailability()
    }

    private fun updateContentAvailability() {
        val isM3u = activeType == TYPE_M3U
        listOf(R.id.cardMovies, R.id.cardSeries).forEach { id ->
            findViewById<CardView>(id).apply {
                alpha = if (isM3u) 0.42f else 1f
                isEnabled = !isM3u
                isFocusable = !isM3u
                contentDescription = if (isM3u) {
                    "Bu bölüm M3U hesaplarında kullanılamaz"
                } else {
                    null
                }
            }
        }
    }

    // --- AKILLI GÜNCELLEME SİMÜLASYONU ---
    private fun updatePlaylistData() {
        // Animasyonu başlat
        ivRefreshIcon.visibility = View.GONE
        pbLoading.visibility = View.VISIBLE
        btnRefresh.isEnabled = false // Tıklamayı engelle
        tvLastUpdate.text = "Veriler güncelleniyor..."
        val active = AccountsStore.getActive(this)
        if (active == null) {
            restoreRefreshState("Hesap bulunamadı.")
            return
        }

        if (active.type == TYPE_M3U) {
            lifecycleScope.launch {
                try {
                    M3uRepository.loadOrFetch(this@DashboardActivity, active.url, forceRefresh = true)
                    setLastUpdateNow()
                    restoreRefreshState("M3U listesi güncellendi!")
                } catch (e: Exception) {
                    restoreRefreshState("M3U güncellenemedi: ${e.localizedMessage}")
                }
            }
            return
        }

        if (active.username.isNullOrBlank() || active.password.isNullOrBlank()) {
            restoreRefreshState("Hesap bilgileri eksik. Lütfen yeniden giriş yapın.")
            return
        }

        val api = ApiClient.getClient(active.url).create(XtreamApi::class.java)
        api.getLiveCategories(active.username ?: "", active.password ?: "").enqueue(object : retrofit2.Callback<List<LiveCategory>> {
            override fun onResponse(
                call: retrofit2.Call<List<LiveCategory>>,
                response: retrofit2.Response<List<LiveCategory>>
            ) {
                if (response.isSuccessful) {
                    setLastUpdateNow()
                    restoreRefreshState("Tüm içerik başarıyla güncellendi!")
                } else {
                    restoreRefreshState("Güncelleme hatası (HTTP ${response.code()})")
                }
            }

            override fun onFailure(call: retrofit2.Call<List<LiveCategory>>, t: Throwable) {
                restoreRefreshState("Güncelleme başarısız: ${t.localizedMessage}")
            }
        })
    }

    private fun setLastUpdateNow() {
        val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        val currentDate = sdf.format(Date())
        Prefs.app(this).edit().putString("LAST_UPDATE", currentDate).apply()
        tvLastUpdate.text = "Son Güncelleme: $currentDate"
    }

    private fun restoreRefreshState(message: String) {
        // A Retrofit callback can arrive after an orientation change. Do not let
        // the destroyed Activity update views or display a stale Toast.
        if (isFinishing || isDestroyed) return
        pbLoading.visibility = View.GONE
        ivRefreshIcon.visibility = View.VISIBLE
        btnRefresh.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
