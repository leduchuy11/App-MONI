package com.example.appmoni.ui.login

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentForgotPasswordBinding
import com.example.appmoni.ui.showCustomToast
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Bắt sự kiện khi nhấn nút "Tiếp tục"
        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            // Nếu email hợp lệ thì mới gọi hàm gửi mail
            if (validateEmail(email)) {
                sendResetPasswordEmail(email)
            }
        }
    }

    // Hàm kiểm tra định dạng email
    private fun validateEmail(email: String): Boolean {
        binding.textInputLayout8.error = null

        if (email.isEmpty()) {
            binding.textInputLayout8.error = "Vui lòng nhập email"
            binding.etEmail.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayout8.error = "Định dạng email không hợp lệ"
            binding.etEmail.requestFocus()
            return false
        }
        return true
    }

    // Hàm gọi Firebase để gửi email đặt lại mật khẩu
    private fun sendResetPasswordEmail(email: String) {
        setLoadingState(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                setLoadingState(false)

                if (task.isSuccessful) {
                    // Thành công: Hiện Toast và quay lại màn hình Đăng nhập
                    requireContext().showCustomToast(
                        "Link đặt lại mật khẩu đã được gửi vào Email của bạn!",
                        R.drawable.moni_toast
                    )
                    findNavController().popBackStack()
                } else {
                    // Thất bại: Hiện Toast báo lỗi
                    val error = task.exception?.message ?: "Gửi mail thất bại"
                    requireContext().showCustomToast("Lỗi: $error", R.drawable.avatar_app)
                }
            }
    }

    //Hàm hiện processBar để load
    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.loadingOverlay.visibility = View.VISIBLE
        } else {
            binding.loadingOverlay.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}