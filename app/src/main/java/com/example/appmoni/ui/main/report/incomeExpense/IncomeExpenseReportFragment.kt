package com.example.appmoni.ui.main.report.incomeExpense

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.data.model.report.MonthlyReportItem
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.databinding.FragmentIncomeExpenseReportBinding
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.example.appmoni.viewmodel.report.IncomeExpenseReportViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.util.Calendar
import android.app.AlertDialog
import android.widget.NumberPicker
import android.graphics.drawable.ColorDrawable
import com.example.appmoni.R

class IncomeExpenseReportFragment : Fragment() {
    private var _binding: FragmentIncomeExpenseReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncomeExpenseReportViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private var allTransactions = listOf<TransactionItem>()

    private val colorIncome = Color.parseColor("#1aa349") // Xanh
    private val colorExpense = Color.parseColor("#ec453f") // Đỏ

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeExpenseReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupYearSelector()
        observeData()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupYearSelector() {
        binding.tvSelectedYear.text = "Năm $selectedYear"

        binding.cardYearSelector.setOnClickListener {
            showYearPickerDialog()
        }
    }

    private fun showYearPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_year, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val npYear = dialogView.findViewById<NumberPicker>(R.id.np_year)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close)
        val btnDone = dialogView.findViewById<View>(R.id.btn_done)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        npYear.minValue = currentYear - 10
        npYear.maxValue = currentYear + 10
        npYear.value = selectedYear
        npYear.wrapSelectorWheel = false

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnDone.setOnClickListener {
            selectedYear = npYear.value

            binding.tvSelectedYear.text = "Năm $selectedYear"

            viewModel.processYearlyData(allTransactions, selectedYear)

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        transactionViewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { list ->
            if (list != null) {
                allTransactions = list
                viewModel.processYearlyData(allTransactions, selectedYear)
            }
        }

        viewModel.monthlyData.observe(viewLifecycleOwner) { data ->
            setupBarChart(data)
            setupRecyclerView(data)
        }
    }

    private fun setupBarChart(data: List<MonthlyReportItem>) {
        val chart = binding.barChartYearly

        if (data.isEmpty()) {
            binding.cardChart.visibility = View.GONE
            binding.rvMonthlyList.visibility = View.GONE
            binding.layoutEmptyData.visibility = View.VISIBLE
            return
        }

        binding.cardChart.visibility = View.VISIBLE
        binding.rvMonthlyList.visibility = View.VISIBLE
        binding.layoutEmptyData.visibility = View.GONE

        // Tìm số lớn nhất để quy đổi đơn vị tiền
        var maxVal = 0L
        data.forEach {
            if (it.income > maxVal) maxVal = it.income
            if (it.expense > maxVal) maxVal = it.expense
        }

        val divisor: Float
        val unitText: String
        when {
            maxVal >= 1_000_000_000L -> {
                divisor = 1_000_000_000f; unitText = "Tỷ"
            }

            maxVal >= 1_000_000L -> {
                divisor = 1_000_000f; unitText = "Triệu"
            }

            maxVal >= 1_000L -> {
                divisor = 1_000f; unitText = "Nghìn"
            }

            else -> {
                divisor = 1f; unitText = "Đồng"
            }
        }

        chart.renderer = RoundedStackedBarChartRenderer(
            chart, chart.animator, chart.viewPortHandler, 5f, colorIncome, colorExpense
        )

        // Cấu hình giao diện biểu đồ
        chart.apply {
            setTouchEnabled(false)
            legend.isEnabled = false
            extraTopOffset = 35f

            description.apply {
                isEnabled = true
                text = unitText
                textSize = 12f
                textColor = Color.parseColor("#808080")
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(true)
                axisLineColor = Color.parseColor("#CCCCCC")
                axisMinimum = 0.5f
                axisMaximum = 12.5f
                labelCount = 12
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(
                        value: Float,
                        axis: com.github.mikephil.charting.components.AxisBase?
                    ): String {
                        val v = value.toInt()
                        return if (v in 1..12) v.toString() else ""
                    }
                }
            }

            axisLeft.apply {
                isEnabled = true
                setDrawLabels(true)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                setDrawAxisLine(true)
                axisLineColor = Color.parseColor("#CCCCCC")

                resetAxisMinimum()

                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(
                        value: Float,
                        axis: com.github.mikephil.charting.components.AxisBase?
                    ): String {
                        return if (value == 0f) "0" else DecimalFormat("#.#").format(value)
                    }
                }
            }
            axisRight.isEnabled = false
        }

        // Chuẩn bị data và bơm vào biểu đồ
        val entries = mutableListOf<BarEntry>()
        for (m in 1..12) {
            val item = data.find { it.month == m }
            val inc = (item?.income ?: 0L) / divisor
            val exp = (item?.expense ?: 0L) / divisor
            entries.add(BarEntry(m.toFloat(), floatArrayOf(-exp, inc)))
        }

        val dataSet = BarDataSet(entries, "").apply {
            colors = listOf(colorExpense, colorIncome)
            setDrawValues(false)
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        chart.data = barData
        chart.animateY(1000)

        chart.post {
            chart.description.setPosition(chart.viewPortHandler.offsetLeft() + 15f, 25f)
            chart.invalidate()
        }
    }

    private fun setupRecyclerView(data: List<MonthlyReportItem>) {
        val adapter = MonthlyReportAdapter(data)
        binding.rvMonthlyList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMonthlyList.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}