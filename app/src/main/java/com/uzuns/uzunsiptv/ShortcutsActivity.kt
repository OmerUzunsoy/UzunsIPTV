package com.uzuns.uzunsiptv

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ShortcutsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shortcuts)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }
}
