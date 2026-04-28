package com.example.appmoni.ui.main.wallet.manageSpending.adjustBalance

import com.example.appmoni.viewmodel.record.TransactionViewModel
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.databinding.FragmentAdjustBalanceBinding
import com.example.appmoni.ui.showCustomToast
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat

class AdjustBalanceFragment : Fragment() {

    private var _binding: FragmentAdjustBalanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionViewModel: TransactionViewModel
    private var currentWallet: WalletItem? = null

    private var currentBalance: Long = 0L
    private var differenceAmount: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdjustBalanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel = ViewModelProvider(this).get(TransactionViewModel::class.java)

        // NHẬN DỮ LIỆU VÍ TỪ MÀN HÌNH TRƯỚC VÀ ĐỔ LÊN UI
        currentWallet = arguments?.getSerializable("walletItem") as? WalletItem
        currentWallet?.let { wallet ->
            currentBalance = wallet.balance
            fillDataToUI(wallet)
        }

        setupBalanceCalculator()
        setupClickListeners()
        setupObservers()
    }

    private fun fillDataToUI(wallet: WalletItem) {
        val formatter = DecimalFormat("#,###")

        // Đổ số dư hiện tại trên app
        val formattedCurrentBalance = formatter.format(currentBalance).replace(",", ".")
        binding.tvCurrentBalance.text = "$formattedCurrentBalance đ"

        // đổ dữ liệu vào thẻ card
        binding.etWalletName.setText(wallet.name)

        when (wallet.accountType) {
            "cash" -> {
                binding.tvAccountType.text = "Tiền mặt"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_money)
            }

            "bank" -> {
                binding.tvAccountType.text = "Tài khoản ngân hàng"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_bank)
            }

            "ewallet" -> {
                binding.tvAccountType.text = "Ví điện tử"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_e_wallet)
            }
        }

        binding.layoutAccountName.visibility = View.VISIBLE
        binding.layoutSelectBank.visibility = View.GONE
        binding.layoutSelectEwallet.visibility = View.GONE

        when (wallet.accountType) {
            "bank" -> {
                binding.layoutSelectBank.visibility = View.VISIBLE
                binding.tvBankName.text = wallet.bankName
                val iconResId = resources.getIdentifier(
                    wallet.iconName,
                    "drawable",
                    requireContext().packageName
                )
                if (iconResId != 0) binding.ivBankIcon.setImageResource(iconResId)
            }

            "ewallet" -> {
                binding.layoutSelectEwallet.visibility = View.VISIBLE
                binding.tvEwalletName.text = wallet.bankName
                val iconResId = resources.getIdentifier(
                    wallet.iconName,
                    "drawable",
                    requireContext().packageName
                )
                if (iconResId != 0) binding.ivEwalletIcon.setImageResource(iconResId)
            }
        }
        calculateAndUpdateDifference(0L)
    }

    private fun setupBalanceCalculator() {
        binding.etActualBalance.addTextChangedListener(object : android.text.TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating) return
                isUpdating = true

                val originalText = s.toString()
                val cleanString = originalText.replace(".", "")

                // Nếu ô trống -> Coi như thực tế là 0đ. Nếu có số -> Ép sang Long
                val actualBalance = if (cleanString.isNotEmpty()) {
                    try {
                        cleanString.toLong()
                    } catch (e: Exception) {
                        0L
                    }
                } else {
                    0L
                }

                // Chỉ format text nếu người dùng có nhập chữ
                if (cleanString.isNotEmpty()) {
                    val formatter = DecimalFormat("#,###")
                    val formattedString = formatter.format(actualBalance).replace(",", ".")
                    binding.etActualBalance.setText(formattedString)
                    binding.etActualBalance.setSelection(formattedString.length)
                }

                // Gọi hàm tính toán dùng chung
                calculateAndUpdateDifference(actualBalance)

                isUpdating = false
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnConfirmAdjust.setOnClickListener {
            saveAdjustmentTransaction()
        }
    }

    private fun saveAdjustmentTransaction() {
        val wallet = currentWallet ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Nếu chênh lệch > 0 (Dư tiền) -> Giao dịch THU (income)
        // Nếu chênh lệch < 0 (Hụt tiền) -> Giao dịch CHI (expense)
        val transactionType = if (differenceAmount > 0) "income" else "expense"
        val absoluteAmount = Math.abs(differenceAmount)

        // TẠO GIAO DỊCH ẨN BÙ TRỪ SỐ TIỀN
        val adjustmentTransaction = TransactionItem(
            id = "", // Lát nữa Repository sẽ cấp ID
            userId = userId,
            type = transactionType,
            amount = absoluteAmount,
            walletId = wallet.id,
            walletName = wallet.name,
            walletIcon = wallet.iconName,
            categoryName = "Điều chỉnh số dư",
            categoryIcon = "ic_adjust_balance",
            note = "Hệ thống tự động bù trừ chênh lệch ví",
            dateInMillis = System.currentTimeMillis()
        )

        transactionViewModel.saveTransaction(adjustmentTransaction)
    }

    private fun setupObservers() {
        transactionViewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    requireContext().showCustomToast(
                        "Đã điều chỉnh số dư thành công!",
                        R.drawable.avatar_app
                    )
                    transactionViewModel.resetSaveResult()
                    findNavController().navigateUp()
                } else {
                    val errorMsg = it.exceptionOrNull()?.message ?: "Lỗi không xác định"
                    requireContext().showCustomToast("Lỗi: $errorMsg", R.drawable.avatar_app)
                    transactionViewModel.resetSaveResult()
                }
            }
        }

        transactionViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnConfirmAdjust.isEnabled = !isLoading && differenceAmount != 0L
        }
    }

    // Tự động tính chênh lệch và cập nhật giao diện
    private fun calculateAndUpdateDifference(actualBalance: Long) {
        differenceAmount = actualBalance - currentBalance
        val formatter = DecimalFormat("#,###")

        if (differenceAmount > 0) {
            binding.tvDifference.text =
                "+ ${formatter.format(differenceAmount).replace(",", ".")} đ"
            binding.tvDifference.setTextColor(Color.parseColor("#46A84A")) // Xanh (Dư tiền)
        } else if (differenceAmount < 0) {
            binding.tvDifference.text =
                "- ${formatter.format(Math.abs(differenceAmount)).replace(",", ".")} đ"
            binding.tvDifference.setTextColor(Color.parseColor("#fc565b")) // Đỏ (Hụt tiền)
        } else {
            binding.tvDifference.text = "0 đ"
            binding.tvDifference.setTextColor(Color.parseColor("#333333")) // Đen xám (Khớp tiền)
        }

        // Mở khóa nút Xác nhận nếu có sự chênh lệch
        binding.btnConfirmAdjust.isEnabled = differenceAmount != 0L
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}