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
        val active = AccountsStore.getActive(this)
        if (active != null) {
            val expDate = active.expDate ?: ""
            val intent = Intent(this, DashboardActivity::class.java)
            intent.putExtra("DISPLAY_NAME", active.name)
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
