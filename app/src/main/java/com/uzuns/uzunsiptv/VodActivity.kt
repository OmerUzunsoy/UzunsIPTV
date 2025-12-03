package com.uzuns.uzunsiptv

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import android.view.animation.AccelerateDecelerateInterpolator
import android.app.UiModeManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.uzuns.uzunsiptv.data.db.AppDatabase
import com.uzuns.uzunsiptv.data.db.FavoriteChannel
import com.uzuns.uzunsiptv.data.db.WatchProgress
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VodActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvMovies: RecyclerView
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
    private lateinit var vodAdapter: VodAdapter

    // LİSTELER
    private var allMoviesList = listOf<VodStream>() // API
    private var continueWatchingList = listOf<VodStream>() // DB (Yarım Kalanlar)
    private var favoritesList = listOf<VodStream>() // DB (Favoriler)
    private var apiCategories = listOf<LiveCategory>() // API Kategorileri
    private val continueHistoryMap = mutableMapOf<Int, WatchProgress>()
    private var favoriteEntities = listOf<FavoriteChannel>()
    private var movieMetaMap = mapOf<Int, VodStream>()

    private var activeCategoryId = "ALL" // Başlangıç
    private var hasFocusedMoviesOnce = false
    private val logTag = "VodActivity"
    private lateinit var panelCategories: View
    private var isMenuHidden = false
    private var restoreCategoryFocus = false
    private var canHidePanel = true
    private var currentDisplayedMovies = listOf<VodStream>()
    private var currentRandom: VodStream? = null
    private var remainingRerolls = 0
    private val randomHandler = Handler(Looper.getMainLooper())
    private var previewQueue: List<VodStream> = emptyList()
    private var previewIndex = 0
    private val shuffleDelays = listOf(80L, 140L, 220L, 320L, 450L, 600L)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vod)

        rvCategories = findViewById(R.id.rvCategories)
        rvMovies = findViewById(R.id.rvMovies)
        pbLoading = findViewById(R.id.pbLoading)
        etSearch = findViewById(R.id.etSearch)
        panelCategories = findViewById(R.id.panelCategories)
        btnRandomPick = findViewById(R.id.btnRandomPick)
        randomOverlay = findViewById(R.id.randomOverlay)
        randomCard = findViewById(R.id.randomCard)
        randomPoster = findViewById(R.id.randomPoster)
        randomTitle = findViewById(R.id.randomTitle)
        randomAgain = findViewById(R.id.btnRandomAgain)
        randomGo = findViewById(R.id.btnRandomGo)

        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val mode = uiModeManager.currentModeType
        canHidePanel = mode == Configuration.UI_MODE_TYPE_TELEVISION

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setupRecyclerViews()
        setupSearch()
        setupRandomPick()

        loadLocalData() // Veritabanını dinle
        loadApiData()   // İnternetten çek
        rvCategories.requestFocus()
    }

    private fun loadLocalData() {
        val db = AppDatabase.getDatabase(this)

        // 1. İZLEMEYE DEVAM ET
        lifecycleScope.launch {
            db.watchDao().getAllProgress().collect { progressList ->
                continueHistoryMap.clear()
                progressList.filter { it.streamType == "movie" }.forEach { continueHistoryMap[it.streamId] = it }
                rebuildContinueList()
            }
        }

        // 2. FAVORİLER
        lifecycleScope.launch {
            db.favoriteDao().getAllFavorites().collect { favList ->
                favoriteEntities = favList
                rebuildFavoriteList()
            }
        }
    }

    private fun updateCategoryMenu() {
        restoreCategoryFocus = rvCategories.hasFocus()
        val finalCategories = mutableListOf<LiveCategory>()
        finalCategories.add(LiveCategory("ALL", "TÜM FİLMLER", "0"))
        finalCategories.add(LiveCategory("CONTINUE", "İZLEMEYE DEVAM ET \uD83D\uDC40", "0"))
        finalCategories.add(LiveCategory("RECENT", "SON EKLENENLER \uD83D\uDD25", "0"))
        finalCategories.add(LiveCategory("FAVORITES", "FAVORİLERİM ⭐", "0"))
        finalCategories.addAll(apiCategories)
        categoryAdapter.updateList(finalCategories)
        categoryAdapter.setSelectedCategory(activeCategoryId)
        scrollCategoryToSelected()
        if (restoreCategoryFocus) focusSelectedCategory()
    }

    private fun loadApiData() {
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

        api.getVodCategories(user, pass).enqueue(object : Callback<List<VodCategory>> {
            override fun onResponse(call: Call<List<VodCategory>>, response: Response<List<VodCategory>>) {
                if (response.isSuccessful && response.body() != null) {
                    apiCategories = response.body()!!.map { LiveCategory(it.categoryId, it.categoryName, it.parentId) }
                    updateCategoryMenu()
                    fetchAllMovies(api, user, pass)
                } else {
                    pbLoading.visibility = View.GONE
                    toast("Film kategorileri alınamadı (HTTP ${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<VodCategory>>, t: Throwable) {
                pbLoading.visibility = View.GONE
                toast("Film kategorisi isteği başarısız: ${t.localizedMessage}")
            }
        })
    }

    private fun fetchAllMovies(api: XtreamApi, user: String, pass: String) {
        api.getVodStreams(user, pass).enqueue(object : Callback<List<VodStream>> {
            override fun onResponse(call: Call<List<VodStream>>, response: Response<List<VodStream>>) {
                pbLoading.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    allMoviesList = response.body()!!
                    movieMetaMap = allMoviesList.associateBy { it.streamId }
                    rebuildContinueList()
                    rebuildFavoriteList()
                    updateDisplayedList(getListByCategory(activeCategoryId))
                } else {
                    toast("Film listesi alınamadı (HTTP ${response.code()})")
                }
            }
            override fun onFailure(call: Call<List<VodStream>>, t: Throwable) {
                pbLoading.visibility = View.GONE
                toast("Film isteği başarısız: ${t.localizedMessage}")
            }
        })
    }

    private fun getListByCategory(catId: String): List<VodStream> {
        return when (catId) {
            "CONTINUE" -> continueWatchingList
            "FAVORITES" -> favoritesList
            "RECENT" -> allMoviesList.sortedByDescending { it.streamId }.take(40)
            "ALL" -> allMoviesList
            else -> allMoviesList.filter { it.categoryId == catId }
        }
    }

    private fun updateDisplayedList(list: List<VodStream>) {
        currentDisplayedMovies = list
        vodAdapter.updateList(list)
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter(
            onClick = { selectedCategory ->
                activeCategoryId = selectedCategory.categoryId
                etSearch.text.clear()
                updateDisplayedList(getListByCategory(activeCategoryId))
                // Odak menüde kalsın; sağ oka basınca listeye geçsin
            },
            onNavigateRight = {
                hideCategoryPanel()
                focusFirstMovie(force = true)
            }
        )
        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = categoryAdapter

        vodAdapter = VodAdapter(
            spanCount = 5,
            onClick = { movie ->
                if (activeCategoryId == "CONTINUE") {
                    playMovieFromHistory(movie)
                } else {
                    val intent = Intent(this, VodDetailsActivity::class.java)
                    intent.putExtra("STREAM_ID", movie.streamId)
                    intent.putExtra("NAME", movie.name)
                    intent.putExtra("ICON", movie.streamIcon)
                    intent.putExtra("RATING", movie.rating)
                    intent.putExtra("EXTENSION", movie.containerExtension)
                    startActivity(intent)
                }
            },
            onLongClick = { movie ->
                if (activeCategoryId == "CONTINUE") showDeleteDialog(movie)
            }
        )
        rvMovies.layoutManager = GridLayoutManager(this, 5)
        rvMovies.adapter = vodAdapter
        lockRecyclerAtBottom(rvMovies)
    }

    private fun showDeleteDialog(movie: VodStream) {
        AlertDialog.Builder(this)
            .setTitle("Listeden Kaldır")
            .setMessage("${movie.name} kaldırılsın mı?")
            .setPositiveButton("Evet") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(applicationContext).watchDao().deleteProgress(movie.streamId)
                }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                if (query.isEmpty()) updateDisplayedList(getListByCategory(activeCategoryId))
                else updateDisplayedList(allMoviesList.filter { it.name.lowercase().contains(query) })
            }
        })
    }

    private fun setupRandomPick() {
        btnRandomPick.setOnClickListener { showRandomOverlay() }
        randomOverlay.setOnClickListener { hideRandomOverlay() }
        randomAgain.setOnClickListener { rerollRandom() }
        randomGo.setOnClickListener {
            currentRandom?.let { openMovieDetails(it) }
            hideRandomOverlay()
        }
        randomOverlay.isFocusable = true
        randomOverlay.isClickable = true
        randomOverlay.visibility = View.GONE
        randomPoster.setOnClickListener { }
        randomTitle.setOnClickListener { }
        randomCard.setOnClickListener { }
        randomGo.isFocusable = true
        randomAgain.isFocusable = true
    }

    private fun playMovieFromHistory(movie: VodStream) {
        val progress = continueHistoryMap[movie.streamId]
        if (progress == null) {
            Toast.makeText(this, "Kayıt bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("STREAM_ID", movie.streamId)
        intent.putExtra("STREAM_TYPE", "movie")
        intent.putExtra("STREAM_NAME", movie.name)
        intent.putExtra("STREAM_ICON", movie.streamIcon)
        intent.putExtra("CONTAINER_EXTENSION", progress?.containerExtension ?: movie.containerExtension ?: "mp4")
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (randomOverlay.visibility == View.VISIBLE) {
            hideRandomOverlay()
            return
        }
        if (rvMovies.hasFocus() && handleBackFromList(rvMovies)) return
        super.onBackPressed()
    }

    private fun focusSelectedCategory() {
        scrollCategoryToSelected()
        rvCategories.post {
            val index = categoryAdapter.getSelectedIndex()
            rvCategories.findViewHolderForAdapterPosition(index)?.itemView?.requestFocus()
        }
    }

    private fun rebuildContinueList() {
        continueWatchingList = continueHistoryMap.values
            .sortedByDescending { it.timestamp }
            .map { progress ->
                val meta = movieMetaMap[progress.streamId]
                VodStream(
                    num = 0,
                    name = progress.name,
                    streamType = "movie",
                    streamId = progress.streamId,
                    streamIcon = progress.streamIcon ?: meta?.streamIcon,
                    rating = meta?.rating,
                    rating5 = meta?.rating5,
                    added = progress.timestamp.toString(),
                    categoryId = "CONTINUE",
                    containerExtension = progress.containerExtension ?: meta?.containerExtension ?: "mp4"
                )
            }
        if (activeCategoryId == "CONTINUE") updateDisplayedList(continueWatchingList)
        updateCategoryMenu()
    }

    private fun rebuildFavoriteList() {
        favoritesList = favoriteEntities
            .filter { it.streamType == "movie" }
            .map { fav ->
                val meta = movieMetaMap[fav.streamId]
                VodStream(
                    num = 0,
                    name = fav.name,
                    streamType = "movie",
                    streamId = fav.streamId,
                    streamIcon = meta?.streamIcon ?: fav.streamIcon,
                    rating = meta?.rating,
                    rating5 = meta?.rating5,
                    added = meta?.added,
                    categoryId = "FAVORITES",
                    containerExtension = meta?.containerExtension ?: "mp4"
                )
            }
        if (activeCategoryId == "FAVORITES") updateDisplayedList(favoritesList)
        updateCategoryMenu()
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

    private fun scrollCategoryToSelected() {
        val index = categoryAdapter.getSelectedIndex()
        rvCategories.layoutManager?.scrollToPosition(index)
    }

    private fun handleBackFromList(recyclerView: RecyclerView): Boolean {
        showCategoryPanel()
        focusSelectedCategory()
        return true
    }

    private fun focusFirstMovie(force: Boolean = false) {
        if (!force && hasFocusedMoviesOnce) return
        val count = rvMovies.adapter?.itemCount ?: 0
        if (count == 0) {
            rvMovies.requestFocus()
            return
        }
        hasFocusedMoviesOnce = true
        rvMovies.layoutManager?.scrollToPosition(0)
        rvMovies.post {
            rvMovies.requestFocus()
            rvMovies.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
        }
    }

    private fun showRandomOverlay() {
        if (currentDisplayedMovies.isEmpty()) {
            toast("Bu listede film yok.")
            return
        }
        remainingRerolls = 2
        randomOverlay.visibility = View.VISIBLE
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
        val size = if (currentDisplayedMovies.size >= 6) 6 else currentDisplayedMovies.size
        previewQueue = currentDisplayedMovies.shuffled().take(size).ifEmpty { currentDisplayedMovies }
        previewIndex = 0
        randomAgain.isEnabled = false
        randomGo.isEnabled = false
        randomHandler.removeCallbacksAndMessages(null)
        randomHandler.postDelayed(shuffleRunnable, shuffleDelays.first())
    }

    private fun rerollRandom() {
        if (remainingRerolls <= 0 || currentDisplayedMovies.isEmpty()) return
        remainingRerolls--
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

    private fun pickRandomFinal(): VodStream {
        val finalPick = currentDisplayedMovies.random()
        currentRandom = finalPick
        return finalPick
    }

    private fun finishShuffleWith(pick: VodStream) {
        showRandomCard(pick, dimOnly = false)
        randomAgain.isEnabled = remainingRerolls > 0
        randomGo.isEnabled = true
        if (randomAgain is android.widget.Button) {
            (randomAgain as android.widget.Button).text =
                if (remainingRerolls > 0) "Tekrar (${remainingRerolls})" else "Limit"
        }
        randomCard.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                randomCard.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            .start()
        randomGo.requestFocus()
    }

    private fun showRandomCard(pick: VodStream, dimOnly: Boolean) {
        randomTitle.text = pick.name
        Glide.with(this)
            .load(pick.streamIcon)
            .placeholder(R.drawable.bg_placeholder)
            .error(R.drawable.bg_placeholder)
            .into(randomPoster)
        if (!dimOnly) {
            randomCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
        }
    }

    private fun openMovieDetails(movie: VodStream) {
        val intent = Intent(this, VodDetailsActivity::class.java)
        intent.putExtra("STREAM_ID", movie.streamId)
        intent.putExtra("NAME", movie.name)
        intent.putExtra("ICON", movie.streamIcon)
        intent.putExtra("RATING", movie.rating)
        intent.putExtra("EXTENSION", movie.containerExtension)
        startActivity(intent)
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
}
