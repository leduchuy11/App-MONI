package com.example.appmoni.ui.main.report.debtTracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.databinding.FragmentDebtTrackingBinding
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class DebtTrackingFragment : Fragment() {

    private var _binding: FragmentDebtTrackingBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()

    private lateinit var adapter: DebtTrackingAdapter
    private var allTransactions: List<TransactionItem> = emptyList()

    // Biến lưu vị trí tab
    private var savedTabPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebtTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments?.containsKey("initial_tab_position") == true) {
            savedTabPosition = arguments?.getInt("initial_tab_position", 0) ?: 0
            arguments?.remove("initial_tab_position")
        }

        setupUI()
        setupListeners()

        binding.tabLayout.getTabAt(savedTabPosition)?.select()

        observeData()
    }

    private fun setupUI() {
        adapter = DebtTrackingAdapter { item ->
            val bundle = Bundle().apply {
                putParcelable("transaction_item", item)
            }
            findNavController().navigate(R.id.action_debtTrackingFragment_to_debtDetailFragment, bundle)
        }

        binding.rvDebts.adapter = adapter
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Bắt sự kiện chuyển Tab
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                savedTabPosition = tab?.position ?: 0
                filterAndDisplayData()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

    }

    private fun observeData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.rvDebts.visibility = View.GONE
        binding.cardEmptyState.visibility = View.GONE

        transactionViewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { list ->
            binding.progressBar.visibility = View.GONE
            if (list != null) {
                allTransactions = list
                filterAndDisplayData()
            }
        }
    }

    // Hàm Lọc và Hiển thị giao diện
    private fun filterAndDisplayData() {
        // Lấy vị trí Tab đang chọn: 0 là "Cho vay", 1 là "Còn nợ"
        val isLendTab = binding.tabLayout.selectedTabPosition == 0

        val filteredList = if (isLendTab) {
            // Tab Cho vay: Lấy các khoản 'lend' và chưa thanh toán
            allTransactions.filter { it.type == "lend" && !it.isPaid }
        } else {
            // Tab Còn nợ (Mình đi vay): Lấy các khoản 'borrow' và chưa thanh toán
            allTransactions.filter { it.type == "borrow" && !it.isPaid }
        }

        adapter.submitList(filteredList)

        // Cập nhật trạng thái UI
        if (filteredList.isEmpty()) {
            binding.rvDebts.visibility = View.GONE
            binding.cardEmptyState.visibility = View.VISIBLE

            binding.tvEmptyState.text = if (isLendTab) {
                "Hiện không có khoản cho vay nào"
            } else {
                "Hiện không có khoản nợ nào"
            }
        } else {
            binding.rvDebts.visibility = View.VISIBLE
            binding.cardEmptyState.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}