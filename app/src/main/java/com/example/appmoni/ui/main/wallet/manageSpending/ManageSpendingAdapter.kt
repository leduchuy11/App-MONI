package com.example.appmoni.ui.main.wallet.manageSpending

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.databinding.ItemManageSpendingBinding
import java.text.DecimalFormat

class ManageSpendingAdapter(
    private var wallets: List<WalletItem>,
    // Callback này sẽ báo cho Fragment biết: Người dùng vừa bấm vào dấu 3 chấm của Ví này, mở Menu lên
    private val onOptionClick: (WalletItem, View) -> Unit
) : RecyclerView.Adapter<ManageSpendingAdapter.WalletViewHolder>() {

    inner class WalletViewHolder(val binding: ItemManageSpendingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(wallet: WalletItem) {
            binding.tvWalletName.text = wallet.name

            // Format số tiền (Ví dụ: 1000000 -> 1.000.000 đ)
            val formatter = DecimalFormat("#,###")
            val formattedBalance = formatter.format(wallet.balance).replace(",", ".") + " đ"
            binding.tvWalletBalance.text = formattedBalance

            // Đổ icon
            val iconRes = wallet.getIconResource(binding.root.context)
            if (iconRes != 0) {
                binding.ivWalletIcon.setImageResource(iconRes)
            } else {
                // Nếu lỗi không tìm thấy icon thì set tạm một icon mặc định
                binding.ivWalletIcon.setImageResource(com.example.appmoni.R.drawable.ic_money)
            }

            //Lắng nghe sự kiện bấm vào nút 3 chấm
            binding.btnOptions.setOnClickListener {
                // Truyền cục data của ví hiện tại và cái View (nút 3 chấm) ra ngoài cho Fragment xử lý
                onOptionClick(wallet, it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val binding = ItemManageSpendingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(wallets[position])
    }

    override fun getItemCount(): Int = wallets.size

    // Hàm này để ViewModel gọi khi có dữ liệu mới từ Firebase tải về
    fun updateData(newList: List<WalletItem>) {
        wallets = newList
        notifyDataSetChanged()
    }
}