package com.example.appmoni.data.model.report

data class MonthlyReportItem(
    val month: Int,
    val income: Long,
    val expense: Long
) {
    val balance: Long get() = income - expense
}