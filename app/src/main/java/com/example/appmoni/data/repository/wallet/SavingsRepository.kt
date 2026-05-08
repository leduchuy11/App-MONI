package com.example.appmoni.data.repository.wallet

import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.data.model.wallet.SavingsItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class SavingsRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun saveSavingsAndTransaction(
        savings: SavingsItem,
        transaction: TransactionItem?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            val batch = db.batch()

            val savingsRef = db.collection("users").document(savings.userId)
                .collection("savings").document(savings.id)
            batch.set(savingsRef, savings)

            // Nếu CÓ chọn ví nguồn thì lưu Giao dịch và Trừ tiền
            if (transaction != null && savings.sourceWalletId.isNotEmpty()) {
                // Lưu giao dịch
                val transactionRef = db.collection("users").document(savings.userId)
                    .collection("transactions").document(transaction.id)
                batch.set(transactionRef, transaction)

                // Trừ tiền ví nguồn
                val sourceWalletRef = db.collection("users").document(savings.userId)
                    .collection("wallets").document(savings.sourceWalletId)
                batch.update(sourceWalletRef, "balance", FieldValue.increment(-savings.amount))
            }

            batch.commit().addOnFailureListener { e ->
            }
            onComplete(true, null)

        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }

    fun getSavingsList(userId: String, onResult: (List<SavingsItem>?) -> Unit) {
        db.collection("users").document(userId).collection("savings")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(null)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(SavingsItem::class.java) }
                onResult(list)
            }
    }

    fun deleteSavings(savings: SavingsItem, onComplete: (Boolean, String?) -> Unit) {
        try {
            // 1. Tìm giao dịch trích tiền gốc (Dựa vào ID sổ và Tên danh mục)
            db.collection("users").document(savings.userId)
                .collection("transactions")
                .whereEqualTo("destWalletId", savings.id)
                .whereEqualTo("categoryName", "Gửi tiết kiệm")
                .limit(1)
                .get(Source.CACHE)
                .addOnSuccessListener { snapshots ->
                    val batch = db.batch()

                    // Bước 2: Xóa sổ tiết kiệm
                    val savingsRef = db.collection("users").document(savings.userId)
                        .collection("savings").document(savings.id)
                    batch.delete(savingsRef)

                    // Bước 3: Nếu tìm thấy giao dịch, tiến hành Xóa và Hoàn tiền
                    if (!snapshots.isEmpty) {
                        val transactionDoc = snapshots.documents[0]
                        val transactionAmount = transactionDoc.getLong("amount") ?: 0L
                        val sourceWalletId = transactionDoc.getString("walletId") ?: ""

                        // Xóa giao dịch đó khỏi lịch sử
                        batch.delete(transactionDoc.reference)

                        // Cộng ngược lại tiền vào ví nguồn
                        if (sourceWalletId.isNotEmpty()) {
                            val walletRef = db.collection("users").document(savings.userId)
                                .collection("wallets").document(sourceWalletId)
                            batch.update(
                                walletRef,
                                "balance",
                                FieldValue.increment(transactionAmount)
                            )
                        }
                    }

                    // Bước 4: Thực thi Batch
                    batch.commit().addOnFailureListener {
                    }
                    onComplete(true, null)
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.message)
                }

        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }

    fun updateSavings(
        oldSavings: SavingsItem,
        newSavings: SavingsItem,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            // Tính toán sự chênh lệch tiền: Tiền mới - Tiền cũ
            val amountDiff = newSavings.amount - oldSavings.amount

            // TRƯỜNG HỢP 1: Có thay đổi số tiền và sổ này có ví nguồn
            if (amountDiff != 0L && oldSavings.sourceWalletId.isNotEmpty()) {
                val walletRef = db.collection("users").document(oldSavings.userId)
                    .collection("wallets").document(oldSavings.sourceWalletId)

                if (amountDiff > 0) {
                    // Nếu tiền gửi tăng thêm -> Check xem ví có đủ tiền không
                    walletRef.get(Source.CACHE).addOnSuccessListener { walletDoc ->
                        val currentBalance = walletDoc.getLong("balance") ?: 0L
                        if (currentBalance < amountDiff) {
                            val formatter = java.text.DecimalFormat("#,###")
                            onComplete(
                                false,
                                "Số dư ví nguồn không đủ để nạp thêm ${formatter.format(amountDiff)} đ!"
                            )
                        } else {
                            // Đủ tiền -> Tiến hành cập nhật
                            commitSavingsUpdateBatch(oldSavings, newSavings, amountDiff, onComplete)
                        }
                    }.addOnFailureListener { e ->
                        onComplete(false, "Lỗi kiểm tra ví: ${e.message}")
                    }
                } else {
                    // Nếu tiền gửi giảm đi -> Tăng tiền ở ví
                    commitSavingsUpdateBatch(oldSavings, newSavings, amountDiff, onComplete)
                }
            } else {
                // TRƯỜNG HỢP 2: Không đổi tiền hoặc sổ không có ví nguồn -> Chỉ cần sửa thông tin sổ
                val batch = db.batch()
                val savingsRef = db.collection("users").document(newSavings.userId)
                    .collection("savings").document(newSavings.id)
                batch.set(savingsRef, newSavings)

                batch.commit()
                onComplete(true, null)
            }
        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }

    // Hàm Đóng gói 3 thao tác (Sửa sổ + Trừ/Cộng tiền Ví + Sửa lịch sử Giao dịch) vào 1 Batch
    private fun commitSavingsUpdateBatch(
        oldSavings: SavingsItem,
        newSavings: SavingsItem,
        amountDiff: Long,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val batch = db.batch()
        val userId = newSavings.userId

        // Thao tác 1: Cập nhật thông tin Sổ tiết kiệm
        val savingsRef =
            db.collection("users").document(userId).collection("savings").document(newSavings.id)
        batch.set(savingsRef, newSavings)

        // Thao tác 2: Cập nhật số dư Ví nguồn
        // Nếu Sổ tăng (diff > 0) -> Ví trừ đi. Nếu Sổ giảm (diff < 0) -> Ví cộng vào.
        val walletRef = db.collection("users").document(userId).collection("wallets")
            .document(oldSavings.sourceWalletId)
        batch.update(walletRef, "balance", FieldValue.increment(-amountDiff))

        // Thao tác 3: Tìm Giao dịch gốc và cập nhật lại số tiền
        db.collection("users").document(userId).collection("transactions")
            .whereEqualTo("destWalletId", oldSavings.id)
            .whereEqualTo("categoryName", "Gửi tiết kiệm")
            .limit(1)
            .get(Source.CACHE)
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    val transactionDoc = snapshots.documents[0]
                    batch.update(transactionDoc.reference, "amount", newSavings.amount)
                }

                batch.commit().addOnFailureListener { }
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, "Lỗi khi cập nhật giao dịch: ${e.message}")
            }
    }

    // Tất tooans sổ tiết kiệm
    fun settleSavings(
        savingsId: String,
        userId: String,
        receiveWalletId: String,
        principalTransaction: TransactionItem,
        interestTransaction: TransactionItem?,
        totalAmountToAdd: Long,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            val batch = db.batch()

            // 1. Cập nhật trạng thái sổ thành "notActive"
            val savingsRef = db.collection("users").document(userId)
                .collection("savings").document(savingsId)
            batch.update(savingsRef, "status", "notActive")

            // 2. Cộng tổng tiền (Gốc + Lãi)
            val walletRef = db.collection("users").document(userId)
                .collection("wallets").document(receiveWalletId)
            batch.update(walletRef, "balance", FieldValue.increment(totalAmountToAdd))

            // 3. Ghi nhận Giao dịch 1: Chuyển khoản (Hoàn tiền gốc)
            val principalTxRef = db.collection("users").document(userId)
                .collection("transactions").document(principalTransaction.id)
            batch.set(principalTxRef, principalTransaction)

            // 4. Ghi nhận Giao dịch 2: Thu nhập (Tiền lãi tiết kiệm)
            if (interestTransaction != null) {
                val interestTxRef = db.collection("users").document(userId)
                    .collection("transactions").document(interestTransaction.id)
                batch.set(interestTxRef, interestTransaction)
            }

            batch.commit().addOnFailureListener {  }
            onComplete(true, null)

        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }
}