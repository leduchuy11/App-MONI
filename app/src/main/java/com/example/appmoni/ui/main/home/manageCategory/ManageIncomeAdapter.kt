package com.example.appmoni.ui.main.home.manageCategory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.model.category.CategoryIncomeItem
import com.example.appmoni.databinding.ItemManageCategoryBinding

class ManageIncomeAdapter(
    private val items: List<CategoryIncomeItem>,
    private val onEditClick: (CategoryIncomeItem) -> Unit // Sự kiện báo ra ngoài khi bấm Cây bút
) : RecyclerView.Adapter<ManageIncomeAdapter.IncomeViewHolder>() {

    inner class IncomeViewHolder(val binding: ItemManageCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryIncomeItem, position: Int) {
            val context = binding.root.context

            binding.tvCategoryName.text = item.name

            val imageResId = context.resources.getIdentifier(
                item.iconName, "drawable", context.packageName
            )
            if (imageResId != 0) {
                binding.ivCategoryIcon.setImageResource(imageResId)
            } else {
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_category_salary)
            }

            binding.btnEditCategory.setOnClickListener {
                onEditClick(item)
            }

            binding.root.setOnClickListener {
                onEditClick(item)
            }

            // Xử lý đường kẻ ngang (Divider)
            // Nếu là dòng cuối cùng thì giấu đường kẻ đi cho đẹp
            if (position == items.size - 1) {
                binding.divider.visibility = View.INVISIBLE
            } else {
                binding.divider.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val binding = ItemManageCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return IncomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}