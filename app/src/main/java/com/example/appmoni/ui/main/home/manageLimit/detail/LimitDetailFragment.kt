package com.example.appmoni.ui.main.home.manageLimit.detail

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.limit.LimitItem
import com.example.appmoni.databinding.FragmentLimitDetailBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.home.LimitDetailViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class LimitDetailFragment : Fragment() {

    private var _binding: FragmentLimitDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LimitDetailViewModel

    // Biến hứng dữ liệu từ màn trước truyền sang
    private var currentLimit: LimitItem? = null


    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLimitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nhận dữ liệu
        currentLimit = arguments?.getParcelable("limit_item")

        if (currentLimit == null) {
            requireContext().showToast("Lỗi tải dữ liệu hạn mức", ToastType.ERROR)
            findNavController().popBackStack()
            return
        }

        viewModel = ViewModelProvider(this)[LimitDetailViewModel::class.java]

        // Setup Giao diện
        setupListeners()
        bindOverviewData()
        bindDetailData()

        // Setup Biểu đồ
        setupChartUI()
        viewModel.loadChartData(currentLimit!!)
        viewModel.loadCategoryNames(currentLimit!!.userId, currentLimit!!.categoryIds)
        observeChartData()
        viewModel.categoryNames.observe(viewLifecycleOwner) { namesStr ->
            binding.tvDetailCategories.text = namesStr
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // đổ dữ liệu vào card 1
    private fun bindOverviewData() {
        val limit = currentLimit!!

        binding.tvHeaderTitle.text = limit.name
        binding.tvTotalAmount.text = "%,d đ".format(limit.amount).replace(',', '.')
        binding.tvSpentAmount.text = "Đã tiêu %,d đ".format(limit.spentAmount).replace(',', '.')

        val remaining = limit.amount - limit.spentAmount
        val progressPercent = if (limit.amount > 0) {
            ((limit.spentAmount.toDouble() / limit.amount.toDouble()) * 100).toInt()
        } else 0

        if (remaining >= 0) {
            binding.tvRemainingAmount.text = "Còn %,d đ".format(remaining).replace(',', '.')
            binding.tvRemainingAmount.setTextColor(Color.parseColor("#808080")) // Xám
            binding.progressBar.setIndicatorColor(Color.parseColor("#FF9800")) // Cam
            binding.progressBar.progress = progressPercent
        } else {
            val overspent = kotlin.math.abs(remaining)
            binding.tvRemainingAmount.text = "Vượt %,d đ".format(overspent).replace(',', '.')
            binding.tvRemainingAmount.setTextColor(Color.parseColor("#E53935")) // Đỏ
            binding.progressBar.setIndicatorColor(Color.parseColor("#E53935")) // Đỏ
            binding.progressBar.progress = 100
        }
    }

    // Đổ dữ liệu vào card 2
    private fun bindDetailData() {
        val limit = currentLimit!!

        // Ngân sách
        binding.tvDetailBudget.text = "%,d đ".format(limit.amount).replace(',', '.')

        // Hạng mục chi
        if (limit.categoryIds.contains("all")) {
            binding.tvDetailCategories.text = "Tất cả hạng mục"
        } else {
            binding.tvDetailCategories.text =
                "Đang tải..."
        }

        val context = binding.root.context
        val resId = context.resources.getIdentifier(limit.icon, "drawable", context.packageName)
        if (resId != 0) {
            binding.ivCategoryIcon.setImageResource(resId)
        } else {
            val defaultResId = context.resources.getIdentifier("ic_all_in", "drawable", context.packageName)
            if (defaultResId != 0) binding.ivCategoryIcon.setImageResource(defaultResId)
        }

        // Tài khoản
        val walletText = if (limit.walletId == "all") "Tất cả tài khoản" else ""
        binding.tvDetailWallets.text = walletText

        // Ngày áp dụng
        val startDate = dateFormatter.format(limit.startDateInMillis)
        val endDate = dateFormatter.format(limit.endDateInMillis)
        binding.tvDetailDate.text = "$startDate - $endDate"
    }

    // Cấu hình giao diện biểu đồ đường
    private fun setupChartUI() {
        val lineChart = binding.lineChart

        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false)

        // Trục X
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.parseColor("#808080")
        xAxis.textSize = 10f

        // Trục Y
        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#F0F0F0")
        leftAxis.textColor = Color.parseColor("#808080")
        leftAxis.textSize = 10f
        leftAxis.axisMinimum = 0f
        leftAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            private val format = java.text.DecimalFormat("#.##")

            override fun getFormattedValue(value: Float): String {
                return when {
                    value >= 1_000_000_000 -> "${format.format(value / 1_000_000_000)} Tỷ"
                    value >= 1_000_000 -> "${format.format(value / 1_000_000)} Tr"
                    value >= 1_000 -> "${format.format(value / 1_000)} K"
                    else -> format.format(value)
                }
            }
        }

        lineChart.axisRight.isEnabled = false
    }

    private fun observeChartData() {
        viewModel.chartData.observe(viewLifecycleOwner) { pair ->
            val entries = pair.first
            val xLabels = pair.second

            binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)

            val dataSet = LineDataSet(entries, "Chi tiêu")
            dataSet.color = Color.parseColor("#167AC5")
            dataSet.lineWidth = 2.5f

            dataSet.setDrawValues(false)
            dataSet.setDrawCircles(false)

            dataSet.setDrawFilled(true)
            dataSet.fillColor = Color.parseColor("#BBDEFB")
            dataSet.fillAlpha = 50

            dataSet.highLightColor = Color.parseColor("#167AC5")
            dataSet.highlightLineWidth = 1.5f
            dataSet.setDrawHorizontalHighlightIndicator(false)

            binding.lineChart.data = LineData(dataSet)

            val markerView =
                CustomMarkerView(requireContext(), R.layout.layout_chart_marker, xLabels)
            markerView.chartView = binding.lineChart
            binding.lineChart.marker = markerView

            binding.lineChart.invalidate()
            binding.lineChart.animateX(1000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}