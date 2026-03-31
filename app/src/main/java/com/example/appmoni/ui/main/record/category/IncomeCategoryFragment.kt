package com.example.appmoni.ui.main.record.category

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
import com.example.appmoni.data.record.CategoryIncomeItem
import com.example.appmoni.data.record.DefaultCategories
import com.example.appmoni.databinding.FragmentIncomeCategoryBinding
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.record.CategorySharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class IncomeCategoryFragment : Fragment() {

    private var _binding: FragmentIncomeCategoryBinding? = null
    private val binding get() = _binding!!


    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var sharedViewModel: CategorySharedViewModel

    // Biến nhớ danh sách gốc
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

        sharedViewModel = ViewModelProvider(requireActivity()).get(CategorySharedViewModel::class.java)
        sharedViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            filterData(query)
        }
        loadDataFromFirebase()
    }

    private fun setupRecyclerView() {
        binding.rvIncomeCategories.layoutManager = LinearLayoutManager(requireContext())
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_line)!!)
        binding.rvIncomeCategories.addItemDecoration(divider)
    }

    // Đọc dữ liệu từ Firestore
    private fun loadDataFromFirebase() {
        // Lấy ID của người dùng đang đăng nhập hiện tại
        val currentUser = auth.currentUser
        if (currentUser == null) {
            requireContext().showCustomToast(
                "Lỗi: Chưa đăng nhập!",
                R.drawable.avatar_app
            )
            return
        }
        val userId = currentUser.uid

        binding.progressBar.visibility = View.VISIBLE
        binding.rvIncomeCategories.visibility = View.INVISIBLE

        // Vào data của user
        db.collection("users").document(userId)
            .collection("categories")
            .whereEqualTo("type", "income")
            .get()
            .addOnSuccessListener { documents ->

                binding.progressBar.visibility = View.GONE
                binding.rvIncomeCategories.visibility = View.VISIBLE

                if (documents.isEmpty) {
                    Log.d("Firebase", "Người dùng mới tinh, đang nạp dữ liệu mặc định...")
                    uploadDefaultCategoriesToFirebase(userId)
                } else {
                    val incomeList = mutableListOf<CategoryIncomeItem>()
                    for (doc in documents) {
                        val item = doc.toObject(CategoryIncomeItem::class.java)
                        item.id = doc.id
                        incomeList.add(item)
                    }
                    originalList = incomeList
                    updateUI(originalList)
                }
            }
            .addOnFailureListener { exception ->
                requireContext().showCustomToast(
                    "Lỗi tải dữ liệu: ${exception.message}",
                    R.drawable.avatar_app
                )
            }
    }

    //Đẩy dữ liệu lên Firestore lần đầu
    private fun uploadDefaultCategoriesToFirebase(userId: String) {
        val defaultList = DefaultCategories.getIncomeCategories()
        val userCategoriesRef = db.collection("users").document(userId).collection("categories")

        for (item in defaultList) {
            userCategoriesRef.document(item.id).set(item)
        }

        // Đẩy xong thì gọi lại hàm lấy dữ liệu để hiển thị
        loadDataFromFirebase()
    }

    // Xử lý sự kiện click: chọn danh mục & đóng màn hình
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

    private fun filterData(query: String) {
        if (query.isEmpty()) {
            updateUI(originalList)
        } else {
            val queryNoAccent = query.removeAccents().lowercase()

            val filteredList = originalList.filter { item ->
                val nameNoAccent = item.name.removeAccents().lowercase()
                nameNoAccent.contains(queryNoAccent)
            }

            updateUI(filteredList)
        }
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