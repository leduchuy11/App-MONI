package com.example.appmoni.ui.main.report.incomeAnalysis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.analysis.AnalysisItem
import com.example.appmoni.databinding.ItemIncomeAnalysisBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class IncomeAnalysisAdapter : RecyclerView.Adapter<IncomeAnalysisAdapter.ViewHolder>() {

    private var list = listOf<AnalysisItem>()

    private val formatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = '.'
    })

    fun setData(newList: List<AnalysisItem>) {
        this.list = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemIncomeAnalysisBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemIncomeAnalysisBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.binding.tvDateLabel.text = item.timeLabel
        holder.binding.tvAmount.text = "${formatter.format(item.amount)} đ"
    }

    override fun getItemCount() = list.size
}