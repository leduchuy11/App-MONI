package com.example.appmoni.ui.main.home.history.detailTransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.databinding.FragmentDetailTransactionBinding
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailTransactionFragment : Fragment() {
    private var _binding: FragmentDetailTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()
    private var transaction: TransactionItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nhận dữ liệu từ HistoryFragment gửi sang
        transaction = arguments?.getParcelable("transaction")

        transaction?.let {
            setupUI(it)
        }

        setupListeners()
        setupObservers()
    }

    private fun setupUI(item: TransactionItem) {
        val formatter = DecimalFormat("#,###")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val amountStr = "${formatter.format(item.amount)} đ"
        val dateStr = sdf.format(Date(item.dateInMillis))

        // Hiển thị theo từng loại giao dịch
        when (item.type) {
            "expense" -> {
                binding.tvTypeName.text = "Chi tiền"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_flying_money)
                binding.cardExpense.visibility = View.VISIBLE

                binding.tvExpenseAmount.text = amountStr
                binding.tvExpenseCategory.text = item.categoryName
                binding.tvExpenseWallet.text = item.walletName
                binding.tvExpenseDate.text = dateStr
                binding.tvExpenseNote.text = item.note.ifEmpty { "Không có" }
            }
            "income" -> {
                binding.tvTypeName.text = "Thu tiền"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_collect_money)
                binding.cardIncome.visibility = View.VISIBLE

                binding.tvIncomeAmount.text = amountStr
                binding.tvIncomeCategory.text = item.categoryName
                binding.tvIncomeWallet.text = item.walletName
                binding.tvIncomeDate.text = dateStr
                binding.tvIncomeNote.text = item.note.ifEmpty { "Không có" }
            }
            "transfer" -> {
                binding.tvTypeName.text = "Chuyển khoản"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_transfer)
                binding.cardTransfer.visibility = View.VISIBLE

                binding.tvTransferAmount.text = amountStr
                binding.tvTransferSource.text = item.walletName
                binding.tvTransferDest.text = item.destWalletName
                binding.tvTransferDate.text = dateStr
                binding.tvTransferNote.text = item.note.ifEmpty { "Không có" }
            }
            "lend" -> {
                binding.tvTypeName.text = "Cho vay"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_loan)
                binding.cardLend.visibility = View.VISIBLE

                binding.tvLendAmount.text = amountStr
                binding.tvLendPerson.text = item.personName
                binding.tvLendWallet.text = item.walletName
                binding.tvLendDate.text = dateStr
                binding.tvLendNote.text = item.note.ifEmpty { "Không có" }
            }
            "borrow" -> {
                binding.tvTypeName.text = "Đi vay"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_borrow)
                binding.cardBorrow.visibility = View.VISIBLE

                binding.tvBorrowAmount.text = amountStr
                binding.tvBorrowPerson.text = item.personName
                binding.tvBorrowWallet.text = item.walletName
                binding.tvBorrowDate.text = dateStr
                binding.tvBorrowNote.text = item.note.ifEmpty { "Không có" }
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("   Bạn có chắc chắn muốn xóa giao dịch này? Nếu xóa số tiền sẽ được hoàn trả lại vào ví tương ứng.")
                .setPositiveButton("Xóa") { _, _ ->
                    transaction?.let { viewModel.deleteTransaction(it) }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnDelete.isEnabled = !isLoading
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                if (result.isSuccess) {
                    requireContext().showCustomToast("Đã xóa và cập nhật lại số dư ví!", R.drawable.avatar_app)
                    findNavController().navigateUp()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi không xác định"
                    requireContext().showCustomToast("Lỗi: $errorMsg", R.drawable.avatar_app)
                }
                viewModel.resetDeleteResult()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}