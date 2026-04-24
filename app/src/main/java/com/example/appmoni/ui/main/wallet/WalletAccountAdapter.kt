package com.example.appmoni.ui.main.wallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.databinding.ItemWalletAccountBinding
import java.text.DecimalFormat

class WalletAccountAdapter(
    private var walletList: List<WalletItem>,
    private val onItemClick: (WalletItem) -> Unit
) : RecyclerView.Adapter<WalletAccountAdapter.WalletViewHolder>() {

    inner class WalletViewHolder(val binding: ItemWalletAccountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(wallet: WalletItem) {
            // Đổ tên ví
            binding.tvAccountName.text = wallet.name

            // Đổ số dư (có định dạng dấu chấm)
            val formatter = DecimalFormat("#,###")
            val formattedBalance = formatter.format(wallet.balance).replace(",", ".") + " đ"
            binding.tvAccountBalance.text = formattedBalance

            // Đổ Icon
            val iconResId = binding.root.context.resources.getIdentifier(
                wallet.iconName, "drawable", binding.root.context.packageName
            )
            if (iconResId != 0) {
                binding.ivAccountIcon.setImageResource(iconResId)
            }

            // Bắt sự kiện click vào nguyên cái Card (để chuyển trang)
            binding.root.setOnClickListener {
                onItemClick(wallet)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val binding = ItemWalletAccountBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(walletList[position])
    }

    override fun getItemCount(): Int = walletList.size

    fun updateData(newList: List<WalletItem>) {
        walletList = newList
        notifyDataSetChanged()
    }
}