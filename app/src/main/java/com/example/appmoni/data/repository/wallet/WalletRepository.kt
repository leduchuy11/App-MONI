package com.example.appmoni.data.repository.wallet

import com.example.appmoni.data.model.wallet.WalletItem
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class WalletRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. Lấy danh sách ví
    fun getWalletsByType(userId: String, type: String): Task<QuerySnapshot> {
        return db.collection("users").document(userId)
            .collection("wallets")
            .whereEqualTo("type", type)
            .get()
    }

    // 2. Xóa một ví dựa vào ID
    fun deleteWallet(userId: String, walletId: String): Task<Void> {
        return db.collection("users").document(userId)
            .collection("wallets").document(walletId).delete()
    }

    // 3. Thêm một ví mới
    fun addWallet(userId: String, wallet: WalletItem): Task<Void> {
        // Tạo trước một Document rỗng để lấy ID ngẫu nhiên
        val docRef = db.collection("users").document(userId).collection("wallets").document()
        // Gán cái ID đó ngược lại vào object của chúng ta
        wallet.id = docRef.id
        // Bắt đầu lưu toàn bộ cục data lên mây
        return docRef.set(wallet)
    }
}