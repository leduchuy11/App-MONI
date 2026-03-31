package com.example.appmoni.data.record

import androidx.annotation.DrawableRes

data class CategoryExpenseItem(
    var id: String = "",
    var name: String = "",
    var iconName: String = "",
    var group: String = "",
    var type: String = "expense"
)