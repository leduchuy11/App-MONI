package com.example.appmoni.viewmodel.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmoni.data.local.AppDatabase
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.data.repository.transaction.TransactionRepository
import kotlinx.coroutines.launch

class TransactionViewModel (application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()
    private val repository = TransactionRepository(transactionDao)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _saveResult = MutableLiveData<Result<Unit>?>()
    val saveResult: LiveData<Result<Unit>?> get() = _saveResult

    private val _deleteResult = MutableLiveData<Result<Unit>?>()
    val deleteResult: LiveData<Result<Unit>?> get() = _deleteResult

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

    // Hàm kích hoạt lắng nghe Firebase
    fun startSyncing(userId: String) {
        repository.startListeningToFirebase(userId)
    }

    //  Hàm kéo danh sách từ Room ra
    fun getAllTransactions(userId: String): LiveData<List<TransactionItem>> {
        return repository.getAllTransactions(userId)
    }

    // Hàm xóa 1 giao dịch
    fun deleteTransaction(transaction: TransactionItem) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.deleteTransaction(transaction)
            _deleteResult.value = result
            _isLoading.value = false
        }
    }

    // Reset lại trạng thái để không bị hiện Toast nhiều lần
    fun resetDeleteResult() {
        _deleteResult.value = null
    }
}