package com.example.appmoni.data.model.history

import com.example.appmoni.data.model.transaction.TransactionItem

data class DailyTransactionGroup(
    val dateStr: String,
    val relativeDay: String, // VD: "Hôm nay", hoặc thứ
    val totalIncome: Long,
    val totalExpense: Long,
    val transactions: List<TransactionItem>
)