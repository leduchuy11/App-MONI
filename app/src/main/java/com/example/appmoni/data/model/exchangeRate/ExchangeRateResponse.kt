package com.example.appmoni.data.model.exchangeRate

data class ExchangeRateResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)