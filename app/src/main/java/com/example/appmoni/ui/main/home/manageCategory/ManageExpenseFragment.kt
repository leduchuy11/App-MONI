package com.example.appmoni.ui.main.home.manageCategory

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.model.category.CategoryExpenseGroup
import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.databinding.FragmentManageExpenseBinding
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.example.appmoni.viewmodel.record.ManageCategoryViewModel
import com.google.firebase.auth.FirebaseAuth

class ManageExpenseFragment : Fragment() {

    private var _binding: FragmentManageExpenseBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: CategorySharedViewModel
    private lateinit var viewModel: ManageCategoryViewModel

    private var originalFlatList = listOf<CategoryExpenseItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvExpense.layoutManager = LinearLayoutManager(requireContext())

        sharedViewModel =
            ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)
        viewModel = ViewModelProvider(this).get(ManageCategoryViewModel::class.java)

        // Lắng nghe sự kiện
        setupObservers()

        // BẮT ĐẦU LẤY DỮ LIỆU
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val sharedPref =
                requireActivity().getSharedPreferences("AppMoniPrefs", Context.MODE_PRIVATE)
            val isFirstTime = sharedPref.getBoolean("isFirstTime_${userId}", true)

            // Giao việc cho bồi bàn: "Lấy data chi tiền cho user này đi, kiểm tra xem có phải first time ko"
            viewModel.loadExpenseCategories(userId, isFirstTime)
        }
    }

    // HÀM NGỒI NGHE KẾT QUẢ TỪ VIEWMODEL
    private fun setupObservers() {
        // Hóng Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvExpense.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        }

        // Hóng Lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                requireContext().showCustomToast("Lỗi: $error", R.drawable.avatar_app)
            }
        }

        // Hóng Data mảng Chi tiền
        viewModel.expenseList.observe(viewLifecycleOwner) { list ->
            originalFlatList = list // Lưu lại bản gốc
            val currentQuery = sharedViewModel.searchQuery.value ?: ""
            filterData(currentQuery) // Gọi hàm lọc và vẽ UI
        }

        // Hóng Cờ báo hiệu cập nhật trạng thái Người dùng mới (Nếu có tạo data mặc định)
        viewModel.updateFirstTimeFlag.observe(viewLifecycleOwner) { shouldUpdate ->
            if (shouldUpdate) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val sharedPref =
                        requireActivity().getSharedPreferences("AppMoniPrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("isFirstTime_${userId}", false).apply()
                }
            }
        }

        // Hóng thanh tìm kiếm
        sharedViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            filterData(query)
        }
    }

    private fun filterData(query: String) {
        if (query.isEmpty()) {
            setupRecyclerViewAndGroupData(originalFlatList)
        } else {
            val queryNoAccent = query.removeAccents().lowercase()
            val filteredList = originalFlatList.filter { item ->
                item.name.removeAccents().lowercase().contains(queryNoAccent)
            }
            setupRecyclerViewAndGroupData(filteredList)
        }
    }

    private fun setupRecyclerViewAndGroupData(flatList: List<CategoryExpenseItem>) {
        val groupedData = flatList.groupBy { it.group }
            .map { entry -> CategoryExpenseGroup(groupName = entry.key, items = entry.value) }

        val adapter = ManageOuterCategoryAdapter(groupedData) { clickedItem ->
            val bundle = Bundle()
            bundle.putString("categoryId", clickedItem.id)
            bundle.putString("categoryType", "expense")

            try {
                findNavController().navigate(
                    R.id.action_manageCategoryFragment_to_addEditCategoryFragment,
                    bundle
                )
            } catch (e: Exception) {
                Log.e("NavigationError", "Lỗi chuyển trang: ${e.message}")
                requireContext().showCustomToast(
                    "Lỗi điều hướng! Kiểm tra lại file Navigation.",
                    R.drawable.avatar_app
                )
            }
        }
        binding.rvExpense.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}