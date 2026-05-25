package com.example.appmoni.ui.main.home.manageCategory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentManageCategoryBinding
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.google.android.material.tabs.TabLayoutMediator

class ManageCategoryFragment : Fragment() {

    private var _binding: FragmentManageCategoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel: CategorySharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Kết nối ViewModel và lắng nghe thanh tìm kiếm
        sharedViewModel =
            ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)

        // Reset từ khóa tìm kiếm mỗi khi vào lại màn hình này cho sạch sẽ
        sharedViewModel.updateQuery("")

        binding.edtSearch.addTextChangedListener { text ->
            sharedViewModel.updateQuery(text.toString().trim())
        }

        // Khởi tạo ViewPager2 và TabLayout
        val pagerAdapter = ManageCategoryPagerAdapter(this)
        binding.viewPagerManage.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPagerManage) { tab, position ->
            when (position) {
                0 -> tab.text = "Mục chi"
                1 -> tab.text = "Mục thu"
            }
        }.attach()

        // Xử lý Nút Nổi: Thêm danh mục
        binding.fabAddCategory.setOnClickListener {
            // Lấy Tab hiện tại để biết người dùng muốn thêm danh mục Thu hay Chi
            val currentTab = binding.viewPagerManage.currentItem
            val type = if (currentTab == 0) "expense" else "income"

            val bundle = Bundle()
            bundle.putString("categoryType", type)

            findNavController().navigate(R.id.action_manageCategoryFragment_to_addEditCategoryFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Dọn dẹp ViewBinding để tránh rò rỉ bộ nhớ
        _binding = null
    }

    // ADAPTER QUẢN LÝ 2 TRANG (THU / CHI) CỦA MÀN HÌNH MANAGE
    private inner class ManageCategoryPagerAdapter(fragment: Fragment) :
        FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ManageExpenseFragment() // Mình sẽ tạo Class này ở bước tiếp theo
                1 -> ManageIncomeFragment()  // Mình sẽ tạo Class này ở bước tiếp theo
                else -> Fragment()
            }
        }
    }
}