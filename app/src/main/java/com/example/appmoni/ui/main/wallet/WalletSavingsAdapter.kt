package com.example.appmoni.ui.main.wallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.databinding.ItemWalletSavingsBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WalletSavingsAdapter(
    private var savingsList: List<SavingsItem>,
    private val onItemClick: (SavingsItem) -> Unit
) : RecyclerView.Adapter<WalletSavingsAdapter.SavingsViewHolder>() {

    inner class SavingsViewHolder(val binding: ItemWalletSavingsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavingsItem) {
            binding.tvSavingsName.text = item.name
            binding.tvInterestRate.text = "${item.interestRate}%"

            val formatter = DecimalFormat("#,###")
            val formattedAmount = formatter.format(item.amount).replace(",", ".") + " đ"
            binding.tvSavingsBalance.text = formattedAmount

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = item.depositDateInMillis
            calendar.add(Calendar.MONTH, item.termMonths)

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvSavingsStatus.text = "Đáo hạn: ${sdf.format(calendar.time)}"

            val context = binding.root.context
            val iconResId =
                context.resources.getIdentifier(item.bankIcon, "drawable", context.packageName)
            if (iconResId != 0) {
                binding.ivBankLogo.setImageResource(iconResId)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsViewHolder {
        val binding =
            ItemWalletSavingsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SavingsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavingsViewHolder, position: Int) {
        holder.bind(savingsList[position])
    }

    override fun getItemCount(): Int = savingsList.size

    fun updateData(newList: List<SavingsItem>) {
        savingsList = newList
        notifyDataSetChanged()
    }
}