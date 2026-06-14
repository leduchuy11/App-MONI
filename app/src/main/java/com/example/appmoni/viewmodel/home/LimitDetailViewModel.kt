package com.example.appmoni.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoni.data.local.AppDatabase
import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.data.model.limit.LimitItem
import com.example.appmoni.data.repository.category.CategoryRepository
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class LimitDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    private val _chartData = MutableLiveData<Pair<List<Entry>, List<String>>>()
    val chartData: LiveData<Pair<List<Entry>, List<String>>> get() = _chartData

    private val categoryRepository = CategoryRepository()
    private val _categoryNames = MutableLiveData<String>()
    val categoryNames: LiveData<String> get() = _categoryNames

    fun loadChartData(limit: LimitItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = if (limit.categoryIds.contains("all")) {
                transactionDao.getTransactionsForAllCategories(
                    limit.userId, limit.startDateInMillis, limit.endDateInMillis
                )
            } else {
                transactionDao.getTransactionsForLimit(
                    limit.userId, limit.startDateInMillis, limit.endDateInMillis, limit.categoryIds
                )
            }

            // Thuật toán tính tổng cộng dồn theo từng ngày
            val entries = ArrayList<Entry>()
            val xLabels = ArrayList<String>()

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = limit.startDateInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Vẽ biểu đồ từ Ngày bắt đầu cho đến (Ngày hiện tại hoặc Ngày kết thúc)
            val todayMillis = System.currentTimeMillis()
            val endMillis = minOf(limit.endDateInMillis, todayMillis)

            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            var cumulativeSum = 0f
            var index = 0f

            while (calendar.timeInMillis <= endMillis) {

                xLabels.add(sdf.format(calendar.timeInMillis))

                // Lọc các giao dịch rơi vào ngày này
                val dayStart = calendar.timeInMillis
                val dayEnd = dayStart + 86400000L - 1 // Cộng thêm 24h - 1ms

                val daySpent = transactions
                    .filter { it.dateInMillis in dayStart..dayEnd }
                    .sumOf { it.amount }

                // Cộng dồn vào tổng chi tiêu
                cumulativeSum += daySpent.toFloat()

                // Thêm điểm vào biểu đồ
                entries.add(Entry(index, cumulativeSum))

                // Tiến lên ngày tiếp theo
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                index++
            }

            // Đẩy dữ liệu ra UI Thread
            withContext(Dispatchers.Main) {
                _chartData.value = Pair(entries, xLabels)
            }
        }
    }

    fun loadCategoryNames(userId: String, categoryIds: List<String>) {
        // Nếu chọn tất cả
        if (categoryIds.contains("all") || categoryIds.isEmpty()) {
            _categoryNames.value = "Tất cả hạng mục"
            return
        }

        // Kéo danh sách mục chi từ Firebase về
        categoryRepository.getCategoriesByType(userId, "expense")
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val allExpenseCategories = documents.toObjects(CategoryExpenseItem::class.java)

                    val matchedNames = allExpenseCategories
                        .filter { categoryIds.contains(it.id) }
                        .map { it.name }

                    _categoryNames.value = matchedNames.joinToString(", ")
                } else {
                    _categoryNames.value = "Chưa có danh mục nào"
                }
            }
            .addOnFailureListener {
                _categoryNames.value = "Lỗi tải dữ liệu"
            }
    }
}