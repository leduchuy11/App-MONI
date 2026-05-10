package com.example.appmoni.ui.main.home.manageLimit

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.limit.LimitItem
import com.example.appmoni.databinding.ItemLimitBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ManageLimitAdapter(
    private val onItemClick: (LimitItem) -> Unit,
    private val onMoreOptionsClick: (LimitItem) -> Unit
) : RecyclerView.Adapter<ManageLimitAdapter.LimitViewHolder>() {

    private var limitList = listOf<LimitItem>()

    private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun submitData(newList: List<LimitItem>) {
        limitList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LimitViewHolder {
        val binding = ItemLimitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LimitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LimitViewHolder, position: Int) {
        holder.bind(limitList[position])
    }

    override fun getItemCount(): Int = limitList.size

    inner class LimitViewHolder(private val binding: ItemLimitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LimitItem) {
            // Tên và Ngày
            binding.tvLimitName.text = item.name
            binding.tvLimitDate.text =
                "${dateFormat.format(item.startDateInMillis)} - ${dateFormat.format(item.endDateInMillis)}"

            // Tổng hạn mức
            binding.tvTotalLimitAmount.text = "%,d đ".format(item.amount).replace(',', '.')

            // Icon
            val context = binding.root.context
            val resId = context.resources.getIdentifier(item.icon, "drawable", context.packageName)
            if (resId != 0) {
                binding.ivLimitIcon.setImageResource(resId)
            } else {
                val defaultResId =
                    context.resources.getIdentifier("ic_all_in", "drawable", context.packageName)
                if (defaultResId != 0) binding.ivLimitIcon.setImageResource(defaultResId)
            }

            // LOGIC TÍNH TOÁN

            // tính ngày còn lại

            val currentCalendar =
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
            val currentDayMillis = currentCalendar.timeInMillis

            val endCalendar =
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = item.endDateInMillis
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
            val endDayMillis = endCalendar.timeInMillis

            val startCalendar =
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = item.startDateInMillis
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
            val startDayMillis = startCalendar.timeInMillis

            // Bắt đầu so sánh
            when {
                currentDayMillis > endDayMillis -> {
                    // Đã qua ngày kết thúc
                    binding.tvDaysLeft.text = "Đã hết hạn"
                    binding.tvDaysLeft.setTextColor(android.graphics.Color.parseColor("#E53935")) // Đỏ
                }

                currentDayMillis < startDayMillis -> {
                    // Chưa tới ngày bắt đầu
                    binding.tvDaysLeft.text = "Chưa bắt đầu"
                    binding.tvDaysLeft.setTextColor(android.graphics.Color.parseColor("#808080")) // Xám
                }

                currentDayMillis == endDayMillis -> {
                    // Trùng đúng ngày hôm nay
                    binding.tvDaysLeft.text = "Còn lại hôm nay"
                    binding.tvDaysLeft.setTextColor(android.graphics.Color.parseColor("#FF9800")) // Cam (Cảnh báo sắp hết)
                }

                else -> {
                    // Đang trong tiến trình -> Tính số ngày còn lại
                    val diffMillis = endDayMillis - currentDayMillis
                    val daysLeft = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                    binding.tvDaysLeft.text = "Còn $daysLeft ngày"
                    binding.tvDaysLeft.setTextColor(android.graphics.Color.parseColor("#808080")) // Xám
                }
            }

            // tính số tiền & phần trăm tiến trình
            val spentAmount = item.spentAmount
            val remainingAmount = item.amount - spentAmount

            // Tính %
            val progressPercentage = if (item.amount > 0) {
                ((spentAmount.toDouble() / item.amount.toDouble()) * 100).toInt()
            } else 0

            // Hiển thị số tiền còn lại
            if (remainingAmount >= 0) {
                binding.tvRemainingAmount.text =
                    "Còn lại: %,d đ".format(remainingAmount).replace(',', '.')
                binding.tvRemainingAmount.setTextColor(Color.parseColor("#808080")) // Xám
            } else {
                // Tiêu lố -> Đổi chữ thành Vượt mức và tô đỏ
                val overspent = kotlin.math.abs(remainingAmount)
                binding.tvRemainingAmount.text =
                    "Vượt mức: %,d đ".format(overspent).replace(',', '.')
                binding.tvRemainingAmount.setTextColor(Color.parseColor("#E53935")) // Đỏ
            }

            // Hiển thị thanh Progress
            if (progressPercentage >= 100) {
                binding.progressLimit.progress = 100
                binding.progressLimit.setIndicatorColor(Color.parseColor("#E53935")) // Thanh chạy full và đổi màu Đỏ cảnh báo
            } else {
                binding.progressLimit.progress = progressPercentage
                binding.progressLimit.setIndicatorColor(Color.parseColor("#FF9800")) // Màu cam mặc định
            }

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnMoreOptions.setOnClickListener { onMoreOptionsClick(item) }
        }
    }
}