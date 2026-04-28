package com.example.appmoni.data.repository.transaction

import androidx.lifecycle.LiveData
import com.example.appmoni.data.local.TransactionDao
import com.example.appmoni.data.model.transaction.TransactionItem
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TransactionRepository(private val transactionDao: TransactionDao) {
    private val db = FirebaseFirestore.getInstance()

    // Hàm kéo dữ liệu từ room lên màn hình lịch sử ghi chép
    fun getAllTransactions(userId: String): LiveData<List<TransactionItem>> {
        return transactionDao.getAllTransactions(userId)
    }

    suspend fun saveTransaction(transaction: TransactionItem): Result<Unit> {
        return try {
            val batch = db.batch()

            val transactionRef = db.collection("users")
                .document(transaction.userId)
                .collection("transactions")
                .document()

            transaction.id = transactionRef.id

            transactionDao.insertTransaction(transaction)

            batch.set(transactionRef, transaction)

            val userWalletsRef =
                db.collection("users").document(transaction.userId).collection("wallets")
            when (transaction.type) {
                "expense", "lend" -> {
                    val walletRef = userWalletsRef.document(transaction.walletId)
                    batch.update(walletRef, "balance", FieldValue.increment(-transaction.amount))
                }

                "income", "borrow" -> {
                    val walletRef = userWalletsRef.document(transaction.walletId)
                    batch.update(walletRef, "balance", FieldValue.increment(transaction.amount))
                }

                "transfer" -> {
                    val sourceRef = userWalletsRef.document(transaction.walletId)
                    batch.update(sourceRef, "balance", FieldValue.increment(-transaction.amount))

                    val destRef = userWalletsRef.document(transaction.destWalletId)
                    batch.update(destRef, "balance", FieldValue.increment(transaction.amount))
                }
            }

            // firebase commit nhưng không await (Để tự động dùng hàng đợi Offline)
            batch.commit()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // HÀM NÀY ĐỂ LẮNG NGHE ĐỒNG BỘ TỪ FIREBASE VỀ ROOM
    fun startListeningToFirebase(userId: String) {
        db.collection("users").document(userId).collection("transactions")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    val transaction = dc.document.toObject(TransactionItem::class.java)

                    CoroutineScope(Dispatchers.IO).launch {
                        when (dc.type) {
                            // Nếu Firebase có thêm mới hoặc sửa -> Lưu/Đè vào Room
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                transactionDao.insertTransaction(transaction)
                            }
                            // Nếu Firebase bị xóa -> Xóa tương ứng trong Room
                            DocumentChange.Type.REMOVED -> {
                                transactionDao.deleteTransaction(transaction.id)
                            }
                        }
                    }
                }
            }
    }

    // HÀM XÓA GIAO DỊCH VÀ ĐẢO NGƯỢC SỐ DƯ
    suspend fun deleteTransaction(transaction: TransactionItem): Result<Unit> {
        return try {
            transactionDao.deleteTransaction(transaction.id)

            val batch = db.batch()

            val transactionRef = db.collection("users")
                .document(transaction.userId)
                .collection("transactions")
                .document(transaction.id)

            batch.delete(transactionRef)

            val userWalletsRef =
                db.collection("users").document(transaction.userId).collection("wallets")

            when (transaction.type) {
                "expense", "lend" -> {
                    val walletRef = userWalletsRef.document(transaction.walletId)
                    batch.update(walletRef, "balance", FieldValue.increment(transaction.amount))
                }

                "income", "borrow" -> {
                    val walletRef = userWalletsRef.document(transaction.walletId)
                    batch.update(walletRef, "balance", FieldValue.increment(-transaction.amount))
                }

                "transfer" -> {
                    val sourceRef = userWalletsRef.document(transaction.walletId)
                    batch.update(sourceRef, "balance", FieldValue.increment(transaction.amount))

                    val destRef = userWalletsRef.document(transaction.destWalletId)
                    batch.update(destRef, "balance", FieldValue.increment(-transaction.amount))
                }
            }

            // firebase commit nhưng không await (Để tự động dùng hàng đợi Offline)
            batch.commit()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}