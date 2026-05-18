package com.example.appmoni.ui.main.home.exchangeRate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.exchangeRate.CurrencyItem
import com.example.appmoni.databinding.ItemCurrencySelectionBinding

class CurrencySelectionAdapter(
    private var selectedCode: String,
    private val onItemClick: (CurrencyItem) -> Unit
) : RecyclerView.Adapter<CurrencySelectionAdapter.ViewHolder>() {

    private var list = listOf<CurrencyItem>()

    fun setData(newList: List<CurrencyItem>) {
        this.list = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemCurrencySelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClick(list[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCurrencySelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvName.text = item.name
        holder.binding.tvCode.text = item.code
        holder.binding.tvSymbol.text = item.symbol
        holder.binding.ivFlag.setImageResource(item.flagResId)

        // Nếu trùng mã đang chọn thì hiện dấu tích, ngược lại thì ẩn
        if (item.code == selectedCode) {
            holder.binding.ivCheck.visibility = View.VISIBLE
        } else {
            holder.binding.ivCheck.visibility = View.GONE
        }
    }

    override fun getItemCount() = list.size
}