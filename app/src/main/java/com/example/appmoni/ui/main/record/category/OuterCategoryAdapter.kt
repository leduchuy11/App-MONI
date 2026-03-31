package com.example.appmoni.ui.main.record.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.record.CategoryExpenseItem
import com.example.appmoni.data.record.CategoryExpenseGroup
import com.example.appmoni.databinding.ItemCategoryGroupBinding
import com.example.appmoni.ui.main.record.category.InnerCategoryAdapter

class OuterCategoryAdapter(
    private val groups: List<CategoryExpenseGroup>,
    private val onItemClick: (CategoryExpenseItem) -> Unit
) : RecyclerView.Adapter<OuterCategoryAdapter.OuterViewHolder>() {

    inner class OuterViewHolder(val binding: ItemCategoryGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(group: CategoryExpenseGroup) {
            binding.tvGroupName.text = group.groupName

            // Chỉ cài đặt LayoutManager 1 lần duy nhất
            if (binding.rvInnerItems.layoutManager == null) {
                binding.rvInnerItems.layoutManager = LinearLayoutManager(binding.root.context)
            }
            binding.rvInnerItems.isNestedScrollingEnabled = false

            // Tạo Adapter và truyền click listener
            val innerAdapter = InnerCategoryAdapter(group.items) { clickedItem ->
                onItemClick(clickedItem)
            }
            binding.rvInnerItems.adapter = innerAdapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewHolder {
        val binding =
            ItemCategoryGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OuterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OuterViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size
}