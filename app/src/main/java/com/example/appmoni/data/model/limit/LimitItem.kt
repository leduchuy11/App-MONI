package com.example.appmoni.data.model.limit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LimitItem(
    var id: String = "",
    var userId: String = "",
    var name: String = "",
    var amount: Long = 0L,
    var spentAmount: Long = 0L,

    // Lưu danh sách ID các hạng mục. Nếu là tất cả -> listOf("all")
    var categoryIds: List<String> = listOf("all"),

    // icon danh mục
    var icon: String = "ic_all_in",

    var walletId: String = "all",

    var startDateInMillis: Long = 0L,
    var endDateInMillis: Long = 0L
) : Parcelable