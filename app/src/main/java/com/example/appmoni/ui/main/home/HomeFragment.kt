package com.example.appmoni.ui.main.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentHomeBinding
import com.example.appmoni.ui.main.home.BannerAdapter
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable

    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            // BẬT ĐỒNG BỘ NGAY KHI MỞ TRANG CHỦ
            viewModel.startSyncing(currentUserId)
        }

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
        binding.llCategoryHistory.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
        }
        binding.llCategoryAccount.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_manageSpendingFragment)
        }
        binding.llCategorySaving.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_manageSavingsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //tắt đếm giờ khi thoát màn hình để không bị crash / lag máy
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        _binding = null
    }
}