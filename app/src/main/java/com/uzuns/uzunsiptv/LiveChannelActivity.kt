package com.uzuns.uzunsiptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LiveChannelActivity : AppCompatActivity() {

    private lateinit var rvChannels: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var channelAdapter: ChannelAdapter

    private var allChannelsList = listOf<LiveStream>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_channel)

        val categoryId = intent.getStringExtra("CATEGORY_ID") ?: "ALL"
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Kanallar"

        rvChannels = findViewById(R.id.rvChannels)
        pbLoading = findViewById(R.id.pbLoading)
        tvTitle = findViewById(R.id.tvCategoryTitle)

        tvTitle.text = categoryName

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setupRecyclerView()
        fetchChannels(categoryId)
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(onClick = { channel ->
            // Kanal listesini ve sırasını Manager'a yükle
            if (allChannelsList.isNotEmpty()) {
                ChannelManager.channelList = allChannelsList
                ChannelManager.currentPosition = allChannelsList.indexOf(channel)
            } else {
                ChannelManager.channelList = listOf(channel)
                ChannelManager.currentPosition = 0
            }

            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("STREAM_ID", channel.streamId)
            intent.putExtra("STREAM_TYPE", channel.streamType)
            intent.putExtra("STREAM_NAME", channel.name)

            startActivity(intent)
        })
        rvChannels.layoutManager = GridLayoutManager(this, 4)
        rvChannels.adapter = channelAdapter
    }

    private fun fetchChannels(categoryId: String) {
        pbLoading.visibility = View.VISIBLE

        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val user = prefs.getString("USERNAME", "") ?: ""
        val pass = prefs.getString("PASSWORD", "") ?: ""
        val url = prefs.getString("SERVER_URL", "") ?: ""

        val apiService = ApiClient.getClient(url).create(XtreamApi::class.java)

        apiService.getLiveStreams(user, pass).enqueue(object : Callback<List<LiveStream>> {
            override fun onResponse(call: Call<List<LiveStream>>, response: Response<List<LiveStream>>) {
                pbLoading.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val fullList = response.body()!!

                    val filteredList = if (categoryId == "ALL") {
                        fullList
                    } else {
                        fullList.filter { it.categoryId == categoryId }
                    }

                    allChannelsList = filteredList

                    if (filteredList.isEmpty()) {
                        Toast.makeText(this@LiveChannelActivity, "Kanal bulunamadı", Toast.LENGTH_SHORT).show()
                    }

                    channelAdapter.updateList(filteredList)
                } else {
                    Toast.makeText(this@LiveChannelActivity, "Hata", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<LiveStream>>, t: Throwable) {
                pbLoading.visibility = View.GONE
            }
        })
    }
}
