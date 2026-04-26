package com.example.appmoni.viewmodel.record

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.data.repository.transaction.TransactionRepository
import kotlinx.coroutines.launch

class TransactionViewModel : ViewModel() {
    private val repository = TransactionRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _saveResult = MutableLiveData<Result<Unit>?>()
    val saveResult: LiveData<Result<Unit>?> get() = _saveResult

    fun saveTransaction(transaction: TransactionItem) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.saveTransaction(transaction)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    // Reset trạng thái để không bị hiện Toast nhiều lần khi xoay màn hình
    fun resetSaveResult() {
        _saveResult.value = null
    }
}