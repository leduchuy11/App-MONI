package com.example.appmoni.data.record

import android.content.Context


data class CategoryIncomeItem(
    var id: String = "",
    var name: String = "",
    var iconName: String = "",
    var type: String = "income"
) {
    fun getIconResource(context: Context): Int {
        return context.resources.getIdentifier(iconName, "drawable", context.packageName)
    }
}