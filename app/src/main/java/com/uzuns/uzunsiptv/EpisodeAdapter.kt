package com.uzuns.uzunsiptv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EpisodeAdapter(
    private val onClick: (SeriesEpisode) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {

    private var episodes = listOf<SeriesEpisode>()

    fun updateList(newList: List<SeriesEpisode>) {
        episodes = newList.sortedBy { it.episodeNum }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]

        holder.tvTitle.text = "${episode.episodeNum}. ${episode.title}"

        // Süre bilgisi varsa yaz
        val duration = episode.info?.duration ?: ""
        holder.tvDuration.text = duration

        // Bölüm resmi varsa yükle
        val imageUrl = episode.info?.movieImage
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.bg_placeholder)
            .error(R.drawable.bg_placeholder)
            .into(holder.imgEpisode)

        holder.itemView.setOnClickListener {
            onClick(episode)
        }
    }

    override fun getItemCount() = episodes.size

    class EpisodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvEpisodeTitle)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val imgEpisode: ImageView = view.findViewById(R.id.imgEpisode)
    }
}
