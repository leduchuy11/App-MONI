package com.example.appmoni.ui.main.wallet.manageSavings.addEditSavings

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.databinding.FragmentAddSavingsBinding
import com.example.appmoni.ui.addCurrencyFormatter
import com.example.appmoni.ui.parseCurrencyValue
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.wallet.SavingsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class AddSavingsFragment : Fragment() {

    private var _binding: FragmentAddSavingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var savingsViewModel: SavingsViewModel

    private var depositDateInMillis: Long = System.currentTimeMillis()
    private var selectedBankName: String = ""
    private var selectedBankIcon: String = ""

    private var sourceWalletId: String = ""
    private var sourceWalletName: String = ""
    private var sourceWalletIcon: String = ""

    private var receiveWalletId: String = ""
    private var receiveWalletName: String = ""

    // cờ để cho biết đang chọn ví nhận lãi hay ví nguồn
    private var isSelectingSourceWallet = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savingsViewModel = ViewModelProvider(this).get(SavingsViewModel::class.java)

        binding.etSavingsAmount.addCurrencyFormatter()

        setupListeners()
        setupFragmentResultListeners()
        setupObservers()

        restoreUIState()
    }

    private fun setupObservers() {
        savingsViewModel.saveStatus.observe(viewLifecycleOwner) { result ->
            binding.btnSaveSavings.isEnabled = true

            result.onSuccess { message ->
                requireContext().showCustomToast(message, R.drawable.avatar_app)
                findNavController().navigateUp()
            }

            result.onFailure { exception ->
                requireContext().showCustomToast("${exception.message}", R.drawable.avatar_app)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSelectBank.setOnClickListener {
            val bundle = Bundle().apply { putString("type", "bank") }
            findNavController().navigate(
                R.id.action_addSavingsFragment_to_selectInstitutionFragment,
                bundle
            )
        }

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnInterestPaymentType.setOnClickListener {
            showInterestTypeBottomSheet()
        }

        binding.btnSelectSourceAccount.setOnClickListener {
            isSelectingSourceWallet = true
            findNavController().navigate(R.id.action_addSavingsFragment_to_selectWalletFragment)
        }

        binding.btnSelectReceiveAccount.setOnClickListener {
            isSelectingSourceWallet = false
            findNavController().navigate(R.id.action_addSavingsFragment_to_selectWalletFragment)
        }

        binding.btnSaveSavings.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupFragmentResultListeners() {
        setFragmentResultListener("requestKey_institution") { _, bundle ->
            selectedBankName = bundle.getString("selectedShortName") ?: ""
            selectedBankIcon = bundle.getString("selectedIcon") ?: ""

            binding.tvBankName.text = selectedBankName
            binding.tvBankName.setTextColor(resources.getColor(R.color.black))
            val iconResId =
                resources.getIdentifier(selectedBankIcon, "drawable", requireContext().packageName)
            if (iconResId != 0) binding.ivBankIcon.setImageResource(iconResId)
        }

        setFragmentResultListener("REQUEST_KEY_WALLET") { _, bundle ->
            val wId = bundle.getString("selected_wallet_id") ?: ""
            val wName = bundle.getString("selected_wallet_name") ?: ""
            val wIcon = bundle.getString("selected_wallet_icon") ?: ""

            if (isSelectingSourceWallet) {
                sourceWalletId = wId
                sourceWalletName = wName
                sourceWalletIcon = wIcon
                binding.tvSourceAccountName.text = sourceWalletName
                binding.tvSourceAccountName.setTextColor(resources.getColor(R.color.black))
                val iconResId = resources.getIdentifier(
                    sourceWalletIcon,
                    "drawable",
                    requireContext().packageName
                )
                if (iconResId != 0) binding.ivSourceAccountIcon.setImageResource(iconResId)
            } else {
                receiveWalletId = wId
                receiveWalletName = wName
                binding.tvReceiveAccountName.text = receiveWalletName
                binding.tvReceiveAccountName.setTextColor(resources.getColor(R.color.black))
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = depositDateInMillis

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                depositDateInMillis = selectedCalendar.timeInMillis

                val today = Calendar.getInstance()
                if (year == today.get(Calendar.YEAR) && month == today.get(Calendar.MONTH) && dayOfMonth == today.get(
                        Calendar.DAY_OF_MONTH
                    )
                ) {
                    binding.tvDepositDate.text = "Hôm nay"
                } else {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.tvDepositDate.text = sdf.format(selectedCalendar.time)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showInterestTypeBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_interest_type, null)
        bottomSheetDialog.setContentView(view)

        val btnEnd = view.findViewById<View>(R.id.btn_end_of_term)
        val btnStart = view.findViewById<View>(R.id.btn_start_of_term)
        val btnMonthly = view.findViewById<View>(R.id.btn_monthly)

        btnEnd.setOnClickListener {
            binding.tvInterestPaymentType.text = "Cuối kỳ"
            binding.layoutReceiveAccount.visibility = View.GONE
            bottomSheetDialog.dismiss()
        }

        val fakeFeatureListener = View.OnClickListener {
            requireContext().showCustomToast(
                "Chức năng này hiện không khả dụng!",
                R.drawable.avatar_app
            )
            bottomSheetDialog.dismiss()
        }

        btnStart.setOnClickListener(fakeFeatureListener)
        btnMonthly.setOnClickListener(fakeFeatureListener)

        bottomSheetDialog.show()
    }

    private fun validateAndSave() {
        val amount = binding.etSavingsAmount.text.toString().parseCurrencyValue()
        val termMonths = binding.etTermMonths.text.toString().toIntOrNull() ?: 0
        val interestRate = binding.etInterestRate.text.toString().toDoubleOrNull() ?: 0.0

        if (amount <= 0) {
            requireContext().showCustomToast("Vui lòng nhập số tiền", R.drawable.avatar_app)
            return
        }
        if (selectedBankName.isEmpty()) {
            requireContext().showCustomToast("Vui lòng chọn ngân hàng", R.drawable.avatar_app)
            return
        }
        if (termMonths <= 0) {
            requireContext().showCustomToast("Kỳ hạn phải lớn hơn 0", R.drawable.avatar_app)
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        var savingsName = binding.etSavingsName.text.toString()
        if (savingsName.isEmpty()) savingsName = "Sổ $selectedBankName"

        // Lấy đúng ghi chú người dùng nhập
        val userNote = binding.etSavingsNote.text.toString()

        val newSavingsId = UUID.randomUUID().toString()

        // 1. Tạo Model Sổ Tiết Kiệm
        val newSavings = SavingsItem(
            id = newSavingsId,
            userId = userId,
            amount = amount,
            name = savingsName,
            bankName = selectedBankName,
            bankIcon = selectedBankIcon,
            depositDateInMillis = depositDateInMillis,
            termMonths = termMonths,
            interestRate = interestRate,
            earlyWithdrawalRate = binding.etEarlyWithdrawalRate.text.toString().toDoubleOrNull() ?: 0.0,
            interestPaymentType = "Cuối kỳ",
            sourceWalletId = sourceWalletId,
            sourceWalletName = sourceWalletName,
            note = userNote
        )

        // 2. Tạo Model Giao Dịch
        var transferTransaction: TransactionItem? = null
        if (sourceWalletId.isNotEmpty()) {
            transferTransaction = TransactionItem(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = "transfer",
                amount = amount,

                walletId = sourceWalletId,
                walletName = sourceWalletName,
                walletIcon = sourceWalletIcon,
                destWalletId = newSavingsId,
                destWalletName = savingsName,
                destWalletIcon = selectedBankIcon,

                categoryName = "Gửi tiết kiệm",
                categoryIcon = "ic_transfer",

                note = if (userNote.isNotEmpty()) userNote else "Chuyển tiền vào $savingsName",

                dateInMillis = depositDateInMillis
            )
        }

        binding.btnSaveSavings.isEnabled = false

        savingsViewModel.saveSavingsAndTransaction(newSavings, transferTransaction)
    }

    private fun restoreUIState() {
        // Khôi phục UI Ngân hàng
        if (selectedBankName.isNotEmpty()) {
            binding.tvBankName.text = selectedBankName
            binding.tvBankName.setTextColor(resources.getColor(R.color.black))
            val iconResId = resources.getIdentifier(selectedBankIcon, "drawable", requireContext().packageName)
            if (iconResId != 0) binding.ivBankIcon.setImageResource(iconResId)
        }

        // Khôi phục UI Ví nguồn (Trích tiền từ)
        if (sourceWalletName.isNotEmpty()) {
            binding.tvSourceAccountName.text = sourceWalletName
            binding.tvSourceAccountName.setTextColor(resources.getColor(R.color.black))
            val iconResId = resources.getIdentifier(sourceWalletIcon, "drawable", requireContext().packageName)
            if (iconResId != 0) binding.ivSourceAccountIcon.setImageResource(iconResId)
        }

        // Khôi phục UI Ví nhận lãi (nếu có)
        if (receiveWalletName.isNotEmpty()) {
            binding.tvReceiveAccountName.text = receiveWalletName
            binding.tvReceiveAccountName.setTextColor(resources.getColor(R.color.black))
        }

        // Khôi phục UI Ngày gửi
        val today = Calendar.getInstance()
        val selectedCalendar = Calendar.getInstance().apply { timeInMillis = depositDateInMillis }
        if (selectedCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            selectedCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            binding.tvDepositDate.text = "Hôm nay"
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvDepositDate.text = sdf.format(selectedCalendar.time)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}