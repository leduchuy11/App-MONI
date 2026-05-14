package com.example.appmoni.ui.main.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.databinding.ItemDebtHomeBinding
import java.text.DecimalFormat

class HomeDebtAdapter(
    private val onItemClick: (TransactionItem) -> Unit
) : ListAdapter<TransactionItem, HomeDebtAdapter.DebtViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val binding = ItemDebtHomeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DebtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DebtViewHolder(private val binding: ItemDebtHomeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionItem) {
            val formatter = DecimalFormat("#,###")
            binding.tvDebtAmount.text = "${formatter.format(item.amount)} đ".replace(",", ".")

            val person = item.personName.ifEmpty { "người khác" }

            if (item.type == "lend") {
                // Cho vay (Màu đỏ)
                binding.tvDebtType.text = "Cho vay"
                binding.tvDebtName.text = "Cho $person vay"
                binding.tvDebtAmount.setTextColor(Color.parseColor("#ec453f"))
                binding.ivDebtIcon.setImageResource(R.drawable.ic_loan)
            } else {
                // Đi vay (Màu xanh lá)
                binding.tvDebtType.text = "Đi vay"
                binding.tvDebtName.text = "Vay $person"
                binding.tvDebtAmount.setTextColor(Color.parseColor("#1aa349"))
                binding.ivDebtIcon.setImageResource(R.drawable.ic_borrow)
            }

            binding.layoutItem.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionItem>() {
        override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem) = oldItem == newItem
    }
}