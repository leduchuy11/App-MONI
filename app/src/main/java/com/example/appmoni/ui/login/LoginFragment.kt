package com.example.appmoni.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentLoginBinding
import com.example.appmoni.ui.main.MainActivity
import com.example.appmoni.ui.showCustomToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account.idToken!!)
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

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

    // Hàm quản lý trạng thái Loading
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

        // Validate
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

        setLoadingState(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->

                setLoadingState(false)

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        requireContext().showCustomToast(
                            "Đăng nhập thành công!",
                            R.drawable.moni_toast
                        )
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        requireContext().showCustomToast(
                            "Vui lòng vào email để xác thực tài khoản trước khi đăng nhập!",
                            R.drawable.avatar_app
                        )
                        auth.signOut()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Đăng nhập thất bại"
                    requireContext().showCustomToast("Lỗi: $errorMessage", R.drawable.avatar_app)
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->

                setLoadingState(false)

                if (task.isSuccessful) {
                    requireContext().showCustomToast(
                        "Đăng nhập Google thành công!",
                        R.drawable.moni_toast
                    )
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    val errorMessage = task.exception?.message ?: "Đăng nhập thất bại"
                    requireContext().showCustomToast(
                        "Lỗi Firebase: $errorMessage",
                        R.drawable.avatar_app
                    )
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}