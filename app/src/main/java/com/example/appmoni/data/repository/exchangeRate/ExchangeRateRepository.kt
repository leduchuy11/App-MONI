package com.example.appmoni.data.repository.exchangeRate


import android.content.Context
import com.example.appmoni.data.model.exchangeRate.ExchangeRateResponse
import com.example.appmoni.data.remote.exchangeRate.RetrofitClient
import com.google.gson.Gson

class ExchangeRateRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ExchangeRateCache", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getRates(baseCode: String): ExchangeRateResponse? {
        val cacheKey = "rates_$baseCode"
        val cachedJson = prefs.getString(cacheKey, null)
        val lastUpdate = prefs.getLong("time_$baseCode", 0L)
        val currentTime = System.currentTimeMillis() / 1000

        // 1. KIỂM TRA OFFLINE: Nếu có cache và mới cập nhật trong vòng 24h (86400 giây)
        if (cachedJson != null && (currentTime - lastUpdate < 86400)) {
            return gson.fromJson(cachedJson, ExchangeRateResponse::class.java)
        }

        // 2. GỌI ONLINE: Nếu chưa có cache hoặc cache quá cũ
        try {
            val response = RetrofitClient.api.getLatestRates(baseCode)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                prefs.edit()
                    .putString(cacheKey, gson.toJson(data))
                    .putLong("time_$baseCode", currentTime)
                    .apply()

                return data
            }
        } catch (e: Exception) {
            // RỚT MẠNG: Lỗi kết nối nhưng nếu trong máy có cache cũ, lôi ra dùng tạm
            if (cachedJson != null) {
                return gson.fromJson(cachedJson, ExchangeRateResponse::class.java)
            }
        }

        return null
    }
}