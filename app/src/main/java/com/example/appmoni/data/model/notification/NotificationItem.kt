package com.example.appmoni.data.model.notification

data class NotificationItem(
    val id: String = "",
    val message: String = "",
    val timeInMillis: Long = 0L,
    val type: String = "system", // "system", "export", "reminder", "promo"
    var isRead: Boolean = false
)