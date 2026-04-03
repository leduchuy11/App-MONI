package com.example.appmoni.ui.main.home.manageCategory.addAndEdit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.databinding.ItemIconBinding

class IconAdapter(
    private val icons: List<Int>,
    private val onIconSelected: (Int) -> Unit
) : RecyclerView.Adapter<IconAdapter.IconViewHolder>() {

    class IconViewHolder(val binding: ItemIconBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val iconRes = icons[position]
        holder.binding.ivIcon.setImageResource(iconRes)
        holder.itemView.setOnClickListener { onIconSelected(iconRes) }
    }

    override fun getItemCount() = icons.size
}