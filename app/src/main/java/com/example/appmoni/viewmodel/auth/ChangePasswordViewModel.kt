package com.example.appmoni.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.repository.auth.AuthRepository

class ChangePasswordViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _isSuccess = MutableLiveData<Boolean>()
    val isSuccess: LiveData<Boolean> get() = _isSuccess

    fun changePassword(oldPass: String, newPass: String) {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            repository.changePassword(oldPass, newPass).addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _isSuccess.value = true
                } else {
                    _errorMessage.value =
                        task.exception?.message ?: "Đổi mật khẩu thất bại. Vui lòng thử lại!"
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = e.message ?: "Đã xảy ra lỗi hệ thống."
        }
    }
}