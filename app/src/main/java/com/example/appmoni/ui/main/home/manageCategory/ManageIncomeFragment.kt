package com.example.appmoni.ui.main.home.manageCategory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.record.CategoryExpenseItem
import com.example.appmoni.data.record.CategoryIncomeItem
import com.example.appmoni.data.record.DefaultCategories
import com.example.appmoni.databinding.FragmentIncomeCategoryBinding // Tái sử dụng giao diện cũ!
import com.example.appmoni.databinding.FragmentManageCategoryBinding
import com.example.appmoni.databinding.FragmentManageIncomeBinding
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManageIncomeFragment : Fragment() {

    private var _binding: FragmentManageIncomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedViewModel: CategorySharedViewModel
    private var originalList = listOf<CategoryIncomeItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.rvIncomeCategories.layoutManager = LinearLayoutManager(requireContext())

        sharedViewModel = ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)
        sharedViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            filterData(query)
        }

        loadDataFromFirebase()
    }

    // Hàm load dữ liệu từ firestore
    private fun loadDataFromFirebase() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.rvIncomeCategories.visibility = View.INVISIBLE

        db.collection("users").document(userId).collection("categories")
            .whereEqualTo("type", "income")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                binding.rvIncomeCategories.visibility = View.VISIBLE

                if (!documents.isEmpty) {
                    originalList = documents.toObjects(CategoryIncomeItem::class.java)
                    val currentQuery = sharedViewModel.searchQuery.value ?: ""
                    filterData(currentQuery)
                } else {
                    // TRƯỜNG HỢP RỖNG: Kiểm tra xem là Người mới hay Người cũ thích xóa
                    val sharedPref = requireActivity().getSharedPreferences("AppMoniPrefs", android.content.Context.MODE_PRIVATE)
                    val isFirstTime = sharedPref.getBoolean("isFirstTime_${userId}", true)

                    if (isFirstTime) {
                        // 1. Là người mới tinh -> Gọi hàm tạo dữ liệu mặc định
                        createDefaultIncomeCategories(userId)

                        // Đánh dấu là "Đã nhận data", lần sau rỗng thì không load nữa
                        sharedPref.edit().putBoolean("isFirstTime_${userId}", false).apply()
                    } else {
                        // 2. Là người cũ đã xóa sạch mọi thứ -> Trả về danh sách rỗng
                        originalList = emptyList()
                        val currentQuery = sharedViewModel.searchQuery.value ?: ""
                        filterData(currentQuery)
                    }
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
            }
    }

    // Hàm lọc dữ liệu
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
        // Khởi tạo Adapter mới và truyền danh sách vào
        val adapter = ManageIncomeAdapter(list) { clickedItem ->

            val bundle = Bundle()

            // Nhét ID của danh mục vào Bundle để báo hiệu đây là màn SỬA
            bundle.putString("categoryId", clickedItem.id)

            // Nhét loại danh mục là "income"
            bundle.putString("categoryType", "income")

            // Gọi lệnh chuyển trang sang màn Thêm/Sửa kèm theo data
            try {
                findNavController().navigate(
                    R.id.action_manageCategoryFragment_to_addEditCategoryFragment,
                    bundle
                )
            } catch (e: Exception) {
                Log.e("NavigationError", "Lỗi chuyển trang bên Thu tiền: ${e.message}")
                requireContext().showCustomToast(
                    "Lỗi điều hướng! Kiểm tra lại file Navigation.",
                    R.drawable.avatar_app
                )
            }
        }

        // Gắn Adapter vào RecyclerView
        binding.rvIncomeCategories.adapter = adapter
    }


    // Hàm tạo dữ liệu Thu tiền mặc định lần đầu tiên
    private fun createDefaultIncomeCategories(userId: String) {

        // LẤY DỮ LIỆU CHUẨN CỦA THU TIỀN (INCOME)
        val defaultCategories = DefaultCategories.getIncomeCategories()

        val batch = db.batch()
        val categoryRef = db.collection("users").document(userId).collection("categories")

        for (item in defaultCategories) {
            val docRef = categoryRef.document(item.id)
            batch.set(docRef, item)
        }

        batch.commit().addOnSuccessListener {
            Log.d("Firebase", "Đã tạo xong dữ liệu THU TIỀN mặc định!")
            loadDataFromFirebase()
        }.addOnFailureListener {
            requireContext().showCustomToast(
                "Lỗi tạo dữ liệu mặc định: ${it.message}",
                R.drawable.avatar_app
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}