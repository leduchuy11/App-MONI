package com.example.appmoni.ui.main.report.currentFinance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.local.AppDatabase
import com.example.appmoni.data.repository.category.CategoryRepository
import com.example.appmoni.data.repository.transaction.TransactionRepository
import com.example.appmoni.databinding.FragmentCurrentFinanceBinding
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.example.appmoni.viewmodel.report.ReportViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat

class CurrentFinanceFragment : Fragment() {

    private var _binding: FragmentCurrentFinanceBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()
    private val reportViewModel: ReportViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
                val txRepo = TransactionRepository(dao)
                return ReportViewModel(txRepo, categoryRepo = CategoryRepository()) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrentFinanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeData()

        // Load dữ liệu ban đầu
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            reportViewModel.loadData(userId)
            transactionViewModel.getAllTransactions(userId).observe(viewLifecycleOwner) {
                if (it != null) reportViewModel.setTransactions(it)
            }
        }
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { findNavController().navigateUp() }

        val toWalletManager = {
            findNavController().navigate(R.id.action_currentFinanceFragment_to_manageSpendingFragment)
        }
        binding.layoutItemCash.setOnClickListener { toWalletManager() }
        binding.layoutItemBank.setOnClickListener { toWalletManager() }
        binding.layoutItemEwallet.setOnClickListener { toWalletManager() }

        binding.layoutItemSavings.setOnClickListener {
            findNavController().navigate(R.id.action_currentFinanceFragment_to_manageSavingsFragment)
        }

        val showDevelopToast = View.OnClickListener {
            Toast.makeText(requireContext(), "Tính năng đang được phát triển!", Toast.LENGTH_SHORT).show()
        }
        binding.layoutItemLend.setOnClickListener(showDevelopToast)
        binding.layoutItemBorrow.setOnClickListener(showDevelopToast)
    }

    private fun observeData() {
        val df = DecimalFormat("#,###")
        val suffix = " đ"

        // Khối tổng quan
        reportViewModel.netWorth.observe(viewLifecycleOwner) {
            binding.tvNetWorth.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }
        reportViewModel.totalAssets.observe(viewLifecycleOwner) {
            binding.tvTotalAssets.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }
        reportViewModel.totalLiabilities.observe(viewLifecycleOwner) {
            binding.tvTotalLiabilities.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }

        // Chi tiết Tổng có
        reportViewModel.cashBalance.observe(viewLifecycleOwner) {
            binding.tvCashAmount.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }
        reportViewModel.bankBalance.observe(viewLifecycleOwner) {
            binding.tvBankAmount.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }
        reportViewModel.eWalletBalance.observe(viewLifecycleOwner) {
            binding.tvEwalletAmount.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }
        reportViewModel.savingsBalance.observe(viewLifecycleOwner) {
            binding.tvSavingsAmount.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }
        reportViewModel.lendBalance.observe(viewLifecycleOwner) {
            binding.tvLendAmount.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }

        // Chi tiết Tổng nợ
        reportViewModel.borrowBalance.observe(viewLifecycleOwner) {
            binding.tvBorrowAmount.text = df.format(it ?: 0L).replace(",", ".") + suffix
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}