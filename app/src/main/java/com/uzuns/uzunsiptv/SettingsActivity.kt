package com.uzuns.uzunsiptv

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("SettingsPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        bindSwitch(R.id.switchAutoPlay, "AUTO_PLAY", true)
        bindSwitch(R.id.switchDarkTheme, "DARK_THEME", true) { enabled ->
            ThemeHelper.applyTheme(this)
            recreate()
        }
    }

    private fun bindSwitch(id: Int, key: String, default: Boolean, onChange: ((Boolean) -> Unit)? = null) {
        val sw = findViewById<SwitchMaterial>(id)
        sw.isChecked = prefs.getBoolean(key, default)
        sw.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(key, isChecked).apply()
            Toast.makeText(this, "Ayar güncellendi", Toast.LENGTH_SHORT).show()
            onChange?.invoke(isChecked)
        }
    }
}
