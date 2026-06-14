package com.example.appmoni.ui.main.record.category

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.model.category.CategoryExpenseGroup
import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.databinding.FragmentExpenseCategoryBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.example.appmoni.viewmodel.record.ManageCategoryViewModel
import com.google.firebase.auth.FirebaseAuth

class ExpenseCategoryFragment : Fragment() {

    private var _binding: FragmentExpenseCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: CategorySharedViewModel
    private lateinit var viewModel: ManageCategoryViewModel

    private var originalFlatList = listOf<CategoryExpenseItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvExpense.layoutManager = LinearLayoutManager(requireContext())

        sharedViewModel =
            ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)
        viewModel = ViewModelProvider(this).get(ManageCategoryViewModel::class.java)

        setupObservers()

        // Ra lệnh cho ViewModel lấy dữ liệu
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val sharedPref =
                requireActivity().getSharedPreferences("AppMoniPrefs", Context.MODE_PRIVATE)
            val isFirstTime = sharedPref.getBoolean("isFirstTime_${userId}", true)

            viewModel.loadExpenseCategories(userId, isFirstTime)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvExpense.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                requireContext().showToast("Lỗi: $error", ToastType.ERROR)
            }
        }

        // Khi có data  -> Lưu bản gốc và lọc
        viewModel.expenseList.observe(viewLifecycleOwner) { list ->
            originalFlatList = list
            val currentQuery = sharedViewModel.searchQuery.value ?: ""
            filterData(currentQuery)
        }

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
            .map { entry ->
                CategoryExpenseGroup(groupName = entry.key, items = entry.value)
            }

        val adapter = OuterCategoryAdapter(groupedData) { clickedItem ->
            Log.d("RECORD_DEBUG", "Click nhận được tại Fragment: ${clickedItem.name}")
            val result = bundleOf(
                "selected_id" to clickedItem.id,
                "selected_name" to clickedItem.name,
                "selected_type" to "expense",
                "selected_icon" to clickedItem.iconName
            )

            requireParentFragment().setFragmentResult("REQUEST_KEY_CATEGORY", result)
            findNavController().popBackStack()
        }

        binding.rvExpense.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}