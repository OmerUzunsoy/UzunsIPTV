package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VodDetailsActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvCast: TextView
    private lateinit var imgBackdrop: ImageView
    private lateinit var imgPosterSmall: ImageView
    private lateinit var btnPlay: Button
    private lateinit var btnBack: LinearLayout

    private var streamId: Int = 0
    private var streamExtension: String = "mp4"
    private var movieName: String = ""
    private var streamIcon: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vod_details)

        streamId = intent.getIntExtra("STREAM_ID", 0)
        movieName = intent.getStringExtra("NAME") ?: "Film"
        streamExtension = intent.getStringExtra("EXTENSION") ?: "mp4"
        streamIcon = intent.getStringExtra("ICON")
        val initialRating = intent.getStringExtra("RATING") ?: "N/A"

        initViews()

        tvTitle.text = movieName
        tvRating.text = "IMDB: $initialRating"
        Glide.with(this).load(streamIcon).into(imgBackdrop)
        Glide.with(this).load(streamIcon).into(imgPosterSmall)

        fetchMovieDetails()
        setupButtons()
        btnPlay.post { btnPlay.requestFocus() }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvMovieTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvRating = findViewById(R.id.tvRating)
        tvGenre = findViewById(R.id.tvGenre)
        tvDuration = findViewById(R.id.tvDuration)
        tvCast = findViewById(R.id.tvCast)
        imgBackdrop = findViewById(R.id.imgBackdrop)
        imgPosterSmall = findViewById(R.id.imgPosterSmall)
        btnPlay = findViewById(R.id.btnPlayMovie)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupButtons() {
        btnBack.setOnClickListener { finish() }
        btnBack.isFocusable = true
        btnBack.isFocusableInTouchMode = true

        btnPlay.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("STREAM_ID", streamId)
            intent.putExtra("STREAM_TYPE", "movie")
            intent.putExtra("STREAM_NAME", movieName)
            intent.putExtra("STREAM_ICON", streamIcon)
            intent.putExtra("CONTAINER_EXTENSION", streamExtension)
            startActivity(intent)
        }
    }

    private fun fetchMovieDetails() {
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val user = prefs.getString("USERNAME", "") ?: ""
        val pass = prefs.getString("PASSWORD", "") ?: ""
        val url = prefs.getString("SERVER_URL", "") ?: ""

        val api = ApiClient.getClient(url).create(XtreamApi::class.java)

        // DÜZELTME: 'v' parametresi kullanıldı
        api.getVodInfo(u = user, p = pass, v = streamId).enqueue(object : Callback<VodInfoResponse> {
            override fun onResponse(call: Call<VodInfoResponse>, response: Response<VodInfoResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val info = response.body()!!.info

                    tvDescription.text = info.plot ?: "Açıklama bulunamadı."
                    tvGenre.text = "Tür: ${info.genre ?: "Bilinmiyor"}"
                    tvDuration.text = "Süre: ${info.duration ?: "-"}"
                    tvCast.text = info.cast ?: "-"
                }
            }
            override fun onFailure(call: Call<VodInfoResponse>, t: Throwable) {
                tvDescription.text = "Detaylar yüklenemedi."
            }
        })
    }
}
