package com.example.appmoni.ui.main.record.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.record.CategoryExpenseItem
import com.example.appmoni.databinding.ItemExpenseCategoryBinding

class InnerCategoryAdapter(
    private val items: List<CategoryExpenseItem>,
    private val onItemClick: (CategoryExpenseItem) -> Unit
) : RecyclerView.Adapter<InnerCategoryAdapter.InnerViewHolder>() {

    inner class InnerViewHolder(val binding: ItemExpenseCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryExpenseItem, position: Int) {
            val context = binding.root.context

            // Đổ tên danh mục
            binding.tvCategoryName.text = item.name

            // Biến string thành resource id
            val imageResId = context.resources.getIdentifier(
                item.iconName,
                "drawable",
                context.packageName
            )

            // HIỂN THỊ (Kèm theo phương án dự phòng nếu chưa có icon)
            if (imageResId != 0) {
                binding.ivCategoryIcon.setImageResource(imageResId)
            } else {
                // Hiện icon mặc định nếu bạn chưa kịp thêm cái ảnh vào drawable
                binding.ivCategoryIcon.setImageResource(com.example.appmoni.R.drawable.ic_category_breakfast)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }

            // Ẩn đường kẻ
            if (position == items.size - 1) {
                binding.divider.visibility = View.INVISIBLE
            } else {
                binding.divider.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val binding =
            ItemExpenseCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InnerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}