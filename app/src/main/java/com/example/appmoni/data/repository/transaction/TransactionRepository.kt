package com.example.appmoni.data.repository.transaction

import com.example.appmoni.data.model.transaction.TransactionItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TransactionRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveTransaction(transaction: TransactionItem): Result<Unit> {
        return try {
            val batch = db.batch()

            // 1. Reference cho document giao dịch mới
            val transactionRef = db.collection("users")
                .document(transaction.userId)
                .collection("transactions")
                .document()

            transaction.id = transactionRef.id // Gắn ID ngược lại vào object

            // Thêm lệnh tạo giao dịch vào batch
            batch.set(transactionRef, transaction)

            // 2. Reference đến bộ sưu tập Ví của user này
            val userWalletsRef =
                db.collection("users").document(transaction.userId).collection("wallets")

            // 3. Thêm lệnh cập nhật số dư ví vào batch tùy theo loại giao dịch
            when (transaction.type) {
                "expense", "lend" -> {
                    // Trừ tiền ví nguồn
                    val walletRef = userWalletsRef.document(transaction.walletId)
                    batch.update(walletRef, "balance", FieldValue.increment(-transaction.amount))
                }

                "income", "borrow" -> {
                    // Cộng tiền ví đích
                    val walletRef = userWalletsRef.document(transaction.walletId)
                    batch.update(walletRef, "balance", FieldValue.increment(transaction.amount))
                }

                "transfer" -> {
                    // Trừ tiền ví nguồn
                    val sourceRef = userWalletsRef.document(transaction.walletId)
                    batch.update(sourceRef, "balance", FieldValue.increment(-transaction.amount))

                    // Cộng tiền ví đích
                    val destRef = userWalletsRef.document(transaction.destWalletId)
                    batch.update(destRef, "balance", FieldValue.increment(transaction.amount))
                }
            }

            // 4. THỰC THI TẤT CẢ CÁC LỆNH CÙNG LÚC
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}