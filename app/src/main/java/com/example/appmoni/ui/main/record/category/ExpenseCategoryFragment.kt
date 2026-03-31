package com.example.appmoni.ui.main.record.category

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.record.CategoryExpenseGroup
import com.example.appmoni.data.record.CategoryExpenseItem
import com.example.appmoni.data.record.DefaultCategories
import com.example.appmoni.databinding.FragmentExpenseCategoryBinding
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExpenseCategoryFragment : Fragment() {

    private var _binding: FragmentExpenseCategoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var sharedViewModel: CategorySharedViewModel

    // THÊM BIẾN NÀY ĐỂ NHỚ DANH SÁCH GỐC
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.rvExpense.layoutManager = LinearLayoutManager(requireContext())

        sharedViewModel = ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)
        sharedViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            // Nhận được tín hiệu -> Gọi hàm lọc
            filterData(query)
        }

        // Bắt đầu tải dữ liệu từ Firestore về
        loadDataFromFirebase()
    }

    // Hàm tải dữ liệu từ firestore
    private fun loadDataFromFirebase() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.rvExpense.visibility = View.INVISIBLE

        db.collection("users").document(userId).collection("categories")
            .whereEqualTo("type", "expense")
            .get()
            .addOnSuccessListener { documents ->

                binding.progressBar.visibility = View.GONE
                binding.rvExpense.visibility = View.VISIBLE

                if (documents.isEmpty) {
                    // Nếu rỗng (User mới dùng app) -> Lấy từ kho mặc định đẩy lên mây
                    createDefaultExpenseCategories(userId)
                } else {
                    originalFlatList = documents.toObjects(CategoryExpenseItem::class.java)
                    // Lần đầu mở lên thì hiển thị danh sách gốc
                    val currentQuery = sharedViewModel.searchQuery.value ?: ""
                    filterData(currentQuery)
                }
            }
            .addOnFailureListener {
                requireContext().showCustomToast(
                    "Lỗi tải danh mục: ${it.message}",
                    R.drawable.avatar_app
                )
            }
    }

    // Hàm gộp nhóm và xử lý sự kiện chọn danh mục
    private fun setupRecyclerViewAndGroupData(flatList: List<CategoryExpenseItem>) {

        // Gom nhóm tự động theo thuộc tính "group"
        val groupedData = flatList.groupBy { it.group }
            .map { entry ->
                CategoryExpenseGroup(groupName = entry.key, items = entry.value)
            }

        // Khởi tạo Adapter với dữ liệu đã được gom nhóm
        val adapter = OuterCategoryAdapter(groupedData) { clickedItem ->

            Log.d("RECORD_DEBUG", "Click nhan duoc tai Fragment: ${clickedItem.name}")
            val result = bundleOf(
                "selected_id" to clickedItem.id,
                "selected_name" to clickedItem.name,
                "selected_type" to "expense"
            )

            // Phát tín hiệu lên FragmentManager quản lý CategoryFragment
            requireParentFragment().setFragmentResult("REQUEST_KEY_CATEGORY", result)

            // Đóng toàn bộ CategoryFragment để về RecordFragment
            findNavController().popBackStack()
        }

        binding.rvExpense.adapter = adapter
    }

    // Hàm tạo dữ liệu mặc định lần đầu tiên
    private fun createDefaultExpenseCategories(userId: String) {

        // Lấy dữ liệu chuẩn từ kho DefaultCategories
        val defaultCategories = DefaultCategories.getExpenseCategories()

        val batch = db.batch()
        val categoryRef = db.collection("users").document(userId).collection("categories")

        // Duyệt qua từng món và gói thành 1 cục để đẩy lên mây
        for (item in defaultCategories) {
            val docRef = categoryRef.document(item.id)
            batch.set(docRef, item)
        }

        // Thực thi lệnh đẩy lên Firestore
        batch.commit().addOnSuccessListener {
            Log.d("Firebase", "Đã tạo xong dữ liệu chi tiền mặc định!")
            // Sau khi đẩy xong, tải lại màn hình để nó gộp nhóm và hiển thị
            loadDataFromFirebase()
        }.addOnFailureListener {
            requireContext().showCustomToast(
                "Lỗi tạo dữ liệu mặc định: ${it.message}",
                R.drawable.avatar_app
            )
        }
    }

    // Hàm lọc dữ liệu
    private fun filterData(query: String) {
        if (query.isEmpty()) {
            // Không gõ gì -> Hiển thị toàn bộ
            setupRecyclerViewAndGroupData(originalFlatList)
        } else {
            // Lột sạch dấu và in thường từ khóa người dùng gõ (Ví dụ: "Học" -> "hoc")
            val queryNoAccent = query.removeAccents().lowercase()

            // Lọc danh sách
            val filteredList = originalFlatList.filter { item ->
                // Lột sạch dấu và in thường tên món trong danh sách (Ví dụ: "Học hành" -> "hoc hanh")
                val nameNoAccent = item.name.removeAccents().lowercase()

                // So sánh 2 cục không dấu với nhau
                nameNoAccent.contains(queryNoAccent)
            }

            // Hiển thị kết quả
            setupRecyclerViewAndGroupData(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}