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
import android.widget.LinearLayout
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uzuns.uzunsiptv.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var channelsByCategory = emptyMap<String, List<LiveStream>>()
    private var isM3uMode = false

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
    private var gridSpanCount = 4

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
        isM3uMode = AccountsStore.getActiveType(this) == TYPE_M3U

        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val mode = uiModeManager.currentModeType
        canHidePanel = mode == Configuration.UI_MODE_TYPE_TELEVISION
        gridSpanCount = resolveGridSpanCount()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        btnFavAuto.setOnClickListener { autoAssignFavorites() }
        btnFavClear.setOnClickListener { clearAssignments() }

        adaptLayoutForPhone()
        setupRecyclerViews()
        loadLocalData()
        loadData()
        rvCategories.requestFocus()
    }

    private fun loadLocalData() {
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                db.favoriteDao().getAllFavorites().collect { favs ->
                    favoritesList = favs
                        .filter { if (isM3uMode) it.streamType == "m3u" else it.streamType == "live" }
                        .map {
                            LiveStream(
                                num = "0",
                                name = it.name,
                                streamType = if (isM3uMode) "m3u" else "live",
                                streamId = it.streamId,
                                streamIcon = it.streamIcon,
                                epgChannelId = null,
                                added = null,
                                categoryId = "",
                                customSid = null,
                                tvArchive = null,
                                directSource = it.directSource,
                                tvArchiveDuration = null
                            )
                        }

                    if (activeCategoryId == "FAVORITES") channelAdapter.updateList(favoritesList)
                    updateCategoryMenu()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                db.watchDao().getAllProgress().collect { history ->
                    recentList = history
                        .filter { if (isM3uMode) it.streamType == "m3u" else it.streamType == "live" }
                        .take(10)
                        .map {
                            LiveStream(
                                num = "0",
                                name = it.name,
                                streamType = if (isM3uMode) "m3u" else "live",
                                streamId = it.streamId,
                                streamIcon = it.streamIcon,
                                epgChannelId = null,
                                added = null,
                                categoryId = "",
                                customSid = null,
                                tvArchive = null,
                                directSource = it.directSource,
                                tvArchiveDuration = null
                            )
                        }

                    if (activeCategoryId == "RECENT") channelAdapter.updateList(recentList)
                    updateCategoryMenu()
                }
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
                rvChannels.scrollToPosition(0)
                // Odak menüde kalsın; sağ oka basınca listeye geçsin
            },
            onNavigateRight = {
                hideCategoryPanel()
                focusFirstChannel(force = true)
            }
        )
        val categoryOrientation = if (!canHidePanel && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) LinearLayoutManager.HORIZONTAL else LinearLayoutManager.VERTICAL
        rvCategories.layoutManager = LinearLayoutManager(this, categoryOrientation, false)
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
                intent.putExtra("STREAM_TYPE", if (isM3uMode) "m3u" else "live")
                intent.putExtra("STREAM_NAME", channel.name)
                intent.putExtra("STREAM_ICON", channel.streamIcon)
                if (isM3uMode) {
                    intent.putExtra("DIRECT_URL", channel.directSource)
                }
                startActivity(intent)
            },
            onLongClick = { channel ->
                if (activeCategoryId == "FAVORITES") {
                    showRemoveFavoriteDialog(channel.streamId, channel.name, if (isM3uMode) "m3u" else "live")
                } else {
                    showHotkeyDialog(channel)
                }
            }
        )
        rvChannels.layoutManager = GridLayoutManager(this, gridSpanCount)
        rvChannels.adapter = channelAdapter
        rvChannels.setHasFixedSize(true)
        lockRecyclerAtBottom(rvChannels)
    }

    private fun showRemoveFavoriteDialog(streamId: Int, name: String, streamType: String) {
        AlertDialog.Builder(this)
            .setTitle("Favoriden Çıkar")
            .setMessage("$name favorilerden çıkarılsın mı?")
            .setPositiveButton("Evet") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(applicationContext)
                        .favoriteDao()
                        .deleteByStreamId(streamId, streamType)
                }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun resolveGridSpanCount(): Int {
        val screenWidthDp = resources.configuration.screenWidthDp
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return when {
            canHidePanel -> 4
            screenWidthDp >= 900 -> 4
            screenWidthDp >= 700 -> 3
            isPortrait -> 2
            else -> 3
        }
    }

    private fun adaptLayoutForPhone() {
        if (canHidePanel) return
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return
        val root = findViewById<LinearLayout>(R.id.rootLiveTvLayout)
        val categoryPanel = findViewById<View>(R.id.panelCategories)
        val contentPanel = findViewById<View>(R.id.contentLivePanel)
        root.orientation = LinearLayout.VERTICAL
        rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val horizontalMargin = (14 * resources.displayMetrics.density).toInt()
        (categoryPanel.layoutParams as LinearLayout.LayoutParams).apply {
            width = LinearLayout.LayoutParams.MATCH_PARENT
            height = (136 * resources.displayMetrics.density).toInt()
            weight = 0f
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
        }.also { categoryPanel.layoutParams = it }
        (contentPanel.layoutParams as LinearLayout.LayoutParams).apply {
            width = LinearLayout.LayoutParams.MATCH_PARENT
            height = 0
            weight = 1f
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
        }.also { contentPanel.layoutParams = it }
    }

    private fun getListByCategory(catId: String): List<LiveStream> {
        return when (catId) {
            "RECENT" -> recentList
            "FAVORITES" -> favoritesList
            "ALL" -> allChannelsList
            else -> channelsByCategory[catId].orEmpty()
        }
    }

    private fun loadData() {
        pbLoading.visibility = View.VISIBLE
        if (isM3uMode) {
            loadM3uData()
            return
        }
        val prefs = Prefs.user(this)
        val user = prefs.getString(KEY_USERNAME, "") ?: ""
        val pass = prefs.getString(KEY_PASSWORD, "") ?: ""
        val url = prefs.getString(KEY_SERVER_URL, "") ?: ""
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

    private fun loadM3uData() {
        val prefs = Prefs.user(this)
        val url = prefs.getString(KEY_M3U_URL, "") ?: ""
        if (url.isBlank()) {
            pbLoading.visibility = View.GONE
            toast("M3U URL bulunamadı. Lütfen yeniden ekleyin.")
            return
        }
        lifecycleScope.launch {
            try {
                val channels = M3uRepository.loadOrFetch(this@LiveTvActivity, url, forceRefresh = false)
                allChannelsList = channels
                channelsByCategory = buildChannelIndex(channels)
                apiCategories = buildM3uCategories(channels)
                updateCategoryMenu()
                channelAdapter.updateList(getListByCategory(activeCategoryId))
                pbLoading.visibility = View.GONE
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                toast("M3U yüklenemedi: ${e.localizedMessage}")
            }
        }
    }

    private fun buildM3uCategories(list: List<LiveStream>): List<LiveCategory> {
        val groups = list.mapNotNull { it.categoryId?.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        return groups.map { LiveCategory(it, it, "0") }
    }

    private suspend fun buildChannelIndex(list: List<LiveStream>): Map<String, List<LiveStream>> {
        return withContext(Dispatchers.Default) {
            list.groupBy { it.categoryId.orEmpty() }
        }
    }

    private fun fetchAllChannels(api: XtreamApi, user: String, pass: String) {
        api.getLiveStreams(u = user, p = pass).enqueue(object : Callback<List<LiveStream>> {
            override fun onResponse(call: Call<List<LiveStream>>, response: Response<List<LiveStream>>) {
                pbLoading.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    allChannelsList = response.body()!!
                    lifecycleScope.launch {
                        channelsByCategory = buildChannelIndex(allChannelsList)
                        channelAdapter.updateList(getListByCategory(activeCategoryId))
                    }
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
            channelAdapter.updateList(getListByCategory(activeCategoryId))
            toast("Favorilere 1'den başlayarak numara atandı")
        } else {
            toast("Favori kanal yok")
        }
    }

    private fun clearAssignments() {
        ChannelHotkeyManager.clearAll(this)
        channelAdapter.updateList(getListByCategory(activeCategoryId))
        toast("Tüm atamalar silindi")
    }
}
