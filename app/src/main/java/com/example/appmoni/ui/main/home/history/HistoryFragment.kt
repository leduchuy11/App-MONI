package com.example.appmoni.ui.main.home.history

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.model.history.DailyTransactionGroup
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.databinding.FragmentHistoryBinding
import com.example.appmoni.ui.removeAccents
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    // Lưu danh sách gốc để dùng cho chức năng tìm kiếm
    private var allTransactions = listOf<TransactionItem>()
    // Biến ghi nhớ bộ lọc tháng hiện tại
    private var selectedMonthFilter: String = "Tháng này"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter { selectedTransaction ->
            // Gửi dữ liệu sang màn hình chi tiết
            val bundle = Bundle().apply {
                putParcelable("transaction", selectedTransaction)
            }
            findNavController().navigate(R.id.action_historyFragment_to_detailTransactionFragment, bundle)
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSelectMonth.setOnClickListener {
            showMonthSelectionBottomSheet()
        }

        // Chức năng tìm kiếm theo tên danh mục
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                filterData(query)
            }
        })
    }

    private fun observeData() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModel.getAllTransactions(currentUserId).observe(viewLifecycleOwner) { transactions ->
            allTransactions = transactions
            filterData()
        }
    }

    //  Kiểm tra xem 1 giao dịch có nằm trong tháng đang chọn hay không
    private fun isTransactionInSelectedMonth(timeInMillis: Long, filter: String): Boolean {
        val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val transMonthYear = sdf.format(Date(timeInMillis))

        return if (filter == "Tháng này") {
            val currentMonthYear = sdf.format(Date())
            transMonthYear == currentMonthYear
        } else {
            transMonthYear == filter
        }
    }

    // Hàm Lọc dữ liệu khi gõ ô tìm kiếm
    private fun filterData(query: String = binding.etSearch.text.toString()) {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.cardEmptyState.visibility = View.GONE
        val normalizedQuery = query.trim().lowercase().removeAccents()

        val filteredList = allTransactions.filter { item ->
            // Kiểm tra điều kiện THÁNG
            val matchMonth = isTransactionInSelectedMonth(item.dateInMillis, selectedMonthFilter)

            // Kiểm tra điều kiện TÌM KIẾM
            val matchQuery = if (normalizedQuery.isEmpty()) {
                true
            } else {
                val displayCategoryName = when (item.type) {
                    "transfer" -> "chuyển khoản tới ${item.destWalletName}"
                    "borrow" -> "đi vay"
                    "lend" -> "cho vay"
                    else -> item.categoryName
                }.lowercase()
                val normalizedDisplay = displayCategoryName.removeAccents()
                normalizedDisplay.contains(normalizedQuery)
            }

            // Giao dịch được hiển thị phải thỏa mãn CẢ 2 điều kiện
            matchMonth && matchQuery
        }

        // Gọi thuật toán gom nhóm và đẩy vào Adapter
        val groupedData = groupTransactionsByDay(filteredList)
        adapter.submitList(groupedData)

        binding.progressBar.visibility = View.GONE

        if (groupedData.isEmpty()) {
            binding.cardEmptyState.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else {
            binding.cardEmptyState.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
        }
    }

    // THUẬT TOÁN GOM NHÓM DỮ LIỆU THEO NGÀY
    private fun groupTransactionsByDay(transactions: List<TransactionItem>): List<DailyTransactionGroup> {
        // Nhóm các giao dịch có cùng định dạng ngày "dd/MM/yyyy"
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val groupedMap = transactions.groupBy { sdfDate.format(Date(it.dateInMillis)) }

        val resultList = mutableListOf<DailyTransactionGroup>()

        // Lấy chuỗi ngày hôm nay và hôm qua để so sánh
        val todayStr = sdfDate.format(Date())
        val yesterdayStr = sdfDate.format(Date(System.currentTimeMillis() - 86400000)) // Trừ đi 24h (tính bằng millisecond)

        // Xử lý từng nhóm ngày
        for ((dateStr, listTrans) in groupedMap) {
            // Xác định "Hôm nay", "Hôm qua" hoặc "Thứ mấy"
            val relativeDay = when (dateStr) {
                todayStr -> "Hôm nay"
                yesterdayStr -> "Hôm qua"
                else -> {
                    val dateObj = sdfDate.parse(dateStr)
                    val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale("vi", "VN")) // Ra chữ "Thứ 2", "Thứ 3"
                    dateObj?.let { sdfDayOfWeek.format(it) } ?: ""
                }
            }

            // Tính tổng thu, tổng chi của ngày đó
            var dailyIncome = 0L
            var dailyExpense = 0L

            for (t in listTrans) {
                when (t.type) {
                    "income", "borrow" -> dailyIncome += t.amount
                    "expense", "lend" -> dailyExpense += t.amount
                }
            }

            // Sắp xếp các giao dịch trong cùng 1 ngày: Mới nhất nằm trên
            val sortedTransInDay = listTrans.sortedByDescending { it.dateInMillis }

            resultList.add(
                DailyTransactionGroup(
                    dateStr = dateStr,
                    relativeDay = relativeDay,
                    totalIncome = dailyIncome,
                    totalExpense = dailyExpense,
                    transactions = sortedTransInDay
                )
            )
        }

        // Sắp xếp lại danh sách các Ngày: Ngày gần nhất nằm trên cùng
        return resultList.sortedByDescending {
            sdfDate.parse(it.dateStr)?.time ?: 0L
        }
    }

    // HÀM HIỂN THỊ BOTTOM SHEET
    private fun showMonthSelectionBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_month, null)
        bottomSheetDialog.setContentView(view)

        val tvOptionThisMonth = view.findViewById<TextView>(R.id.tvOptionThisMonth)
        val tvOptionOtherMonth = view.findViewById<TextView>(R.id.tvOptionOtherMonth)

        // 1. Khi bấm chọn "Tháng này"
        tvOptionThisMonth.setOnClickListener {
            selectedMonthFilter = "Tháng này"
            binding.tvCurrentMonth.text = "Tháng này"
            filterData()
            bottomSheetDialog.dismiss()
        }

        // 2. Khi bấm chọn "Tháng khác"
        tvOptionOtherMonth.setOnClickListener {
            bottomSheetDialog.dismiss()
            showDatePickerForOtherMonth()
        }

        bottomSheetDialog.show()
    }

    // BỔ SUNG HÀM NÀY ĐỂ HIỆN BẢNG CHỌN THÁNG/NĂM
    private fun showDatePickerForOtherMonth() {
        // Gọi giao diện vừa tạo
        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.pickerMonth)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.pickerYear)

        // Lấy thời gian hiện tại để đặt làm mặc định
        val calendar = Calendar.getInstance()

        // Cài đặt cho cột Tháng (từ 1 đến 12)
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = calendar.get(Calendar.MONTH) + 1

        // Cài đặt cho cột Năm (cho phép lùi 10 năm và tiến 10 năm)
        val currentYear = calendar.get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear + 10
        yearPicker.value = currentYear

        // Hiển thị Dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Chọn tháng")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selectedMonth = monthPicker.value
                val selectedYear = yearPicker.value

                // Format lại thành "MM/yyyy"
                val formattedMonth = String.format("%02d", selectedMonth)
                val selectedMonthYear = "$formattedMonth/$selectedYear"

                // Cập nhật giao diện và gọi hàm lọc
                selectedMonthFilter = selectedMonthYear
                binding.tvCurrentMonth.text = selectedMonthYear
                filterData()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}