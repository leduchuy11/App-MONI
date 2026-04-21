package com.example.appmoni.ui.main.home.manageCategory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.category.CategoryExpenseGroup
import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.databinding.ItemCategoryGroupBinding

class ManageOuterCategoryAdapter(
    private val groups: List<CategoryExpenseGroup>,
    private val onEditClick: (CategoryExpenseItem) -> Unit
) : RecyclerView.Adapter<ManageOuterCategoryAdapter.OuterViewHolder>() {

    inner class OuterViewHolder(val binding: ItemCategoryGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: CategoryExpenseGroup) {
            binding.tvGroupName.text = group.groupName

            if (binding.rvInnerItems.layoutManager == null) {
                binding.rvInnerItems.layoutManager = LinearLayoutManager(binding.root.context)
            }

            // Tắt cuộn độc lập để không lỗi vuốt
            binding.rvInnerItems.isNestedScrollingEnabled = false

            // Gọi Adapter con và truyền tín hiệu bấm Cây bút lên trên
            val innerAdapter = ManageInnerCategoryAdapter(group.items) { clickedItem ->
                onEditClick(clickedItem)
            }
            binding.rvInnerItems.adapter = innerAdapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewHolder {
        val binding = ItemCategoryGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OuterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OuterViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size
}