package com.uzuns.uzunsiptv

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SeasonAdapter(
    private val onClick: (String) -> Unit // Sezon numarasını (String key) döndürür
) : RecyclerView.Adapter<SeasonAdapter.SeasonViewHolder>() {

    private var seasons = listOf<String>()
    private var selectedPosition = 0

    fun updateList(newList: List<String>) {
        seasons = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_season, parent, false)
        return SeasonViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        val seasonKey = seasons[position]
        holder.tvName.text = "Sezon $seasonKey"

        // Seçili olanı Mavi yap, değilse Gri kalsın
        if (position == selectedPosition) {
            holder.tvName.setBackgroundResource(R.drawable.bg_primary_button) // Mavi + Çerçeve
            holder.tvName.setTextColor(Color.BLACK)
        } else {
            holder.tvName.setBackgroundResource(R.drawable.bg_universal_selector) // Gri
            holder.tvName.setTextColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)

            onClick(seasonKey)
        }
    }

    override fun getItemCount() = seasons.size

    class SeasonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSeasonName)
    }
}