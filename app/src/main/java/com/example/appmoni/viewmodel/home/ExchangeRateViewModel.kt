package com.example.appmoni.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoni.data.model.exchangeRate.CurrencyItem
import com.example.appmoni.data.repository.exchangeRate.ExchangeRateRepository
import com.example.appmoni.data.repository.exchangeRate.CurrencyHelper
import kotlinx.coroutines.launch

class ExchangeRateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExchangeRateRepository(application)

    private val _currencyList = MutableLiveData<List<CurrencyItem>>()
    val currencyList: LiveData<List<CurrencyItem>> get() = _currencyList

    // Trạng thái của màn hình
    var currentBaseCode = "VND"
    var currentBaseSymbol = "đ"
    var currentBaseName = "Việt Nam Đồng"
    var currentAmount = 0.0
    var isSwapMode = false // Đảo chiều công thức (1 VND = X hay 1 X = VND)



    private var allCurrencies = listOf<CurrencyItem>()

    init {
        fetchRates(currentBaseCode,currentBaseCode,currentBaseName)
    }

    fun fetchRates(baseCode: String, baseSymbol: String,baseName: String) {
        this.currentBaseCode = baseCode
        this.currentBaseSymbol = baseSymbol
        this.currentBaseName = baseName
        viewModelScope.launch {
            val response = repository.getRates(baseCode)
            val baseCurrencies = CurrencyHelper.getSupportedCurrencies()

            if (response != null && response.result == "success") {
                allCurrencies = baseCurrencies.map { item ->
                    val rateFromApi = response.rates[item.code] ?: 0.0
                    item.copy(rate = rateFromApi)
                }
                    .filter { it.code != currentBaseCode }

                calculateAndPost()
            }
        }
    }

    fun updateAmount(amount: Double) {
        currentAmount = amount
        calculateAndPost()
    }

    fun toggleSwapMode() {
        isSwapMode = !isSwapMode
        calculateAndPost()
    }

    // Tính toán số liệu và đẩy lên giao diện
    private fun calculateAndPost() {
        if (allCurrencies.isEmpty()) return

        val updatedList = allCurrencies.map { it.copy() }
        _currencyList.value = updatedList
    }

    fun search(query: String) {
        if (query.isEmpty()) {
            calculateAndPost()
        } else {
            val lowerQuery = query.lowercase()
            val filtered = allCurrencies.filter {
                it.code.lowercase().contains(lowerQuery) ||
                        it.name.lowercase().contains(lowerQuery) ||
                        it.symbol.lowercase().contains(lowerQuery)
            }
            _currencyList.value = filtered
        }
    }
}