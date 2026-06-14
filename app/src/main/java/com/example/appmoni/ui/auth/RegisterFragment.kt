package com.example.appmoni.ui.auth

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
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.showToast
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

        setupObservers()

        binding.btnRegister.setOnClickListener {
            performRegister()
        }
    }

    // HÀM OBSERVE TỪ VIEWMODEL
    private fun setupObservers() {

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            setLoadingState(isLoading)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                requireContext().showToast("Lỗi: $errorMessage", ToastType.ERROR)
            }
        }

        viewModel.isAuthSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                requireContext().showToast("Đăng ký thành công! Vui lòng kiểm tra email để xác thực.",
                    ToastType.SUCCESS)
                findNavController().popBackStack()
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
        binding.btnRegister.isEnabled = enableUI
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmpw.text.toString().trim()

        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmpassword.error = null

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

        viewModel.register(email, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}