package com.example.appmoni.ui.main.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appmoni.data.local.AppDatabase
import com.example.appmoni.data.repository.transaction.TransactionRepository
import com.example.appmoni.databinding.FragmentReportBinding
import com.example.appmoni.viewmodel.report.ReportViewModel
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import androidx.core.graphics.toColorInt
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.report.ExpenseCategoryReport

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()

    private val reportViewModel: ReportViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
                val txRepo = TransactionRepository(dao)
                val catRepo = com.example.appmoni.data.repository.category.CategoryRepository()

                return ReportViewModel(txRepo, categoryRepo = catRepo) as T
            }
        }
    }

    private val chartColors = listOf(
        "#ffbc37", "#ef5361", "#33c09d", "#9b7bea", "#0dafef"
    ).map { it.toColorInt() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            reportViewModel.loadData(userId)

            transactionViewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { txList ->
                if (txList != null) {
                    reportViewModel.setTransactions(txList)
                }
            }
        }

        observeFinanceData()
        setupPieChart()
        setupListeners()

        reportViewModel.expenseStructure.observe(viewLifecycleOwner) { structure ->
            updatePieChartUI(structure)
        }
    }

    private fun setupListeners() {
        binding.tvStructureDetail.setOnClickListener {
            findNavController().navigate(R.id.action_reportFragment_to_expenseStructureFragment)
        }
        binding.cardCurrentFinance.setOnClickListener {
            findNavController().navigate(R.id.action_reportFragment_to_currentFinanceFragment)
        }
        binding.cardFinanceDetail.setOnClickListener {
            findNavController().navigate(R.id.action_reportFragment_to_currentFinanceFragment)
        }
        binding.cardBtnDebtTracking.setOnClickListener {
            findNavController().navigate(R.id.action_reportFragment_to_debtTrackingFragment)
        }
        binding.cardBtnCashflow.setOnClickListener {
            findNavController().navigate(R.id.action_reportFragment_to_incomeExpenseReportFragment)
        }
        binding.cardBtnExpenseAnalysis.setOnClickListener {
            findNavController().navigate(R.id.action_reportFragment_to_expenseAnalysisFragment)
        }
        binding.cardBtnIncomeAnalysis.setOnClickListener {
            findNavController().navigate(R.id.action_reportFragment_to_incomeAnalysisFragment)
        }
    }

    // Quan sát 3 biến tổng và in ra UI
    private fun observeFinanceData() {
        val formatter = DecimalFormat("#,###")

        // Tổng Có
        reportViewModel.totalAssets.observe(viewLifecycleOwner) { amount ->
            binding.tvTotalAssets.text = formatter.format(amount ?: 0L).replace(",", ".")
        }

        // Tổng Nợ
        reportViewModel.totalLiabilities.observe(viewLifecycleOwner) { amount ->
            binding.tvTotalLiabilities.text = formatter.format(amount ?: 0L).replace(",", ".")
        }

        // Tài sản ròng
        reportViewModel.netWorth.observe(viewLifecycleOwner) { amount ->
            val formatted = formatter.format(amount ?: 0L).replace(",", ".")
            binding.tvFinanceTotal.text = "$formatted đ"

            if ((amount ?: 0L) < 0) {
                binding.tvFinanceTotal.setTextColor(android.graphics.Color.parseColor("#fc565b")) // Âm thì đổi đỏ
            } else {
                binding.tvFinanceTotal.setTextColor(android.graphics.Color.parseColor("#0088FF")) // Dương xanh
            }
        }
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            isDrawHoleEnabled = true
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setDrawCenterText(false)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            transparentCircleRadius = 0f
            holeRadius = 60f
            animateY(1000)
        }
    }

    private fun updatePieChartUI(data: List<ExpenseCategoryReport>) {
        binding.cardExpenseStructure.visibility = View.VISIBLE

        if (data.isEmpty()) {
            binding.layoutChartContent.visibility = View.GONE
            binding.layoutEmptyChart.visibility = View.VISIBLE
            return
        }

        binding.layoutChartContent.visibility = View.VISIBLE
        binding.layoutEmptyChart.visibility = View.GONE

        val entries = data.mapIndexed { index, item ->
            com.github.mikephil.charting.data.PieEntry(item.percent, "")
        }
        val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "").apply {
            colors = chartColors
            setDrawValues(false)
        }
        binding.pieChart.data = com.github.mikephil.charting.data.PieData(dataSet)
        binding.pieChart.invalidate()

        val legendLayouts = listOf(
            binding.layoutTop1,
            binding.layoutTop2,
            binding.layoutTop3,
            binding.layoutTop4,
            binding.layoutOther
        )
        val nameViews = listOf(
            binding.tvTop1Name,
            binding.tvTop2Name,
            binding.tvTop3Name,
            binding.tvTop4Name,
            binding.tvOtherName
        )
        val percentViews = listOf(
            binding.tvTop1Percent,
            binding.tvTop2Percent,
            binding.tvTop3Percent,
            binding.tvTop4Percent,
            binding.tvOtherPercent
        )
        val colorViews = listOf(
            binding.viewColor1,
            binding.viewColor2,
            binding.viewColor3,
            binding.viewColor4,
            binding.viewColorOther
        )

        legendLayouts.forEach { it.visibility = View.GONE }

        data.forEachIndexed { i, item ->
            if (i < legendLayouts.size) {
                legendLayouts[i].visibility = View.VISIBLE
                nameViews[i].text = item.categoryName
                percentViews[i].text = "%.1f%%".format(item.percent)
                colorViews[i].backgroundTintList = android.content.res.ColorStateList.valueOf(chartColors[i])
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}