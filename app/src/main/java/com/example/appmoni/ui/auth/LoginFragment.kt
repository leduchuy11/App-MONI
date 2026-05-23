package com.example.appmoni.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentLoginBinding
import com.example.appmoni.ui.main.MainActivity
import com.example.appmoni.ui.main.home.notification.AlarmScheduler
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.auth.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel

    private lateinit var googleSignInClient: GoogleSignInClient

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    viewModel.loginWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    setLoadingState(false)
                    requireContext().showCustomToast(
                        "Lỗi Google Sign-In: ${e.message}",
                        R.drawable.avatar_app
                    )
                }
            } else {
                setLoadingState(false)
            }
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showWelcomeNotification()
        } else {
            requireContext().showCustomToast(
                "Bạn đã tắt thông báo, Moni sẽ không thể nhắc nhở bạn ghi chép!",
                R.drawable.avatar_app
            )
        }
        // Xử lý quyền xong xuôi mới được chuyển màn hình
        navigateToMain()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Gọi hàm lắng nghe dữ liệu từ ViewModel
        setupObservers()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.btnLogin.setOnClickListener {
            performLogin()
        }
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.btnLoginGoogle.setOnClickListener {
            setLoadingState(true)
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    // HÀM OBSERVE KẾT QUẢ TỪ VIEWMODEL
    private fun setupObservers() {
        // Hóng trạng thái Loading để bật/tắt vòng xoay
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            setLoadingState(isLoading)
        }

        // Hóng thông báo lỗi để hiển thị Toast
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                requireContext().showCustomToast("Lỗi: $errorMessage", R.drawable.avatar_app)
                viewModel.clearErrorMessage()
            }
        }

        // Hóng trạng thái thành công để chuyển trang
        viewModel.isAuthSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                checkAndRequestNotificationPermission()
            }
        }
    }

    private fun navigateToMain() {
        requireContext().showCustomToast("Đăng nhập thành công!", R.drawable.avatar_app)
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        val enableUI = !isLoading
        binding.tilEmail.isEnabled = enableUI
        binding.tilPassword.isEnabled = enableUI
        binding.btnLogin.isEnabled = enableUI
        binding.btnLoginGoogle.isEnabled = enableUI
        binding.tvForgotPassword.isEnabled = enableUI
        binding.tvRegister.isEnabled = enableUI
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        // Kiểm tra lỗi giao diện (Validate)
        if (email.isEmpty()) {
            binding.tilEmail.error = "Vui lòng nhập email"
            binding.etEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Định dạng email không hợp lệ"
            binding.etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Vui lòng nhập mật khẩu"
            binding.etPassword.requestFocus()
            return
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
            binding.etPassword.requestFocus()
            return
        }

        viewModel.login(email, password)
    }

    private fun showWelcomeNotification() {
        val channelId = "welcome_channel"
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Chào mừng",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_congratulations) // Đảm bảo bạn có icon này
            .setContentTitle("Chào mừng bạn đến với Moni")
            .setContentText("Hãy bắt đầu ghi chép chi tiêu ngay hôm nay nhé!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(0, builder.build())

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val notiItem = com.example.appmoni.data.model.notification.NotificationItem(
                message = "Chào mừng bạn đến với Moni. Hãy bắt đầu ghi chép chi tiêu ngay hôm nay nhé!",
                timeInMillis = System.currentTimeMillis(),
                type = "system",
                isRead = false
            )
            db.collection("users").document(userId).collection("notifications").add(notiItem)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showWelcomeNotification()
            navigateToMain()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}