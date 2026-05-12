package com.example.appmoni.viewmodel.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.data.model.report.ExpenseCategoryReport
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.data.repository.category.CategoryRepository
import com.example.appmoni.data.repository.transaction.TransactionRepository
import com.example.appmoni.data.repository.wallet.SavingsRepository
import com.example.appmoni.data.repository.wallet.WalletRepository
import java.util.Calendar

class ReportViewModel(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository = WalletRepository(),
    private val savingsRepo: SavingsRepository = SavingsRepository(),
    private val categoryRepo: CategoryRepository = CategoryRepository()
) : ViewModel() {

    // Các biến chứa data thô
    private val _wallets = MutableLiveData<List<WalletItem>>()
    private val _savings = MutableLiveData<List<SavingsItem>>()
    private val _transactions = MutableLiveData<List<TransactionItem>>()
    private val _expenseCategories = MutableLiveData<List<CategoryExpenseItem>>()

    // Biến đầu ra cho UI
    private val _totalAssets = MediatorLiveData<Long>()
    val totalAssets: LiveData<Long> get() = _totalAssets

    private val _totalLiabilities = MediatorLiveData<Long>()
    val totalLiabilities: LiveData<Long> get() = _totalLiabilities

    private val _netWorth = MediatorLiveData<Long>()
    val netWorth: LiveData<Long> get() = _netWorth

    private val _expenseStructure = MediatorLiveData<List<ExpenseCategoryReport>>()
    val expenseStructure: LiveData<List<ExpenseCategoryReport>> get() = _expenseStructure


    // Biến lưu tháng đang chọn
    private val _currentMonthFilter = MutableLiveData<String>("Tháng này")

    private val _totalExpenseSelectedMonth = MutableLiveData<Long>()
    val totalExpenseSelectedMonth: LiveData<Long> get() = _totalExpenseSelectedMonth

    // Data cho Biểu đồ (Chỉ chứa tối đa 5 item: Top 4 + Khác)
    private val _pieChartDataSelectedMonth = MediatorLiveData<List<ExpenseCategoryReport>>()
    val pieChartDataSelectedMonth: LiveData<List<ExpenseCategoryReport>> get() = _pieChartDataSelectedMonth

    // Data cho Danh sách (Chứa TẤT CẢ các nhóm)
    private val _detailListSelectedMonth = MediatorLiveData<List<ExpenseCategoryReport>>()
    val detailListSelectedMonth: LiveData<List<ExpenseCategoryReport>> get() = _detailListSelectedMonth

    private val _isLoadingStructure = MutableLiveData<Boolean>()
    val isLoadingStructure: LiveData<Boolean> get() = _isLoadingStructure

    init {
        // 1. tổng nợ
        _totalLiabilities.addSource(_transactions) { txList ->
            val totalBorrow =
                txList?.filter { it.type == "borrow" && !it.isPaid }?.sumOf { it.amount } ?: 0L
            _totalLiabilities.value = totalBorrow
            updateNetWorth()
        }

        // 2. tổng có
        val updateAssets = {
            var sum = 0L
            sum += _wallets.value?.filter { it.isActive }?.sumOf { it.balance } ?: 0L
            sum += _savings.value?.filter { it.status == "active" }?.sumOf { it.amount } ?: 0L
            sum += _transactions.value?.filter { it.type == "lend" && !it.isPaid }
                ?.sumOf { it.amount } ?: 0L

            _totalAssets.value = sum
            updateNetWorth()
        }
        _totalAssets.addSource(_wallets) { updateAssets() }
        _totalAssets.addSource(_savings) { updateAssets() }
        _totalAssets.addSource(_transactions) { updateAssets() }

        // 3. tài sản ròng
        _netWorth.addSource(_totalAssets) { updateNetWorth() }
        _netWorth.addSource(_totalLiabilities) { updateNetWorth() }

        // 4. cơ cấu chi tiêu
        _expenseStructure.addSource(_transactions) { txList ->
            calculateExpenseStructure(txList, _expenseCategories.value)
        }
        _expenseStructure.addSource(_expenseCategories) { catList ->
            calculateExpenseStructure(_transactions.value, catList)
        }

        val updateSelectedMonthStructure = {
            calculateForSelectedMonth(
                _transactions.value,
                _expenseCategories.value,
                _currentMonthFilter.value ?: "Tháng này"
            )
        }

        _pieChartDataSelectedMonth.addSource(_transactions) { updateSelectedMonthStructure() }
        _pieChartDataSelectedMonth.addSource(_expenseCategories) { updateSelectedMonthStructure() }
        _pieChartDataSelectedMonth.addSource(_currentMonthFilter) { updateSelectedMonthStructure() }

        _detailListSelectedMonth.addSource(_transactions) { updateSelectedMonthStructure() }
        _detailListSelectedMonth.addSource(_expenseCategories) { updateSelectedMonthStructure() }
        _detailListSelectedMonth.addSource(_currentMonthFilter) { updateSelectedMonthStructure() }
    }

    private fun updateNetWorth() {
        val assets = _totalAssets.value ?: 0L
        val liabilities = _totalLiabilities.value ?: 0L
        _netWorth.value = assets - liabilities
    }

    fun loadData(userId: String) {
        _isLoadingStructure.value = true
        walletRepo.listenToWalletsByType(
            userId, "spending",
            onResult = { _wallets.postValue(it) },
            onError = { }
        )
        savingsRepo.getSavingsList(userId) { list ->
            _savings.postValue(list ?: emptyList())
        }

        categoryRepo.listenToCategoriesByType(userId, "expense") { list ->
            _expenseCategories.postValue(list)
        }
    }

    fun setTransactions(transactions: List<TransactionItem>) {
        _transactions.value = transactions
    }

    private fun calculateExpenseStructure(
        txList: List<TransactionItem>?,
        categoryList: List<CategoryExpenseItem>?
    ) {
        if (txList.isNullOrEmpty()) {
            _expenseStructure.postValue(emptyList())
            return
        }

        val categoryGroupMap = categoryList?.associate { it.id to it.group } ?: emptyMap()

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val currentMonthExpenses = txList.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateInMillis }
            tx.type == "expense" &&
                    txCal.get(Calendar.MONTH) == currentMonth &&
                    txCal.get(Calendar.YEAR) == currentYear
        }

        val totalExpense = currentMonthExpenses.sumOf { it.amount }
        if (totalExpense == 0L) {
            _expenseStructure.postValue(emptyList())
            return
        }

        val grouped = currentMonthExpenses.groupBy { tx ->
            categoryGroupMap[tx.categoryId] ?: "Khác"
        }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        // Xử lý Top 4 + Khác
        val result = mutableListOf<ExpenseCategoryReport>()
        if (grouped.size <= 5) {
            grouped.forEach { (name, amount) ->
                result.add(
                    ExpenseCategoryReport(
                        name,
                        amount,
                        (amount.toFloat() / totalExpense) * 100
                    )
                )
            }
        } else {
            for (i in 0 until 4) {
                val (name, amount) = grouped[i]
                result.add(
                    ExpenseCategoryReport(
                        name,
                        amount,
                        (amount.toFloat() / totalExpense) * 100
                    )
                )
            }
            val otherAmount = grouped.subList(4, grouped.size).sumOf { it.second }

            // Tìm xem Top 4 có dính chữ "Khác" không để cộng dồn, tránh bị in ra 2 dòng chữ Khác
            val existingOtherIndex = result.indexOfFirst { it.categoryName == "Khác" }
            if (existingOtherIndex != -1) {
                val oldOther = result[existingOtherIndex]
                val newAmount = oldOther.amount + otherAmount
                result[existingOtherIndex] = ExpenseCategoryReport(
                    "Khác", newAmount, (newAmount.toFloat() / totalExpense) * 100
                )
            } else {
                result.add(
                    ExpenseCategoryReport(
                        "Khác",
                        otherAmount,
                        (otherAmount.toFloat() / totalExpense) * 100
                    )
                )
            }
        }

        _expenseStructure.postValue(result)
    }

    // Hàm này giờ chỉ làm nhiệm vụ Cập nhật bộ lọc tháng
    fun filterExpenseStructureByMonth(filterMonth: String) {
        _currentMonthFilter.value = filterMonth
    }

    private fun calculateForSelectedMonth(
        txList: List<TransactionItem>?,
        categoryList: List<CategoryExpenseItem>?,
        filterMonth: String
    ) {

        if (categoryList == null || txList == null) return

        if (txList.isEmpty()) {
            _pieChartDataSelectedMonth.postValue(emptyList())
            _detailListSelectedMonth.postValue(emptyList())
            _totalExpenseSelectedMonth.postValue(0L)
            _isLoadingStructure.postValue(false)
            return
        }

        val categoryMap = categoryList.associateBy { it.id } ?: emptyMap()

        // Tính tháng/năm
        val (targetMonth, targetYear) = if (filterMonth == "Tháng này") {
            val cal = Calendar.getInstance()
            Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
        } else {
            val parts = filterMonth.split("/")
            if (parts.size == 2) Pair(parts[0].toInt() - 1, parts[1].toInt()) else Pair(-1, -1)
        }

        val filteredExpenses = txList.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateInMillis }
            tx.type == "expense" && txCal.get(Calendar.MONTH) == targetMonth && txCal.get(Calendar.YEAR) == targetYear
        }

        val totalExpense = filteredExpenses.sumOf { it.amount }
        _totalExpenseSelectedMonth.postValue(totalExpense)

        if (totalExpense == 0L) {
            _pieChartDataSelectedMonth.postValue(emptyList())
            _detailListSelectedMonth.postValue(emptyList())
            _isLoadingStructure.postValue(false)
            return
        }

        val groupedTransactions = filteredExpenses.groupBy { tx ->
            categoryMap[tx.categoryId]?.group ?: "Khác"
        }

        val fullList = mutableListOf<ExpenseCategoryReport>()
        for ((groupName, txListInGroup) in groupedTransactions) {
            val groupTotal = txListInGroup.sumOf { it.amount }
            val groupPercent = (groupTotal.toFloat() / totalExpense) * 100

            val firstTx = txListInGroup.firstOrNull()
            val iconName = firstTx?.let { categoryMap[it.categoryId]?.iconName } ?: ""

            fullList.add(ExpenseCategoryReport(groupName, groupTotal, groupPercent, iconName))
        }

        fullList.sortByDescending { it.amount }
        _detailListSelectedMonth.postValue(fullList)

        // tạo danh sách rút gọn (cho pie chart)
        val chartData = mutableListOf<ExpenseCategoryReport>()
        if (fullList.size <= 5) {
            chartData.addAll(fullList)
        } else {
            for (i in 0 until 4) {
                chartData.add(fullList[i])
            }
            val otherAmount = fullList.subList(4, fullList.size).sumOf { it.amount }
            val otherPercent = (otherAmount.toFloat() / totalExpense) * 100
            chartData.add(ExpenseCategoryReport("Khác", otherAmount, otherPercent, ""))
        }

        _pieChartDataSelectedMonth.postValue(chartData)
        _isLoadingStructure.postValue(false)
    }
}