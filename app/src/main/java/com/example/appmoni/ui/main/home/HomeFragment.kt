package com.example.appmoni.ui.main.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentHomeBinding
import com.example.appmoni.ui.main.home.BannerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageList = listOf(
            R.drawable.img_banner_1,
            R.drawable.img_banner_2,
            R.drawable.img_banner_3,
            R.drawable.img_banner_4,
            R.drawable.img_banner_5
        )

        val adapter = BannerAdapter(imageList)

        binding.vpBannerHome.adapter = adapter

        // Liên kết dấu chấm tròn
        TabLayoutMediator(binding.tabIndicatorBanner, binding.vpBannerHome) { _, _ -> }.attach()

        // Lập trình Auto-scroll
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (binding.vpBannerHome.adapter?.itemCount == 0) return

                val currentItem = binding.vpBannerHome.currentItem

                if (currentItem == imageList.size - 1) {
                    binding.vpBannerHome.setCurrentItem(0, false)
                } else {
                    binding.vpBannerHome.setCurrentItem(currentItem + 1, true)
                }

                autoScrollHandler.postDelayed(this, 5000)
            }
        }

        autoScrollHandler.postDelayed(autoScrollRunnable, 5000)


        binding.llCategoryList.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_manageCategoryFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //tắt đếm giờ khi thoát màn hình để không bị crash / lag máy
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        _binding = null
    }
}