package com.example.appmoni.ui.main.profile.changePassword

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentChangePasswordBinding
import com.example.appmoni.ui.auth.LoginActivity
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.auth.ChangePasswordViewModel
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChangePasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnConfirmChange.setOnClickListener {
            performChangePassword()
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            val isEnable = !isLoading
            binding.etOldPassword.isEnabled = isEnable
            binding.etNewPassword.isEnabled = isEnable
            binding.etConfirmPassword.isEnabled = isEnable
            binding.btnConfirmChange.isEnabled = isEnable
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                val friendlyMessage = when {
                    errorMsg.contains("INVALID_LOGIN_CREDENTIALS") || errorMsg.contains("credential is incorrect") -> "Mật khẩu hiện tại không đúng. Vui lòng kiểm tra lại!"
                    errorMsg.contains("network error") || errorMsg.contains("network connection") -> "Không có kết nối mạng. Vui lòng kiểm tra Wifi/4G!"
                    else -> "Lỗi: $errorMsg"
                }
                requireContext().showCustomToast(friendlyMessage, R.drawable.avatar_app)
            }
        }

        viewModel.isSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                requireContext().showCustomToast(
                    "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.",
                    R.drawable.avatar_app
                )

                FirebaseAuth.getInstance().signOut()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun performChangePassword() {
        val oldPass = binding.etOldPassword.text.toString().trim()
        val newPass = binding.etNewPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()

        binding.tilOldPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        if (oldPass.isEmpty()) {
            binding.tilOldPassword.error = "Vui lòng nhập mật khẩu hiện tại"
            binding.etOldPassword.requestFocus()
            return
        }

        if (newPass.length < 6) {
            binding.tilNewPassword.error = "Mật khẩu mới phải từ 6 ký tự"
            binding.etNewPassword.requestFocus()
            return
        }

        if (newPass == oldPass) {
            binding.tilNewPassword.error = "Mật khẩu mới phải khác mật khẩu hiện tại"
            binding.etNewPassword.requestFocus()
            return
        }

        if (newPass != confirmPass) {
            binding.tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            binding.etConfirmPassword.requestFocus()
            return
        }

        viewModel.changePassword(oldPass, newPass)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}