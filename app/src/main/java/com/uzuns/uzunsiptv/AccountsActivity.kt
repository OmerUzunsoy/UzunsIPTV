package com.uzuns.uzunsiptv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog

class AccountsActivity : AppCompatActivity() {

    private lateinit var adapter: AccountAdapter
    private lateinit var tvActiveName: TextView
    private lateinit var tvActiveServer: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnLogoutActive: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        tvActiveName = findViewById(R.id.tvActiveAccountName)
        tvActiveServer = findViewById(R.id.tvActiveAccountServer)
        tvEmpty = findViewById(R.id.tvEmptyAccounts)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnAdd = findViewById<Button>(R.id.btnAddAccount)
        btnLogoutActive = findViewById(R.id.btnLogoutActive)
        val recycler = findViewById<RecyclerView>(R.id.rvAccounts)

        btnBack.setOnClickListener { finish() }
        btnAdd.setOnClickListener { showAddDialog() }
        btnLogoutActive.setOnClickListener { logoutActive() }

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = AccountAdapter(emptyList()) {
            Toast.makeText(this, "${it.name} seçildi", Toast.LENGTH_SHORT).show()
        }
        recycler.adapter = adapter

        loadActiveAccount()
        loadStoredAccounts()
    }

    override fun onResume() {
        super.onResume()
        loadActiveAccount()
        loadStoredAccounts()
    }

    private fun loadActiveAccount() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = prefs.getString("USERNAME", null)
        val profileName = prefs.getString("PROFILE_NAME", null)
        val server = prefs.getString("SERVER_URL", null)

        if (username != null && server != null) {
            val name = if (!profileName.isNullOrEmpty()) profileName else username
            tvActiveName.text = name
            tvActiveServer.text = server
            tvActiveServer.visibility = View.VISIBLE
            btnLogoutActive.isEnabled = true
            btnLogoutActive.alpha = 1f
        } else {
            tvActiveName.text = "Henüz giriş yapılmadı."
            tvActiveServer.text = ""
            tvActiveServer.visibility = View.GONE
            btnLogoutActive.isEnabled = false
            btnLogoutActive.alpha = 0.6f
        }
    }

    private fun loadStoredAccounts() {
        val prefs = getSharedPreferences("AccountsPrefs", MODE_PRIVATE)
        val storedSet = prefs.getStringSet("M3U_LIST", emptySet()) ?: emptySet()
        val list = storedSet.map {
            val parts = it.split("|")
            val name = parts.getOrNull(0) ?: "Playlist"
            val url = parts.getOrNull(1) ?: ""
            AccountItem(name, url, "M3U")
        }
        adapter.update(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun logoutActive() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(this, "Aktif hesaptan çıkış yapıldı", Toast.LENGTH_SHORT).show()
        loadActiveAccount()
    }

    private fun showAddDialog() {
        val options = arrayOf("Xtream API", "M3U Playlist")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hesap Ekle")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, LoginActivity::class.java))
                    1 -> startActivity(Intent(this, M3uActivity::class.java))
                }
            }.show()
    }
}
