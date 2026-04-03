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
import com.example.appmoni.data.record.CategoryExpenseGroup
import com.example.appmoni.data.record.CategoryExpenseItem
import com.example.appmoni.data.record.DefaultCategories
import com.example.appmoni.databinding.FragmentExpenseCategoryBinding // Tái sử dụng giao diện cũ!
import com.example.appmoni.databinding.FragmentManageExpenseBinding
import com.example.appmoni.databinding.FragmentManageIncomeBinding
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManageExpenseFragment : Fragment() {

    private var _binding: FragmentManageExpenseBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedViewModel: CategorySharedViewModel
    private var originalFlatList = listOf<CategoryExpenseItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.rvExpense.layoutManager = LinearLayoutManager(requireContext())

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
        binding.rvExpense.visibility = View.INVISIBLE

        db.collection("users").document(userId).collection("categories")
            .whereEqualTo("type", "expense")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                binding.rvExpense.visibility = View.VISIBLE

                if (!documents.isEmpty) {
                    originalFlatList = documents.toObjects(CategoryExpenseItem::class.java)
                    val currentQuery = sharedViewModel.searchQuery.value ?: ""
                    filterData(currentQuery)
                } else {
                    // TRƯỜNG HỢP RỖNG: Kiểm tra xem là Người mới hay Người cũ thích xóa
                    val sharedPref = requireActivity().getSharedPreferences("AppMoniPrefs", android.content.Context.MODE_PRIVATE)
                    val isFirstTime = sharedPref.getBoolean("isFirstTime_${userId}", true)

                    if (isFirstTime) {
                        // 1. Là người mới tinh -> Gọi hàm tạo dữ liệu mặc định
                        createDefaultExpenseCategories(userId)

                        // Đánh dấu là "Đã nhận data", lần sau rỗng thì không load nữa
                        sharedPref.edit().putBoolean("isFirstTime_${userId}", false).apply()
                    } else {
                        // 2. Là người cũ đã xóa sạch mọi thứ -> Trả về danh sách rỗng
                        originalFlatList = emptyList()
                        val currentQuery = sharedViewModel.searchQuery.value ?: ""
                        filterData(currentQuery)
                    }
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                requireContext().showCustomToast("Lỗi: ${it.message}", R.drawable.avatar_app)
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

    // Hàm đổ dữ liệu lên reycleView
    private fun setupRecyclerViewAndGroupData(flatList: List<CategoryExpenseItem>) {
        val groupedData = flatList.groupBy { it.group }
            .map { entry -> CategoryExpenseGroup(groupName = entry.key, items = entry.value) }

        // Dùng Adapter của màn hình Quản lý
        val adapter = ManageOuterCategoryAdapter(groupedData) { clickedItem ->

            // 1. Bắt sự kiện bấm vào Item hoặc Cây bút
            // Tạo một cái Bundle để chứa dữ liệu truyền đi
            val bundle = Bundle()

            // Nhét ID của danh mục vào Bundle để màn hình AddEdit biết là đang SỬA
            bundle.putString("categoryId", clickedItem.id)

            // Nhét loại danh mục (Thu/Chi) vào Bundle
            // Chú ý: Vì đây là ManageExpenseFragment nên type luôn là "expense"
            bundle.putString("categoryType", "expense")

            // 2. Chuyển hướng sang màn hình AddEditCategoryFragment
            try {
                findNavController().navigate(
                    R.id.action_manageCategoryFragment_to_addEditCategoryFragment,
                    bundle
                )
            } catch (e: Exception) {
                // In ra lỗi nếu NavController không tìm thấy action
                Log.e("NavigationError", "Lỗi chuyển trang: ${e.message}")
                requireContext().showCustomToast(
                    "Lỗi điều hướng! Kiểm tra lại file Navigation.",
                    R.drawable.avatar_app
                )
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}