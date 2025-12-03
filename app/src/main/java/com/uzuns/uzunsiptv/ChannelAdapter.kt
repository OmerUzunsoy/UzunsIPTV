package com.uzuns.uzunsiptv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.uzuns.uzunsiptv.ChannelHotkeyManager

class ChannelAdapter(
    private val onClick: (LiveStream) -> Unit,
    private val onLongClick: ((LiveStream) -> Unit)? = null
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private var channels = listOf<LiveStream>()

    fun updateList(newList: List<LiveStream>) {
        channels = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]

        holder.tvName.text = channel.name
        val ctx = holder.itemView.context
        val hotkeyNumber = ChannelHotkeyManager.getNumberForStream(ctx, channel.streamId)
        val displayNumber = hotkeyNumber ?: channel.num?.toIntOrNull()?.toString() ?: channel.streamId.toString()
        holder.tvNum.text = "#$displayNumber"

        // Kanal Logosu Yükleme (Glide Kütüphanesi ile)
        Glide.with(holder.itemView.context)
            .load(channel.streamIcon)
            .placeholder(R.mipmap.ic_launcher) // Resim yoksa bu çıkar
            .error(R.mipmap.ic_launcher)      // Hata olursa bu çıkar
            .into(holder.imgLogo)

        holder.itemView.setOnClickListener {
            onClick(channel)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(channel)
            true
        }
    }

    override fun getItemCount() = channels.size

    class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvChannelName)
        val tvNum: TextView = view.findViewById(R.id.tvStreamId)
        val imgLogo: ImageView = view.findViewById(R.id.imgChannelLogo)
    }
}
