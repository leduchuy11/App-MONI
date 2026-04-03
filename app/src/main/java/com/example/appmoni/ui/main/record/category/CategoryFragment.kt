package com.example.appmoni.ui.main.record.category

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentCategoryBinding
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.google.android.material.tabs.TabLayoutMediator

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: CategorySharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)

        // Reset từ khóa tìm kiếm mỗi khi vào lại màn hình này cho sạch sẽ
        sharedViewModel.updateQuery("")

        // Lắng nghe bàn phím và phát tín hiệu
        binding.etSearch.addTextChangedListener { text ->
            // Mỗi khi gõ 1 chữ, cập nhật chữ đó cho sharedViewModel
            sharedViewModel.updateQuery(text.toString().trim())
        }

        // Nút Back quay về màn hình Ghi chép
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Khởi tạo Adapter cho ViewPage2
        val pagerAdapter = CategoryPagerAdapter(this)
        binding.viewPagerCategory.adapter = pagerAdapter

        // Đồng bộ TabLayout và ViewPage2
        TabLayoutMediator(binding.tabLayout, binding.viewPagerCategory) { tab, position ->
            when (position) {
                0 -> tab.text = "Chi tiền"
                1 -> tab.text = "Thu tiền"
            }
        }.attach()

        // Chuyển tab đúng với loại danh mục
        val type = arguments?.getString("transaction_type") ?: "expense"
        if (type == "income") {
            binding.viewPagerCategory.setCurrentItem(1, false)
        } else {
            binding.viewPagerCategory.setCurrentItem(0, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // CLASS ADAPTER NÀY LÀM NHIỆM VỤ QUẢN LÝ 2 VIEWPAGE2
    private inner class CategoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ExpenseCategoryFragment()
                1 -> IncomeCategoryFragment()
                else -> Fragment()
            }
        }
    }
}