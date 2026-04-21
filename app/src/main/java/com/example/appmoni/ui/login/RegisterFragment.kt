package com.example.appmoni.ui.login

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentRegisterBinding
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.auth.AuthViewModel

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Gọi hàm lắng nghe kết quả
        setupObservers()

        binding.btnRegister.setOnClickListener {
            performRegister()
        }
    }

    // HÀM OBSERVE TỪ VIEWMODEL
    private fun setupObservers() {
        // Hóng Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            setLoadingState(isLoading)
        }

        // Hóng Lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                requireContext().showCustomToast("Lỗi: $errorMessage", R.drawable.avatar_app)
            }
        }

        // Hóng Thành công
        viewModel.isAuthSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                requireContext().showCustomToast(
                    "Đăng ký thành công! Vui lòng kiểm tra email để xác thực.",
                    R.drawable.avatar_app
                )
                findNavController().popBackStack() // Quay lại màn hình đăng nhập
            }
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

        viewModel.register(email, password, name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}