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

    init {
        // 1. tổng nợ
        _totalLiabilities.addSource(_transactions) { txList ->
            val totalBorrow = txList?.filter { it.type == "borrow" && !it.isPaid }?.sumOf { it.amount } ?: 0L
            _totalLiabilities.value = totalBorrow
            updateNetWorth()
        }

        // 2. tổng có
        val updateAssets = {
            var sum = 0L
            sum += _wallets.value?.filter { it.isActive }?.sumOf { it.balance } ?: 0L
            sum += _savings.value?.filter { it.status == "active" }?.sumOf { it.amount } ?: 0L
            sum += _transactions.value?.filter { it.type == "lend" && !it.isPaid }?.sumOf { it.amount } ?: 0L

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
    }

    private fun updateNetWorth() {
        val assets = _totalAssets.value ?: 0L
        val liabilities = _totalLiabilities.value ?: 0L
        _netWorth.value = assets - liabilities
    }

    fun loadData(userId: String) {
        walletRepo.listenToWalletsByType(userId, "spending",
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
                result.add(ExpenseCategoryReport(name, amount, (amount.toFloat() / totalExpense) * 100))
            }
        } else {
            for (i in 0 until 4) {
                val (name, amount) = grouped[i]
                result.add(ExpenseCategoryReport(name, amount, (amount.toFloat() / totalExpense) * 100))
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
                result.add(ExpenseCategoryReport("Khác", otherAmount, (otherAmount.toFloat() / totalExpense) * 100))
            }
        }

        _expenseStructure.postValue(result)
    }
}