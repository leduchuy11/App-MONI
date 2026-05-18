package com.example.appmoni.data.remote.exchangeRate

import com.example.appmoni.data.model.exchangeRate.ExchangeRateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {
    // Gọi đến đường dẫn: https://open.er-api.com/v6/latest/{base_currency}
    @GET("v6/latest/{base_currency}")
    suspend fun getLatestRates(
        @Path("base_currency") baseCurrency: String
    ): Response<ExchangeRateResponse>
}