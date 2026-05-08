package com.example.appmoni.ui.main.wallet.manageSavings.detail

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.databinding.FragmentDetailSavingsBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailSavingsFragment : Fragment() {

    private var _binding: FragmentDetailSavingsBinding? = null
    private val binding get() = _binding!!

    private var savingsItem: SavingsItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nhận cục dữ liệu SavingsItem
        savingsItem = arguments?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable("savings_item", SavingsItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable("savings_item") as? SavingsItem
            }
        }

        // Hiển thị dữ liệu lên giao diện
        savingsItem?.let { setupUI(it) }

        setupListeners()
    }

    private fun setupUI(item: SavingsItem) {
        val currencyFormat = DecimalFormat("#,###")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        binding.tvSavingsName.text = item.name
        binding.tvInstitution.text = item.bankName
        binding.tvTerm.text = "${item.termMonths} tháng"
        binding.tvInterestRate.text = "${item.interestRate} %/năm"
        binding.tvEarlyWithdrawalRate.text = "${item.earlyWithdrawalRate} %/năm"
        binding.tvInterestPaymentType.text = item.interestPaymentType

        binding.tvNote.text = item.note.ifEmpty { "Không có ghi chú" }
        binding.tvSourceWallet.text = item.sourceWalletName.ifEmpty { "Không trích từ ví nào" }

        val formattedAmount = currencyFormat.format(item.amount).replace(",", ".")
        binding.tvInitialAmount.text = "$formattedAmount đ"

        binding.tvDepositDate.text = dateFormat.format(Date(item.depositDateInMillis))

        // Công thức: Lãi = Tiền gốc * (Lãi suất / 100) * (Số tháng / 12)
        val interestAmount = item.amount * (item.interestRate / 100.0) * (item.termMonths / 12.0)
        val totalSettlement = item.amount + interestAmount.toLong()

        val formattedTotal = currencyFormat.format(totalSettlement).replace(",", ".")
        binding.tvSettlementAmount.text = "$formattedTotal đ"

        binding.tvDepositDate.text = dateFormat.format(Date(item.depositDateInMillis))

        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = item.depositDateInMillis
        calendar.add(java.util.Calendar.MONTH, item.termMonths)

        binding.tvMaturityDate.text = dateFormat.format(calendar.time)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}