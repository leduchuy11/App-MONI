package com.example.appmoni.data.repository.limit


import com.example.appmoni.data.model.limit.LimitItem
import com.google.firebase.firestore.FirebaseFirestore

class LimitRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun addLimit(limit: LimitItem) {
        val limitRef = db.collection("users").document(limit.userId).collection("limits").document()
        limit.id = limitRef.id

        limitRef.set(limit)
    }

    fun getLimitsRealtime(
        userId: String,
        onUpdate: (List<LimitItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users").document(userId).collection("limits")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }
                val list = snapshots?.toObjects(LimitItem::class.java) ?: emptyList()
                onUpdate(list)
            }
    }

    fun deleteLimit(userId: String, limitId: String) {
        db.collection("users").document(userId).collection("limits").document(limitId).delete()
    }

    fun updateLimit(limit: LimitItem) {
        db.collection("users").document(limit.userId).collection("limits").document(limit.id)
            .set(limit)
    }
}