package com.example.appmoni.ui.main.home

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentHomeBinding
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.example.appmoni.viewmodel.wallet.SavingsViewModel
import com.example.appmoni.viewmodel.wallet.WalletViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable

    private val viewModel: TransactionViewModel by viewModels()
    private val walletViewModel: WalletViewModel by viewModels()
    private val savingsViewModel: SavingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            // bật đồng bộ ngay khi mở trang chủ
            viewModel.startSyncing(currentUserId)

            walletViewModel.loadWallets(currentUserId)
            savingsViewModel.loadSavings(currentUserId)

            // Gọi hàm lắng nghe dữ liệu để vẽ biểu đồ
            setupChartObserver(currentUserId)
            setupTotalBalanceObserver()
        }

        setupBanner()
        setupListeners()
    }


    // Hàm lọc dữ liệu & tính toán thu chi tháng này
    private fun setupChartObserver(userId: String) {
        viewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { transactions ->
            if (transactions.isNullOrEmpty()) {
                showEmptyChart()
                return@observe
            }

            // Lấy tháng và năm hiện tại
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            var totalIncome = 0L
            var totalExpense = 0L

            // Lọc các giao dịch trong tháng này
            val currentMonthTransactions = transactions.filter { tx ->
                val txCalendar = Calendar.getInstance()
                txCalendar.timeInMillis = tx.dateInMillis
                txCalendar.get(Calendar.MONTH) == currentMonth && txCalendar.get(Calendar.YEAR) == currentYear
            }

            if (currentMonthTransactions.isEmpty()) {
                showEmptyChart()
                return@observe
            }

            // Phân loại Thu / Chi
            for (tx in currentMonthTransactions) {
                // Thu gồm income và borrow
                if (tx.type == "income" || tx.type == "borrow") {
                    totalIncome += tx.amount
                }
                // Chi gồm expense và lend
                else if (tx.type == "expense" || tx.type == "lend") {
                    totalExpense += tx.amount
                }
            }

            // Nếu tháng này có giao dịch nhưng toàn bộ là 0đ (ví dụ chỉ có chuyển khoản)
            if (totalIncome == 0L && totalExpense == 0L) {
                showEmptyChart()
            } else {
                showChartData(totalIncome, totalExpense)
            }
        }
    }

    // Hàm xử lý giao diện khi không có dữ liệu
    private fun showEmptyChart() {
        binding.cardChart.visibility = View.GONE
        binding.cardChartEmpty.visibility = View.VISIBLE
    }

    // Hàm hiển thị dữ liệu và vẽ biểu đồ bar chart
    private fun showChartData(income: Long, expense: Long) {
        binding.cardChart.visibility = View.VISIBLE
        binding.cardChartEmpty.visibility = View.GONE

        // Cập nhật text số tiền
        val formatter = DecimalFormat("#,###")
        binding.tvChartIncome.text = "${formatter.format(income).replace(",", ".")} đ"
        binding.tvChartExpense.text = "${formatter.format(expense).replace(",", ".")} đ"

        val balance = income - expense
        binding.tvChartBalance.text = "${formatter.format(balance).replace(",", ".")} đ"

        if (balance < 0) {
            binding.tvChartBalance.setTextColor(Color.parseColor("#333333"))
        } else {
            binding.tvChartBalance.setTextColor(Color.parseColor("#333333"))
        }

        // Gọi hàm vẽ biểu đồ
        setupBarChart(income, expense)
    }

    private fun setupBarChart(income: Long, expense: Long) {
        val chart = binding.barChart

        chart.setTouchEnabled(false)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.setScaleEnabled(false)
        chart.isDoubleTapToZoomEnabled = false

        chart.xAxis.isEnabled = false
        chart.axisLeft.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.axisLeft.axisMinimum = 0f

        // Cột 0 là Thu, Cột 1 là Chi
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, income.toFloat()))
        entries.add(BarEntry(0.9f, expense.toFloat()))

        val dataSet = BarDataSet(entries, "Thu Chi")

        dataSet.colors = listOf(
            Color.parseColor("#1aa349"),
            Color.parseColor("#ec453f")
        )
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        barData.barWidth = 0.7f

        chart.renderer = RoundedBarChartRenderer(chart, chart.animator, chart.viewPortHandler, 8f)

        chart.data = barData

        chart.animateY(1000)
        chart.invalidate()
    }

    private fun setupBanner() {
        val imageList = listOf(
            R.drawable.img_banner_1,
            R.drawable.img_banner_2,
            R.drawable.img_banner_3,
            R.drawable.img_banner_4,
            R.drawable.img_banner_5
        )

        val adapter = BannerAdapter(imageList)
        binding.vpBannerHome.adapter = adapter
        TabLayoutMediator(binding.tabIndicatorBanner, binding.vpBannerHome) { _, _ -> }.attach()

        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (binding.vpBannerHome.adapter?.itemCount == 0) return
                val currentItem = binding.vpBannerHome.currentItem
                if (currentItem == imageList.size - 1) {
                    binding.vpBannerHome.setCurrentItem(0, false)
                } else {
                    binding.vpBannerHome.setCurrentItem(currentItem + 1, true)
                }
                autoScrollHandler.postDelayed(this, 5000)
            }
        }
        autoScrollHandler.postDelayed(autoScrollRunnable, 5000)
    }

    private fun setupListeners() {
        binding.llCategoryList.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_manageCategoryFragment)
        }
        binding.llCategoryHistory.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
        }
        binding.llCategoryAccount.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_manageSpendingFragment)
        }
        binding.llCategorySaving.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_manageSavingsFragment)
        }
    }

    // Hàm tính tổng số dư
    private fun setupTotalBalanceObserver() {
        val totalBalanceLiveData = MediatorLiveData<Long>()

        var totalWallet = 0L
        var totalSavings = 0L

        // Lắng nghe sự thay đổi của Wallet
        totalBalanceLiveData.addSource(walletViewModel.walletList) { wallets ->
            totalWallet = 0L
            wallets?.forEach { wallet ->
                if (wallet.isActive) {
                    totalWallet += wallet.balance
                }
            }
            totalBalanceLiveData.value = totalWallet + totalSavings
        }

        // Lắng nghe sự thay đổi của Sổ tiết kiệm
        totalBalanceLiveData.addSource(savingsViewModel.savingsList) { savingsList ->
            totalSavings = 0L
            savingsList?.forEach { saving ->
                if (saving.status == "active") {
                    totalSavings += saving.amount
                }
            }
            totalBalanceLiveData.value = totalWallet + totalSavings
        }

        // Cập nhật UI mỗi khi có thay đổi
        totalBalanceLiveData.observe(viewLifecycleOwner) { total ->
            val formatter = DecimalFormat("#,###")
            val formattedTotal = formatter.format(total ?: 0L).replace(",", ".")
            binding.tvTotalBalance.text = "$formattedTotal đ"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        _binding = null
    }
}