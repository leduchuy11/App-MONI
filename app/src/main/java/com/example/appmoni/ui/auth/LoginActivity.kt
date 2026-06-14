package com.example.appmoni.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.ActivityLoginBinding
import com.example.appmoni.ui.main.MainActivity
import com.example.appmoni.viewmodel.auth.AuthViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var navController: NavController
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Hỏi ViewModel xem người dùng đã hợp lệ chưa
        if (viewModel.isUserLoggedInAndVerified()) {
            // Người dùng đã đăng nhập -> Chuyển thẳng sang MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Đóng LoginActivity lại để khi ấn nút Back ở MainActivity không bị quay lại đây
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLogin)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupActionBarWithNavController(navController)

        // Ẩn Toolbar ở LoginFragment, hiện ở các Fragment khác
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

            if (destination.id == R.id.loginFragment) {
                supportActionBar?.hide()

                binding.appBarLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        this@LoginActivity,
                        R.color.white
                    )
                )
                binding.toolbarLogin.setBackgroundColor(
                    ContextCompat.getColor(
                        this@LoginActivity,
                        R.color.white
                    )
                )

                window.statusBarColor = ContextCompat.getColor(this@LoginActivity, R.color.white)

                windowInsetsController.isAppearanceLightStatusBars = true
            } else {
                supportActionBar?.show()

                binding.appBarLayout.setBackgroundColor(
                    ContextCompat.getColor(this@LoginActivity, R.color.blue_dark)
                )
                binding.toolbarLogin.setBackgroundColor(
                    ContextCompat.getColor(this@LoginActivity, R.color.blue_main)
                )

                window.statusBarColor =
                    ContextCompat.getColor(this@LoginActivity, R.color.blue_dark)

                windowInsetsController.isAppearanceLightStatusBars = false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}