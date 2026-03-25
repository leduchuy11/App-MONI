package com.example.appmoni.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.ActivityLoginBinding
import com.example.appmoni.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        //  Khởi tạo Firebase Auth
        auth = FirebaseAuth.getInstance()
        //  Kiểm tra trạng thái đăng nhập ngay lập tức
        if (auth.currentUser != null && auth.currentUser?.isEmailVerified == true) {
            // Người dùng đã đăng nhập -> Chuyển thẳng sang MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Đóng LoginActivity lại để khi ấn nút Back ở MainActivity không bị quay lại đây
            finish()
            // Return luôn để không chạy các lệnh vẽ giao diện bên dưới nữa
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // Thiết lập Toolbar thông qua binding
        setSupportActionBar(binding.toolbarLogin)

        // Tìm NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Kết nối Toolbar với NavController
        setupActionBarWithNavController(navController)

        // Ẩn Toolbar ở LoginFragment, hiện ở các Fragment khác
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

            if (destination.id == R.id.loginFragment) {
                supportActionBar?.hide()

                binding.appBarLayout.setBackgroundColor(ContextCompat.getColor(this@LoginActivity, R.color.white))
                binding.toolbarLogin.setBackgroundColor(ContextCompat.getColor(this@LoginActivity, R.color.white))

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

                window.statusBarColor = ContextCompat.getColor(this@LoginActivity, R.color.blue_dark)

                windowInsetsController.isAppearanceLightStatusBars = false
            }
        }
    }

    // Xử lý sự kiện nhấn nút back trên Toolbar
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}