package com.example.appmoni.ui.main.home.manageLimit.addEditLimit // Sửa lại package

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.category.CategoryExpenseGroup
import com.example.appmoni.databinding.ItemCategoryLimitCardBinding
import com.example.appmoni.databinding.ItemCategoryLimitChildBinding

class SelectCategoryLimitAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<SelectCategoryLimitAdapter.GroupViewHolder>() {

    private var groups = listOf<CategoryExpenseGroup>()
    val selectedCategoryIds = mutableSetOf<String>()

    fun submitData(newGroups: List<CategoryExpenseGroup>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedCategoryIds.clear()
        groups.forEach { group ->
            group.items.forEach { child ->
                selectedCategoryIds.add(child.id)
            }
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedCategoryIds.size)
    }

    fun deselectAll() {
        selectedCategoryIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getSelectedCount(): Int = selectedCategoryIds.size

    // Tính tổng số lượng tất cả các hạng mục con
    fun getTotalItemCount(): Int = groups.sumOf { it.items.size }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemCategoryLimitCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size

    inner class GroupViewHolder(private val binding: ItemCategoryLimitCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: CategoryExpenseGroup) {
            binding.tvGroupName.text = group.groupName

            // Xóa hết các view con cũ trước khi vẽ lại để tránh bị trùng lặp khi cuộn
            binding.llItemsContainer.removeAllViews()

            // Vòng lặp vẽ các hạng mục con nhét vào trong Thẻ Nhóm
            group.items.forEachIndexed { index, item ->
                val childBinding = ItemCategoryLimitChildBinding.inflate(
                    LayoutInflater.from(binding.root.context),
                    binding.llItemsContainer,
                    false
                )

                childBinding.tvCategoryName.text = item.name

                // Set icon
                val context = binding.root.context
                val resId = context.resources.getIdentifier(item.iconName, "drawable", context.packageName)
                if (resId != 0) childBinding.ivCategoryIcon.setImageResource(resId)

                // Ẩn đường kẻ ở mục cuối cùng của nhóm (chuẩn UI đẹp)
                childBinding.viewDivider.visibility = if (index == group.items.size - 1) View.GONE else View.VISIBLE

                // Set trạng thái Checkbox
                childBinding.cbCategory.setOnCheckedChangeListener(null)
                childBinding.cbCategory.isChecked = selectedCategoryIds.contains(item.id)

                // Bắt sự kiện Click
                val clickListener = View.OnClickListener {
                    if (selectedCategoryIds.contains(item.id)) {
                        selectedCategoryIds.remove(item.id)
                    } else {
                        selectedCategoryIds.add(item.id)
                    }
                    // Cập nhật giao diện nút tick ngay lập tức mà không cần load lại toàn bộ danh sách
                    childBinding.cbCategory.isChecked = selectedCategoryIds.contains(item.id)
                    onSelectionChanged(selectedCategoryIds.size)
                }

                childBinding.root.setOnClickListener(clickListener)
                childBinding.cbCategory.setOnClickListener(clickListener)

                binding.llItemsContainer.addView(childBinding.root)
            }
        }
    }
}