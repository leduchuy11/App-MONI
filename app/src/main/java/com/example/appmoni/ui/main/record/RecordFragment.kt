package com.example.appmoni.ui.main.record

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.widget.addTextChangedListener
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentRecordBinding
import com.example.appmoni.databinding.LayoutPopupTransactionBinding
import com.example.appmoni.ui.showCustomToast
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import com.example.appmoni.ui.addCurrencyFormatter
import com.example.appmoni.ui.parseCurrencyValue

class RecordFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    // Lưu thời gian giao dịch (mặc định là lúc vừa mở màn hình này lên)
    private var selectedDateInMillis: Long = System.currentTimeMillis()

    // BIẾN TRẠNG THÁI & LƯU TRỮ
    private var currentSelectingType: String = "" // Cây cờ ghi nhớ xuất phát điểm
    private var currentTransactionType: String = "expense"

    // 2 biến lưu ô lựa chọn danh mục mặc định của loai chi tiền và thu tiền
    private var selectedExpenseCategoryId: String = "exp_1"
    private var selectedIncomeCategoryId: String = "inc_1"

    // 2 biến này để lưu icon danh mục:
    private var selectedExpenseCategoryIcon: String = "ic_food"
    private var selectedIncomeCategoryIcon: String = "ic_salary"

    // CÁC BIẾN LƯU VÍ ĐƯỢC CHỌN TỪ SELECTFRAGMENT

    // Cờ ghi nhớ ô nào đang gọi màn hình chọn Ví
    private var currentSelectingWalletFor: String = ""

    // Các biến lưu thông tin Ví được chọn (ID và Icon)
    private var selectedExpenseWalletId: String = ""
    private var selectedExpenseWalletIcon: String = ""

    private var selectedIncomeWalletId: String = ""
    private var selectedIncomeWalletIcon: String = ""

    private var selectedTransferSourceWalletId: String = ""
    private var selectedTransferSourceWalletIcon: String = ""
    private var selectedTransferDestWalletId: String = ""
    private var selectedTransferDestWalletIcon: String = ""

    private var selectedLendWalletId: String = ""
    private var selectedLendWalletIcon: String = ""

    private var selectedBorrowWalletId: String = ""
    private var selectedBorrowWalletIcon: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Nhận kết quả từ CategoryFragment
        setupCategoryResultListener()
        //Nhận kết quả từ SelectWalletFragment
        setupWalletResultListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set mặc định cho ô danh mục ở chi tiền và thu tien
        binding.itemCategoryExpense.tvValue.text = "Ăn sáng"
        binding.itemCategoryIncome.tvValue.text = "Lương"

        binding.etAmount.addCurrencyFormatter()

        // LẮNG NGHE ĐỂ ĐỔI MÀU CHỮ "đ"
        binding.etAmount.addTextChangedListener { text ->
            if (text.isNullOrEmpty()) {
                // Nếu chưa nhập gì: Chữ "đ" màu xám
                binding.tvCurrencySymbol.setTextColor("#BCBABA".toColorInt())
            } else {
                // Nếu đã nhập: Chữ "đ" đổi màu theo loại giao dịch
                binding.tvCurrencySymbol.setTextColor(getActiveAmountColor())
            }
        }

        // Phục hồi lại đúng Tab đang chọn trước khi đi sang màn hình khác
        updateTransactionTypeUI(currentTransactionType)

        setupTransactionTypeSelector()
        setupFormDate()
        setupFormCategory()
        setupFormWallet()
        setupSaveButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //Hàm chọn loại giao dịch
    private fun setupTransactionTypeSelector() {
        binding.cardTypeSelector.setOnClickListener {
            val popupBinding = LayoutPopupTransactionBinding.inflate(layoutInflater)
            val popupWindow = PopupWindow(
                popupBinding.root,
                binding.cardTypeSelector.width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popupWindow.elevation = 10f

            // Ẩn tất cả dấu tích
            popupBinding.icCheckExpense.visibility = View.INVISIBLE
            popupBinding.icCheckIncome.visibility = View.INVISIBLE
            popupBinding.icCheckTransfer.visibility = View.INVISIBLE
            popupBinding.icCheckLend.visibility = View.INVISIBLE
            popupBinding.icCheckBorrow.visibility = View.INVISIBLE

            // Tự động bật dấu tích ở mục đang chọn
            when (currentTransactionType) {
                "expense" -> popupBinding.icCheckExpense.visibility = View.VISIBLE
                "income" -> popupBinding.icCheckIncome.visibility = View.VISIBLE
                "transfer" -> popupBinding.icCheckTransfer.visibility = View.VISIBLE
                "lend" -> popupBinding.icCheckLend.visibility = View.VISIBLE
                "borrow" -> popupBinding.icCheckBorrow.visibility = View.VISIBLE
            }

            // Gán sự kiện click
            popupBinding.rowExpense.setOnClickListener {
                updateTransactionTypeUI("expense")
                popupWindow.dismiss()
            }
            popupBinding.rowIncome.setOnClickListener {
                updateTransactionTypeUI("income")
                popupWindow.dismiss()
            }
            popupBinding.rowTransfer.setOnClickListener {
                updateTransactionTypeUI("transfer")
                popupWindow.dismiss()
            }
            popupBinding.rowLend.setOnClickListener {
                updateTransactionTypeUI("lend")
                popupWindow.dismiss()
            }
            popupBinding.rowBorrow.setOnClickListener {
                updateTransactionTypeUI("borrow")
                popupWindow.dismiss()
            }

            popupWindow.showAsDropDown(binding.cardTypeSelector, 0, 10)
        }
    }

    //Hàm gộp sự kiện chọn ngày tháng
    private fun setupFormDate() {
        val onDateClicked = View.OnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày giao dịch")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDateInMillis = selection

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val selectedDateString = sdf.format(Date(selection))

                // Lấy ngày ở điện thoại
                val todayString = sdf.format(Date())

                val displayString = if (selectedDateString == todayString) {
                    "Hôm nay"
                } else {
                    selectedDateString
                }

                // 5. Cập nhật chữ hiển thị lên giao diện
                binding.itemDateExpense.tvValue.text = displayString
                binding.itemDateIncome.tvValue.text = displayString
                binding.itemDateTransfer.tvValue.text = displayString
                binding.itemDateLend.tvValue.text = displayString
                binding.itemDateBorrow.tvValue.text = displayString
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
        // Gắn hành động mở lịch cho tất cả thẻ Ngày tháng ở 5 form
        binding.itemDateExpense.root.setOnClickListener(onDateClicked)
        binding.itemDateIncome.root.setOnClickListener(onDateClicked)
        binding.itemDateTransfer.root.setOnClickListener(onDateClicked)
        binding.itemDateLend.root.setOnClickListener(onDateClicked)
        binding.itemDateBorrow.root.setOnClickListener(onDateClicked)

    }

    // Hàm gộp sự kiện chọn danh mục
    private fun setupFormCategory() {
        // Bấm từ form CHI TIỀN
        binding.itemCategoryExpense.root.setOnClickListener {
            currentSelectingType = "expense" // Cắm cờ
            val bundle = bundleOf("transaction_type" to "expense")
            findNavController().navigate(R.id.action_recordFragment_to_categoryFragment, bundle)
        }

        // Bấm từ form THU TIỀN
        binding.itemCategoryIncome.root.setOnClickListener {
            currentSelectingType = "income" // Cắm cờ
            val bundle = bundleOf("transaction_type" to "income")
            findNavController().navigate(R.id.action_recordFragment_to_categoryFragment, bundle)
        }
    }

    // Hàm lắng nghe kết quả từ CategoryFragment
    private fun setupCategoryResultListener() {
        setFragmentResultListener("REQUEST_KEY_CATEGORY") { _, bundle ->

            val selectedId = bundle.getString("selected_id") ?: return@setFragmentResultListener
            val selectedName = bundle.getString("selected_name")
            val returnedType = bundle.getString("selected_type")
            val selectedIcon = bundle.getString("selected_icon") ?: ""

            if (currentSelectingType.isNotEmpty() && currentSelectingType != returnedType) {
                val typeName = if (currentSelectingType == "expense") "Chi tiền" else "Thu tiền"
                requireContext().showCustomToast(
                    "Vui lòng chọn đúng danh mục cho $typeName!",
                    R.drawable.avatar_app
                )
                return@setFragmentResultListener
            }

            // ĐIỀN CHỮ VÀ LƯU LẠI ID + ICON MỚI
            if (currentSelectingType == "expense") {
                binding.itemCategoryExpense.tvValue.text = selectedName
                selectedExpenseCategoryId = selectedId
                selectedExpenseCategoryIcon = selectedIcon
            } else if (currentSelectingType == "income") {
                binding.itemCategoryIncome.tvValue.text = selectedName
                selectedIncomeCategoryId = selectedId
                selectedIncomeCategoryIcon = selectedIcon
            }

            this.currentSelectingType = "" // Reset cờ
        }
    }

    // Tự động cập nhật giao diện theo loại giao dịch
    private fun updateTransactionTypeUI(type: String) {
        currentTransactionType = type // Cập nhật trí nhớ

        // 1. Tàng hình tất cả Form nhập liệu trước
        binding.cardDetailsExpense.visibility = View.GONE
        binding.cardDetailsIncome.visibility = View.GONE
        binding.cardDetailsTransfer.visibility = View.GONE
        binding.cardDetailsLend.visibility = View.GONE
        binding.cardDetailsBorrow.visibility = View.GONE
        binding.btnQr.visibility = View.GONE

        // 2. Set màu chữ mặc định
        binding.tvTypeName.setTextColor(Color.parseColor("#167AC5"))

        // 3. Hiển thị đúng Form
        when (type) {
            "expense" -> {
                binding.tvTypeName.text = "Chi tiền"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_flying_money)
                binding.btnQr.visibility = View.VISIBLE
                binding.cardDetailsExpense.visibility = View.VISIBLE
            }

            "income" -> {
                binding.tvTypeName.text = "Thu tiền"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_collect_money)
                binding.cardDetailsIncome.visibility = View.VISIBLE
            }

            "transfer" -> {
                binding.tvTypeName.text = "Chuyển khoản"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_transfer)
                binding.cardDetailsTransfer.visibility = View.VISIBLE
            }

            "lend" -> {
                binding.tvTypeName.text = "Cho vay"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_loan)
                binding.cardDetailsLend.visibility = View.VISIBLE
            }

            "borrow" -> {
                binding.tvTypeName.text = "Đi vay"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_borrow)
                binding.cardDetailsBorrow.visibility = View.VISIBLE
            }
        }

        val activeColor = getActiveAmountColor()

        //  Đổi màu text của ô nhập tiền
        binding.etAmount.setTextColor(activeColor)

        // Cập nhật lại màu cho chữ "đ" nếu người dùng đang nhập dở mà bấm chuyển Tab
        if (binding.etAmount.text.toString().isNotEmpty()) {
            binding.tvCurrencySymbol.setTextColor(activeColor)
        } else {
            binding.tvCurrencySymbol.setTextColor("#BCBABA".toColorInt())
        }
    }

    // Hàm điều hướng sang màn chọn ví
    private fun setupFormWallet() {
        binding.itemSourceExpense.root.setOnClickListener {
            currentSelectingWalletFor = "expense_source"
            findNavController().navigate(R.id.action_recordFragment_to_selectWalletFragment)
        }

        binding.itemDestinationIncome.root.setOnClickListener {
            currentSelectingWalletFor = "income_dest"
            findNavController().navigate(R.id.action_recordFragment_to_selectWalletFragment)
        }

        binding.itemSourceTransfer.root.setOnClickListener {
            currentSelectingWalletFor = "transfer_source"
            findNavController().navigate(R.id.action_recordFragment_to_selectWalletFragment)
        }

        binding.itemDestinationTransfer.root.setOnClickListener {
            currentSelectingWalletFor = "transfer_dest"
            findNavController().navigate(R.id.action_recordFragment_to_selectWalletFragment)
        }

        binding.itemSourceLend.root.setOnClickListener {
            currentSelectingWalletFor = "lend_source"
            findNavController().navigate(R.id.action_recordFragment_to_selectWalletFragment)
        }

        binding.itemDestinationBorrow.root.setOnClickListener {
            currentSelectingWalletFor = "borrow_dest"
            findNavController().navigate(R.id.action_recordFragment_to_selectWalletFragment)
        }
    }

    // Hàm nhận kết quả trả về từ fragment chọn ví
    private fun setupWalletResultListener() {
        setFragmentResultListener("REQUEST_KEY_WALLET") { _, bundle ->
            val walletId =
                bundle.getString("selected_wallet_id") ?: return@setFragmentResultListener
            val walletName = bundle.getString("selected_wallet_name")
            val walletIcon = bundle.getString("selected_wallet_icon") ?: ""

            // Màu đen đậm dùng khi đã chọn xong
            val activeColor = "#333333".toColorInt()

            when (currentSelectingWalletFor) {
                "expense_source" -> {
                    binding.itemSourceExpense.tvValue.text = walletName
                    binding.itemSourceExpense.tvValue.setTextColor(activeColor)
                    binding.itemSourceExpense.tvValue.setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    selectedExpenseWalletId = walletId
                    selectedExpenseWalletIcon = walletIcon
                }

                "income_dest" -> {
                    binding.itemDestinationIncome.tvValue.text = walletName
                    binding.itemDestinationIncome.tvValue.setTextColor(activeColor)
                    binding.itemDestinationIncome.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedIncomeWalletId = walletId
                    selectedIncomeWalletIcon = walletIcon
                }

                "transfer_source" -> {
                    binding.itemSourceTransfer.tvValue.text = walletName
                    binding.itemSourceTransfer.tvValue.setTextColor(activeColor)
                    binding.itemSourceTransfer.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedTransferSourceWalletId = walletId
                    selectedTransferSourceWalletIcon = walletIcon
                }

                "transfer_dest" -> {
                    binding.itemDestinationTransfer.tvValue.text = walletName
                    binding.itemDestinationTransfer.tvValue.setTextColor(activeColor)
                    binding.itemDestinationTransfer.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedTransferDestWalletId = walletId
                    selectedTransferDestWalletIcon = walletIcon
                }

                "lend_source" -> {
                    binding.itemSourceLend.tvValue.text = walletName
                    binding.itemSourceLend.tvValue.setTextColor(activeColor)
                    binding.itemSourceLend.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedLendWalletId = walletId
                    selectedLendWalletIcon = walletIcon
                }

                "borrow_dest" -> {
                    binding.itemDestinationBorrow.tvValue.text = walletName
                    binding.itemDestinationBorrow.tvValue.setTextColor(activeColor)
                    binding.itemDestinationBorrow.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedBorrowWalletId = walletId
                    selectedBorrowWalletIcon = walletIcon
                }
            }

            currentSelectingWalletFor = ""
        }
    }

    // Hàm ấn nút lưu
    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {

            // 1. KIỂM TRA SỐ TIỀN
            val amountStr = binding.etAmount.text.toString().trim()
            if (amountStr.isEmpty() || amountStr == "0") {
                requireContext().showCustomToast(
                    "Bạn chưa nhập số tiền giao dịch!",
                    R.drawable.avatar_app
                )
                return@setOnClickListener // Dừng lại luôn
            }

            // Ép kiểu sang Long để chuẩn bị lưu
            val amount = amountStr.parseCurrencyValue()
            if (amount <= 0) {
                requireContext().showCustomToast("Số tiền không hợp lệ!", R.drawable.avatar_app)
                return@setOnClickListener
            }

            // 2. KIỂM TRA ĐIỀU KIỆN RIÊNG CHO TỪNG LOẠI GIAO DỊCH
            when (currentTransactionType) {
                "expense" -> {
                    if (selectedExpenseWalletId.isEmpty()) {
                        requireContext().showCustomToast(
                            "Vui lòng chọn tài khoản chi tiền!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    // TODO: Gói dữ liệu và gọi ViewModel lưu Expense
                }

                "income" -> {
                    if (selectedIncomeWalletId.isEmpty()) {
                        requireContext().showCustomToast(
                            "Vui lòng chọn tài khoản nhận tiền!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    // TODO: Gói dữ liệu và gọi ViewModel lưu Income
                }

                "transfer" -> {
                    if (selectedTransferSourceWalletId.isEmpty()) {
                        requireContext().showCustomToast(
                            "Vui lòng chọn tài khoản nguồn!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    if (selectedTransferDestWalletId.isEmpty()) {
                        requireContext().showCustomToast(
                            "Vui lòng chọn tài khoản đích!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    // Tài khoản nguồn và đích không được trùng nhau
                    if (selectedTransferSourceWalletId == selectedTransferDestWalletId) {
                        requireContext().showCustomToast(
                            "Tài khoản nguồn và đích phải khác nhau!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    // TODO: Gói dữ liệu và gọi ViewModel lưu Transfer
                }

                "lend" -> {
                    val debtorName = binding.itemNameLend.etValue.text.toString().trim()
                    if (debtorName.isEmpty()) {
                        requireContext().showCustomToast(
                            "Vui lòng nhập tên người vay!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    if (selectedLendWalletId.isEmpty()) {
                        requireContext().showCustomToast(
                            "Vui lòng chọn tài khoản trích tiền cho vay!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    // TODO: Gói dữ liệu và gọi ViewModel lưu Lend
                }

                "borrow" -> {
                    val creditorName = binding.itemNameBorrow.etValue.text.toString().trim()
                    if (creditorName.isEmpty()) {
                        requireContext().showCustomToast(
                            "Vui lòng nhập tên chủ nợ!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    if (selectedBorrowWalletId.isEmpty()) {
                        requireContext().showCustomToast(
                            "Vui lòng chọn tài khoản nhận tiền đi vay!",
                            R.drawable.avatar_app
                        )
                        return@setOnClickListener
                    }
                    // TODO: Gói dữ liệu và gọi ViewModel lưu Borrow
                }
            }

            // 3. THÔNG BÁO THÀNH CÔNG (Tạm thời)
            // Nếu code chạy được tới tận đây, nghĩa là đã vượt qua mọi ải kiểm duyệt!
            requireContext().showCustomToast(
                "Dữ liệu hợp lệ! Đang lưu lên hệ thống...",
                R.drawable.avatar_app
            )

        }
    }

    // Hàm lấy màu sắc số tiền dựa theo loại giao dịch
    private fun getActiveAmountColor(): Int {
        return when (currentTransactionType) {
            "expense", "lend" -> "#fc565b".toColorInt() // Màu Đỏ
            "income" , "transfer", "borrow" -> "#46A84A".toColorInt() // Màu Xanh lá
            else -> "#2E8B57".toColorInt()
        }
    }

}