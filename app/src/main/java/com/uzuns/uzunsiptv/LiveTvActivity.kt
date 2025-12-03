package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.app.UiModeManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uzuns.uzunsiptv.data.db.AppDatabase
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LiveTvActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var pbLoading: ProgressBar

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private var allChannelsList = listOf<LiveStream>()
    private var favoritesList = listOf<LiveStream>()
    private var recentList = listOf<LiveStream>()
    private var apiCategories = listOf<LiveCategory>()

    private var activeCategoryId = "ALL"
    private var hasFocusedChannelsOnce = false
    private val logTag = "LiveTvActivity"
    private lateinit var panelCategories: View
    private var isMenuHidden = false
    private var restoreCategoryFocus = false
    private var canHidePanel = true
    private lateinit var favActions: View
    private lateinit var btnFavAuto: View
    private lateinit var btnFavClear: View

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tv)

        rvCategories = findViewById(R.id.rvCategories)
        rvChannels = findViewById(R.id.rvChannels)
        pbLoading = findViewById(R.id.pbLoading)
        panelCategories = findViewById(R.id.panelCategories)
        favActions = findViewById(R.id.favActions)
        btnFavAuto = findViewById(R.id.btnFavAuto)
        btnFavClear = findViewById(R.id.btnFavClear)

        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val mode = uiModeManager.currentModeType
        canHidePanel = mode == Configuration.UI_MODE_TYPE_TELEVISION

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        btnFavAuto.setOnClickListener { autoAssignFavorites() }
        btnFavClear.setOnClickListener { clearAssignments() }

        setupRecyclerViews()
        loadLocalData()
        loadData()
        rvCategories.requestFocus()
    }

    private fun loadLocalData() {
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            db.favoriteDao().getAllFavorites().collect { favs ->
                favoritesList = favs
                    .filter { it.streamType == "live" }
                    .map {
                        // EKSİK PARAMETRELER DOLDURULDU
                        LiveStream(
                            num = "0",
                            name = it.name,
                            streamType = "live",
                            streamId = it.streamId,
                            streamIcon = it.streamIcon,
                            epgChannelId = null,
                            added = null,
                            categoryId = "",
                            customSid = null,
                            tvArchive = null,
                            directSource = "",
                            tvArchiveDuration = null
                        )
                    }

                if (activeCategoryId == "FAVORITES") channelAdapter.updateList(favoritesList)
                updateCategoryMenu()
            }
        }

        lifecycleScope.launch {
            db.watchDao().getAllProgress().collect { history ->
                recentList = history
                    .filter { it.streamType == "live" }
                    .take(10)
                    .map {
                        // EKSİK PARAMETRELER DOLDURULDU
                        LiveStream(
                            num = "0",
                            name = it.name,
                            streamType = "live",
                            streamId = it.streamId,
                            streamIcon = it.streamIcon,
                            epgChannelId = null,
                            added = null,
                            categoryId = "",
                            customSid = null,
                            tvArchive = null,
                            directSource = "",
                            tvArchiveDuration = null
                        )
                    }

                if (activeCategoryId == "RECENT") channelAdapter.updateList(recentList)
                updateCategoryMenu()
            }
        }
    }

    private fun updateCategoryMenu() {
        restoreCategoryFocus = rvCategories.hasFocus()
        val finalCats = mutableListOf<LiveCategory>()
        finalCats.add(LiveCategory("ALL", "TÜM KANALLAR", "0"))
        finalCats.add(LiveCategory("RECENT", "SON İZLENENLER \uD83D\uDD52", "0"))
        finalCats.add(LiveCategory("FAVORITES", "FAVORİLERİM ⭐", "0"))
        finalCats.addAll(apiCategories)
        categoryAdapter.updateList(finalCats)
        categoryAdapter.setSelectedCategory(activeCategoryId)
        scrollCategoryToSelected()
        if (restoreCategoryFocus) focusSelectedCategory()
        favActions.visibility = if (activeCategoryId == "FAVORITES") View.VISIBLE else View.GONE
        ChannelManager.categoryList = finalCats
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter(
            onClick = { selectedCategory ->
                activeCategoryId = selectedCategory.categoryId
                channelAdapter.updateList(getListByCategory(activeCategoryId))
                // Odak menüde kalsın; sağ oka basınca listeye geçsin
            },
            onNavigateRight = {
                hideCategoryPanel()
                focusFirstChannel(force = true)
            }
        )
        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = categoryAdapter

        channelAdapter = ChannelAdapter(
            onClick = { channel ->
                val currentList = getListByCategory(activeCategoryId)
                if (currentList.isNotEmpty()) {
                    ChannelManager.channelList = currentList
                    ChannelManager.currentPosition = currentList.indexOf(channel)
                } else {
                    ChannelManager.channelList = listOf(channel)
                    ChannelManager.currentPosition = 0
                }

                val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra("STREAM_ID", channel.streamId)
                intent.putExtra("STREAM_TYPE", "live")
                intent.putExtra("STREAM_NAME", channel.name)
                intent.putExtra("STREAM_ICON", channel.streamIcon)
                startActivity(intent)
            },
            onLongClick = { channel -> showHotkeyDialog(channel) }
        )
        rvChannels.layoutManager = GridLayoutManager(this, 4)
        rvChannels.adapter = channelAdapter
        lockRecyclerAtBottom(rvChannels)
    }

    private fun getListByCategory(catId: String): List<LiveStream> {
        return when (catId) {
            "RECENT" -> recentList
            "FAVORITES" -> favoritesList
            "ALL" -> allChannelsList
            else -> allChannelsList.filter { it.categoryId == catId }
        }
    }

    private fun loadData() {
        pbLoading.visibility = View.VISIBLE
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val user = prefs.getString("USERNAME", "") ?: ""
        val pass = prefs.getString("PASSWORD", "") ?: ""
        val url = prefs.getString("SERVER_URL", "") ?: ""
        if (user.isBlank() || pass.isBlank() || url.isBlank()) {
            pbLoading.visibility = View.GONE
            toast("Hesap bilgileri bulunamadı. Lütfen yeniden giriş yapın.")
            return
        }
        val api = ApiClient.getClient(url).create(XtreamApi::class.java)

        api.getLiveCategories(u = user, p = pass).enqueue(object : Callback<List<LiveCategory>> {
            override fun onResponse(call: Call<List<LiveCategory>>, response: Response<List<LiveCategory>>) {
                if (response.isSuccessful && response.body() != null) {
                    apiCategories = response.body()!!
                    updateCategoryMenu()
                    fetchAllChannels(api, user, pass)
                } else {
                    pbLoading.visibility = View.GONE
                    toast("Kategori alınamadı (HTTP ${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<LiveCategory>>, t: Throwable) {
                pbLoading.visibility = View.GONE
                toast("Kategori isteği başarısız: ${t.localizedMessage}")
            }
        })
    }

    private fun fetchAllChannels(api: XtreamApi, user: String, pass: String) {
        api.getLiveStreams(u = user, p = pass).enqueue(object : Callback<List<LiveStream>> {
            override fun onResponse(call: Call<List<LiveStream>>, response: Response<List<LiveStream>>) {
                pbLoading.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    allChannelsList = response.body()!!
                    channelAdapter.updateList(getListByCategory(activeCategoryId))
                } else {
                    toast("Kanal listesi alınamadı (HTTP ${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<LiveStream>>, t: Throwable) {
                pbLoading.visibility = View.GONE
                toast("Kanal isteği başarısız: ${t.localizedMessage}")
            }
        })
    }

    override fun onBackPressed() {
        if (rvChannels.hasFocus() && handleBackFromList(rvChannels)) return
        super.onBackPressed()
    }

    private fun lockRecyclerAtBottom(recyclerView: RecyclerView) {
        recyclerView.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    showCategoryPanel()
                    rvCategories.post { focusSelectedCategory() }
                    return@setOnKeyListener true
                }
                KeyEvent.KEYCODE_BACK -> return@setOnKeyListener handleBackFromList(recyclerView)
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    val focused = recyclerView.focusedChild ?: return@setOnKeyListener false
                    val position = recyclerView.getChildAdapterPosition(focused)
                    if (position != RecyclerView.NO_POSITION) {
                        val total = recyclerView.adapter?.itemCount ?: return@setOnKeyListener false
                        val spanCount = (recyclerView.layoutManager as? GridLayoutManager)?.spanCount ?: 1
                        val lastRowCount = if (total % spanCount == 0) spanCount else total % spanCount
                        if (position >= total - lastRowCount) return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun focusSelectedCategory() {
        scrollCategoryToSelected()
        rvCategories.post {
            val index = categoryAdapter.getSelectedIndex()
            rvCategories.findViewHolderForAdapterPosition(index)?.itemView?.requestFocus()
        }
    }

    private fun scrollCategoryToSelected() {
        val index = categoryAdapter.getSelectedIndex()
        rvCategories.layoutManager?.scrollToPosition(index)
    }

    private fun handleBackFromList(recyclerView: RecyclerView): Boolean {
        showCategoryPanel()
        focusSelectedCategory()
        return true
    }

    private fun focusFirstChannel(force: Boolean = false) {
        if (!force && hasFocusedChannelsOnce) return
        val count = rvChannels.adapter?.itemCount ?: 0
        if (count == 0) {
            rvChannels.requestFocus()
            return
        }
        hasFocusedChannelsOnce = true
        rvChannels.layoutManager?.scrollToPosition(0)
        rvChannels.post {
            rvChannels.requestFocus()
            rvChannels.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        android.util.Log.e(logTag, msg)
    }

    private fun hideCategoryPanel() {
        if (!canHidePanel) return
        if (isMenuHidden) return
        isMenuHidden = true
        panelCategories.animate().translationX(-panelCategories.width.toFloat()).alpha(0f).setDuration(200).withEndAction {
            panelCategories.visibility = View.GONE
        }.start()
    }

    private fun showCategoryPanel() {
        if (!canHidePanel) return
        if (!isMenuHidden) return
        isMenuHidden = false
        panelCategories.visibility = View.VISIBLE
        panelCategories.post {
            panelCategories.translationX = -panelCategories.width.toFloat()
            panelCategories.alpha = 0f
            panelCategories.animate().translationX(0f).alpha(1f).setDuration(200).start()
        }
    }

    private fun showHotkeyDialog(channel: LiveStream) {
        val existing = ChannelHotkeyManager.getNumberForStream(this, channel.streamId)
        val defaultNumber = existing
            ?: channel.num?.trimStart('0').takeUnless { it.isNullOrEmpty() }
            ?: ChannelHotkeyManager.firstAvailableNumber(this)
        val editText = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Kanal numarası"
            setText(defaultNumber)
            setSelection(text.length)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Kısayol Ata")
            .setMessage("Kumanda numarasını girin (ör. 101)")
            .setView(editText)
            .setPositiveButton("Kaydet") { _, _ ->
                val num = editText.text.toString().trim()
                if (num.isNotEmpty()) {
                    ChannelHotkeyManager.assignHotkey(this, num, channel.streamId)
                    Toast.makeText(this, "Kısayol kaydedildi: $num", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_GUIDE)) {
            if (activeCategoryId == "FAVORITES") {
                showFavoritesActions()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showFavoritesActions() {
        val options = arrayOf("Oto Atama", "Atamaları Sil")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Favoriler")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        autoAssignFavorites()
                    }
                    1 -> {
                        clearAssignments()
                    }
                }
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun autoAssignFavorites() {
        if (favoritesList.isNotEmpty()) {
            ChannelHotkeyManager.assignSequential(this, favoritesList)
            channelAdapter.notifyDataSetChanged()
            toast("Favorilere 1'den başlayarak numara atandı")
        } else {
            toast("Favori kanal yok")
        }
    }

    private fun clearAssignments() {
        ChannelHotkeyManager.clearAll(this)
        channelAdapter.notifyDataSetChanged()
        toast("Tüm atamalar silindi")
    }
}
