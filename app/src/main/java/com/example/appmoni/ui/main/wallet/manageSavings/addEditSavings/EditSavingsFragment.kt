package com.example.appmoni.ui.main.wallet.manageSavings.addEditSavings

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.databinding.FragmentEditSavingsBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.addCurrencyFormatter
import com.example.appmoni.ui.parseCurrencyValue
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.wallet.SavingsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditSavingsFragment : Fragment() {

    private var _binding: FragmentEditSavingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var savingsViewModel: SavingsViewModel

    // Biến lưu trữ Sổ tiết kiệm gốc (Dữ liệu cũ)
    private var oldSavingsItem: SavingsItem? = null

    // Các biến phụ để lưu trữ dữ liệu thay đổi tạm thời
    private var currentDepositDateInMillis: Long = 0L
    private var currentBankName: String = ""
    private var currentBankIcon: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savingsViewModel = ViewModelProvider(this).get(SavingsViewModel::class.java)

        // Nhận dữ liệu cũ từ Bundle
        oldSavingsItem = arguments?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable("savings_item", SavingsItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable("savings_item") as? SavingsItem
            }
        }

        // Đổ dữ liệu lên UI
        oldSavingsItem?.let { fillDataToUI(it) }

        setupListeners()
        setupObservers()

        setFragmentResultListener("requestKey_institution") { _, bundle ->
            currentBankName = bundle.getString("selectedShortName") ?: ""
            currentBankIcon = bundle.getString("selectedIcon") ?: ""

            binding.tvBankName.text = currentBankName

            if (currentBankIcon.isNotEmpty()) {
                val iconResId = resources.getIdentifier(currentBankIcon, "drawable", requireContext().packageName)
                if (iconResId != 0) {
                    binding.ivBankIcon.setImageResource(iconResId)
                }
            }
        }
    }

    private fun fillDataToUI(item: SavingsItem) {
        currentDepositDateInMillis = item.depositDateInMillis
        currentBankName = item.bankName
        currentBankIcon = item.bankIcon

        // Đổ text vào các EditText
        val formatter = java.text.DecimalFormat("#,###")
        val formattedAmount = formatter.format(item.amount).replace(",", ".")
        binding.etSavingsAmount.setText(formattedAmount)
        binding.etSavingsName.setText(item.name)
        binding.etTermMonths.setText(item.termMonths.toString())
        binding.etInterestRate.setText(item.interestRate.toString())
        binding.etEarlyWithdrawalRate.setText(item.earlyWithdrawalRate.toString())
        binding.etSavingsNote.setText(item.note)

        // Text tĩnh
        binding.tvBankName.text = item.bankName.ifEmpty { "Chưa chọn ngân hàng" }
        binding.tvInterestPaymentType.text = item.interestPaymentType

        if (item.bankIcon.isNotEmpty()) {
            val iconResId = resources.getIdentifier(item.bankIcon, "drawable", requireContext().packageName)
            if (iconResId != 0) {
                binding.ivBankIcon.setImageResource(iconResId)
            }
        }

        // 2. Ô VÍ NGUỒN: Đổ text và đổi Icon
        if (item.sourceWalletName.isNotEmpty()) {
            binding.tvSourceAccountName.text = "Tiền chuyển từ ${item.sourceWalletName}"
            binding.ivSourceAccountIcon.setImageResource(R.drawable.ic_record_wallet1)
            binding.ivSourceAccountIcon.clearColorFilter()
        } else {
            binding.tvSourceAccountName.text = "Không trích từ ví nào"
        }

        // Ngày tháng
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvDepositDate.text = dateFormat.format(Date(item.depositDateInMillis))
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.etSavingsAmount.addCurrencyFormatter()

        // Mở lịch để đổi ngày gửi
        binding.btnSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Nút Lưu thay đổi
        binding.btnSaveSavings.setOnClickListener {
            saveEditedSavings()
        }
        // Nút trả lãi
        binding.btnInterestPaymentType.setOnClickListener {
            showInterestPaymentBottomSheet()
        }

        binding.btnSelectBank.setOnClickListener {
            val bundle = Bundle().apply {
                putString("type", "bank")
            }
            findNavController().navigate(
                R.id.action_editSavingsFragment_to_selectInstitutionFragment,
                bundle
            )
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDepositDateInMillis

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(selectedYear, selectedMonth, selectedDay)
                currentDepositDateInMillis = newCalendar.timeInMillis

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.tvDepositDate.text = dateFormat.format(newCalendar.time)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun saveEditedSavings() {
        if (oldSavingsItem == null) return

        // Lấy dữ liệu mới từ giao diện
        val newAmount = binding.etSavingsAmount.text.toString().parseCurrencyValue()

        if (newAmount <= 0) {
            requireContext().showToast("Số tiền phải lớn hơn 0", ToastType.WARNING)
            return
        }

        val newTerm = binding.etTermMonths.text.toString().toIntOrNull() ?: 0
        if (newTerm <= 0) {
            requireContext().showToast("Kỳ hạn phải lớn hơn 0", ToastType.WARNING)
            return
        }

        val newInterestRate = binding.etInterestRate.text.toString().toDoubleOrNull() ?: 0.0
        val newEarlyRate = binding.etEarlyWithdrawalRate.text.toString().toDoubleOrNull() ?: 0.1

        // Tạo ra object mới mang dữ liệu đã sửa
        val newSavingsItem = oldSavingsItem!!.copy(
            amount = newAmount,
            name = binding.etSavingsName.text.toString().trim(),
            bankName = currentBankName,
            bankIcon = currentBankIcon,
            depositDateInMillis = currentDepositDateInMillis,
            termMonths = newTerm,
            interestRate = newInterestRate,
            earlyWithdrawalRate = newEarlyRate,
            note = binding.etSavingsNote.text.toString().trim()
            // Không copy/sửa sourceWalletId hay sourceWalletName ở đây
        )

        binding.btnSaveSavings.isEnabled = false
        savingsViewModel.updateSavings(oldSavingsItem!!, newSavingsItem)
    }

    private fun setupObservers() {
        savingsViewModel.updateStatus.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.btnSaveSavings.isEnabled = true

                result.onSuccess { msg ->
                    requireContext().showToast(msg, ToastType.SUCCESS)
                    savingsViewModel.resetUpdateStatus()

                    findNavController().navigateUp()
                }.onFailure { e ->
                    requireContext().showToast("Lỗi: ${e.message}", ToastType.ERROR)
                    savingsViewModel.resetUpdateStatus()
                }
            }
        }
    }

    private fun showInterestPaymentBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())

        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_interest_type, null)
        bottomSheetDialog.setContentView(view)

        view.findViewById<View>(R.id.btn_end_of_term).setOnClickListener {
            binding.tvInterestPaymentType.text = "Cuối kỳ"
            bottomSheetDialog.dismiss()
        }

        view.findViewById<View>(R.id.btn_start_of_term).setOnClickListener {
            binding.tvInterestPaymentType.text = "Đầu kỳ"
            requireContext().showToast("Chức năng này hiện không khả dụng!", ToastType.WARNING)
            bottomSheetDialog.dismiss()
        }

        view.findViewById<View>(R.id.btn_monthly).setOnClickListener {
            binding.tvInterestPaymentType.text = "Định kỳ hằng tháng"
            requireContext().showToast("Chức năng này hiện không khả dụng!", ToastType.WARNING)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}