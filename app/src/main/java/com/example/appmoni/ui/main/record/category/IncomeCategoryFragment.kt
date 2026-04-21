package com.example.appmoni.ui.main.record.category

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.model.category.CategoryIncomeItem
import com.example.appmoni.databinding.FragmentIncomeCategoryBinding
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.example.appmoni.viewmodel.record.ManageCategoryViewModel
import com.google.firebase.auth.FirebaseAuth

class IncomeCategoryFragment : Fragment() {

    private var _binding: FragmentIncomeCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: CategorySharedViewModel
    private lateinit var viewModel: ManageCategoryViewModel

    private var originalList = listOf<CategoryIncomeItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        sharedViewModel =
            ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)
        viewModel = ViewModelProvider(this).get(ManageCategoryViewModel::class.java)

        // Lắng nghe sự kiện
        setupObservers()

        // Tải dữ liệu Thu tiền
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val sharedPref =
                requireActivity().getSharedPreferences("AppMoniPrefs", Context.MODE_PRIVATE)
            val isFirstTime = sharedPref.getBoolean("isFirstTime_${userId}", true)

            viewModel.loadIncomeCategories(userId, isFirstTime)
        }
    }

    private fun setupRecyclerView() {
        binding.rvIncomeCategories.layoutManager = LinearLayoutManager(requireContext())
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_line)!!)
        binding.rvIncomeCategories.addItemDecoration(divider)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvIncomeCategories.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                requireContext().showCustomToast("Lỗi: $error", R.drawable.avatar_app)
            }
        }

        // Hóng data mảng Thu tiền
        viewModel.incomeList.observe(viewLifecycleOwner) { list ->
            originalList = list
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
            updateUI(originalList)
        } else {
            val queryNoAccent = query.removeAccents().lowercase()
            val filteredList = originalList.filter { item ->
                item.name.removeAccents().lowercase().contains(queryNoAccent)
            }
            updateUI(filteredList)
        }
    }

    private fun updateUI(list: List<CategoryIncomeItem>) {
        val adapter = IncomeCategoryAdapter(list) { clickedItem ->
            val result = bundleOf(
                "selected_id" to clickedItem.id,
                "selected_name" to clickedItem.name,
                "selected_type" to clickedItem.type
            )

            requireParentFragment().setFragmentResult("REQUEST_KEY_CATEGORY", result)
            findNavController().popBackStack()
        }
        binding.rvIncomeCategories.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY_CATEGORY = "REQUEST_KEY_CATEGORY"
        const val BUNDLE_KEY_ID = "selected_id"
        const val BUNDLE_KEY_NAME = "selected_name"
        const val BUNDLE_KEY_ICON = "selected_icon"
        const val BUNDLE_KEY_TYPE = "selected_type"
    }
}