package com.example.appmoni.viewmodel.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.model.notification.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationViewModel : ViewModel() {

    private val _notifications = MutableLiveData<List<NotificationItem>>()
    val notifications: LiveData<List<NotificationItem>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadNotifications(userId: String) {
        _isLoading.value = true
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("notifications")
            .orderBy("timeInMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                    }
                    _notifications.value = list
                }
            }
    }

    fun markAsRead(userId: String, notificationId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .update("isRead", true)
    }
}