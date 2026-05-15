package com.example.appmoni.viewmodel.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.model.report.MonthlyReportItem
import com.example.appmoni.data.model.transaction.TransactionItem
import java.util.Calendar

class IncomeExpenseReportViewModel : ViewModel() {
    private val _monthlyData = MutableLiveData<List<MonthlyReportItem>>()
    val monthlyData: LiveData<List<MonthlyReportItem>> get() = _monthlyData

    fun processYearlyData(txList: List<TransactionItem>, year: Int) {
        val result = mutableListOf<MonthlyReportItem>()

        for (month in 0..11) {
            val calendar = Calendar.getInstance()

            // Lọc các giao dịch thuộc tháng/năm được chọn
            val filtered = txList.filter {
                calendar.timeInMillis = it.dateInMillis
                calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month
            }

            var income = 0L
            var expense = 0L

            for (tx in filtered) {
                if (tx.type == "income" || tx.type == "borrow") income += tx.amount
                else if (tx.type == "expense" || tx.type == "lend") expense += tx.amount
            }

            if (income > 0 || expense > 0) {
                result.add(MonthlyReportItem(month + 1, income, expense))
            }
        }
        _monthlyData.value = result.sortedBy { it.month }
    }
}