package com.example.appmoni.viewmodel.record


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.data.model.category.CategoryIncomeItem
import com.example.appmoni.data.model.category.DefaultCategories
import com.example.appmoni.data.repository.category.CategoryRepository

class ManageCategoryViewModel : ViewModel() {
    private val repository = CategoryRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _expenseList = MutableLiveData<List<CategoryExpenseItem>>()
    val expenseList: LiveData<List<CategoryExpenseItem>> get() = _expenseList

    private val _incomeList = MutableLiveData<List<CategoryIncomeItem>>()
    val incomeList: LiveData<List<CategoryIncomeItem>> get() = _incomeList

    // Biến này để ra hiệu cho Fragment biết: "Đã tạo dữ liệu mặc định xong rồi, cập nhật cờ FirstTime đi!"
    private val _updateFirstTimeFlag = MutableLiveData<Boolean>()
    val updateFirstTimeFlag: LiveData<Boolean> get() = _updateFirstTimeFlag


    // HÀM XỬ LÝ LẤY DỮ LIỆU MỤC CHI TIỀN
    fun loadExpenseCategories(userId: String, isFirstTime: Boolean) {
        _isLoading.value = true

        repository.getCategoriesByType(userId, "expense")
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Nếu có data -> Ép kiểu và ném ra cho UI
                    _expenseList.value = documents.toObjects(CategoryExpenseItem::class.java)
                    _isLoading.value = false
                } else {
                    // Nếu rỗng
                    if (isFirstTime) {
                        val defaults = DefaultCategories.getExpenseCategories()
                        repository.createDefaultExpenseCategories(userId, defaults)
                            .addOnSuccessListener {
                                _expenseList.value = defaults // Trả luôn list mặc định ra UI cho nhanh
                                _updateFirstTimeFlag.value = true // Phát tín hiệu báo UI lưu cờ
                                _isLoading.value = false
                            }
                            .addOnFailureListener {
                                _errorMessage.value = it.message
                                _isLoading.value = false
                            }
                    } else {
                        // Người cũ đã xóa hết danh mục
                        _expenseList.value = emptyList()
                        _isLoading.value = false
                    }
                }
            }
            .addOnFailureListener {
                _errorMessage.value = it.message
                _isLoading.value = false
            }
    }

    // HÀM XỬ LÝ LẤY DỮ LIỆU MỤC THU TIỀN
    fun loadIncomeCategories(userId: String, isFirstTime: Boolean) {
        _isLoading.value = true

        repository.getCategoriesByType(userId, "income")
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    _incomeList.value = documents.toObjects(CategoryIncomeItem::class.java)
                    _isLoading.value = false
                } else {
                    if (isFirstTime) {
                        val defaults = DefaultCategories.getIncomeCategories()
                        repository.createDefaultIncomeCategories(userId, defaults)
                            .addOnSuccessListener {
                                _incomeList.value = defaults
                                _updateFirstTimeFlag.value = true
                                _isLoading.value = false
                            }
                            .addOnFailureListener {
                                _errorMessage.value = it.message
                                _isLoading.value = false
                            }
                    } else {
                        _incomeList.value = emptyList()
                        _isLoading.value = false
                    }
                }
            }
            .addOnFailureListener {
                _errorMessage.value = it.message
                _isLoading.value = false
            }
    }
}