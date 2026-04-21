package com.example.appmoni.viewmodel.record

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.data.repository.category.CategoryRepository

class AddEditCategoryViewModel : ViewModel() {
    private val repository = CategoryRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _actionSuccess =
        MutableLiveData<String>() // Phát ra thông báo "Thêm thành công", "Sửa thành công"...
    val actionSuccess: LiveData<String> get() = _actionSuccess

    // Biến này để hứng dữ liệu cũ lúc người dùng muốn Sửa
    private val _categoryToEdit = MutableLiveData<CategoryExpenseItem?>()
    val categoryToEdit: LiveData<CategoryExpenseItem?> get() = _categoryToEdit


    // HÀM 1: Lấy dữ liệu cũ để điền vào Form
    fun loadCategoryForEdit(userId: String, categoryId: String) {
        _isLoading.value = true
        repository.getCategoryById(userId, categoryId)
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val item = document.toObject(CategoryExpenseItem::class.java)
                    _categoryToEdit.value = item
                } else {
                    _errorMessage.value = "Không tìm thấy dữ liệu cũ!"
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _errorMessage.value = it.message
                _isLoading.value = false
            }
    }

    // HÀM 2: Lưu (Bao gồm cả Thêm mới hoặc Cập nhật)
    fun saveCategory(
        userId: String,
        categoryId: String?,
        name: String,
        group: String,
        type: String,
        iconName: String
    ) {
        _isLoading.value = true

        val categoryData = hashMapOf(
            "name" to name,
            "group" to group,
            "type" to type,
            "iconName" to iconName
        )

        if (categoryId == null) {
            // THÊM MỚI
            repository.addCategory(userId, categoryData)
                .addOnSuccessListener {
                    _actionSuccess.value = "Đã lưu danh mục thành công!"
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _errorMessage.value = it.message
                    _isLoading.value = false
                }
        } else {
            // CẬP NHẬT
            categoryData["id"] = categoryId
            repository.updateCategory(userId, categoryId, categoryData)
                .addOnSuccessListener {
                    _actionSuccess.value = "Cập nhật thành công!"
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _errorMessage.value = it.message
                    _isLoading.value = false
                }
        }
    }

    // HÀM 3: Xóa
    fun deleteCategory(userId: String, categoryId: String) {
        _isLoading.value = true
        repository.deleteCategory(userId, categoryId)
            .addOnSuccessListener {
                _actionSuccess.value = "Đã xóa danh mục!"
                _isLoading.value = false
            }
            .addOnFailureListener {
                _errorMessage.value = it.message
                _isLoading.value = false
            }
    }
}