package com.uzuns.uzunsiptv

import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val onClick: (LiveCategory) -> Unit,
    private val onNavigateRight: (() -> Unit)? = null
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var categories = listOf<LiveCategory>()
    private var selectedPosition = 0 // Hangi sıradaki seçili? (Başta 0)

    fun updateList(newList: List<LiveCategory>) {
        categories = newList
        if (selectedPosition >= categories.size) {
            selectedPosition = if (categories.isEmpty()) 0 else categories.lastIndex
        }
        notifyDataSetChanged()
    }

    fun getSelectedIndex(): Int = selectedPosition

    fun setSelectedCategory(categoryId: String) {
        if (categories.isEmpty()) return
        val newIndex = categories.indexOfFirst { it.categoryId == categoryId }.takeIf { it >= 0 } ?: 0
        if (newIndex == selectedPosition) return
        val old = selectedPosition
        selectedPosition = newIndex
        notifyItemChanged(old)
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = category.categoryName

        // SEÇİLİ DURUM KONTROLÜ
        // Eğer bu satır seçiliyse 'isSelected = true' yapıyoruz.
        // XML'deki bg_menu_item dosyası bunu algılayıp rengi mavi yapıyor.
        holder.rootLayout.isSelected = (position == selectedPosition)

        // Yazı rengini de ayarlayalım (Seçiliyse Siyah, değilse Gri)
        if (position == selectedPosition) {
            holder.tvName.setTextColor(Color.BLACK)
        } else {
            holder.tvName.setTextColor(Color.parseColor("#BBBBBB"))
        }

        holder.itemView.setOnClickListener {
            // Eski seçiliyi normale döndür
            notifyItemChanged(selectedPosition)

            // Yeni seçiliyi işaretle
            selectedPosition = holder.adapterPosition
            notifyItemChanged(selectedPosition)

            onClick(category)
        }

        holder.itemView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        holder.itemView.performClick()
                        onNavigateRight?.invoke()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        onNavigateRight?.invoke()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    override fun getItemCount() = categories.size

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val rootLayout: LinearLayout = view.findViewById(R.id.rootLayout)
    }
}
