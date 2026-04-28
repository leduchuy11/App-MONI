package com.example.appmoni.ui.main.home.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.model.history.DailyTransactionGroup
import com.example.appmoni.databinding.ItemDailyCardHistoryBinding
import com.example.appmoni.databinding.ItemTransactionHistoryBinding
import com.example.appmoni.data.model.transaction.TransactionItem
import java.text.DecimalFormat

class HistoryAdapter(
    private val onItemClick: (TransactionItem) -> Unit
) : ListAdapter<DailyTransactionGroup, HistoryAdapter.DailyViewHolder>(DailyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyViewHolder {
        val binding =
            ItemDailyCardHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DailyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailyViewHolder(private val binding: ItemDailyCardHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(group: DailyTransactionGroup) {
            val formatter = DecimalFormat("#,###")

            binding.tvDate.text = group.dateStr
            binding.tvRelativeDay.text = group.relativeDay

            // Ẩn/hiện tổng thu chi nếu bằng 0
            if (group.totalIncome > 0) {
                binding.tvTotalIncome.visibility = View.VISIBLE
                binding.tvTotalIncome.text = "${formatter.format(group.totalIncome)} đ"
            } else {
                binding.tvTotalIncome.visibility = View.GONE
            }

            if (group.totalExpense > 0) {
                binding.tvTotalExpense.visibility = View.VISIBLE
                binding.tvTotalExpense.text = "${formatter.format(group.totalExpense)} đ"
            } else {
                binding.tvTotalExpense.visibility = View.GONE
            }

            // Clear view cũ trong container để tránh bị trùng lặp khi cuộn
            binding.containerTransactions.removeAllViews()

            val inflater = LayoutInflater.from(binding.root.context)

            // Dùng withIndex() để xử lý cả việc ẩn đường kẻ cuối cùng
            for ((index, item) in group.transactions.withIndex()) {
                val rowBinding = ItemTransactionHistoryBinding.inflate(
                    inflater,
                    binding.containerTransactions,
                    true
                )

                val amountStr = formatter.format(item.amount) + " đ"

                // xử lý text, icon và màu sắc cho từng loại
                when (item.type) {
                    "expense" -> {
                        rowBinding.tvCategoryName.text = item.categoryName
                        val iconResId = binding.root.context.resources.getIdentifier(
                            item.categoryIcon, "drawable", binding.root.context.packageName
                        )
                        if (iconResId != 0) rowBinding.ivIcon.setImageResource(iconResId)

                        rowBinding.tvAmount.text = amountStr
                        rowBinding.tvAmount.setTextColor(Color.parseColor("#fc565b")) // Đỏ
                    }

                    "income" -> {
                        rowBinding.tvCategoryName.text = item.categoryName
                        val iconResId = binding.root.context.resources.getIdentifier(
                            item.categoryIcon, "drawable", binding.root.context.packageName
                        )
                        if (iconResId != 0) rowBinding.ivIcon.setImageResource(iconResId)

                        rowBinding.tvAmount.text = amountStr
                        rowBinding.tvAmount.setTextColor(Color.parseColor("#46A84A")) // Xanh lá
                    }

                    "transfer" -> {
                        rowBinding.tvCategoryName.text = "Chuyển khoản"
                        rowBinding.ivIcon.setImageResource(R.drawable.ic_transfer)
                        rowBinding.tvAmount.text = amountStr
                        rowBinding.tvAmount.setTextColor(Color.parseColor("#128BEF")) // Xanh biển
                    }

                    "borrow" -> {
                        rowBinding.tvCategoryName.text = "Đi vay"
                        rowBinding.ivIcon.setImageResource(R.drawable.ic_borrow)
                        rowBinding.tvAmount.text = amountStr
                        rowBinding.tvAmount.setTextColor(Color.parseColor("#46A84A")) // Xanh lá
                    }

                    "lend" -> {
                        rowBinding.tvCategoryName.text = "Cho vay"
                        rowBinding.ivIcon.setImageResource(R.drawable.ic_loan)
                        rowBinding.tvAmount.text = amountStr
                        rowBinding.tvAmount.setTextColor(Color.parseColor("#fc565b")) // Đỏ
                    }
                }

                // Ẩn đường kẻ ở bản ghi cuối cùng của mỗi ngày
                if (index == group.transactions.size - 1) {
                    rowBinding.vDivider.visibility = View.GONE
                } else {
                    rowBinding.vDivider.visibility = View.VISIBLE
                }

                // Lắng nghe sự kiện click vào 1 dòng -> Nhảy sang màn chi tiết
                rowBinding.root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }

    class DailyDiffCallback : DiffUtil.ItemCallback<DailyTransactionGroup>() {
        override fun areItemsTheSame(
            oldItem: DailyTransactionGroup,
            newItem: DailyTransactionGroup
        ): Boolean = oldItem.dateStr == newItem.dateStr

        override fun areContentsTheSame(
            oldItem: DailyTransactionGroup,
            newItem: DailyTransactionGroup
        ): Boolean = oldItem == newItem
    }
}