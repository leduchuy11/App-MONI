package com.example.appmoni.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appmoni.data.model.transaction.TransactionItem

@Dao
interface TransactionDao {

    //1.  THÊM HOẶC CẬP NHẬT GIAO DỊCH
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionItem)

    // 2. LẤY DANH SÁCH GIAO DỊCH
    @Query("SELECT * FROM transactions WHERE userId = :uid ORDER BY dateInMillis DESC")
    fun getAllTransactions(uid: String): LiveData<List<TransactionItem>>

    // 3. XÓA GIAO DỊCH
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    // 4. LÀM SẠCH (Dùng khi đăng xuất)
    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    // 5. Lấy tổng số tiền chi của 1 user, trong 1 khoảng thời gian, và thuộc 1 nhóm danh mục nhất định
    @Query(
        """
        SELECT SUM(amount) FROM transactions 
        WHERE userId = :userId 
        AND type = 'expense' 
        AND dateInMillis >= :startDate 
        AND dateInMillis <= :endDate 
        AND categoryId IN (:categoryIds)
    """
    )
    suspend fun getTotalExpenseForLimit(
        userId: String,
        startDate: Long,
        endDate: Long,
        categoryIds: List<String>
    ): Long?

    // 6. Hàm tính tổng nếu chọn "Tất cả hạng mục"
    @Query(
        """
        SELECT SUM(amount) FROM transactions 
        WHERE userId = :userId 
        AND type = 'expense' 
        AND dateInMillis >= :startDate 
        AND dateInMillis <= :endDate
    """
    )
    suspend fun getTotalExpenseForAllCategories(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Long?

    // 7. Lấy toàn bộ giao dịch của một Hạn mức (Lọc theo danh sách Category)
    @Query(
        """
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND type = 'expense' 
        AND dateInMillis >= :startDate 
        AND dateInMillis <= :endDate 
        AND categoryId IN (:categoryIds)
        ORDER BY dateInMillis ASC
    """
    )
    suspend fun getTransactionsForLimit(
        userId: String,
        startDate: Long,
        endDate: Long,
        categoryIds: List<String>
    ): List<TransactionItem>

    // 8. Lấy toàn bộ giao dịch (Dùng khi người dùng chọn "Tất cả hạng mục")
    @Query(
        """
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND type = 'expense' 
        AND dateInMillis >= :startDate 
        AND dateInMillis <= :endDate
        ORDER BY dateInMillis ASC
    """
    )
    suspend fun getTransactionsForAllCategories(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<TransactionItem>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND type = 'income' 
        AND dateInMillis >= :startDate 
        AND dateInMillis <= :endDate
        ORDER BY dateInMillis ASC
    """
    )
    suspend fun getAllTransactionsIncome(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<TransactionItem>

    // Lấy tất cả giao dịch trong khoảng thời gian
    @Query(
        """
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND dateInMillis >= :startDate 
        AND dateInMillis <= :endDate
        ORDER BY dateInMillis ASC
    """
    )
    suspend fun getTransactionsForExport(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<TransactionItem>

}