package com.example.appmoni.data.model.exchangeRate

data class CurrencyItem(
    val symbol: String,
    val code: String,
    val name: String,
    val flagResId: Int,
    var rate: Double = 0.0
)