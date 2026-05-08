package com.example.appmoni.data.repository.wallet

import com.example.appmoni.data.model.wallet.WalletItem
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source

class WalletRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. Lấy danh sách ví
    fun listenToWalletsByType(
        userId: String,
        type: String,
        onResult: (List<WalletItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users").document(userId)
            .collection("wallets")
            .whereEqualTo("type", type)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    // Dữ liệu trong Local Cache thay đổi là nó tự đẩy lên UI ngay
                    val list = snapshots.toObjects(WalletItem::class.java)
                    onResult(list)
                }
            }
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

    // 4. Cập nhật thông tin ví và đồng bộ lịch sử giao dịch
    fun updateWallet(userId: String, wallet: WalletItem) {
        // Cập nhật thông tin bản thân cái ví
        db.collection("users").document(userId)
            .collection("wallets").document(wallet.id)
            .set(wallet)

        // Đi tìm các giao dịch cũ (Ví nguồn) ngay trong bộ nhớ tạm
        db.collection("users").document(userId).collection("transactions")
            .whereEqualTo("walletId", wallet.id)
            .get(Source.CACHE)
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    val batch = db.batch()
                    for (doc in snapshots.documents) {
                        batch.update(
                            doc.reference,
                            "walletName", wallet.name,
                            "walletIcon", wallet.iconName
                        )
                    }
                    batch.commit() // Firebase tự xử lý: offline thì lưu tạm, online thì đẩy lên
                }
            }

        // Đi tìm các giao dịch Chuyển khoản (Ví đích) ngay trong bộ nhớ tạm
        db.collection("users").document(userId).collection("transactions")
            .whereEqualTo("destWalletId", wallet.id)
            .get(Source.CACHE)
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    val batch = db.batch()
                    for (doc in snapshots.documents) {
                        batch.update(
                            doc.reference,
                            "destWalletName", wallet.name,
                            "destWalletIcon", wallet.iconName
                        )
                    }
                    batch.commit()
                }
            }

        // Đi tìm các Sổ tiết kiệm có nguồn tiền từ ví này để cập nhật lại tên ví
        db.collection("users").document(userId).collection("savings")
            .whereEqualTo("sourceWalletId", wallet.id)
            .get(Source.CACHE)
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    val batch = db.batch()
                    for (doc in snapshots.documents) {
                        batch.update(
                            doc.reference,
                            "sourceWalletName", wallet.name
                        )
                    }
                    batch.commit()
                }
            }
    }

    // 5. Ngưng sử dụng 1 ví
    fun archiveWallet(userId: String, walletId: String): Task<Void> {
        return db.collection("users").document(userId)
            .collection("wallets").document(walletId)
            .update("isActive", false) // Chỉ cập nhật đúng trường isActive
    }

    // 6. Hàm kiểm tra xem ví này đã có giao dịch nào chưa
    fun checkHasTransactions(
        userId: String,
        walletId: String,
        onResult: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val transactionsRef = db.collection("users").document(userId).collection("transactions")

        transactionsRef.whereEqualTo("walletId", walletId).limit(1).get()
            .addOnSuccessListener { srcSnapshot ->
                if (!srcSnapshot.isEmpty) {
                    onResult(true)
                } else {
                    transactionsRef.whereEqualTo("destWalletId", walletId).limit(1).get()
                        .addOnSuccessListener { destSnapshot ->
                            onResult(!destSnapshot.isEmpty)
                        }
                        .addOnFailureListener { e -> onError(e) }
                }
            }
            .addOnFailureListener { e -> onError(e) }
    }
}