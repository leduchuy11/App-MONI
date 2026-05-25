package com.example.appmoni.ui.main.report.debtTracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.databinding.FragmentDebtDetailBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebtDetailFragment : Fragment() {

    private var _binding: FragmentDebtDetailBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()

    // Biến lưu trữ thông tin vay/nợ truyền từ màn trước
    private var currentTransaction: TransactionItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebtDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentTransaction = arguments?.getParcelable("transaction_item")

        if (currentTransaction == null) {
            requireContext().showToast("Lỗi tải dữ liệu!", ToastType.ERROR)
            findNavController().navigateUp()
            return
        }

        setupUI()
        setupListeners()
        observeData()
    }

    private fun setupUI() {
        val item = currentTransaction!!
        val formatter = DecimalFormat("#,###")
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        binding.tvPersonNameHeader.text = item.personName.ifEmpty { "Khách " }
        binding.tvAmount.text = "${formatter.format(item.amount)} đ".replace(",", ".")
        binding.tvWalletName.text = item.walletName
        binding.tvDate.text = dateFormatter.format(Date(item.dateInMillis))
        binding.tvNote.text = item.note.ifEmpty { "Không có" }

        if (item.type == "borrow") {
            binding.btnAction.text = "Đánh dấu đã trả nợ"
        } else {
            binding.btnAction.text = "Đánh dấu đã thu nợ"
        }
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAction.setOnClickListener {
            currentTransaction?.let { transaction ->
                showConfirmationDialog(transaction)
            }
        }
    }

    // Hàm hiển thị hộp thoại xác nhận
    private fun showConfirmationDialog(transaction: TransactionItem) {
        val title =
            if (transaction.type == "borrow") "Xác nhận đã trả nợ?" else "Xác nhận đã thu nợ?"
        val message =
            "Khoản này sẽ được gạch bỏ khỏi danh sách theo dõi.\nLưu ý: Hành động này chỉ mang tính chất ghi nhớ, không làm thay đổi số dư ví hiện tại của bạn và không thể hoàn tác."

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Đồng ý") { dialog, _ ->
                transactionViewModel.markDebtAsPaid(transaction)
                dialog.dismiss()
            }
            .show()
    }

    private fun observeData() {
        transactionViewModel.updateStatusResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                if (result.isSuccess) {
                    requireContext().showToast("Đã bỏ theo dõi", ToastType.SUCCESS)
                    findNavController().navigateUp()
                } else {
                    requireContext().showToast("Có lỗi xảy ra, vui lòng thử lại!", ToastType.ERROR)
                }
                transactionViewModel.resetUpdateStatus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}