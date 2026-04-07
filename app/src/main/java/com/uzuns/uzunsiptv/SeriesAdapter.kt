package com.uzuns.uzunsiptv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class SeriesAdapter(
    private val spanCount: Int,
    private val onClick: (SeriesStream) -> Unit,
    private val onLongClick: ((SeriesStream) -> Unit)? = null
) : RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder>() {

    private var seriesItems = listOf<SeriesStream>()

    fun updateList(newList: List<SeriesStream>) {
        seriesItems = newList.toList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        // Film tasarımıyla (item_vod) aynı yapıyı kullanabiliriz, yeniden tasarlamaya gerek yok
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vod, parent, false)
        return SeriesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val series = seriesItems[position]

        holder.tvName.text = series.name

        val ratingText = if (series.rating.isNullOrEmpty()) "N/A" else series.rating
        holder.tvRating.text = ratingText

        Glide.with(holder.itemView.context)
            .load(series.cover)
            .placeholder(R.drawable.bg_placeholder)
            .error(R.drawable.bg_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.imgPoster)

        holder.itemView.setOnClickListener {
            onClick(series)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(series)
            onLongClick != null
        }

        holder.itemView.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val total = itemCount
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

    override fun getItemCount(): Int = seriesItems.size

    class SeriesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ID'ler item_vod.xml ile aynı olmalı
        val tvName: TextView = view.findViewById(R.id.tvMovieName)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val imgPoster: ImageView = view.findViewById(R.id.imgPoster)
    }
}
