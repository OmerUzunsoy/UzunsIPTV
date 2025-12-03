package com.uzuns.uzunsiptv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AccountItem(
    val name: String,
    val server: String,
    val status: String = "Kayıtlı"
)

class AccountAdapter(
    private var items: List<AccountItem>,
    private val onClick: (AccountItem) -> Unit
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
        holder.tvDetail.text = item.server
        holder.tvStatus.text = item.status
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgAccountIcon)
        val tvName: TextView = view.findViewById(R.id.tvAccountName)
        val tvDetail: TextView = view.findViewById(R.id.tvAccountDetail)
        val tvStatus: TextView = view.findViewById(R.id.tvAccountStatus)
    }
}
