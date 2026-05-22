package com.example.appmoni.data.repository.auth

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // 1. Kiểm tra trạng thái đăng nhập
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // 2. Đăng nhập bằng Email/Password
    fun loginWithEmail(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }

    // 3. Đăng ký tài khoản mới
    fun registerWithEmail(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }

    // 4. Cập nhật tên hiển thị cho User
    fun updateDisplayName(user: FirebaseUser, name: String): Task<Void> {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        return user.updateProfile(profileUpdates)
    }

    // 5. Gửi email xác thực
    fun sendEmailVerification(user: FirebaseUser): Task<Void> {
        return user.sendEmailVerification()
    }

    // 6. Quên mật khẩu
    fun sendPasswordResetEmail(email: String): Task<Void> {
        return auth.sendPasswordResetEmail(email)
    }

    // 7. Đăng xuất
    fun signOut() {
        auth.signOut()
    }

    // 8. Đăng nhập bằng google
    fun loginWithGoogle(idToken: String): Task<AuthResult> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential)
    }

    // 9. Đổi mật khẩu
    fun changePassword(oldPassword: String, newPassword: String): Task<Void> {
        val user = auth.currentUser
            ?: return Tasks.forException(Exception("Người dùng chưa đăng nhập"))
        val email = user.email
            ?: return Tasks.forException(Exception("Không lấy được thông tin email"))

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)

        return user.reauthenticate(credential).continueWithTask { task ->
            if (task.isSuccessful) {
                user.updatePassword(newPassword)
            } else {
                Tasks.forException(task.exception ?: Exception("Xác thực mật khẩu cũ thất bại"))
            }
        }
    }
}