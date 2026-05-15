package com.example.appmoni.ui.main.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.data.model.limit.LimitItem
import com.example.appmoni.databinding.ItemLimitHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HomeLimitAdapter(private val onItemClick: (LimitItem) -> Unit) :
    RecyclerView.Adapter<HomeLimitAdapter.LimitViewHolder>() {

    private var limits = listOf<LimitItem>()
    private val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun submitData(newList: List<LimitItem>) {
        limits = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LimitViewHolder {
        val binding =
            ItemLimitHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LimitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LimitViewHolder, position: Int) {
        holder.bind(limits[position])
    }

    override fun getItemCount() = limits.size

    inner class LimitViewHolder(private val binding: ItemLimitHomeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(limit: LimitItem) {
            binding.tvLimitName.text = limit.name
            binding.tvTotalLimit.text = "%,d đ".format(limit.amount).replace(',', '.')
            binding.tvSpentAmount.text = "%,d đ".format(limit.spentAmount).replace(',', '.')

            val startStr = dateFormatter.format(limit.startDateInMillis)
            val endStr = dateFormatter.format(limit.endDateInMillis)
            binding.tvLimitPeriod.text = "$startStr - $endStr"

            val context = binding.root.context
            val resId = context.resources.getIdentifier(limit.icon, "drawable", context.packageName)
            if (resId != 0) {
                binding.ivLimitIcon.setImageResource(resId)
            } else {
                val defaultResId = context.resources.getIdentifier("ic_all_in", "drawable", context.packageName)
                if (defaultResId != 0) binding.ivLimitIcon.setImageResource(defaultResId)
            }

            val currentCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val currentDayMillis = currentCalendar.timeInMillis

            val endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = limit.endDateInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endDayMillis = endCalendar.timeInMillis

            val startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = limit.startDateInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startDayMillis = startCalendar.timeInMillis

            binding.ivExpiredIcon.visibility = android.view.View.GONE

            when {
                currentDayMillis > endDayMillis -> {
                    binding.ivExpiredIcon.visibility = android.view.View.VISIBLE

                    binding.tvDaysLeft.text = " Đã hết hạn"
                    binding.tvDaysLeft.setTextColor(Color.parseColor("#E53935")) // Màu Đỏ
                }
                currentDayMillis < startDayMillis -> {
                    // Chưa tới ngày bắt đầu
                    binding.tvDaysLeft.text = "Chưa bắt đầu"
                    binding.tvDaysLeft.setTextColor(Color.parseColor("#808080")) // Màu Xám
                }
                currentDayMillis == endDayMillis -> {
                    // Trùng đúng ngày hôm nay
                    binding.tvDaysLeft.text = "Còn lại hôm nay"
                    binding.tvDaysLeft.setTextColor(Color.parseColor("#FF9800")) // Màu Cam cảnh báo
                }
                else -> {
                    // Đang trong tiến trình -> Tính số ngày còn lại
                    val diffMillis = endDayMillis - currentDayMillis
                    val daysLeft = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                    binding.tvDaysLeft.text = "Còn $daysLeft ngày"
                    binding.tvDaysLeft.setTextColor(Color.parseColor("#808080")) // Màu Xám
                }
            }

            val progressPercentage = if (limit.amount > 0) {
                ((limit.spentAmount.toDouble() / limit.amount.toDouble()) * 100).toInt()
            } else 0

            if (progressPercentage >= 100) {
                binding.progressLimit.progress = 100
                binding.progressLimit.setIndicatorColor(Color.parseColor("#E53935")) // Đỏ cảnh báo
            } else {
                binding.progressLimit.progress = progressPercentage
                binding.progressLimit.setIndicatorColor(Color.parseColor("#FF9800")) // Cam an toàn
            }

            // Sự kiện click
            binding.root.setOnClickListener { onItemClick(limit) }
        }
    }
}