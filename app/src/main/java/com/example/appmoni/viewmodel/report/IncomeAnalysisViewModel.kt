package com.example.appmoni.viewmodel.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmoni.data.local.AppDatabase
import com.example.appmoni.data.model.analysis.AnalysisItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class IncomeAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    private val _analysisList = MutableLiveData<List<AnalysisItem>>()
    val analysisList: LiveData<List<AnalysisItem>> get() = _analysisList

    private val _totalIncome = MutableLiveData<Long>()
    val totalIncome: LiveData<Long> get() = _totalIncome

    private val _averageIncome = MutableLiveData<Long>()
    val averageIncome: LiveData<Long> get() = _averageIncome

    fun loadData(userId: String, startDate: Long, endDate: Long, mode: TimeMode) {
        viewModelScope.launch {
            val transactions = transactionDao.getAllTransactionsIncome(userId, startDate, endDate)

            // Tính Tổng thu
            var total = 0L
            for (tx in transactions) {
                total += tx.amount
            }
            _totalIncome.value = total

            // Tính Trung bình thu ( theo Ngày / Tháng / Năm)
            val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
            val endCal = Calendar.getInstance().apply { timeInMillis = endDate }

            _averageIncome.value = when (mode) {
                TimeMode.DAY -> {
                    val diffInMillis = endDate - startDate
                    val daysCount = (diffInMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(0) + 1
                    if (daysCount > 0) total / daysCount else 0L
                }
                TimeMode.MONTH -> {
                    val yearsDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
                    val monthsDiff = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
                    val monthsCount = (yearsDiff * 12) + monthsDiff + 1
                    if (monthsCount > 0) total / monthsCount else 0L
                }
                TimeMode.YEAR -> {
                    val yearsDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
                    val yearsCount = yearsDiff + 1
                    if (yearsCount > 0) total / yearsCount else 0L
                }
            }

            // Xây dựng trục thời gian và gom nhóm dữ liệu
            val formatPattern = when (mode) {
                TimeMode.DAY -> "dd/MM/yyyy"
                TimeMode.MONTH -> "MM/yyyy"
                TimeMode.YEAR -> "yyyy"
            }
            val sdf = SimpleDateFormat(formatPattern, Locale.getDefault())

            val groupedMap = java.util.LinkedHashMap<String, Long>()
            val timestampMap = mutableMapOf<String, Long>()

            val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
            val endCalendar = Calendar.getInstance().apply { timeInMillis = endDate }

            while (!calendar.after(endCalendar)) {
                val dateStr = sdf.format(calendar.time)
                groupedMap[dateStr] = 0L
                timestampMap[dateStr] = calendar.timeInMillis

                when (mode) {
                    TimeMode.DAY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                    TimeMode.MONTH -> calendar.add(Calendar.MONTH, 1)
                    TimeMode.YEAR -> calendar.add(Calendar.YEAR, 1)
                }
            }

            for (tx in transactions) {
                val dateStr = sdf.format(Date(tx.dateInMillis))
                if (groupedMap.containsKey(dateStr)) {
                    groupedMap[dateStr] = groupedMap[dateStr]!! + tx.amount
                }
            }

            val resultList = groupedMap.map { (timeLabel, amount) ->
                AnalysisItem(
                    timeLabel = timeLabel,
                    amount = amount,
                    timestamp = timestampMap[timeLabel] ?: 0L
                )
            }

            _analysisList.value = resultList
        }
    }
}