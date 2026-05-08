package com.example.appmoni.viewmodel.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.data.repository.wallet.SavingsRepository

class SavingsViewModel : ViewModel() {

    private val repository = SavingsRepository()

    private val _saveStatus = MutableLiveData<Result<String>>()
    val saveStatus: LiveData<Result<String>> get() = _saveStatus

    private val _savingsList = MutableLiveData<List<SavingsItem>>()
    val savingsList: LiveData<List<SavingsItem>> get() = _savingsList

    private val _deleteStatus = MutableLiveData<Result<String>?>()
    val deleteStatus: LiveData<Result<String>?> get() = _deleteStatus

    private val _updateStatus = MutableLiveData<Result<String>?>()
    val updateStatus: LiveData<Result<String>?> get() = _updateStatus

    private val _settleStatus = MutableLiveData<Result<String>?>()
    val settleStatus: LiveData<Result<String>?> get() = _settleStatus

    fun saveSavingsAndTransaction(savings: SavingsItem, transaction: TransactionItem?) {
        repository.saveSavingsAndTransaction(savings, transaction) { isSuccess, error ->
            if (isSuccess) {
                _saveStatus.postValue(Result.success("Thêm sổ tiết kiệm thành công!"))
            } else {
                _saveStatus.postValue(Result.failure(Exception(error ?: "Có lỗi xảy ra khi lưu")))
            }
        }
    }

    fun loadSavings(userId: String) {
        repository.getSavingsList(userId) { list ->
            _savingsList.postValue(list ?: emptyList())
        }
    }

    fun deleteSavings(savings: SavingsItem) {
        repository.deleteSavings(savings) { isSuccess, error ->
            if (isSuccess) {
                _deleteStatus.postValue(Result.success("Đã xóa sổ và hoàn tiền thành công!"))
            } else {
                _deleteStatus.postValue(Result.failure(Exception(error ?: "Có lỗi xảy ra khi xóa")))
            }
        }
    }

    fun updateSavings(oldSavings: SavingsItem, newSavings: SavingsItem) {
        repository.updateSavings(oldSavings, newSavings) { isSuccess, error ->
            if (isSuccess) {
                _updateStatus.postValue(Result.success("Cập nhật sổ tiết kiệm thành công!"))
            } else {
                _updateStatus.postValue(Result.failure(Exception(error ?: "Có lỗi xảy ra khi cập nhật")))
            }
        }
    }

    // Hàm tất toán
    fun settleSavings(
        savingsId: String,
        userId: String,
        receiveWalletId: String,
        principalTransaction: TransactionItem,
        interestTransaction: TransactionItem?,
        totalAmountToAdd: Long
    ) {
        repository.settleSavings(
            savingsId, userId, receiveWalletId,
            principalTransaction, interestTransaction, totalAmountToAdd
        ) { isSuccess, error ->
            if (isSuccess) {
                _settleStatus.postValue(Result.success("Tất toán sổ tiết kiệm thành công!"))
            } else {
                _settleStatus.postValue(Result.failure(Exception(error ?: "Có lỗi xảy ra khi tất toán")))
            }
        }
    }

    // Hàm reset trạng thái
    fun resetSettleStatus() {
        _settleStatus.value = null
    }

    fun resetDeleteStatus() {
        _deleteStatus.value = null
    }
    fun resetUpdateStatus() {
        _updateStatus.value = null
    }
}