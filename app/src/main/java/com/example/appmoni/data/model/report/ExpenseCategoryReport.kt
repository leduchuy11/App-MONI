package com.example.appmoni.data.model.report

data class ExpenseCategoryReport(
    val categoryName: String,
    val amount: Long,
    val percent: Float,
    val iconName: String = ""
)