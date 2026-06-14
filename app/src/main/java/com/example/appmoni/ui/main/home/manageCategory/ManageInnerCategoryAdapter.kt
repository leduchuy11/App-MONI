package com.example.appmoni.ui.main.home.manageCategory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.databinding.ItemManageCategoryBinding

class ManageInnerCategoryAdapter(
    private val items: List<CategoryExpenseItem>,
    private val onEditClick: (CategoryExpenseItem) -> Unit // Sự kiện bấm vào cây bút
) : RecyclerView.Adapter<ManageInnerCategoryAdapter.InnerViewHolder>() {

    inner class InnerViewHolder(val binding: ItemManageCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryExpenseItem, position: Int) {
            val context = binding.root.context

            binding.tvCategoryName.text = item.name

            val imageResId = context.resources.getIdentifier(
                item.iconName, "drawable", context.packageName
            )
            if (imageResId != 0) {
                binding.ivCategoryIcon.setImageResource(imageResId)
            } else {
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_category_breakfast)
            }

            binding.btnEditCategory.setOnClickListener {
                onEditClick(item)
            }

            binding.root.setOnClickListener {
                onEditClick(item)
            }

            // Ẩn đường kẻ ở dòng cuối cùng
            if (position == items.size - 1) {
                binding.divider.visibility = View.INVISIBLE
            } else {
                binding.divider.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val binding = ItemManageCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return InnerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}