package com.example.appmoni.ui.main.home.notification

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.model.notification.NotificationItem
import com.example.appmoni.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter :
    ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding =
            ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NotificationItem) {
            binding.tvNotiMessage.text = item.message

            binding.tvNotiTime.text = formatTime(item.timeInMillis)


            when (item.type) {
                "system" -> binding.ivNotiIcon.setImageResource(R.drawable.ic_congratulations)
                "reminder" -> binding.ivNotiIcon.setImageResource(R.drawable.ic_reminder)
                else -> binding.ivNotiIcon.setImageResource(R.drawable.avatar_app)
            }

        }

        private fun formatTime(timeInMillis: Long): String {
            if (DateUtils.isToday(timeInMillis)) {
                return "Hôm nay"
            }

            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            val notiDate = java.util.Calendar.getInstance().apply {
                this.timeInMillis = timeInMillis
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            val diffInMillis = today.timeInMillis - notiDate.timeInMillis
            val days = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

            return when {
                days == 1 -> "Hôm qua"
                days in 2..5 -> "$days ngày trước"
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(Date(timeInMillis))
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem) =
            oldItem == newItem
    }
}