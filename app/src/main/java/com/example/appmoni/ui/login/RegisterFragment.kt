package com.example.appmoni.ui.login

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentRegisterBinding
import com.example.appmoni.ui.showCustomToast
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            performRegister()
        }
    }

    // Hàm quản lý trạng thái Loading
    private fun setLoadingState(isLoading: Boolean) {

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        val enableUI = !isLoading

        binding.tilEmail.isEnabled = enableUI
        binding.tilPassword.isEnabled = enableUI
        binding.tilConfirmpassword.isEnabled = enableUI
        binding.tilName.isEnabled = enableUI
        binding.btnRegister.isEnabled = enableUI
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmpw.text.toString().trim()
        val name = binding.etName.text.toString().trim()

        // Xóa lỗi cũ
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmpassword.error = null
        binding.tilName.error = null

        // Validation cơ bản
        if (name.isEmpty()) {
            binding.tilName.error = "Vui lòng nhập họ tên"
            binding.etName.requestFocus()
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email không hợp lệ"
            binding.etEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Mật khẩu phải từ 6 ký tự"
            binding.etPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.tilConfirmpassword.error = "Mật khẩu nhập lại không khớp"
            binding.etConfirmpw.requestFocus()
            return
        }

        setLoadingState(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null) {
                        // Tạo yêu cầu cập nhật tên
                        val profileUpdates =
                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                        // Đẩy tên lên server
                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    // Thành công 100% -> Gửi email xác thực
                                    sendVerificationEmail()
                                } else {
                                    // Lỗi tên thì báo lỗi nhưng vẫn cho gửi email xác thực
                                    requireContext().showCustomToast(
                                        "Lỗi cập nhật tên: ${profileTask.exception?.message}",
                                        R.drawable.avatar_app
                                    )
                                    sendVerificationEmail()
                                }
                            }
                    } else {
                        // Tắt loading nếu lỗi hệ thống
                        setLoadingState(false)
                        requireContext().showCustomToast(
                            "Lỗi hệ thống: Không tìm thấy tài khoản vừa tạo",
                            R.drawable.avatar_app
                        )
                    }

                } else {
                    // Đăng ký thất bại -> Tắt loading
                    setLoadingState(false)
                    val errorMessage = task.exception?.message ?: "Đăng ký thất bại"
                    requireContext().showCustomToast("Lỗi: $errorMessage", R.drawable.avatar_app)
                }
            }
    }

    private fun sendVerificationEmail() {
        val user = auth.currentUser

        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener { task ->
                // Mọi thứ hoàn tất (dù gửi mail thành công hay lỗi) -> Tắt loading
                setLoadingState(false)

                if (task.isSuccessful) {
                    requireContext().showCustomToast(
                        "Đăng ký thành công! Vui lòng kiểm tra email để xác thực.",
                        R.drawable.moni_toast
                    )
                    auth.signOut()
                    findNavController().popBackStack() // Quay lại màn hình đăng nhập
                } else {
                    requireContext().showCustomToast(
                        "Lỗi gửi email xác thực: ${task.exception?.message}",
                        R.drawable.avatar_app
                    )
                }
            }
        } else {
            setLoadingState(false)
            requireContext().showCustomToast("Không thể gửi email lúc này", R.drawable.avatar_app)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}