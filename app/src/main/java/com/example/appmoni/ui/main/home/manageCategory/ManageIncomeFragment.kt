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
import com.example.appmoni.data.model.category.CategoryIncomeItem
import com.example.appmoni.databinding.FragmentManageIncomeBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.example.appmoni.viewmodel.record.ManageCategoryViewModel
import com.google.firebase.auth.FirebaseAuth

class ManageIncomeFragment : Fragment() {

    private var _binding: FragmentManageIncomeBinding? = null
    private val binding get() = _binding!!


    private lateinit var sharedViewModel: CategorySharedViewModel
    private lateinit var viewModel: ManageCategoryViewModel

    private var originalList = listOf<CategoryIncomeItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvIncomeCategories.layoutManager = LinearLayoutManager(requireContext())

        sharedViewModel =
            ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)
        viewModel = ViewModelProvider(requireActivity()).get(ManageCategoryViewModel::class.java)

        setupObservers()

        // Ra lệnh cho viewmodel lấy dữ liệu
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val sharedPref =
                requireActivity().getSharedPreferences("AppMoniPrefs", Context.MODE_PRIVATE)
            val isFirstTime = sharedPref.getBoolean("isFirstTime_${userId}", true)

            viewModel.loadIncomeCategories(userId, isFirstTime)
        }
    }

    // Hàm ngồi nghe từ viewmodel
    private fun setupObservers() {

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvIncomeCategories.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        }


        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                requireContext().showToast("Lỗi: $error", ToastType.ERROR)
            }
        }

        // Hóng Data mảng Thu tiền
        viewModel.incomeList.observe(viewLifecycleOwner) { list ->
            originalList = list // Lưu bản gốc
            val currentQuery = sharedViewModel.searchQuery.value ?: ""
            filterData(currentQuery) // Lọc và hiển thị
        }

        // Hóng Cờ FirstTime (Nếu mây rỗng và phải tạo data mặc định)
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

    // HÀM LỌC
    private fun filterData(query: String) {
        if (query.isEmpty()) {
            updateUI(originalList)
        } else {
            val queryNoAccent = query.removeAccents().lowercase()
            val filteredList = originalList.filter { item ->
                item.name.removeAccents().lowercase().contains(queryNoAccent)
            }
            updateUI(filteredList)
        }
    }

    // HÀM ĐỔ DỮ LIỆU LÊN UI
    private fun updateUI(list: List<CategoryIncomeItem>) {
        val adapter = ManageIncomeAdapter(list) { clickedItem ->
            val bundle = Bundle()
            bundle.putString("categoryId", clickedItem.id)
            bundle.putString("categoryType", "income")

            try {
                findNavController().navigate(
                    R.id.action_manageCategoryFragment_to_addEditCategoryFragment,
                    bundle
                )
            } catch (e: Exception) {
                Log.e("NavigationError", "Lỗi chuyển trang bên Thu tiền: ${e.message}")
            }
        }
        binding.rvIncomeCategories.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}