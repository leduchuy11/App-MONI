package com.example.appmoni.ui.main.home.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.appmoni.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "daily_reminder_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo Channel cho Android 8.0 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở ghi chép",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("Moni nhắc bạn")
            .setContentText("Đừng quên ghi chép lại chi tiêu ngày hôm nay nhé!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())

        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val notiItem = com.example.appmoni.data.model.notification.NotificationItem(
                message = "Đừng quên ghi chép lại chi tiêu ngày hôm nay nhé!",
                timeInMillis = System.currentTimeMillis(),
                type = "reminder",
                isRead = false
            )
            db.collection("users").document(userId).collection("notifications").add(notiItem)
        }
        AlarmScheduler.scheduleDailyReminders(context)
    }
}