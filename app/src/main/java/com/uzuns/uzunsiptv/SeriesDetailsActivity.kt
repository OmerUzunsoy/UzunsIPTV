package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

    private lateinit var seasonAdapter: SeasonAdapter
    private lateinit var episodeAdapter: EpisodeAdapter

    private var seriesId: Int = 0
    private var seriesName: String = ""
    private var allEpisodesMap = mapOf<String, List<SeriesEpisode>>()
    private var currentEpisodes = listOf<SeriesEpisode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_details)

        seriesId = intent.getIntExtra("SERIES_ID", 0)
        seriesName = intent.getStringExtra("NAME") ?: "Dizi"
        val coverUrl = intent.getStringExtra("COVER")
        val rating = intent.getStringExtra("RATING") ?: "N/A"

        initViews()

        tvTitle.text = seriesName
        tvRating.text = "IMDB: $rating"
        Glide.with(this).load(coverUrl).into(imgPoster)

        setupRecyclerViews()
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

        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        seasonAdapter = SeasonAdapter { seasonKey ->
            loadEpisodesForSeason(seasonKey)
        }
        rvSeasons.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSeasons.adapter = seasonAdapter

        episodeAdapter = EpisodeAdapter { episode ->
            val intent = Intent(this, PlayerActivity::class.java)
            try {
                intent.putExtra("STREAM_ID", episode.id.toInt())
            } catch (e: Exception) {
                return@EpisodeAdapter
            }
            EpisodeManager.setEpisodes(currentEpisodes, episode.id)
            intent.putExtra("STREAM_TYPE", "series")
            intent.putExtra("STREAM_NAME", "${episode.episodeNum}. ${episode.title}")
            intent.putExtra("CONTAINER_EXTENSION", episode.containerExtension ?: "mp4")
            intent.putExtra("SERIES_ID", seriesId)
            intent.putExtra("STREAM_ICON", episode.info?.movieImage)
            startActivity(intent)
        }
        rvEpisodes.layoutManager = LinearLayoutManager(this)
        rvEpisodes.adapter = episodeAdapter
    }

    private fun fetchSeriesDetails() {
        pbEpisodes.visibility = View.VISIBLE
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val user = prefs.getString("USERNAME", "") ?: ""
        val pass = prefs.getString("PASSWORD", "") ?: ""
        val url = prefs.getString("SERVER_URL", "") ?: ""
        val api = ApiClient.getClient(url).create(XtreamApi::class.java)

        api.getSeriesInfo(u = user, p = pass, s = seriesId).enqueue(object : Callback<SeriesInfoResponse> {
            override fun onResponse(call: Call<SeriesInfoResponse>, response: Response<SeriesInfoResponse>) {
                pbEpisodes.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    tvPlot.text = data.info.plot ?: "Açıklama yok."
                    allEpisodesMap = data.episodes
                    val seasonList = allEpisodesMap.keys.toList().sortedBy { it.toIntOrNull() ?: 0 }

                    if (seasonList.isNotEmpty()) {
                        seasonAdapter.updateList(seasonList)
                        rvSeasons.post { rvSeasons.requestFocus() }
                        loadEpisodesForSeason(seasonList[0])
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
            EpisodeManager.setEpisodes(currentEpisodes, currentEpisodes.firstOrNull()?.id ?: "")
            rvEpisodes.post {
                rvEpisodes.requestFocus()
                rvEpisodes.layoutManager?.scrollToPosition(0)
            }
        }
    }
}
