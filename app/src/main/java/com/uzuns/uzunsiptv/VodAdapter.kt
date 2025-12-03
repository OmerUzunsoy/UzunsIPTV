package com.uzuns.uzunsiptv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class VodAdapter(
    private val spanCount: Int,
    private val onClick: (VodStream) -> Unit,
    private val onLongClick: ((VodStream) -> Unit)? = null
) : RecyclerView.Adapter<VodAdapter.VodViewHolder>() {

    private var movies = listOf<VodStream>()

    fun updateList(newList: List<VodStream>) {
        movies = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vod, parent, false)
        return VodViewHolder(view)
    }

    override fun onBindViewHolder(holder: VodViewHolder, position: Int) {
        val movie = movies[position]

        holder.tvName.text = movie.name

        val ratingText = if (movie.rating.isNullOrEmpty()) "N/A" else movie.rating
        holder.tvRating.text = ratingText

        Glide.with(holder.itemView.context)
            .load(movie.streamIcon)
            .placeholder(R.drawable.bg_placeholder)
            .error(R.drawable.bg_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.imgPoster)

        // Tıklama (Normal)
        holder.itemView.setOnClickListener {
            onClick(movie)
        }

        // Uzun Basma (YENİ)
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(movie)
            true // Olayı tükettik, normal tıklama çalışmasın
        }

        holder.itemView.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val total = movies.size
                        val lastRowCount = if (total % spanCount == 0) spanCount else total % spanCount
                        if (position >= total - lastRowCount) {
                            return@setOnKeyListener true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                        val col = position % spanCount
                        if (col == 0) return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    override fun getItemCount() = movies.size

    class VodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMovieName)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val imgPoster: ImageView = view.findViewById(R.id.imgPoster)
    }
}
