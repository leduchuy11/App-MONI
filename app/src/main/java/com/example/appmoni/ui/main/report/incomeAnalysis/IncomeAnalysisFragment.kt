package com.example.appmoni.ui.main.report.incomeAnalysis

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.report.TimeMode
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.example.appmoni.databinding.FragmentIncomeAnalysisBinding
import com.example.appmoni.ui.main.report.expenseAnalysis.AnalysisCustomMarkerView
import com.example.appmoni.viewmodel.report.IncomeAnalysisViewModel

class IncomeAnalysisFragment : Fragment() {

    private var _binding: FragmentIncomeAnalysisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncomeAnalysisViewModel by viewModels()
    private lateinit var adapter: IncomeAnalysisAdapter

    private var currentMode = TimeMode.DAY
    private var startDate: Long = 0L
    private var endDate: Long = 0L

    private val moneyFormatter =
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = '.'
        })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentMode = TimeMode.DAY
        setDefaultDatesForMode(currentMode)

        setupChart()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        loadData()
    }

    private fun setDefaultDatesForMode(mode: TimeMode) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        when (mode) {
            TimeMode.DAY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                startDate = calendar.timeInMillis

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                )
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                endDate = calendar.timeInMillis
            }

            TimeMode.MONTH -> {
                calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
                startDate = calendar.timeInMillis

                calendar.set(currentYear, Calendar.DECEMBER, 31, 23, 59, 59)
                endDate = calendar.timeInMillis
            }

            TimeMode.YEAR -> {
                calendar.set(currentYear - 3, Calendar.JANUARY, 1, 0, 0, 0)
                startDate = calendar.timeInMillis

                calendar.set(currentYear + 3, Calendar.DECEMBER, 31, 23, 59, 59)
                endDate = calendar.timeInMillis
            }
        }
        updateDateTextViews()
    }

    private fun updateDateTextViews() {
        val pattern = when (currentMode) {
            TimeMode.DAY -> "dd/MM/yyyy"
            TimeMode.MONTH -> "MM/yyyy"
            TimeMode.YEAR -> "yyyy"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        binding.tvDateFrom.text = "Từ: ${sdf.format(Date(startDate))}"
        binding.tvDateTo.text = "Đến: ${sdf.format(Date(endDate))}"

        binding.tvAverageLabel.text = when (currentMode) {
            TimeMode.DAY -> "Trung bình thu/ngày"
            TimeMode.MONTH -> "Trung bình thu/tháng"
            TimeMode.YEAR -> "Trung bình thu/năm"
        }
    }

    private fun setupRecyclerView() {
        adapter = IncomeAnalysisAdapter()
        binding.rvIncomeList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvIncomeList.adapter = adapter
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                verticalAlignment =Legend.LegendVerticalAlignment.TOP
                horizontalAlignment =Legend.LegendHorizontalAlignment.LEFT
                orientation =Legend.LegendOrientation.HORIZONTAL
                setDrawInside(true)
                yOffset = 0f
                xOffset = 0f
                textColor = Color.parseColor("#808080")
                textSize = 12f
            }
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            extraBottomOffset = 10f

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawLabels(false)
                setDrawAxisLine(true)
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = "#EEEEEE".toColorInt()
                axisMinimum = 0f
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentMode = when (tab?.position) {
                    0 -> TimeMode.DAY
                    1 -> TimeMode.MONTH
                    2 -> TimeMode.YEAR
                    else -> TimeMode.DAY
                }
                setDefaultDatesForMode(currentMode)
                loadData()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.layoutDateFrom.setOnClickListener { showPicker(isStartDate = true) }
        binding.layoutDateTo.setOnClickListener { showPicker(isStartDate = false) }
    }

    private fun showPicker(isStartDate: Boolean) {
        when (currentMode) {
            TimeMode.DAY -> showDayPicker(isStartDate)
            TimeMode.MONTH -> showMonthYearPicker(isStartDate)
            TimeMode.YEAR -> showYearPicker(isStartDate)
        }
    }

    private fun showDayPicker(isStartDate: Boolean) {
        val calendar =
            Calendar.getInstance().apply { timeInMillis = if (isStartDate) startDate else endDate }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val sel = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
                val selTime = sel.timeInMillis

                if (isStartDate) {
                    if (selTime > endDate) {
                        requireContext().showCustomToast(
                            "Ngày bắt đầu không được lớn hơn ngày kết thúc",
                            R.drawable.avatar_app
                        )
                        return@DatePickerDialog
                    }
                    startDate = selTime
                } else {
                    if (selTime < startDate) {
                        requireContext().showCustomToast(
                            "Ngày kết thúc không được nhỏ hơn ngày bắt đầu",
                            R.drawable.avatar_app
                        )
                        return@DatePickerDialog
                    }
                    endDate = selTime
                }

                // Giới hạn 91 ngày
                if ((endDate - startDate) / (1000 * 60 * 60 * 24) > 91) {
                    requireContext().showCustomToast(
                        "Tab Ngày hỗ trợ xem tối đa 91 ngày",
                        R.drawable.ic_horizontal_triangle
                    )
                    if (isStartDate) endDate = startDate + (91L * 24 * 60 * 60 * 1000)
                    else startDate = endDate - (91L * 24 * 60 * 60 * 1000)
                }
                updateDateTextViews(); loadData()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showMonthYearPicker(isStartDate: Boolean) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_select_month)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val npMonth = dialog.findViewById<NumberPicker>(R.id.pickerMonth)
        val npYear = dialog.findViewById<NumberPicker>(R.id.pickerYear)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btn_close)
        val btnDone = dialog.findViewById<MaterialButton>(R.id.btn_done)

        val cal =
            Calendar.getInstance().apply { timeInMillis = if (isStartDate) startDate else endDate }

        npMonth.minValue = 1
        npMonth.maxValue = 12
        npMonth.value = cal.get(Calendar.MONTH) + 1

        npYear.minValue = 2000
        npYear.maxValue = 2100
        npYear.value = cal.get(Calendar.YEAR)

        btnClose.setOnClickListener { dialog.dismiss() }

        btnDone.setOnClickListener {
            val selMonth = npMonth.value - 1
            val selYear = npYear.value
            val selCal = Calendar.getInstance()

            if (isStartDate) {
                selCal.set(selYear, selMonth, 1, 0, 0, 0)
                if (selCal.timeInMillis > endDate) {
                    requireContext().showCustomToast(
                        "Tháng bắt đầu không được lớn hơn tháng kết thúc",
                        R.drawable.avatar_app
                    )
                    return@setOnClickListener
                }
                startDate = selCal.timeInMillis
            } else {
                selCal.set(selYear, selMonth, 1, 0, 0, 0)
                selCal.set(
                    Calendar.DAY_OF_MONTH,
                    selCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                )
                selCal.set(Calendar.HOUR_OF_DAY, 23)
                selCal.set(Calendar.MINUTE, 59)
                selCal.set(Calendar.SECOND, 59)

                if (selCal.timeInMillis < startDate) {
                    requireContext().showCustomToast(
                        "Tháng kết thúc không được nhỏ hơn tháng bắt đầu",
                        R.drawable.avatar_app
                    )
                    return@setOnClickListener
                }
                endDate = selCal.timeInMillis
            }
            updateDateTextViews()
            loadData()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showYearPicker(isStartDate: Boolean) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_select_year)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val npYear = dialog.findViewById<NumberPicker>(R.id.np_year)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btn_close)
        val btnDone = dialog.findViewById<MaterialButton>(R.id.btn_done)

        val cal =
            Calendar.getInstance().apply { timeInMillis = if (isStartDate) startDate else endDate }
        npYear.minValue = 2000
        npYear.maxValue = 2100
        npYear.value = cal.get(Calendar.YEAR)

        btnClose.setOnClickListener { dialog.dismiss() }
        btnDone.setOnClickListener {
            val selYear = npYear.value
            val selCal = Calendar.getInstance()

            if (isStartDate) {
                selCal.set(selYear, Calendar.JANUARY, 1, 0, 0, 0)
                if (selCal.timeInMillis > endDate) {
                    requireContext().showCustomToast(
                        "Năm bắt đầu không được lớn hơn năm kết thúc",
                        R.drawable.avatar_app
                    )
                    return@setOnClickListener
                }
                startDate = selCal.timeInMillis
            } else {
                selCal.set(selYear, Calendar.DECEMBER, 31, 23, 59, 59)
                if (selCal.timeInMillis < startDate) {
                    requireContext().showCustomToast(
                        "Năm kết thúc không được nhỏ hơn năm bắt đầu",
                        R.drawable.avatar_app
                    )
                    return@setOnClickListener
                }
                endDate = selCal.timeInMillis
            }
            updateDateTextViews(); loadData()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModel.loadData(userId, startDate, endDate, currentMode)
    }

    private fun observeViewModel() {
        viewModel.totalIncome.observe(viewLifecycleOwner) { total ->
            binding.tvTotalIncome.text = "${moneyFormatter.format(total)} đ"
        }

        viewModel.averageIncome.observe(viewLifecycleOwner) { avg ->
            binding.tvAverageIncome.text = "${moneyFormatter.format(avg)} đ"
        }

        viewModel.analysisList.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                binding.layoutHasData.visibility = View.GONE
                binding.layoutEmptyData.visibility = View.VISIBLE
                return@observe
            } else {
                binding.layoutHasData.visibility = View.VISIBLE
                binding.layoutEmptyData.visibility = View.GONE
            }

            val maxAmount = list.maxOfOrNull { it.amount } ?: 0L
            val (divisor, unitText) = when {
                maxAmount >= 1_000_000_000L -> 1_000_000_000f to "Tỷ"
                maxAmount >= 1_000_000L -> 1_000_000f to "Triệu"
                maxAmount >= 1_000L -> 1_000f to "Nghìn"
                else -> 1f to "Đồng"
            }

            val legendEntry = LegendEntry().apply {
                label = unitText
                form =
                    Legend.LegendForm.NONE
            }
            binding.lineChart.legend.setCustom(listOf(legendEntry))

            val entries = ArrayList<Entry>()
            list.forEachIndexed { index, item ->
                entries.add(Entry(index.toFloat(), (item.amount / divisor)))
            }

            val dataSet = LineDataSet(entries, "").apply {
                color = "#42A5F5".toColorInt()
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = "#BBDEFB".toColorInt()
                fillAlpha = 150
                mode = LineDataSet.Mode.LINEAR
                setDrawHighlightIndicators(true)
                highLightColor = "#90CAF9".toColorInt()
                highlightLineWidth = 1f
            }

            val marker =
                AnalysisCustomMarkerView(requireContext(), R.layout.layout_chart_marker2, list)
            marker.chartView = binding.lineChart
            binding.lineChart.marker = marker

            binding.lineChart.data = LineData(dataSet)
            binding.lineChart.invalidate()

            val listForAdapter = list.filter { it.amount > 0 }
            adapter.setData(listForAdapter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}