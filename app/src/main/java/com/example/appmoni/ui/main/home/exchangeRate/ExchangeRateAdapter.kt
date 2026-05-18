package com.example.appmoni.ui.main.home.exchangeRate


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.exchangeRate.CurrencyItem
import com.example.appmoni.databinding.ItemExchangeRateBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ExchangeRateAdapter(
    private var amount: Double,
    private var baseCode: String,
    private var baseName: String,
    private var isSwapMode: Boolean
) : RecyclerView.Adapter<ExchangeRateAdapter.ViewHolder>() {

    private var currencies = listOf<CurrencyItem>()
    private val formatter = DecimalFormat("#,##0.######", DecimalFormatSymbols(Locale.getDefault()).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    })

    fun setData(newList: List<CurrencyItem>, newAmount: Double, newBase: String,newBaseName: String, swap: Boolean) {
        this.currencies = newList
        this.amount = newAmount
        this.baseCode = newBase
        this.baseName = newBaseName
        this.isSwapMode = swap
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemExchangeRateBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemExchangeRateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = currencies[position]

        holder.binding.tvCurrencySymbol.text = item.symbol
        holder.binding.tvCurrencyCode.text = item.code
        holder.binding.ivCountryFlag.setImageResource(item.flagResId)

        val converted = amount * item.rate

        holder.binding.tvConvertedAmount.text = if (amount == 0.0) "0" else formatter.format(converted)

        if (!isSwapMode) {
            // Chế độ xuôi: 1 VND ≈ 0,000038 USD
            holder.binding.tvExchangeRateFormula.text =
                "1 $baseCode ≈ ${formatter.format(item.rate)} ${item.name}"
        } else {
            // Chế độ ngược: 1 USD ≈ 25.400 VND
            val reverseRate = if (item.rate != 0.0) 1.0 / item.rate else 0.0
            holder.binding.tvExchangeRateFormula.text = "1 ${item.code} ≈ ${formatter.format(reverseRate)} $baseName"
        }
    }

    override fun getItemCount() = currencies.size
}