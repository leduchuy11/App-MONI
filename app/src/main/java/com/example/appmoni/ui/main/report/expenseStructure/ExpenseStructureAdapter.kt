package com.example.appmoni.ui.main.report.expenseStructure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.report.ExpenseCategoryReport
import com.example.appmoni.databinding.ItemExpenseStructureDetailBinding
import java.text.DecimalFormat

class ExpenseStructureAdapter(
    private val chartColors: List<Int>
) : RecyclerView.Adapter<ExpenseStructureAdapter.ViewHolder>() {

    private val items = mutableListOf<ExpenseCategoryReport>()

    fun submitList(newList: List<ExpenseCategoryReport>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseStructureDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = chartColors[position % chartColors.size]

        val isLastItem = position == itemCount - 1

        holder.bind(items[position], color, isLastItem)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemExpenseStructureDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExpenseCategoryReport, colorInt: Int, isLastItem: Boolean) {
            val formatter = DecimalFormat("#,###")

            binding.tvCategoryGroupName.text = item.categoryName
            binding.tvPercent.text = "(%.1f%%)".format(item.percent).replace(".", ",")
            binding.tvAmount.text = "${formatter.format(item.amount)} đ".replace(",", ".")


            // Thanh Progress Bar set màu theo thứ tự
            val progressValue = (item.percent * 10).toInt()
            binding.progressBar.progress = progressValue
            binding.progressBar.setIndicatorColor(colorInt)

            // Lấy ID ảnh từ string iconName và gán vào ImageView
            if (item.iconName.isNotEmpty()) {
                val context = binding.root.context
                val resId = context.resources.getIdentifier(item.iconName, "drawable", context.packageName)
                if (resId != 0) {
                    binding.ivCategoryIcon.setImageResource(resId)
                }
            } else {
                binding.ivCategoryIcon.setImageDrawable(null)
            }
            binding.viewDivider.visibility = if (isLastItem) View.GONE else View.VISIBLE
        }
    }
}