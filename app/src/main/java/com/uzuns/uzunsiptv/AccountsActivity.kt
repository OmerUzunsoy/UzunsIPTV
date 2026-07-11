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
import androidx.recyclerview.widget.GridLayoutManager
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

        val spanCount = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) 3 else 5
        recycler.layoutManager = GridLayoutManager(this, spanCount)
        adapter = AccountAdapter(emptyList(), {
            val account = AccountsStore.getAll(this).firstOrNull { acc -> acc.id == it.id }
            if (account != null) {
                AccountsStore.setActive(this, account)
                Toast.makeText(this, "${account.name} aktif edildi", Toast.LENGTH_SHORT).show()
                loadActiveAccount()
                loadStoredAccounts()
            }
        }, { showAvatarPicker(it) })
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
        val active = AccountsStore.getActive(this)
        if (active != null) {
            tvActiveName.text = active.name
            tvActiveServer.text = active.url
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
        val active = AccountsStore.getActive(this)
        val list = AccountsStore.getAll(this).map { acc ->
            AccountItem(
                id = acc.id,
                name = acc.name,
                server = acc.url,
                type = acc.type,
                isActive = acc.id == active?.id,
                avatarIndex = Prefs.settings(this).getInt("AVATAR_${acc.id}", kotlin.math.abs(acc.id.hashCode()) % 6)
            )
        }
        adapter.update(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAvatarPicker(item: AccountItem) {
        val choices = arrayOf("Adaçayı", "Okyanus", "Mercan", "Lavanta", "Kum", "Gece Mavisi")
        AlertDialog.Builder(this)
            .setTitle("${item.name} için profil rengi")
            .setSingleChoiceItems(choices, item.avatarIndex) { dialog, which ->
                Prefs.settings(this).edit().putInt("AVATAR_${item.id}", which).apply()
                dialog.dismiss()
                loadStoredAccounts()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun logoutActive() {
        AccountsStore.clearActive(this)
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
