package com.example.appmoni.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appmoni.data.model.transaction.TransactionItem

@Dao
interface TransactionDao {

    // THÊM HOẶC CẬP NHẬT GIAO DỊCH
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionItem)

    // LẤY DANH SÁCH GIAO DỊCH
    @Query("SELECT * FROM transactions WHERE userId = :uid ORDER BY dateInMillis DESC")
    fun getAllTransactions(uid: String): LiveData<List<TransactionItem>>

    // XÓA GIAO DỊCH
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    // 4. LÀM SẠCH KHO (Dùng khi đăng xuất)
    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}