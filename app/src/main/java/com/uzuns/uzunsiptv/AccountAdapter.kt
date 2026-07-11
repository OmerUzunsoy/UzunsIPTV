package com.uzuns.uzunsiptv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AccountItem(
    val id: String,
    val name: String,
    val server: String,
    val type: String,
    val isActive: Boolean,
    val avatarIndex: Int
)

class AccountAdapter(
    private var items: List<AccountItem>,
    private val onClick: (AccountItem) -> Unit,
    private val onLongClick: (AccountItem) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    fun update(newList: List<AccountItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvDetail.text = if (item.type == TYPE_M3U) "M3U" else "XTREAM"
        holder.tvStatus.text = if (item.isActive) "SEÇİLİ" else ""
        val colors = intArrayOf(0xFF587E7A.toInt(), 0xFF667A9A.toInt(), 0xFF9A6F67.toInt(), 0xFF7D719C.toInt(), 0xFF8B7A5F.toInt(), 0xFF52758A.toInt())
        holder.imgIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(colors[item.avatarIndex % colors.size])
        holder.imgIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }

    override fun getItemCount(): Int = items.size

    class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgAccountIcon)
        val tvName: TextView = view.findViewById(R.id.tvAccountName)
        val tvDetail: TextView = view.findViewById(R.id.tvAccountDetail)
        val tvStatus: TextView = view.findViewById(R.id.tvAccountStatus)
    }
}
