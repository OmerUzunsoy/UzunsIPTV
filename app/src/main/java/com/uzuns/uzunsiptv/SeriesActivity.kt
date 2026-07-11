package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
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
import com.bumptech.glide.Glide
import com.uzuns.uzunsiptv.data.db.AppDatabase
import com.uzuns.uzunsiptv.data.db.WatchProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SeriesActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvSeries: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var etSearch: EditText
    private lateinit var btnRandomPick: View
    private lateinit var randomOverlay: View
    private lateinit var randomCard: View
    private lateinit var randomPoster: ImageView
    private lateinit var randomTitle: TextView
    private lateinit var randomAgain: View
    private lateinit var randomGo: View

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var seriesAdapter: SeriesAdapter

    private var allSeriesList = listOf<SeriesStream>()
    private var continueList = listOf<SeriesStream>()
    private var favoritesList = listOf<SeriesStream>()
    private var apiCategories = listOf<LiveCategory>()
    private val seriesHistoryMap = mutableMapOf<Int, WatchProgress>()
    private val continueBySeries = mutableMapOf<Int, WatchProgress>()
    private var seriesByCategory = emptyMap<String, List<SeriesStream>>()

    private var activeCategoryId = "ALL"
    private var hasFocusedSeriesOnce = false
    private val logTag = "SeriesActivity"
    private lateinit var panelCategories: View
    private var isMenuHidden = false
    private var restoreCategoryFocus = false
    private var canHidePanel = true
    private var currentDisplayedSeries = listOf<SeriesStream>()
    private var currentRandom: SeriesStream? = null
    private var remainingRerolls = 0
    private val randomHandler = Handler(Looper.getMainLooper())
    private var previewQueue: List<SeriesStream> = emptyList()
    private var previewIndex = 0
    private val shuffleDelays = listOf(80L, 140L, 220L, 320L, 450L, 600L)
    private var searchJob: Job? = null
    private var gridSpanCount = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series)

        rvCategories = findViewById(R.id.rvCategories)
        rvSeries = findViewById(R.id.rvSeries)
        pbLoading = findViewById(R.id.pbLoading)
        etSearch = findViewById(R.id.etSearch)
        panelCategories = findViewById(R.id.panelCategories)
        btnRandomPick = findViewById(R.id.btnRandomPickSeries)
        randomOverlay = findViewById(R.id.randomOverlaySeries)
        randomCard = findViewById(R.id.randomCardSeries)
        randomPoster = findViewById(R.id.randomPosterSeries)
        randomTitle = findViewById(R.id.randomTitleSeries)
        randomAgain = findViewById(R.id.btnRandomAgainSeries)
        randomGo = findViewById(R.id.btnRandomGoSeries)

        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val mode = uiModeManager.currentModeType
        canHidePanel = mode == Configuration.UI_MODE_TYPE_TELEVISION
        gridSpanCount = resolveGridSpanCount()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        adaptLayoutForPhone()
        setupRecyclerViews()
        setupSearch()
        setupRandomPick()

        if (AccountsStore.getActiveType(this) == TYPE_M3U) {
            toast("M3U hesaplarında dizi arşivi desteklenmiyor.")
            finish()
            return
        }
        loadLocalData()
        loadData()
        rvCategories.requestFocus()
    }

    private fun loadLocalData() {
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                db.watchDao().getAllProgress().collect { list ->
                seriesHistoryMap.clear()
                continueBySeries.clear()
                val history = list.filter { it.streamType == "series" }.sortedByDescending { it.timestamp }
                history.forEach { seriesHistoryMap[it.streamId] = it }
                history.forEach { progress ->
                    val seriesId = progress.parentSeriesId ?: progress.streamId
                    val current = continueBySeries[seriesId]
                    if (current == null || shouldReplaceSeriesProgress(current, progress)) {
                        continueBySeries[seriesId] = progress
                    }
                }
                rebuildContinueList()
                if (activeCategoryId == "CONTINUE") updateDisplayedList(continueList)
                updateCategoryMenu()
            }
        }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                db.favoriteDao().getAllFavorites().collect { list ->
                    favoritesList = list
                        .filter { it.streamType == "series" }
                        .mapNotNull { favorite ->
                            val series = allSeriesList.firstOrNull { it.seriesId == favorite.streamId }
                            series ?: SeriesStream(0, favorite.name, favorite.streamId, favorite.streamIcon, null, null, null, null, null, "FAVORITES", null)
                                .takeIf { favorite.streamId != 0 }
                        }
        if (activeCategoryId == "FAVORITES") updateDisplayedList(favoritesList)
        updateCategoryMenu()
    }
            }
        }
    }

    private fun updateCategoryMenu() {
        restoreCategoryFocus = rvCategories.hasFocus()
        val finalCats = mutableListOf<LiveCategory>()
        finalCats.add(LiveCategory("ALL", "TÜM DİZİLER", "0"))
        finalCats.add(LiveCategory("CONTINUE", "İZLEMEYE DEVAM ET \uD83D\uDC40", "0"))
        finalCats.add(LiveCategory("RECENT", "SON EKLENENLER \uD83D\uDD25", "0"))
        finalCats.add(LiveCategory("FAVORITES", "FAVORİLERİM ⭐", "0"))
        finalCats.addAll(apiCategories)

        // DÜZELTME BURADA
        categoryAdapter.updateList(finalCats)
        categoryAdapter.setSelectedCategory(activeCategoryId)
        scrollCategoryToSelected()
        if (restoreCategoryFocus) focusSelectedCategory()
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter(
            onClick = { selectedCategory ->
                activeCategoryId = selectedCategory.categoryId
                etSearch.text.clear()
                updateDisplayedList(getListByCategory(activeCategoryId))
                rvSeries.scrollToPosition(0)
                // Odak menüde kalsın; sağ oka basınca listeye geçsin
            },
            onNavigateRight = {
                hideCategoryPanel()
                focusFirstSeries(force = true)
            }
        )
        val categoryOrientation = if (!canHidePanel && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) LinearLayoutManager.HORIZONTAL else LinearLayoutManager.VERTICAL
        rvCategories.layoutManager = LinearLayoutManager(this, categoryOrientation, false)
        rvCategories.adapter = categoryAdapter

        seriesAdapter = SeriesAdapter(
            spanCount = gridSpanCount,
            onClick = { series ->
                if (activeCategoryId == "CONTINUE") {
                    playContinueSeries(series.seriesId)
                } else {
                    val intent = Intent(this, SeriesDetailsActivity::class.java)
                    intent.putExtra("SERIES_ID", series.seriesId)
                    intent.putExtra("NAME", series.name)
                    intent.putExtra("COVER", series.cover)
                    intent.putExtra("RATING", series.rating)
                    startActivity(intent)
                }
            },
            onLongClick = { series ->
                when (activeCategoryId) {
                    "FAVORITES" -> showRemoveFavoriteDialog(series.seriesId, series.name)
                    "CONTINUE" -> continueBySeries[series.seriesId]?.let { progress ->
                        showDeleteContinueDialog(progress.streamId, series.name)
                    }
                }
            }
        )
        rvSeries.layoutManager = GridLayoutManager(this, gridSpanCount)
        rvSeries.adapter = seriesAdapter
        rvSeries.setHasFixedSize(true)
        lockRecyclerAtBottom(rvSeries)
    }

    private fun showRemoveFavoriteDialog(streamId: Int, name: String) {
        AlertDialog.Builder(this)
            .setTitle("Favoriden Çıkar")
            .setMessage("$name favorilerden çıkarılsın mı?")
            .setPositiveButton("Evet") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(applicationContext)
                        .favoriteDao()
                        .deleteByStreamId(streamId, "series")
                }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun showDeleteContinueDialog(streamId: Int, name: String) {
        AlertDialog.Builder(this)
            .setTitle("Listeden Kaldır")
            .setMessage("$name izlemeye devam et listesinden kaldırılsın mı?")
            .setPositiveButton("Evet") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(applicationContext)
                        .watchDao()
                        .deleteProgress(streamId)
                }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun getListByCategory(catId: String): List<SeriesStream> {
        return when (catId) {
            "CONTINUE" -> continueList
            "FAVORITES" -> favoritesList
            "RECENT" -> allSeriesList.sortedByDescending { it.seriesId }.take(40)
            "ALL" -> allSeriesList
            else -> seriesByCategory[catId].orEmpty()
        }
    }

    private fun updateDisplayedList(list: List<SeriesStream>) {
        currentDisplayedSeries = list
        seriesAdapter.updateList(list)
    }

    private fun loadData() {
        pbLoading.visibility = View.VISIBLE
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

        api.getSeriesCategories(u = user, p = pass).enqueue(object : Callback<List<SeriesCategory>> {
            override fun onResponse(call: Call<List<SeriesCategory>>, response: Response<List<SeriesCategory>>) {
                if (response.isSuccessful && response.body() != null) {
                    apiCategories = response.body()!!.map { LiveCategory(it.categoryId, it.categoryName, it.parentId) }
                    updateCategoryMenu()
                    fetchAllSeries(api, user, pass)
                } else {
                    pbLoading.visibility = View.GONE
                    toast("Dizi kategorileri alınamadı (HTTP ${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<SeriesCategory>>, t: Throwable) {
                pbLoading.visibility = View.GONE
                toast("Dizi kategorisi isteği başarısız: ${t.localizedMessage}")
            }
        })
    }

    private fun fetchAllSeries(api: XtreamApi, user: String, pass: String) {
        api.getSeries(u = user, p = pass).enqueue(object : Callback<List<SeriesStream>> {
            override fun onResponse(call: Call<List<SeriesStream>>, response: Response<List<SeriesStream>>) {
                pbLoading.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    allSeriesList = response.body()!!
                    seriesByCategory = allSeriesList.groupBy { it.categoryId }
                    rebuildContinueList()
                    favoritesList = favoritesList.mapNotNull { favorite ->
                        allSeriesList.firstOrNull { it.seriesId == favorite.seriesId } ?: favorite
                    }
                    updateDisplayedList(getListByCategory(activeCategoryId))
                } else {
                    toast("Dizi listesi alınamadı (HTTP ${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<SeriesStream>>, t: Throwable) {
                pbLoading.visibility = View.GONE
                toast("Dizi isteği başarısız: ${t.localizedMessage}")
            }
        })
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(250)
                    val filtered = withContext(Dispatchers.Default) {
                        if (query.isEmpty()) getListByCategory(activeCategoryId)
                        else allSeriesList.filter { it.name.lowercase().contains(query) }
                    }
                    updateDisplayedList(filtered)
                }
            }
        })
    }

    private fun playSeriesFromHistory(episodeId: Int) {
        val progress = seriesHistoryMap[episodeId]
        if (progress == null) {
            Toast.makeText(this, "Kayıt bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("STREAM_ID", progress.streamId)
        intent.putExtra("STREAM_TYPE", "series")
        intent.putExtra("STREAM_NAME", progress.name)
        intent.putExtra("STREAM_ICON", progress.streamIcon)
        intent.putExtra("CONTAINER_EXTENSION", progress.containerExtension ?: "mp4")
        progress.parentSeriesId?.let { intent.putExtra("SERIES_ID", it) }
        startActivity(intent)
    }

    private fun rebuildContinueList() {
        continueList = continueBySeries.entries.map { entry ->
            val seriesId = entry.key
            val progress = entry.value
            val series = allSeriesList.firstOrNull { it.seriesId == seriesId }
            SeriesStream(
                num = 0,
                name = series?.name ?: progress.name,
                seriesId = seriesId,
                cover = series?.cover ?: progress.streamIcon,
                plot = series?.plot,
                cast = series?.cast,
                director = series?.director,
                genre = series?.genre,
                rating = series?.rating,
                categoryId = "CONTINUE",
                lastModified = series?.lastModified
            )
        }.sortedByDescending { continueBySeries[it.seriesId]?.timestamp ?: 0L }
    }

    private fun playContinueSeries(seriesId: Int) {
        val progress = continueBySeries[seriesId] ?: return
        val parentId = progress.parentSeriesId ?: seriesId
        val series = allSeriesList.firstOrNull { it.seriesId == parentId }
        startActivity(
            Intent(this, SeriesDetailsActivity::class.java).apply {
                putExtra("SERIES_ID", parentId)
                putExtra("NAME", series?.name ?: progress.name.substringAfter(". ", progress.name))
                putExtra("COVER", series?.cover ?: progress.streamIcon)
                putExtra("RATING", series?.rating)
                putExtra("AUTO_PLAY_EPISODE_ID", progress.streamId.toString())
            }
        )
    }

    private fun shouldReplaceSeriesProgress(current: WatchProgress, candidate: WatchProgress): Boolean {
        if (candidate.timestamp != current.timestamp) return candidate.timestamp > current.timestamp
        return extractEpisodeNumber(candidate.name) >= extractEpisodeNumber(current.name)
    }

    private fun extractEpisodeNumber(name: String): Int {
        return name.substringBefore('.').trim().toIntOrNull() ?: 0
    }

    override fun onBackPressed() {
        if (randomOverlay.visibility == View.VISIBLE) {
            hideRandomOverlay()
            return
        }
        if (rvSeries.hasFocus() && handleBackFromList(rvSeries)) return
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
                        if (position >= total - lastRowCount) {
                            return@setOnKeyListener true
                        }
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

    private fun focusFirstSeries(force: Boolean = false) {
        if (!force && hasFocusedSeriesOnce) return
        val count = rvSeries.adapter?.itemCount ?: 0
        if (count == 0) {
            rvSeries.requestFocus()
            return
        }
        hasFocusedSeriesOnce = true
        rvSeries.layoutManager?.scrollToPosition(0)
        rvSeries.post {
            rvSeries.requestFocus()
            rvSeries.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
        }
    }

    private fun setupRandomPick() {
        btnRandomPick.setOnClickListener { showRandomOverlay() }
        randomOverlay.setOnClickListener { hideRandomOverlay() }
        randomAgain.setOnClickListener { rerollRandom() }
        randomGo.setOnClickListener {
            currentRandom?.let { openSeriesDetails(it) }
            hideRandomOverlay()
        }
        randomOverlay.isFocusable = true
        randomOverlay.isClickable = true
        randomOverlay.visibility = View.GONE
        randomCard.setOnClickListener { }
        randomPoster.setOnClickListener { }
        randomTitle.setOnClickListener { }
        randomGo.isFocusable = true
        randomAgain.isFocusable = true
    }

    private fun resolveGridSpanCount(): Int {
        val screenWidthDp = resources.configuration.screenWidthDp
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return when {
            canHidePanel -> 5
            screenWidthDp >= 900 -> 5
            screenWidthDp >= 700 -> 4
            isPortrait -> 2
            else -> 3
        }
    }

    private fun adaptLayoutForPhone() {
        if (canHidePanel) return
        if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) return
        val root = findViewById<LinearLayout>(R.id.rootSeriesLayout)
        val categoryPanel = findViewById<View>(R.id.panelCategories)
        val contentPanel = findViewById<View>(R.id.contentSeriesPanel)
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

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        android.util.Log.e(logTag, msg)
    }

    private fun showRandomOverlay() {
        if (currentDisplayedSeries.isEmpty()) {
            toast("Bu listede dizi yok.")
            return
        }
        randomOverlay.visibility = View.VISIBLE
        randomOverlay.translationY = 0f
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            randomCard.translationY = -(120 * resources.displayMetrics.density)
        } else {
            randomCard.translationY = 0f
        }
        startPreviewShuffle()
        randomCard.alpha = 0f
        randomCard.scaleX = 0.9f
        randomCard.scaleY = 0.9f
        randomCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { randomGo.requestFocus() }
            .start()
    }

    private fun hideRandomOverlay() {
        randomOverlay.visibility = View.GONE
        currentRandom = null
        randomHandler.removeCallbacksAndMessages(null)
    }

    private fun startPreviewShuffle() {
        val size = if (currentDisplayedSeries.size >= 6) 6 else currentDisplayedSeries.size
        previewQueue = currentDisplayedSeries.shuffled().take(size).ifEmpty { currentDisplayedSeries }
        previewIndex = 0
        randomAgain.isEnabled = false
        randomGo.isEnabled = false
        randomHandler.removeCallbacksAndMessages(null)
        randomHandler.postDelayed(shuffleRunnable, shuffleDelays.first())
    }

    private fun rerollRandom() {
        if (currentDisplayedSeries.isEmpty()) return
        startPreviewShuffle()
    }

    private val shuffleRunnable = object : Runnable {
        override fun run() {
            if (previewQueue.isEmpty()) {
                finishShuffleWith(pickRandomFinal())
                return
            }
            val tempPick = previewQueue[previewIndex % previewQueue.size]
            showRandomCard(tempPick, dimOnly = true)
            previewIndex++
            if (previewIndex < shuffleDelays.size) {
                randomHandler.postDelayed(this, shuffleDelays[previewIndex])
            } else {
                finishShuffleWith(pickRandomFinal())
            }
        }
    }

    private fun pickRandomFinal(): SeriesStream {
        val finalPick = currentDisplayedSeries.random()
        currentRandom = finalPick
        return finalPick
    }

    private fun finishShuffleWith(pick: SeriesStream) {
        showRandomCard(pick, dimOnly = false)
        randomAgain.isEnabled = true
        randomGo.isEnabled = true
        if (randomAgain is android.widget.Button) {
            (randomAgain as android.widget.Button).text = "Başka Seç"
        }
        randomCard.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                randomCard.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            .start()
        randomGo.requestFocus()
    }

    private fun showRandomCard(pick: SeriesStream, dimOnly: Boolean) {
        randomTitle.text = pick.name
        Glide.with(this)
            .load(pick.cover)
            .placeholder(R.drawable.bg_placeholder)
            .error(R.drawable.bg_placeholder)
            .into(randomPoster)
        if (!dimOnly) {
            randomCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
        }
    }

    private fun openSeriesDetails(series: SeriesStream) {
        val intent = Intent(this, SeriesDetailsActivity::class.java)
        intent.putExtra("SERIES_ID", series.seriesId)
        intent.putExtra("NAME", series.name)
        intent.putExtra("COVER", series.cover)
        intent.putExtra("RATING", series.rating)
        startActivity(intent)
    }

    override fun onDestroy() {
        randomHandler.removeCallbacksAndMessages(null)
        searchJob?.cancel()
        super.onDestroy()
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
}
