package com.example.appmoni.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.repository.auth.AuthRepository

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    // Trạng thái Loading (xoay vòng vòng)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Thông báo lỗi
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // Trạng thái thành công (Đăng nhập/Đăng ký xong)
    private val _isAuthSuccess = MutableLiveData<Boolean>()
    val isAuthSuccess: LiveData<Boolean> get() = _isAuthSuccess

    // Trạng thái quên mật khẩu thành công
    private val _isResetEmailSent = MutableLiveData<Boolean>()
    val isResetEmailSent: LiveData<Boolean> get() = _isResetEmailSent

    fun login(email: String, password: String) {
        _isLoading.value = true
        repository.loginWithEmail(email, password).addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                val user = repository.getCurrentUser()
                if (user != null && user.isEmailVerified) {
                    _isAuthSuccess.value = true
                } else {
                    _errorMessage.value = "Vui lòng xác thực email trước khi đăng nhập!"
                    repository.signOut()
                }
            } else {
                _errorMessage.value = task.exception?.message ?: "Đăng nhập thất bại"
            }
        }
    }

    fun register(email: String, password: String) {
        _isLoading.value = true
        repository.registerWithEmail(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = repository.getCurrentUser()
                if (user != null) {
                    repository.sendEmailVerification(user).addOnCompleteListener { verifyTask ->
                        _isLoading.value = false
                        if (verifyTask.isSuccessful) {
                            _isAuthSuccess.value = true
                            repository.signOut()
                        } else {
                            _errorMessage.value = verifyTask.exception?.message
                        }
                    }
                }
            } else {
                _isLoading.value = false
                _errorMessage.value = task.exception?.message ?: "Đăng ký thất bại"
            }
        }
    }

    fun resetPassword(email: String) {
        _isLoading.value = true
        repository.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                _isResetEmailSent.value = true
            } else {
                _errorMessage.value = task.exception?.message
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _isLoading.value = true
        repository.loginWithGoogle(idToken).addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                _isAuthSuccess.value = true
            } else {
                _errorMessage.value = task.exception?.message ?: "Đăng nhập Google thất bại"
            }
        }
    }

    // Hàm kiểm tra xem người dùng đã đăng nhập và xác thực email chưa
    fun isUserLoggedInAndVerified(): Boolean {
        val user = repository.getCurrentUser()
        return user != null && user.isEmailVerified
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

}