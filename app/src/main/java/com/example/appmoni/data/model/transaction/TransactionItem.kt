package com.example.appmoni.data.model.transaction

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Entity(tableName = "transactions")
@Parcelize
data class TransactionItem(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    var userId: String = "",
    var type: String = "", // expense, income, transfer, lend, borrow
    var amount: Long = 0L,
    var dateInMillis: Long = 0L,
    var note: String = "",

    // Danh mục (cho Chi/Thu)
    var categoryId: String = "",
    var categoryName: String = "",
    var categoryIcon: String = "",

    // Ví chính: Ví nguồn (Chi, Chuyển, Cho vay) HOẶC Ví đích (Thu, Đi vay)
    var walletId: String = "",
    var walletName: String = "",
    var walletIcon: String = "",

    // Ví đích phụ (CHỈ DÙNG cho Chuyển khoản)
    var destWalletId: String = "",
    var destWalletName: String = "",
    var destWalletIcon: String = "",

    // Tên người (CHỈ DÙNG cho Cho vay / Đi vay)
    var personName: String = "",
    @get:PropertyName("isPaid")
    @set:PropertyName("isPaid")
    var isPaid: Boolean = false
) : Parcelable