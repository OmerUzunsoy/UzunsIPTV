package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.uzuns.uzunsiptv.data.db.AppDatabase
import com.uzuns.uzunsiptv.data.db.FavoriteChannel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SeriesDetailsActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvPlot: TextView
    private lateinit var imgPoster: ImageView
    private lateinit var rvSeasons: RecyclerView
    private lateinit var rvEpisodes: RecyclerView
    private lateinit var pbEpisodes: ProgressBar
    private lateinit var btnBack: LinearLayout
    private lateinit var btnFavorite: Button

    private lateinit var seasonAdapter: SeasonAdapter
    private lateinit var episodeAdapter: EpisodeAdapter

    private var seriesId: Int = 0
    private var seriesName: String = ""
    private var seriesCover: String? = null
    private var allEpisodesMap = mapOf<String, List<SeriesEpisode>>()
    private var currentEpisodes = listOf<SeriesEpisode>()
    private var autoPlayEpisodeId: String? = null
    private var hasAutoPlayed = false
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_details)

        seriesId = intent.getIntExtra("SERIES_ID", 0)
        seriesName = intent.getStringExtra("NAME") ?: "Dizi"
        seriesCover = intent.getStringExtra("COVER")
        autoPlayEpisodeId = intent.getStringExtra("AUTO_PLAY_EPISODE_ID")
        val rating = intent.getStringExtra("RATING") ?: "N/A"

        initViews()

        tvTitle.text = seriesName
        tvRating.text = "IMDB: $rating"
        Glide.with(this).load(seriesCover).into(imgPoster)

        setupRecyclerViews()
        setupActions()
        checkFavoriteStatus()
        fetchSeriesDetails()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvRating = findViewById(R.id.tvRating)
        tvPlot = findViewById(R.id.tvPlot)
        imgPoster = findViewById(R.id.imgPoster)
        rvSeasons = findViewById(R.id.rvSeasons)
        rvEpisodes = findViewById(R.id.rvEpisodes)
        pbEpisodes = findViewById(R.id.pbEpisodes)
        btnBack = findViewById(R.id.btnBack)
        btnFavorite = findViewById(R.id.btnFavoriteSeries)

        btnBack.setOnClickListener { finish() }
    }

    private fun setupActions() {
        btnFavorite.setOnClickListener { toggleFavorite() }
    }

    private fun setupRecyclerViews() {
        seasonAdapter = SeasonAdapter { seasonKey ->
            loadEpisodesForSeason(seasonKey)
        }
        rvSeasons.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSeasons.adapter = seasonAdapter

        episodeAdapter = EpisodeAdapter { episode ->
            openEpisode(episode)
        }
        rvEpisodes.layoutManager = LinearLayoutManager(this)
        rvEpisodes.adapter = episodeAdapter
    }

    private fun fetchSeriesDetails() {
        pbEpisodes.visibility = View.VISIBLE
        val prefs = Prefs.user(this)
        val user = prefs.getString(KEY_USERNAME, "") ?: ""
        val pass = prefs.getString(KEY_PASSWORD, "") ?: ""
        val url = prefs.getString(KEY_SERVER_URL, "") ?: ""
        val api = ApiClient.getClient(url).create(XtreamApi::class.java)

        api.getSeriesInfo(u = user, p = pass, s = seriesId).enqueue(object : Callback<SeriesInfoResponse> {
            override fun onResponse(call: Call<SeriesInfoResponse>, response: Response<SeriesInfoResponse>) {
                pbEpisodes.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    tvPlot.text = data.info.plot?.takeIf { it.isNotBlank() } ?: ""
                    allEpisodesMap = data.episodes
                    val seasonList = allEpisodesMap.keys.toList().sortedBy { it.toIntOrNull() ?: 0 }

                    if (seasonList.isNotEmpty()) {
                        seasonAdapter.updateList(seasonList)
                        rvSeasons.post { rvSeasons.requestFocus() }
                        loadEpisodesForSeason(resolveTargetSeason(seasonList))
                    }
                }
            }
            override fun onFailure(call: Call<SeriesInfoResponse>, t: Throwable) {
                pbEpisodes.visibility = View.GONE
            }
        })
    }

    private fun loadEpisodesForSeason(seasonKey: String) {
        val episodes = allEpisodesMap[seasonKey]
        if (episodes != null) {
            currentEpisodes = episodes.sortedBy { it.episodeNum }
            episodeAdapter.updateList(currentEpisodes)
            val targetEpisodeId = autoPlayEpisodeId
            val selectedEpisodeId = currentEpisodes.firstOrNull { it.id == targetEpisodeId }?.id
                ?: currentEpisodes.firstOrNull()?.id
                ?: ""
            EpisodeManager.setEpisodes(currentEpisodes, selectedEpisodeId)
            rvEpisodes.post {
                val focusIndex = currentEpisodes.indexOfFirst { it.id == selectedEpisodeId }.coerceAtLeast(0)
                rvEpisodes.requestFocus()
                rvEpisodes.layoutManager?.scrollToPosition(focusIndex)
                rvEpisodes.findViewHolderForAdapterPosition(focusIndex)?.itemView?.requestFocus()
            }
            maybeAutoPlaySelectedEpisode()
        }
    }

    private fun resolveTargetSeason(seasonList: List<String>): String {
        val episodeId = autoPlayEpisodeId ?: return seasonList.first()
        return seasonList.firstOrNull { season ->
            allEpisodesMap[season].orEmpty().any { it.id == episodeId }
        } ?: seasonList.first()
    }

    private fun maybeAutoPlaySelectedEpisode() {
        val episodeId = autoPlayEpisodeId ?: return
        if (hasAutoPlayed) return
        val target = currentEpisodes.firstOrNull { it.id == episodeId } ?: return
        hasAutoPlayed = true
        autoPlayEpisodeId = null
        openEpisode(target)
    }

    private fun openEpisode(episode: SeriesEpisode) {
        val intent = Intent(this, PlayerActivity::class.java)
        try {
            intent.putExtra("STREAM_ID", episode.id.toInt())
        } catch (_: Exception) {
            return
        }
        EpisodeManager.setEpisodes(currentEpisodes, episode.id)
        intent.putExtra("STREAM_TYPE", "series")
        intent.putExtra("STREAM_NAME", "${episode.episodeNum}. ${episode.title}")
        intent.putExtra("SERIES_NAME", seriesName)
        intent.putExtra("CONTAINER_EXTENSION", episode.containerExtension ?: "mp4")
        intent.putExtra("SERIES_ID", seriesId)
        intent.putExtra("STREAM_ICON", episode.info?.movieImage)
        intent.putExtra("SERIES_COVER", seriesCover)
        startActivity(intent)
    }

    private fun checkFavoriteStatus() {
        lifecycleScope.launch {
            isFavorite = AppDatabase.getDatabase(applicationContext)
                .favoriteDao()
                .isFavorite(seriesId, "series")
            updateFavoriteButton()
        }
    }

    private fun toggleFavorite() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(applicationContext).favoriteDao()
            if (isFavorite) {
                dao.deleteByStreamId(seriesId, "series")
                isFavorite = false
                Toast.makeText(this@SeriesDetailsActivity, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show()
            } else {
                dao.insert(
                    FavoriteChannel(
                        streamId = seriesId,
                        name = seriesName,
                        streamType = "series",
                        streamIcon = seriesCover,
                        categoryName = "Diziler",
                        directSource = null
                    )
                )
                isFavorite = true
                Toast.makeText(this@SeriesDetailsActivity, "Favorilere eklendi", Toast.LENGTH_SHORT).show()
            }
            updateFavoriteButton()
        }
    }

    private fun updateFavoriteButton() {
        btnFavorite.text = if (isFavorite) "FAVORİDEN ÇIKAR" else "FAVORİLERE EKLE"
    }
}
