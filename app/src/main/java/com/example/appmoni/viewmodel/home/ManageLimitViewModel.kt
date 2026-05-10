package com.example.appmoni.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoni.data.local.AppDatabase
import com.example.appmoni.data.model.limit.LimitItem
import com.example.appmoni.data.repository.limit.LimitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageLimitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LimitRepository()
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> get() = _saveStatus

    private val _limitList = MutableLiveData<List<LimitItem>>()
    val limitList: LiveData<List<LimitItem>> get() = _limitList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun saveLimit(limit: LimitItem) {
        try {
            repository.addLimit(limit)
            _saveStatus.value = true
        } catch (e: Exception) {
            _saveStatus.value = false
        }
    }

    fun loadLimits(userId: String) {
        repository.getLimitsRealtime(userId,
            onUpdate = { rawList ->
                calculateSpentAmounts(userId, rawList)
            },
            onError = { e -> _errorMessage.value = e.message }
        )
    }

    // Hàm tính toán số tiền đã chi
    private fun calculateSpentAmounts(userId: String, rawList: List<LimitItem>) {
        viewModelScope.launch {
            val calculatedList = withContext(Dispatchers.IO) {
                rawList.map { limit ->
                    // Tính tổng tiền cho từng hạn mức
                    val spent = if (limit.categoryIds.contains("all")) {
                        transactionDao.getTotalExpenseForAllCategories(
                            userId = userId,
                            startDate = limit.startDateInMillis,
                            endDate = limit.endDateInMillis
                        ) ?: 0L
                    } else {
                        transactionDao.getTotalExpenseForLimit(
                            userId = userId,
                            startDate = limit.startDateInMillis,
                            endDate = limit.endDateInMillis,
                            categoryIds = limit.categoryIds
                        ) ?: 0L
                    }

                    limit.spentAmount = spent
                    limit // Trả về object đã được cập nhật
                }
            }

            _limitList.value = calculatedList
        }
    }

    fun deleteLimit(limit: LimitItem) {
        try {
            repository.deleteLimit(limit.userId, limit.id)
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi xóa: ${e.message}"
        }
    }

    fun updateLimit(limit: LimitItem) {
        try {
            repository.updateLimit(limit)
            _saveStatus.value = true
        } catch (e: Exception) {
            _saveStatus.value = false
            _errorMessage.value = "Lỗi khi cập nhật: ${e.message}"
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = false
    }
}