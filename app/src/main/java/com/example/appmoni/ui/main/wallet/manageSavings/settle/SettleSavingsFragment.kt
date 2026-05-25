package com.example.appmoni.ui.main.wallet.manageSavings.settle

import android.app.DatePickerDialog
import android.graphics.Color
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
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.databinding.FragmentSettleSavingsBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.wallet.SavingsViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class SettleSavingsFragment : Fragment() {

    private var _binding: FragmentSettleSavingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var savingsViewModel: SavingsViewModel
    private var savingsItem: SavingsItem? = null

    // Các biến phục vụ tính toán
    private var maturityDateInMillis: Long = 0L
    private var withdrawDateInMillis: Long = System.currentTimeMillis()

    // Biến kết quả tính toán
    private var calculatedInterest: Long = 0L
    private var totalAmount: Long = 0L

    // Biến lưu Ví nhận tiền
    private var selectedReceiveWalletId: String = ""
    private var selectedReceiveWalletName: String = ""
    private var selectedReceiveWalletIcon: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettleSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savingsViewModel = ViewModelProvider(this).get(SavingsViewModel::class.java)

        // Lấy dữ liệu Sổ từ màn hình trước
        savingsItem = arguments?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable("savings_item", SavingsItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable("savings_item") as? SavingsItem
            }
        }

        savingsItem?.let {
            // Tính ngày đáo hạn: Ngày gửi + Số tháng kỳ hạn
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.depositDateInMillis
            calendar.add(Calendar.MONTH, it.termMonths)
            maturityDateInMillis = calendar.timeInMillis

            // Gán Text mặc định cho ngày tháng
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvMaturityDate.text = dateFormat.format(Date(maturityDateInMillis))
            binding.tvWithdrawDate.text = dateFormat.format(Date(withdrawDateInMillis))

            // Tính lãi lần đầu (mặc định lấy ngày hôm nay)
            calculateAndDisplayAmount()
        }

        setupListeners()
        setupObservers()

        // Hứng dữ liệu Ví nhận tiền từ SelectWalletFragment trả về
        setFragmentResultListener("REQUEST_KEY_WALLET") { _, bundle ->
            selectedReceiveWalletId = bundle.getString("selected_wallet_id") ?: ""
            selectedReceiveWalletName = bundle.getString("selected_wallet_name") ?: ""
            selectedReceiveWalletIcon = bundle.getString("selected_wallet_icon") ?: ""

            // Cập nhật lại Text
            if (selectedReceiveWalletName.isNotEmpty()) {
                binding.tvReceiveWalletName.text = selectedReceiveWalletName
                binding.tvReceiveWalletName.setTextColor(resources.getColor(R.color.black, null))
            }

            // Cập nhật lại Icon
            if (selectedReceiveWalletIcon.isNotEmpty()) {
                val iconResId = resources.getIdentifier(
                    selectedReceiveWalletIcon,
                    "drawable",
                    requireContext().packageName
                )
                if (iconResId != 0) {
                    binding.icReceiveWallet.setImageResource(iconResId)
                }
            }
        }
    }

    // HÀM TÍNH TOÁN LÃI SUẤT
    private fun calculateAndDisplayAmount() {
        if (savingsItem == null) return
        val item = savingsItem!!

        // Tính số ngày thực gửi
        val diffInMillis = withdrawDateInMillis - item.depositDateInMillis
        val exactDays = diffInMillis / (1000 * 60 * 60 * 24)

        if (exactDays <= 0) {
            calculatedInterest = 0L // Rút ngay trong ngày gửi -> Không có lãi
            binding.tvAppliedRate.text = "0% (Rút ngay trong ngày)"
            binding.tvAppliedRate.setTextColor(resources.getColor(R.color.black, null))
        } else if (withdrawDateInMillis >= maturityDateInMillis) {
            // 1. RÚT ĐÚNG HẠN HOẶC QUÁ HẠN -> Lãi suất chuẩn (tính theo tháng)
            val interestDouble = item.amount * (item.interestRate / 100) * item.termMonths / 12
            calculatedInterest = interestDouble.toLong()

            binding.tvAppliedRate.text = "${item.interestRate} %/năm (Đúng hạn)"
            binding.tvAppliedRate.setTextColor(Color.parseColor("#4CAF50")) // Màu xanh
        } else {
            // 2. RÚT TRƯỚC HẠN -> Lãi suất không kỳ hạn (tính theo số ngày thực tế)
            val interestDouble = item.amount * (item.earlyWithdrawalRate / 100) * exactDays / 365
            calculatedInterest = interestDouble.toLong()

            binding.tvAppliedRate.text = "${item.earlyWithdrawalRate} %/năm (Rút trước hạn)"
            binding.tvAppliedRate.setTextColor(Color.parseColor("#FF9800")) // Màu cam cảnh báo
        }

        // Cập nhật tổng tiền hiển thị lên UI
        totalAmount = item.amount + calculatedInterest
        val formatter = DecimalFormat("#,###")
        binding.tvTotalSettleAmount.text = formatter.format(totalAmount).replace(",", ".")
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Chọn ngày rút
        binding.btnSelectWithdrawDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Chọn ví nhận tiền
        binding.btnSelectReceiveWallet.setOnClickListener {
            findNavController().navigate(R.id.action_settleSavingsFragment_to_selectWalletFragment)
        }

        // Nút Tất toán
        binding.btnSettleSavings.setOnClickListener {
            validateAndSettle()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = withdrawDateInMillis

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, day)

                // Không được chọn ngày trong tương lai
                if (newCalendar.timeInMillis > System.currentTimeMillis()) {
                    requireContext().showToast("Ngày tất toán không được lớn hơn ngày hiện tại!",
                        ToastType.WARNING)
                    return@DatePickerDialog
                }

                // Không được chọn ngày rút nhỏ hơn ngày gửi
                if (newCalendar.timeInMillis < savingsItem!!.depositDateInMillis) {
                    requireContext().showToast("Ngày tất toán không được trước ngày gửi!",
                        ToastType.WARNING)
                    return@DatePickerDialog
                }

                withdrawDateInMillis = newCalendar.timeInMillis

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.tvWithdrawDate.text = dateFormat.format(newCalendar.time)

                // Tính toán lại con số tiền lãi
                calculateAndDisplayAmount()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun validateAndSettle() {
        if (selectedReceiveWalletId.isEmpty()) {
            requireContext().showToast("Vui lòng chọn tài khoản nhận tiền!",
                ToastType.WARNING)
            return
        }

        val item = savingsItem ?: return
        val currentUserId = item.userId
        binding.btnSettleSavings.isEnabled = false

        // Tạo giao dịch 1: trả lại tiền gốc
        val principalTx = TransactionItem(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            type = "transfer",
            amount = item.amount,
            dateInMillis = withdrawDateInMillis,
            note = binding.etSettleNote.text.toString().trim().ifEmpty { "Hoàn gốc tất toán sổ: ${item.name}" },
            categoryId = "SYSTEM_SAVINGS",
            categoryName = "Tất toán gốc",
            walletId = item.id,
            walletName = item.name,
            walletIcon = item.bankIcon.ifEmpty { "ic_bank" },
            destWalletId = selectedReceiveWalletId,
            destWalletName = selectedReceiveWalletName,
            destWalletIcon = selectedReceiveWalletIcon
        )

        // Tạo giao dịch 2: thu nhập từ lãi suất
        val interestTx = if (calculatedInterest > 0) {
            TransactionItem(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                type = "income",
                amount = calculatedInterest,
                dateInMillis = withdrawDateInMillis,
                note = "Lãi tiết kiệm sổ: ${item.name}",
                categoryId = "SYSTEM_SAVINGS",
                categoryName = "Lãi tiết kiệm",
                categoryIcon = "ic_category_saving_interest",
                walletId = selectedReceiveWalletId,
                walletName = selectedReceiveWalletName,
                walletIcon = selectedReceiveWalletIcon
            )
        } else null

        savingsViewModel.settleSavings(
            savingsId = item.id,
            userId = currentUserId,
            receiveWalletId = selectedReceiveWalletId,
            principalTransaction = principalTx,
            interestTransaction = interestTx,
            totalAmountToAdd = totalAmount
        )
    }

    private fun setupObservers() {
        savingsViewModel.settleStatus.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.btnSettleSavings.isEnabled = true

                result.onSuccess { msg ->
                    requireContext().showToast(msg, ToastType.SUCCESS)
                    savingsViewModel.resetSettleStatus()

                    findNavController().navigateUp()
                }.onFailure { e ->
                    requireContext().showToast("Lỗi: ${e.message}", ToastType.ERROR)
                    savingsViewModel.resetSettleStatus()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}