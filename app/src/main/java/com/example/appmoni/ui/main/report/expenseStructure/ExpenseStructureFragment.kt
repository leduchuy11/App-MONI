package com.example.appmoni.ui.main.report.expenseStructure

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.local.AppDatabase
import com.example.appmoni.data.model.report.ExpenseCategoryReport
import com.example.appmoni.data.repository.category.CategoryRepository
import com.example.appmoni.data.repository.transaction.TransactionRepository
import com.example.appmoni.databinding.FragmentExpenseStructureBinding
import com.example.appmoni.viewmodel.report.ReportViewModel
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.util.Calendar

class ExpenseStructureFragment : Fragment() {

    private var _binding: FragmentExpenseStructureBinding? = null
    private val binding get() = _binding!!

    // Kéo list data gốc từ Room
    private val transactionViewModel: TransactionViewModel by viewModels()

    private val reportViewModel: ReportViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
                val txRepo = TransactionRepository(dao)
                val catRepo = CategoryRepository()
                return ReportViewModel(txRepo, categoryRepo = catRepo) as T
            }
        }
    }

    private lateinit var adapter: ExpenseStructureAdapter
    private var selectedMonthFilter: String = "Tháng này"

    private val chartColors = listOf(
        "#ffbc37", "#ef5361", "#33c09d", "#9b7bea", "#0dafef"
    ).map { it.toColorInt() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseStructureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
        observeData()
    }

    private fun setupUI() {
        binding.pieChart.apply {
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setDrawCenterText(false)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            transparentCircleRadius = 0f
            holeRadius = 60f
            animateY(1000)
        }

        adapter = ExpenseStructureAdapter(chartColors)
        binding.rvExpenseDetails.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpenseDetails.adapter = adapter
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSelectMonth.setOnClickListener {
            showMonthSelectionBottomSheet()
        }
    }

    private fun observeData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            reportViewModel.loadData(userId)
            transactionViewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { txList ->
                if (txList != null) {
                    reportViewModel.setTransactions(txList)
                    reportViewModel.filterExpenseStructureByMonth(selectedMonthFilter)
                }
            }
        }

        reportViewModel.isLoadingStructure.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (isLoading) {
                // Đang tải -> Giấu hết nội dung
                binding.layoutContent.visibility = View.GONE
                binding.cardEmptyState.visibility = View.GONE
            } else {
                // Tải xong -> Kiểm tra lại xem danh sách hiện tại rỗng hay có data để hiện
                val currentList = reportViewModel.detailListSelectedMonth.value
                if (currentList.isNullOrEmpty()) {
                    binding.cardEmptyState.visibility = View.VISIBLE
                    binding.layoutContent.visibility = View.GONE
                } else {
                    binding.cardEmptyState.visibility = View.GONE
                    binding.layoutContent.visibility = View.VISIBLE
                }
            }
        }

        // Quan sát Tổng chi tiêu
        reportViewModel.totalExpenseSelectedMonth.observe(viewLifecycleOwner) { total ->
            val formatter = DecimalFormat("#,###")
            binding.tvTotalExpense.text = "${formatter.format(total ?: 0L)} đ".replace(",", ".")
        }

        // Quan sát Data cho Biểu đồ
        reportViewModel.pieChartDataSelectedMonth.observe(viewLifecycleOwner) { chartData ->
            updatePieChartUI(chartData)
        }

        // Quan sát Data cho Danh sách chi tiết
        reportViewModel.detailListSelectedMonth.observe(viewLifecycleOwner) { fullList ->
            adapter.submitList(fullList)

            if (reportViewModel.isLoadingStructure.value == false) {
                if (fullList.isEmpty()) {
                    binding.layoutContent.visibility = View.GONE
                    binding.cardEmptyState.visibility = View.VISIBLE
                } else {
                    binding.layoutContent.visibility = View.VISIBLE
                    binding.cardEmptyState.visibility = View.GONE
                }
            }
        }
    }

    private fun updatePieChartUI(chartData: List<ExpenseCategoryReport>) {
        if (chartData.isEmpty()) return

        val entries = chartData.map { item ->
            PieEntry(item.percent, "")
        }
        val dataSet = PieDataSet(entries, "").apply {
            colors = chartColors
            setDrawValues(false)
        }
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()

        val legendLayouts = listOf(binding.layoutTop1, binding.layoutTop2, binding.layoutTop3, binding.layoutTop4, binding.layoutOther)
        val nameViews = listOf(binding.tvTop1Name, binding.tvTop2Name, binding.tvTop3Name, binding.tvTop4Name, binding.tvOtherName)
        val percentViews = listOf(binding.tvTop1Percent, binding.tvTop2Percent, binding.tvTop3Percent, binding.tvTop4Percent, binding.tvOtherPercent)
        val colorViews = listOf(binding.viewColor1, binding.viewColor2, binding.viewColor3, binding.viewColor4, binding.viewColorOther)

        legendLayouts.forEach { it.visibility = View.GONE }

        chartData.forEachIndexed { i, item ->
            if (i < legendLayouts.size) {
                legendLayouts[i].visibility = View.VISIBLE
                nameViews[i].text = item.categoryName
                percentViews[i].text = "%.1f%%".format(item.percent).replace(".", ",")
                colorViews[i].backgroundTintList = ColorStateList.valueOf(chartColors[i])
            }
        }
    }

    private fun showMonthSelectionBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_month, null)
        bottomSheetDialog.setContentView(view)

        val tvOptionThisMonth = view.findViewById<TextView>(R.id.tvOptionThisMonth)
        val tvOptionOtherMonth = view.findViewById<TextView>(R.id.tvOptionOtherMonth)

        tvOptionThisMonth.setOnClickListener {
            selectedMonthFilter = "Tháng này"
            binding.tvCurrentMonth.text = "Tháng này"
            reportViewModel.filterExpenseStructureByMonth(selectedMonthFilter)
            bottomSheetDialog.dismiss()
        }

        tvOptionOtherMonth.setOnClickListener {
            bottomSheetDialog.dismiss()
            showDatePickerForOtherMonth()
        }

        bottomSheetDialog.show()
    }

    private fun showDatePickerForOtherMonth() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.pickerMonth)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.pickerYear)

        val calendar = Calendar.getInstance()
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = calendar.get(Calendar.MONTH) + 1

        val currentYear = calendar.get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear + 10
        yearPicker.value = currentYear

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn tháng")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val formattedMonth = String.format("%02d", monthPicker.value)
                val selectedMonthYear = "$formattedMonth/${yearPicker.value}"

                selectedMonthFilter = selectedMonthYear
                binding.tvCurrentMonth.text = selectedMonthYear

                reportViewModel.filterExpenseStructureByMonth(selectedMonthFilter)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}