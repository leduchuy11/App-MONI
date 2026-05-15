package com.example.appmoni.ui.main.report.incomeExpense

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.report.MonthlyReportItem
import com.example.appmoni.databinding.ItemMonthlyReportBinding
import java.text.DecimalFormat

class MonthlyReportAdapter(private val data: List<MonthlyReportItem>) :
    RecyclerView.Adapter<MonthlyReportAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMonthlyReportBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMonthlyReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val formatter = DecimalFormat("#,###")

        holder.binding.tvMonth.text = "Tháng ${item.month}"
        holder.binding.tvIncome.text = "${formatter.format(item.income).replace(",", ".")} đ"
        holder.binding.tvExpense.text = "${formatter.format(item.expense).replace(",", ".")} đ"

        val balance = item.income - item.expense
        holder.binding.tvBalance.text = "${formatter.format(balance).replace(",", ".")} đ"

        holder.binding.tvBalance.setTextColor(Color.parseColor("#333333"))
    }

    override fun getItemCount(): Int = data.size
}