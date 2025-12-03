package com.uzuns.uzunsiptv

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.uzuns.uzunsiptv.EpisodeManager
import com.uzuns.uzunsiptv.ChannelHotkeyManager
import com.uzuns.uzunsiptv.data.db.AppDatabase
import com.uzuns.uzunsiptv.data.db.FavoriteChannel
import com.uzuns.uzunsiptv.data.db.WatchProgress
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class PlayerActivity : AppCompatActivity() {

    private var playerView: StyledPlayerView? = null
    private var pbLoading: ProgressBar? = null
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    private var tvChannelName: TextView? = null
    private var tvSystemTime: TextView? = null
    private var tvResolution: TextView? = null
    private var btnAspectRatio: LinearLayout? = null
    private var tvAspectRatioText: TextView? = null

    private var btnPlayPause: ImageButton? = null
    private var btnNext: ImageButton? = null
    private var btnPrev: ImageButton? = null
    private var btnForward: Button? = null
    private var btnRewind: Button? = null

    private var btnSpeed: ImageView? = null
    private var btnAudio: ImageView? = null
    private var btnSubtitle: ImageView? = null
    private var btnFavorite: ImageView? = null

    private var timeBar: DefaultTimeBar? = null
    private var tvLiveBadge: TextView? = null
    private var tvExoPosition: TextView? = null
    private var tvExoDuration: TextView? = null
    private var tvDivider: TextView? = null
    private var tvSeekPreview: TextView? = null
    private var tvNumberOverlay: TextView? = null
    private var overlayContainer: View? = null
    private var overlayCard: View? = null
    private var rvOverlayCategories: RecyclerView? = null
    private var overlaySearch: android.widget.EditText? = null
    private var rvOverlay: RecyclerView? = null
    private var overlayAdapter: OverlayChannelAdapter? = null
    private var overlayCatAdapter: OverlayCategoryAdapter? = null
    private var overlayChannels: MutableList<LiveStream> = mutableListOf()
    private var filteredOverlay: MutableList<LiveStream> = mutableListOf()
    private var overlayCategories: List<LiveCategory> = listOf(
        LiveCategory("ALL", "TÜM KANALLAR", "0")
    )
    private var overlayActiveCat = "ALL"
    private var selectedOverlayIndex = -1

    private var currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var currentStreamId: Int = 0
    private var currentStreamName: String = ""
    private var currentStreamIcon: String? = null
    private var streamType: String = "live"
    private var currentExtension: String = "ts"
    private var currentSeriesId: Int? = null
    private var isFavorite: Boolean = false
    private var autoPlayNext: Boolean = true

    private var pendingSeekMs: Long = 0L
    private val seekHandler = Handler(Looper.getMainLooper())
    private val seekRunnable = Runnable { applyPendingSeek() }
    private val numberHandler = Handler(Looper.getMainLooper())
    private val numberRunnable = Runnable { applyNumberHotkey() }
    private var pendingNumberInput: String = ""

    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvSystemTime?.text = sdf.format(Date())
            timeHandler.postDelayed(this, 1000 * 60)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        pbLoading = findViewById(R.id.pbLoading)
        tvNumberOverlay = findViewById(R.id.tvNumberOverlay)
        playerView?.controllerShowTimeoutMs = 2000
        playerView?.setControllerHideOnTouch(true)

        tvChannelName = playerView?.findViewById(R.id.tvChannelNamePlayer)
        tvSystemTime = playerView?.findViewById(R.id.tvSystemTime)
        tvResolution = playerView?.findViewById(R.id.tvResolution)
        tvResolution?.text = "—"
        btnAspectRatio = playerView?.findViewById(R.id.btnAspectRatio)
        tvAspectRatioText = playerView?.findViewById(R.id.tvAspectRatio)

        btnPlayPause = playerView?.findViewById(R.id.btnPlayPause)
        btnNext = playerView?.findViewById(R.id.btnNextChannel)
        btnPrev = playerView?.findViewById(R.id.btnPrevChannel)
        btnForward = playerView?.findViewById(R.id.btnForward)
        btnRewind = playerView?.findViewById(R.id.btnRewind)

        btnSpeed = playerView?.findViewById(R.id.btnSpeed)
        btnAudio = playerView?.findViewById(R.id.btnAudio)
        btnSubtitle = playerView?.findViewById(R.id.btnSubtitle)
        btnFavorite = playerView?.findViewById(R.id.btnFavorite)

        timeBar = playerView?.findViewById(R.id.exo_progress)
        tvLiveBadge = playerView?.findViewById(R.id.tvLiveBadge)
        tvExoPosition = playerView?.findViewById(R.id.exo_position)
        tvExoDuration = playerView?.findViewById(R.id.exo_duration)
        tvDivider = playerView?.findViewById(R.id.tvDivider)
        tvSeekPreview = findViewById(R.id.tvSeekOverlay)
        overlayContainer = findViewById(R.id.channelOverlay)
        overlayCard = findViewById(R.id.channelOverlayCard)
        overlaySearch = findViewById(R.id.etOverlaySearch)
        rvOverlayCategories = findViewById(R.id.rvOverlayCategories)
        rvOverlay = findViewById(R.id.rvOverlayChannels)

        currentStreamId = intent.getIntExtra("STREAM_ID", 0)
        currentStreamName = intent.getStringExtra("STREAM_NAME") ?: "Yayın"
        currentStreamIcon = intent.getStringExtra("STREAM_ICON")
        streamType = intent.getStringExtra("STREAM_TYPE") ?: "live"
        currentExtension = intent.getStringExtra("CONTAINER_EXTENSION") ?: "ts"
        currentSeriesId = intent.getIntExtra("SERIES_ID", 0).takeIf { it != 0 }

        tvChannelName?.text = currentStreamName
        timeHandler.post(timeRunnable)

        val settings = getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        autoPlayNext = settings.getBoolean("AUTO_PLAY", true)

        configureUiForType(streamType)
        setupButtons()
        checkFavoriteStatus()
        setupOverlay()
        btnPlayPause?.post { btnPlayPause?.requestFocus() }

        if (currentStreamId != 0) {
            initializePlayer(currentStreamId, currentExtension)
        }

        if (isVod()) {
            tvAspectRatioText?.text = "DOLDUR"
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            playerView?.setResizeMode(currentResizeMode)
        } else {
            tvAspectRatioText?.text = "SIĞDIR"
            playerView?.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
        }
    }

    private fun saveOrDeleteProgress() {
        if (exoPlayer == null) return

        val currentPos = exoPlayer!!.currentPosition
        val duration = exoPlayer!!.duration

        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(applicationContext).watchDao()

            if (isVod()) {
                if (duration > 0) {
                    val percentage = (currentPos.toDouble() / duration.toDouble()) * 100

                    if (percentage >= 90.0) {
                        dao.deleteProgress(currentStreamId)
                    } else if (currentPos > 30000) {
                        val progress = WatchProgress(
                            streamId = currentStreamId,
                            name = currentStreamName,
                            streamType = streamType,
                            streamIcon = currentStreamIcon,
                            position = currentPos,
                            duration = duration,
                            timestamp = System.currentTimeMillis(),
                            containerExtension = currentExtension,
                            parentSeriesId = currentSeriesId
                        )
                        dao.insertProgress(progress)
                    }
                }
            } else {
                val progress = WatchProgress(
                    streamId = currentStreamId,
                    name = currentStreamName,
                    streamType = "live",
                    streamIcon = currentStreamIcon,
                    position = 0,
                    duration = 0,
                    timestamp = System.currentTimeMillis(),
                    containerExtension = null,
                    parentSeriesId = null
                )
                dao.insertProgress(progress)
            }
        }
    }

    private fun checkResumePosition() {
        if (isVod()) {
            lifecycleScope.launch {
                val dao = AppDatabase.getDatabase(applicationContext).watchDao()
                val savedPos = dao.getPosition(currentStreamId)
                if (savedPos != null && savedPos > 0) {
                    exoPlayer?.seekTo(savedPos)
                    Toast.makeText(this@PlayerActivity, "Kaldığınız yerden devam ediliyor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configureUiForType(type: String) {
        when (type) {
            "live" -> {
                btnNext?.visibility = View.VISIBLE
                btnPrev?.visibility = View.VISIBLE
                tvLiveBadge?.visibility = View.VISIBLE
                btnForward?.visibility = View.GONE
                btnRewind?.visibility = View.GONE
                timeBar?.visibility = View.GONE
                tvExoPosition?.visibility = View.GONE
                tvExoDuration?.visibility = View.GONE
                tvDivider?.visibility = View.GONE
            }
            "series" -> {
                btnNext?.visibility = View.VISIBLE
                btnPrev?.visibility = View.VISIBLE
                tvLiveBadge?.visibility = View.GONE
                btnForward?.visibility = View.VISIBLE
                btnRewind?.visibility = View.VISIBLE
                timeBar?.visibility = View.VISIBLE
                tvExoPosition?.visibility = View.VISIBLE
                tvExoDuration?.visibility = View.VISIBLE
                tvDivider?.visibility = View.VISIBLE
            }
            else -> { // movie
                btnNext?.visibility = View.GONE
                btnPrev?.visibility = View.GONE
                tvLiveBadge?.visibility = View.GONE
                btnForward?.visibility = View.VISIBLE
                btnRewind?.visibility = View.VISIBLE
                timeBar?.visibility = View.VISIBLE
                tvExoPosition?.visibility = View.VISIBLE
                tvExoDuration?.visibility = View.VISIBLE
                tvDivider?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupButtons() {
        btnFavorite?.setOnClickListener { toggleFavorite(); playerView?.showController() }

        btnAspectRatio?.setOnClickListener {
            currentResizeMode = when (currentResizeMode) {
                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            tvAspectRatioText?.text = when(currentResizeMode) {
                AspectRatioFrameLayout.RESIZE_MODE_FILL -> "DOLDUR"
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "YAKINLAŞTIR"
                else -> "SIĞDIR"
            }
            playerView?.setResizeMode(currentResizeMode)
            playerView?.showController()
        }

        btnPlayPause?.setOnClickListener {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
                btnPlayPause?.setImageResource(android.R.drawable.ic_media_play)
            } else {
                exoPlayer?.play()
                btnPlayPause?.setImageResource(android.R.drawable.ic_media_pause)
            }
            playerView?.showController()
        }

        btnForward?.setOnClickListener {
            exoPlayer?.let { player ->
                val duration = player.duration.takeIf { d -> d > 0 } ?: Long.MAX_VALUE
                val target = (player.currentPosition + 10_000).coerceAtMost(duration)
                player.seekTo(target)
            }
            playerView?.showController()
        }
        btnRewind?.setOnClickListener {
            exoPlayer?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
            playerView?.showController()
        }

        btnAudio?.setOnClickListener { showTrackSelectionDialog(C.TRACK_TYPE_AUDIO, "Ses Dili") }
        btnSubtitle?.setOnClickListener { showTrackSelectionDialog(C.TRACK_TYPE_TEXT, "Altyazı") }

        btnNext?.setOnClickListener {
            if (streamType == "live") {
                val next = ChannelManager.nextChannel()
                if (next != null) changeChannel(next.name, next.streamId, next.streamIcon)
            } else if (streamType == "series") {
                EpisodeManager.next()?.let { changeEpisode(it) }
            }
        }
        btnPrev?.setOnClickListener {
            if (streamType == "live") {
                val prev = ChannelManager.previousChannel()
                if (prev != null) changeChannel(prev.name, prev.streamId, prev.streamIcon)
            } else if (streamType == "series") {
                EpisodeManager.previous()?.let { changeEpisode(it) }
            }
        }
        btnSpeed?.setOnClickListener { showSpeedDialog() }
    }

    private fun setupOverlay() {
        overlayAdapter = OverlayChannelAdapter()
        overlayCatAdapter = OverlayCategoryAdapter { cat ->
            overlayActiveCat = cat.categoryId
            filterOverlay(overlaySearch?.text?.toString()?.lowercase()?.trim() ?: "")
            rvOverlayCategories?.post {
                val index = overlayCategories.indexOfFirst { it.categoryId == overlayActiveCat }.coerceAtLeast(0)
                rvOverlayCategories?.findViewHolderForAdapterPosition(index)?.itemView?.requestFocus()
            }
        }
        rvOverlayCategories?.layoutManager = LinearLayoutManager(this)
        rvOverlayCategories?.adapter = overlayCatAdapter
        rvOverlay?.layoutManager = LinearLayoutManager(this)
        rvOverlay?.adapter = overlayAdapter
        rvOverlay?.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                hideChannelOverlay()
                return@setOnKeyListener true
            }
            false
        }

        overlaySearch?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterOverlay(s?.toString()?.lowercase()?.trim() ?: "")
            }
        })
    }

    private fun changeChannel(name: String, id: Int, icon: String?) {
        saveOrDeleteProgress()
        pendingSeekMs = 0L
        seekHandler.removeCallbacks(seekRunnable)
        numberHandler.removeCallbacks(numberRunnable)
        pendingNumberInput = ""
        tvNumberOverlay?.visibility = View.GONE
        tvSeekPreview?.visibility = View.GONE
        tvChannelName?.text = name
        currentStreamName = name
        currentStreamId = id
        currentStreamIcon = icon
        currentExtension = "ts"
        currentSeriesId = null
        checkFavoriteStatus()
        releasePlayer()
        initializePlayer(id, "ts")
    }

    private fun changeChannelById(streamId: Int) {
        val channel = ChannelManager.getChannelById(streamId)
        if (channel != null) {
            changeChannel(channel.name, channel.streamId, channel.streamIcon)
        } else {
            Toast.makeText(this, "Kanal bulunamadı", Toast.LENGTH_SHORT).show()
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
                    overlayAdapter?.notifyDataSetChanged()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun changeEpisode(episode: SeriesEpisode) {
        saveOrDeleteProgress()
        pendingSeekMs = 0L
        seekHandler.removeCallbacks(seekRunnable)
        tvSeekPreview?.visibility = View.GONE
        tvChannelName?.text = "${episode.episodeNum}. ${episode.title}"
        currentStreamName = "${episode.episodeNum}. ${episode.title}"
        currentStreamId = episode.id.toIntOrNull() ?: return
        currentStreamIcon = episode.info?.movieImage
        currentExtension = episode.containerExtension ?: "mp4"
        streamType = "series"
        checkFavoriteStatus()
        releasePlayer()
        initializePlayer(currentStreamId, currentExtension)
    }

    private fun checkFavoriteStatus() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(applicationContext).favoriteDao()
            isFavorite = dao.isFavorite(currentStreamId)
            updateFavoriteIcon()
        }
    }

    private fun toggleFavorite() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(applicationContext).favoriteDao()
            if (isFavorite) {
                dao.deleteByStreamId(currentStreamId)
                isFavorite = false
                Toast.makeText(this@PlayerActivity, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show()
            } else {
                val newFav = FavoriteChannel(
                    streamId = currentStreamId,
                    name = currentStreamName,
                    streamType = streamType,
                    streamIcon = currentStreamIcon,
                    categoryName = "Genel"
                )
                dao.insert(newFav)
                isFavorite = true
                Toast.makeText(this@PlayerActivity, "Favorilere eklendi", Toast.LENGTH_SHORT).show()
            }
            updateFavoriteIcon()
        }
    }

    private fun updateFavoriteIcon() {
        if (isFavorite) {
            btnFavorite?.setImageResource(android.R.drawable.btn_star_big_on)
            btnFavorite?.setColorFilter(android.graphics.Color.parseColor("#FFD700"))
        } else {
            btnFavorite?.setImageResource(android.R.drawable.btn_star_big_off)
            btnFavorite?.setColorFilter(android.graphics.Color.WHITE)
        }
    }

    private fun showSpeedDialog() {
        val speeds = arrayOf("0.5x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")
        val vals = floatArrayOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog.Builder(this).setTitle("Oynatma Hızı").setItems(speeds) { _, w ->
            exoPlayer?.playbackParameters = PlaybackParameters(vals[w])
        }.show()
    }

    private fun showTrackSelectionDialog(trackType: Int, title: String) {
        if (exoPlayer == null || trackSelector == null) return
        val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo ?: return
        val rendererIndex = (0 until mappedTrackInfo.rendererCount).find { mappedTrackInfo.getRendererType(it) == trackType } ?: return

        val groupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
        val items = mutableListOf<String>()
        val indices = mutableListOf<Pair<Int, Int>>()
        items.add("Kapat / Varsayılan"); indices.add(Pair(-1, -1))

        for (i in 0 until groupArray.length) {
            val group = groupArray[i]
            for (j in 0 until group.length) {
                if (mappedTrackInfo.getTrackSupport(rendererIndex, i, j) == C.FORMAT_HANDLED) {
                    val format = group.getFormat(j)
                    var label = format.label
                    if (label == null) {
                        val lang = format.language
                        label = if (lang != null && lang != "und") Locale(lang).displayLanguage else "Ses İzi $i"
                    }
                    items.add(label)
                    indices.add(Pair(i, j))
                }
            }
        }

        AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()) { _, w ->
            val (gIdx, tIdx) = indices[w]
            val builder = trackSelector!!.buildUponParameters()
            val disable = gIdx == -1
            builder.setRendererDisabled(rendererIndex, disable)
            builder.clearSelectionOverrides(rendererIndex)
            if (!disable) {
                val group = groupArray[gIdx]
                val override = DefaultTrackSelector.SelectionOverride(gIdx, tIdx)
                builder.setSelectionOverride(rendererIndex, groupArray, override)
            }
            trackSelector!!.setParameters(builder)
            playerView?.hideController()
        }.show()
        playerView?.hideController()
    }

    private fun initializePlayer(id: Int, ext: String) {
        currentStreamId = id
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val url = prefs.getString("SERVER_URL", "") ?: ""
        val user = prefs.getString("USERNAME", "") ?: ""
        val pass = prefs.getString("PASSWORD", "") ?: ""
        var baseUrl = url
        if (!baseUrl.endsWith("/")) baseUrl += "/"
        val isLive = streamType == "live"
        val vodPath = if (streamType == "series") "series" else "movie"
        val finalUrl = if (isLive) {
            "${baseUrl}live/$user/$pass/$id.ts"
        } else {
            "${baseUrl}$vodPath/$user/$pass/$id.$ext"
        }

        trackSelector = DefaultTrackSelector(this)
        trackSelector!!.setParameters(trackSelector!!.buildUponParameters().setPreferredAudioLanguage(null).setPreferredTextLanguage(null))

        exoPlayer = ExoPlayer.Builder(this).setTrackSelector(trackSelector!!).build()
        playerView?.player = exoPlayer

        val dataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).setUserAgent("UzunsIPTV")
        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(finalUrl)))

        exoPlayer?.setMediaSource(mediaSource)
        exoPlayer?.prepare()

        checkResumePosition()

        exoPlayer?.playWhenReady = true

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                pbLoading?.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                if (state == Player.STATE_READY) {
                    if (exoPlayer?.isPlaying == true) btnPlayPause?.setImageResource(android.R.drawable.ic_media_pause)
                    exoPlayer?.videoFormat?.let { updateResolutionLabel(it.width, it.height) }
                }
                if (state == Player.STATE_ENDED) {
                    if (autoPlayNext && streamType == "live") {
                        btnNext?.performClick()
                    } else {
                        playerView?.showController()
                    }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                pbLoading?.visibility = View.GONE
                Toast.makeText(this@PlayerActivity, "Hata", Toast.LENGTH_SHORT).show()
            }
            override fun onVideoSizeChanged(videoSize: com.google.android.exoplayer2.video.VideoSize) {
                updateResolutionLabel(videoSize.width, videoSize.height)
            }
        })
    }

    private fun isVod(): Boolean = streamType == "movie" || streamType == "series"

    private fun updateResolutionLabel(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            tvResolution?.text = "—"
            return
        }
        val descriptor = when {
            height >= 2160 -> "4K"
            height >= 1440 -> "2K"
            height >= 1080 -> "Full HD"
            height >= 720 -> "HD"
            else -> "SD"
        }
        tvResolution?.text = "${width}x$height $descriptor"
    }

    private fun accumulateSeek(offsetMs: Long, showController: Boolean = false) {
        if (!isVod() || exoPlayer == null) return
        pendingSeekMs += offsetMs
        val player = exoPlayer ?: return
        val current = player.currentPosition
        val duration = if (player.duration > 0) player.duration else Long.MAX_VALUE
        val target = (current + pendingSeekMs).coerceIn(0L, duration)
        pendingSeekMs = target - current
        tvSeekPreview?.text = if (pendingSeekMs >= 0) "+${pendingSeekMs / 1000}s" else "-${abs(pendingSeekMs) / 1000}s"
        tvSeekPreview?.visibility = View.VISIBLE
        seekHandler.removeCallbacks(seekRunnable)
        seekHandler.postDelayed(seekRunnable, 1200)
        if (showController) playerView?.showController()
    }

    private fun applyPendingSeek() {
        if (!isVod() || exoPlayer == null || pendingSeekMs == 0L) {
            tvSeekPreview?.visibility = View.GONE
            pendingSeekMs = 0L
            return
        }
        val player = exoPlayer ?: return
        val duration = if (player.duration > 0) player.duration else Long.MAX_VALUE
        val newPos = (player.currentPosition + pendingSeekMs).coerceIn(0L, duration)
        player.seekTo(newPos)
        pendingSeekMs = 0L
        tvSeekPreview?.visibility = View.GONE
    }

    private fun handleNumberInput(digit: Int): Boolean {
        if (streamType != "live") return false
        pendingNumberInput = (pendingNumberInput + digit.toString()).takeLast(4)
        tvNumberOverlay?.text = pendingNumberInput
        tvNumberOverlay?.visibility = View.VISIBLE
        numberHandler.removeCallbacks(numberRunnable)
        numberHandler.postDelayed(numberRunnable, 2000)
        return true
    }

    private fun applyNumberHotkey() {
        if (pendingNumberInput.isBlank()) {
            tvNumberOverlay?.visibility = View.GONE
            return
        }
        val streamId = ChannelHotkeyManager.getStreamIdForNumber(this, pendingNumberInput)
        if (streamId != null) {
            changeChannelById(streamId)
        }
        pendingNumberInput = ""
        tvNumberOverlay?.visibility = View.GONE
    }

    private fun showChannelOverlay() {
        if (streamType != "live") return
        overlayCategories = ChannelManager.categoryList.ifEmpty { overlayCategories }
        overlayCatAdapter?.submitList(overlayCategories)
        overlayActiveCat = overlayCategories.firstOrNull()?.categoryId ?: "ALL"
        overlayChannels = ChannelManager.channelList.toMutableList()
        filterOverlay("")
        selectedOverlayIndex = -1
        overlayAdapter?.notifyDataSetChanged()
        overlayContainer?.visibility = View.VISIBLE
        overlayCard?.alpha = 0f
        overlayCard?.scaleX = 0.9f
        overlayCard?.scaleY = 0.9f
        overlayCard?.animate()?.alpha(1f)?.scaleX(1f)?.scaleY(1f)?.setDuration(200)?.start()
        overlaySearch?.setText("")
        overlaySearch?.requestFocus()
    }

    private fun hideChannelOverlay() {
        overlayContainer?.visibility = View.GONE
        selectedOverlayIndex = -1
    }

    private fun filterOverlay(query: String) {
        val base = if (overlayActiveCat == "ALL") {
            overlayChannels
        } else {
            overlayChannels.filter { it.categoryId == overlayActiveCat }
        }
        filteredOverlay = if (query.isEmpty()) {
            base.toMutableList()
        } else {
            base.filter { it.name.lowercase().contains(query) }.toMutableList()
        }
        selectedOverlayIndex = -1
        overlayAdapter?.notifyDataSetChanged()
    }

    inner class OverlayChannelAdapter : RecyclerView.Adapter<OverlayChannelAdapter.OverlayVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverlayVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
            return OverlayVH(view)
        }

        override fun getItemCount(): Int = filteredOverlay.size

        override fun onBindViewHolder(holder: OverlayVH, position: Int) {
            val item = filteredOverlay[position]
            holder.tvName.text = item.name
            val number = ChannelHotkeyManager.getNumberForStream(holder.itemView.context, item.streamId)
                ?: item.num?.toIntOrNull()?.toString()
                ?: item.streamId.toString()
            holder.tvNum.text = "#$number"
            Glide.with(holder.itemView.context)
                .load(item.streamIcon)
                .placeholder(R.drawable.bg_placeholder)
                .error(R.drawable.bg_placeholder)
                .into(holder.imgLogo)

            holder.itemView.setOnClickListener {
                changeChannel(item.name, item.streamId, item.streamIcon)
                hideChannelOverlay()
            }

            holder.itemView.setOnLongClickListener {
                showHotkeyDialog(item)
                true
            }
        }

        inner class OverlayVH(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvChannelName)
            val tvNum: TextView = view.findViewById(R.id.tvStreamId)
            val imgLogo: ImageView = view.findViewById(R.id.imgChannelLogo)
        }
    }

    inner class OverlayCategoryAdapter(
        private val onClick: (LiveCategory) -> Unit
    ) : RecyclerView.Adapter<OverlayCategoryAdapter.CatVH>() {
        private var cats = listOf<LiveCategory>()

        fun submitList(list: List<LiveCategory>) {
            cats = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return CatVH(view)
        }

        override fun getItemCount(): Int = cats.size

        override fun onBindViewHolder(holder: CatVH, position: Int) {
            val cat = cats[position]
            holder.tvName.text = cat.categoryName
            holder.root.isSelected = cat.categoryId == overlayActiveCat
            holder.itemView.setOnClickListener {
                overlayActiveCat = cat.categoryId
                notifyDataSetChanged()
                onClick(cat)
            }
        }

        inner class CatVH(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvCategoryName)
            val root: View = view.findViewById(R.id.rootLayout)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (overlayContainer?.visibility == View.VISIBLE) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU -> {
                        hideChannelOverlay(); return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // allow recycler listener to handle
                        return rvOverlay?.dispatchKeyEvent(event) ?: false
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        return rvOverlay?.dispatchKeyEvent(event) ?: false
                    }
                }
            }
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                    btnPlayPause?.performClick()
                    return true
                }
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
                KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                    val digit = when (event.keyCode) {
                        KeyEvent.KEYCODE_0 -> 0
                        KeyEvent.KEYCODE_1 -> 1
                        KeyEvent.KEYCODE_2 -> 2
                        KeyEvent.KEYCODE_3 -> 3
                        KeyEvent.KEYCODE_4 -> 4
                        KeyEvent.KEYCODE_5 -> 5
                        KeyEvent.KEYCODE_6 -> 6
                        KeyEvent.KEYCODE_7 -> 7
                        KeyEvent.KEYCODE_8 -> 8
                        else -> 9
                    }
                    if (handleNumberInput(digit)) return true
                }
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    when (streamType) {
                        "live" -> btnNext?.performClick()
                        "series" -> EpisodeManager.next()?.let { changeEpisode(it) }
                        else -> accumulateSeek(10_000, showController = false)
                    }
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    when (streamType) {
                        "live" -> btnPrev?.performClick()
                        "series" -> EpisodeManager.previous()?.let { changeEpisode(it) }
                        else -> accumulateSeek(-10_000, showController = false)
                    }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    finish()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    if (isVod()) {
                        accumulateSeek(10_000, showController = false)
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    if (isVod()) {
                        accumulateSeek(-10_000, showController = false)
                        return true
                    }
                }
                KeyEvent.KEYCODE_MENU -> {
                    showChannelOverlay()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (playerView?.isControllerFullyVisible == true) {
            playerView?.hideController()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        timeHandler.removeCallbacks(timeRunnable)
        seekHandler.removeCallbacks(seekRunnable)
        numberHandler.removeCallbacks(numberRunnable)
        saveOrDeleteProgress()
        releasePlayer()
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
        pendingSeekMs = 0L
        tvSeekPreview?.visibility = View.GONE
    }
}
