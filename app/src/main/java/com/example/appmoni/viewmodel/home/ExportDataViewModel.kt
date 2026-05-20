package com.example.appmoni.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoni.data.local.AppDatabase
import com.example.appmoni.data.model.transaction.TransactionItem
import kotlinx.coroutines.launch

class ExportDataViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    private val _exportData = MutableLiveData<List<TransactionItem>>()
    val exportData: LiveData<List<TransactionItem>> get() = _exportData

    fun getTransactionsForExport(userId: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val transactions = transactionDao.getTransactionsForExport(userId, startDate, endDate)
            _exportData.value = transactions
        }
    }
}