package com.example.appmoni.ui.main.record.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.record.CategoryIncomeItem
import com.example.appmoni.databinding.ItemIncomeCategoryBinding

class IncomeCategoryAdapter(
    private val items: List<CategoryIncomeItem>,
    private val onItemClick: (CategoryIncomeItem) -> Unit
) : RecyclerView.Adapter<IncomeCategoryAdapter.IncomeViewHolder>() {

    inner class IncomeViewHolder(val binding: ItemIncomeCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryIncomeItem) {
            // Đổ dữ liệu vào UI
            binding.tvCategoryName.text = item.name
            val imageResId = item.getIconResource(binding.root.context)
            if (imageResId != 0) {
                binding.ivCategoryIcon.setImageResource(imageResId)
            }

            // Lắng nghe thao tác bấm của người dùng
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val binding = ItemIncomeCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return IncomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}