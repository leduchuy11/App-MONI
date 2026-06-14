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
import com.example.appmoni.databinding.FragmentForgotPasswordBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.auth.AuthViewModel

class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        setupObservers()

        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            // Nếu email hợp lệ thì giao việc cho ViewModel gửi mail
            if (validateEmail(email)) {
                viewModel.resetPassword(email)
            }
        }
    }

    // HÀM QUAN SÁT TỪ VIEWMODEL
    private fun setupObservers() {
        // Hóng Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            setLoadingState(isLoading)
        }

        // Hóng Lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                requireContext().showToast("Lỗi: $errorMessage", ToastType.ERROR)
            }
        }

        // Hóng Thành công (Gửi mail xong)
        viewModel.isResetEmailSent.observe(viewLifecycleOwner) { isSent ->
            if (isSent) {
                requireContext().showCustomToast(
                    "Link đặt lại mật khẩu đã được gửi vào Email của bạn!",
                    R.drawable.avatar_app
                )
                findNavController().popBackStack()
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

    // Hàm hiện processBar để load
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