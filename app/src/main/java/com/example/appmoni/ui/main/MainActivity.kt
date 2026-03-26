package com.example.appmoni.ui.main

import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge(
            // Status Bar Nền Xanh, Icon Trắng
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#008fd5")),

            // Navigation Bar Nền Đen, Icon tự động thành Trắng
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )


        // Thiết lập Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        // Làm to nút ghi chép
        val addMenuItem = binding.bottomNavigationView.findViewById<View>(R.id.recordFragment)
        addMenuItem.scaleX = 1.2f
        addMenuItem.scaleY = 1.2f

        //Hiệu ứng khi icon được chọn
        navController.addOnDestinationChangedListener { _, destination, _ ->
            for (i in 0 until binding.bottomNavigationView.menu.size()) {
                val item = binding.bottomNavigationView.menu.getItem(i)
                val itemView = binding.bottomNavigationView.findViewById<View>(item.itemId)

                val iconView = itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_view)

                // Nếu là nút Ghi chép ở giữa thì luôn giữ nó to 1.3f
                if (item.itemId == R.id.recordFragment) {
                    iconView?.scaleX = 1.3f
                    iconView?.scaleY = 1.3f
                    continue
                }

                if (item.itemId == destination.id) {
                    // Các tab khác khi được chọn: Phóng to 1.2 lần mượt mà
                    iconView?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(150)?.start()
                } else {
                    // Khi không được chọn: Thu về kích thước gốc 1.0
                    iconView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150)?.start()
                }
            }
        }
    }
}