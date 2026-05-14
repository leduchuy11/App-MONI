package com.example.appmoni.ui.main.report.debtTracking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.databinding.ItemDebtTrackingBinding
import java.text.DecimalFormat

class DebtTrackingAdapter(
    private val onItemClick: (TransactionItem) -> Unit
) : ListAdapter<TransactionItem, DebtTrackingAdapter.DebtViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val binding = ItemDebtTrackingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DebtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class DebtViewHolder(private val binding: ItemDebtTrackingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionItem) {
            val formatter = DecimalFormat("#,###")

            binding.tvPersonName.text = item.personName.ifEmpty { "Khách vãng lai" }

            val initial = item.personName.trim().firstOrNull()?.toString()?.uppercase() ?: "?"
            binding.tvAvatarInitial.text = initial

            binding.tvAmount.text = "${formatter.format(item.amount)} đ".replace(",", ".")

            binding.layoutItem.setOnClickListener {
                onItemClick(item)
            }

            binding.ivMoreOptions.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionItem>() {
        override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TransactionItem,
            newItem: TransactionItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}