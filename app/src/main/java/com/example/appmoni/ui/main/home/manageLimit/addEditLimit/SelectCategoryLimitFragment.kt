package com.example.appmoni.ui.main.home.manageLimit.addEditLimit

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.data.model.category.CategoryExpenseGroup
import com.example.appmoni.databinding.FragmentSelectCategoryLimitBinding
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.viewmodel.record.ManageCategoryViewModel
import com.google.firebase.auth.FirebaseAuth

class SelectCategoryLimitFragment : Fragment() {

    private var _binding: FragmentSelectCategoryLimitBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SelectCategoryLimitAdapter
    private lateinit var viewModel: ManageCategoryViewModel

    // Lưu trữ danh sách gốc
    private var allCategories = listOf<CategoryExpenseGroup>()

    private var initialSelectedIds: List<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectCategoryLimitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ManageCategoryViewModel::class.java]

        initialSelectedIds = arguments?.getStringArrayList("EXISTING_CATEGORY_IDS")

        setupRecyclerView()
        observeViewModel()
        loadData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = SelectCategoryLimitAdapter { selectedCount ->
            binding.tvSelectedCount.text = "$selectedCount hạng mục"
            updateSelectAllCheckboxState()
        }

        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.expenseList.observe(viewLifecycleOwner) { flatList ->
            if (flatList != null) {
                val groupedList = flatList.groupBy { it.group }
                    .map { (groupName, items) ->
                        CategoryExpenseGroup(groupName, items)
                    }

                allCategories = groupedList

                if (initialSelectedIds != null) {
                    val ids = initialSelectedIds!!
                    if (ids.contains("all")) {
                        // Nếu đang là "Tất cả", đẩy data vào Adapter rồi gọi lệnh chọn tất cả
                        adapter.submitData(allCategories)
                        adapter.selectAll()
                    } else {
                        // Nếu chỉ chọn một vài cái, đổ ID vào bộ nhớ của Adapter trước
                        adapter.selectedCategoryIds.clear()
                        adapter.selectedCategoryIds.addAll(ids)
                        // Sau đó mới vẽ giao diện (Lúc vẽ nó sẽ thấy ID trong set và tự tick)
                        adapter.submitData(allCategories)
                    }
                    // Reset biến này bằng null để tránh việc nó tự tick lại khi người dùng gõ tìm kiếm
                    initialSelectedIds = null
                } else {
                    // Mặc định (Nếu không có data truyền sang)
                    adapter.submitData(allCategories)
                }

                binding.tvSelectedCount.text = "${adapter.getSelectedCount()} hạng mục"
                updateSelectAllCheckboxState()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu: $error", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    // Gọi hàm load dữ liệu từ Firebase
    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val sharedPref =
                requireActivity().getSharedPreferences("AppMoniPrefs", Context.MODE_PRIVATE)
            val isFirstTime = sharedPref.getBoolean("isFirstTime_${userId}", true)

            viewModel.loadExpenseCategories(userId, isFirstTime)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cbSelectAll.setOnClickListener {
            if (binding.cbSelectAll.isChecked) {
                adapter.selectAll()
            } else {
                adapter.deselectAll()
            }
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterData(s.toString())
            }
        })

        binding.btnConfirmSelection.setOnClickListener {
            val selectedIds = adapter.selectedCategoryIds.toList()

            // tìm tên icon của hạng mục đầu tiên (Nếu có chọn)
            var firstIconName = "ic_all_in"
            val totalItems = adapter.getTotalItemCount()

            if (selectedIds.isNotEmpty() && selectedIds.size != totalItems) {
                // Nếu có chọn và không phải là chọn tất cả
                val firstSelectedId = selectedIds.first()

                for (group in allCategories) {
                    val foundItem = group.items.find { it.id == firstSelectedId }
                    if (foundItem != null) {
                        firstIconName = foundItem.iconName
                        break
                    }
                }
            }

            // Đóng gói cả Danh sách ID VÀ Tên Icon gửi về
            setFragmentResult(
                "REQUEST_KEY_CATEGORY",
                bundleOf(
                    "SELECTED_IDS" to selectedIds,
                    "FIRST_ICON_NAME" to firstIconName,
                    "IS_SELECT_ALL" to (selectedIds.size == totalItems) // Cờ báo hiệu có chọn tất cả không
                )
            )
            findNavController().popBackStack()
        }
    }

    private fun filterData(query: String) {
        if (query.isEmpty()) {
            adapter.submitData(allCategories)
            return
        }

        val cleanQuery = query.removeAccents().lowercase()
        val filteredList = mutableListOf<CategoryExpenseGroup>()

        for (group in allCategories) {
            val filteredItems = group.items.filter {
                it.name.removeAccents().lowercase().contains(cleanQuery)
            }
            if (filteredItems.isNotEmpty()) {
                filteredList.add(CategoryExpenseGroup(group.groupName, filteredItems))
            }
        }
        adapter.submitData(filteredList)
    }

    private fun updateSelectAllCheckboxState() {
        val totalItems = adapter.getTotalItemCount()
        val selectedItems = adapter.getSelectedCount()
        binding.cbSelectAll.isChecked = (totalItems > 0 && totalItems == selectedItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}