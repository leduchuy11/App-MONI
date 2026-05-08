package com.example.appmoni.ui.main.wallet.manageSavings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.databinding.ItemManageSavingsBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManageSavingsAdapter(
    private var savingsList: List<SavingsItem>,
    private val onItemClick: (SavingsItem) -> Unit,
    private val onMenuClick: (SavingsItem) -> Unit
) : RecyclerView.Adapter<ManageSavingsAdapter.SavingsViewHolder>() {

    inner class SavingsViewHolder(private val binding: ItemManageSavingsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavingsItem, position: Int, totalSize: Int) {
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
            val iconResId = context.resources.getIdentifier(item.bankIcon, "drawable", context.packageName)
            if (iconResId != 0) {
                binding.ivBankLogo.setImageResource(iconResId)
            }

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnMoreOptions.setOnClickListener { onMenuClick(item) }

            // [MỚI THÊM] Xử lý ẩn/hiện đường kẻ
            if (position == totalSize - 1) {
                // Nếu là phần tử cuối cùng -> Ẩn đi (Dùng INVISIBLE để vẫn giữ khoảng cách margin)
                binding.vDivider.visibility = View.INVISIBLE
            } else {
                // Các phần tử khác -> Hiện bình thường
                binding.vDivider.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsViewHolder {
        val binding = ItemManageSavingsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SavingsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavingsViewHolder, position: Int) {
        holder.bind(savingsList[position], position, savingsList.size)
    }

    override fun getItemCount(): Int = savingsList.size

    fun updateData(newList: List<SavingsItem>) {
        savingsList = newList
        notifyDataSetChanged()
    }
}