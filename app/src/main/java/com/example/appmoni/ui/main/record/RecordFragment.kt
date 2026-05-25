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
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import androidx.fragment.app.viewModels
import com.example.appmoni.data.model.transaction.TransactionItem
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.addCurrencyFormatter
import com.example.appmoni.ui.parseCurrencyValue
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.record.TransactionViewModel
import com.google.firebase.auth.FirebaseAuth

class RecordFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()

    // Lưu thời gian giao dịch (mặc định là lúc vừa mở màn hình này lên)
    private var selectedDateInMillis: Long = System.currentTimeMillis()

    // BIẾN TRẠNG THÁI & LƯU TRỮ
    private var currentSelectingType: String = "" // Cây cờ ghi nhớ xuất phát điểm
    private var currentTransactionType: String = "expense"

    // 2 biến lưu ô lựa chọn danh mục mặc định của loai chi tiền và thu tiền
    private var selectedExpenseCategoryId: String = "exp_1"
    private var selectedIncomeCategoryId: String = "inc_1"

    // 2 biến lưu tên danh mục được chọn
    private var selectedExpenseCategoryName: String = "Ăn sáng"
    private var selectedIncomeCategoryName: String = "Lương"

    // 2 biến này để lưu icon danh mục:
    private var selectedExpenseCategoryIcon: String = "ic_category_breakfast"
    private var selectedIncomeCategoryIcon: String = "ic_category_salary"

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

    // Các biến lưu TÊN Ví được chọn (để phục hồi UI)
    private var selectedExpenseWalletName: String = ""
    private var selectedIncomeWalletName: String = ""
    private var selectedTransferSourceWalletName: String = ""
    private var selectedTransferDestWalletName: String = ""
    private var selectedLendWalletName: String = ""
    private var selectedBorrowWalletName: String = ""

    // Biến lưu trữ nội dung nhập liệu (để phục hồi khi quay lại màn hình)
    private var savedNote: String = ""
    private var savedLendPerson: String = ""
    private var savedBorrowPerson: String = ""

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
        setupObservers()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Phục hồi giao diện UI
        restoreUIState()
    }

    override fun onDestroyView() {
        // LƯU LẠI GHI CHÚ VÀ TÊN NGƯỜI TRƯỚC KHI CHUYỂN MÀN HÌNH
        if (_binding != null) {
            // Chỉ lấy ghi chú của tab đang được chọn
            savedNote = when (currentTransactionType) {
                "expense" -> binding.itemNoteExpense.etValue.text.toString()
                "income" -> binding.itemNoteIncome.etValue.text.toString()
                "transfer" -> binding.itemNoteTransfer.etValue.text.toString()
                "lend" -> binding.itemNoteLend.etValue.text.toString()
                "borrow" -> binding.itemNoteBorrow.etValue.text.toString()
                else -> ""
            }
            // Lưu tên người
            savedLendPerson = binding.itemNameLend.etValue.text.toString()
            savedBorrowPerson = binding.itemNameBorrow.etValue.text.toString()
        }

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
                requireContext().showToast(
                    "Vui lòng chọn đúng danh mục cho $typeName!",
                    ToastType.WARNING
                )
                return@setFragmentResultListener
            }

            // ĐIỀN CHỮ VÀ LƯU LẠI ID + ICON MỚI
            if (currentSelectingType == "expense") {
                if (selectedName != null) {
                    selectedExpenseCategoryName = selectedName
                }
                binding.itemCategoryExpense.tvValue.text = selectedName
                selectedExpenseCategoryId = selectedId
                selectedExpenseCategoryIcon = selectedIcon
            } else if (currentSelectingType == "income") {
                if (selectedName != null) {
                    selectedIncomeCategoryName = selectedName
                }
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
                binding.btnQr.visibility = View.GONE
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
                    if (walletName != null) {
                        selectedExpenseWalletName = walletName
                    }
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
                    if (walletName != null) {
                        selectedIncomeWalletName = walletName
                    }
                    binding.itemDestinationIncome.tvValue.text = walletName
                    binding.itemDestinationIncome.tvValue.setTextColor(activeColor)
                    binding.itemDestinationIncome.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedIncomeWalletId = walletId
                    selectedIncomeWalletIcon = walletIcon
                }

                "transfer_source" -> {
                    if (walletName != null) {
                        selectedTransferSourceWalletName = walletName
                    }
                    binding.itemSourceTransfer.tvValue.text = walletName
                    binding.itemSourceTransfer.tvValue.setTextColor(activeColor)
                    binding.itemSourceTransfer.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedTransferSourceWalletId = walletId
                    selectedTransferSourceWalletIcon = walletIcon
                }

                "transfer_dest" -> {
                    if (walletName != null) {
                        selectedTransferDestWalletName = walletName
                    }
                    binding.itemDestinationTransfer.tvValue.text = walletName
                    binding.itemDestinationTransfer.tvValue.setTextColor(activeColor)
                    binding.itemDestinationTransfer.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedTransferDestWalletId = walletId
                    selectedTransferDestWalletIcon = walletIcon
                }

                "lend_source" -> {
                    if (walletName != null) {
                        selectedLendWalletName = walletName
                    }
                    binding.itemSourceLend.tvValue.text = walletName
                    binding.itemSourceLend.tvValue.setTextColor(activeColor)
                    binding.itemSourceLend.tvValue.setTypeface(null, Typeface.BOLD)

                    selectedLendWalletId = walletId
                    selectedLendWalletIcon = walletIcon
                }

                "borrow_dest" -> {
                    if (walletName != null) {
                        selectedBorrowWalletName = walletName
                    }
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

            // KIỂM TRA SỐ TIỀN
            val amountStr = binding.etAmount.text.toString().trim()
            if (amountStr.isEmpty() || amountStr == "0") {
                requireContext().showToast("Bạn chưa nhập số tiền giao dịch!", ToastType.WARNING)
                return@setOnClickListener
            }

            // Ép kiểu sang Long để chuẩn bị lưu
            val amount = amountStr.parseCurrencyValue()
            if (amount <= 0) {
                requireContext().showToast("Số tiền không hợp lệ!", ToastType.WARNING)
                return@setOnClickListener
            }

            // Lấy ID người dùng hiện tại
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                requireContext().showToast("Lỗi: Chưa đăng nhập!", ToastType.ERROR)
                return@setOnClickListener
            }

            // Tạo đối tượng Giao dịch
            val transaction = TransactionItem(
                userId = currentUserId,
                type = currentTransactionType,
                amount = amount,
                dateInMillis = selectedDateInMillis
            )

            // 2. KIỂM TRA ĐIỀU KIỆN RIÊNG CHO TỪNG LOẠI GIAO DỊCH
            when (currentTransactionType) {
                "expense" -> {
                    if (selectedExpenseWalletId.isEmpty()) {
                        requireContext().showToast(
                            "Vui lòng chọn tài khoản chi tiền!",
                            ToastType.WARNING
                        )
                        return@setOnClickListener
                    }
                    // Gói dữ liệu Chi tiền
                    transaction.categoryId = selectedExpenseCategoryId
                    transaction.categoryName = selectedExpenseCategoryName
                    transaction.categoryIcon = selectedExpenseCategoryIcon

                    transaction.walletId = selectedExpenseWalletId
                    transaction.walletName = binding.itemSourceExpense.tvValue.text.toString()
                    transaction.walletIcon = selectedExpenseWalletIcon

                    transaction.note = binding.itemNoteExpense.etValue.text.toString().trim()
                }

                "income" -> {
                    if (selectedIncomeWalletId.isEmpty()) {
                        requireContext().showToast(
                            "Vui lòng chọn tài khoản nhận tiền!",
                            ToastType.WARNING
                        )
                        return@setOnClickListener
                    }
                    // Gói dữ liệu Thu tiền
                    transaction.categoryId = selectedIncomeCategoryId
                    transaction.categoryName = selectedIncomeCategoryName
                    transaction.categoryIcon = selectedIncomeCategoryIcon

                    transaction.walletId = selectedIncomeWalletId
                    transaction.walletName = binding.itemDestinationIncome.tvValue.text.toString()
                    transaction.walletIcon = selectedIncomeWalletIcon

                    transaction.note = binding.itemNoteIncome.etValue.text.toString().trim()
                }

                "transfer" -> {
                    if (selectedTransferSourceWalletId.isEmpty()) {
                        requireContext().showToast(
                            "Vui lòng chọn tài khoản nguồn!",
                            ToastType.WARNING
                        )
                        return@setOnClickListener
                    }
                    if (selectedTransferDestWalletId.isEmpty()) {
                        requireContext().showToast(
                            "Vui lòng chọn tài khoản đích!",
                            ToastType.WARNING
                        )
                        return@setOnClickListener
                    }
                    // Tài khoản nguồn và đích không được trùng nhau
                    if (selectedTransferSourceWalletId == selectedTransferDestWalletId) {
                        requireContext().showToast(
                            "Tài khoản nguồn và đích phải khác nhau!",
                            ToastType.WARNING
                        )
                        return@setOnClickListener
                    }
                    // Gói dữ liệu Chuyển khoản
                    transaction.walletId = selectedTransferSourceWalletId // Ví Nguồn
                    transaction.walletName = binding.itemSourceTransfer.tvValue.text.toString()
                    transaction.walletIcon = selectedTransferSourceWalletIcon

                    transaction.destWalletId = selectedTransferDestWalletId // Ví Đích
                    transaction.destWalletName =
                        binding.itemDestinationTransfer.tvValue.text.toString()
                    transaction.destWalletIcon = selectedTransferDestWalletIcon

                    transaction.note = binding.itemNoteTransfer.etValue.text.toString().trim()
                }

                "lend" -> {
                    val debtorName = binding.itemNameLend.etValue.text.toString().trim()
                    if (debtorName.isEmpty()) {
                        requireContext().showToast(
                            "Vui lòng nhập tên người vay!",
                            ToastType.WARNING
                        )
                        return@setOnClickListener
                    }
                    if (selectedLendWalletId.isEmpty()) {
                        requireContext().showToast(
                            "Vui lòng chọn tài khoản trích tiền cho vay!",
                            ToastType.WARNING
                        )
                        return@setOnClickListener
                    }
                    // Gói dữ liệu Cho vay
                    transaction.personName = debtorName
                    transaction.walletId = selectedLendWalletId
                    transaction.walletName = binding.itemSourceLend.tvValue.text.toString()
                    transaction.walletIcon = selectedLendWalletIcon

                    transaction.note = binding.itemNoteLend.etValue.text.toString().trim()
                }

                "borrow" -> {
                    val creditorName = binding.itemNameBorrow.etValue.text.toString().trim()
                    if (creditorName.isEmpty()) {
                        requireContext().showToast("Vui lòng nhập tên chủ nợ!", ToastType.WARNING)
                        return@setOnClickListener
                    }
                    if (selectedBorrowWalletId.isEmpty()) {
                        requireContext().showToast(
                            "Vui lòng chọn tài khoản nhận tiền đi vay!",
                            ToastType.WARNING
                        )
                        return@setOnClickListener
                    }
                    // Gói dữ liệu Đi vay
                    transaction.personName = creditorName
                    transaction.walletId = selectedBorrowWalletId
                    transaction.walletName = binding.itemDestinationBorrow.tvValue.text.toString()
                    transaction.walletIcon = selectedBorrowWalletIcon

                    transaction.note = binding.itemNoteBorrow.etValue.text.toString().trim()
                }
            }

            // Lưu giao dịch lên firebase
            viewModel.saveTransaction(transaction)
        }
    }

    // Hàm lấy màu sắc số tiền dựa theo loại giao dịch
    private fun getActiveAmountColor(): Int {
        return when (currentTransactionType) {
            "expense", "lend" -> "#fc565b".toColorInt() // Màu Đỏ
            "income", "borrow" -> "#46A84A".toColorInt() // Màu Xanh lá
            "transfer" -> "#128BEF".toColorInt()
            else -> "#46A84A".toColorInt()
        }
    }

    private fun setupObservers() {
        // Lắng nghe trạng thái Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Khóa nút bấm khi đang lưu để tránh user bấm liên tục 2 lần
            binding.btnSaveTransaction.isEnabled = !isLoading
            if (isLoading) {
                binding.btnSaveTransaction.text = "Lưu giao dịch"
            } else {
                binding.btnSaveTransaction.text = "Lưu giao dịch"
            }
        }

        // Lắng nghe kết quả trả về
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                if (result.isSuccess) {
                    requireContext().showToast("Lưu giao dịch thành công!", ToastType.SUCCESS)
                    // Lưu xong thì quay về màn hình trước đó
                    resetForm()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Có lỗi xảy ra"
                    requireContext().showToast("Lỗi: $errorMsg", ToastType.ERROR)
                }
                // Reset lại trạng thái để không bị hiện Toast nhiều lần nếu xoay màn hình
                viewModel.resetSaveResult()
            }
        }
    }

    // Hàm reset form nhập liệu
    private fun resetForm() {
        // 1. Reset số tiền
        binding.etAmount.setText("")

        // 2. Reset ngày về hôm nay
        selectedDateInMillis = System.currentTimeMillis()
        val todayString = "Hôm nay"
        binding.itemDateExpense.tvValue.text = todayString
        binding.itemDateIncome.tvValue.text = todayString
        binding.itemDateTransfer.tvValue.text = todayString
        binding.itemDateLend.tvValue.text = todayString
        binding.itemDateBorrow.tvValue.text = todayString

        // 3. Xóa trắng các ô ghi chú
        binding.itemNoteExpense.etValue.setText("")
        binding.itemNoteIncome.etValue.setText("")
        binding.itemNoteTransfer.etValue.setText("")
        binding.itemNoteLend.etValue.setText("")
        binding.itemNoteBorrow.etValue.setText("")

        // 4. Xóa tên người vay/chủ nợ
        binding.itemNameLend.etValue.setText("")
        binding.itemNameBorrow.etValue.setText("")

        // 5. Reset Danh mục về mặc định ban đầu
        selectedExpenseCategoryId = "exp_1"
        selectedExpenseCategoryName = "Ăn sáng"
        selectedExpenseCategoryIcon = "ic_food"
        binding.itemCategoryExpense.tvValue.text = "Ăn sáng"

        selectedIncomeCategoryId = "inc_1"
        selectedIncomeCategoryName = "Lương"
        selectedIncomeCategoryIcon = "ic_salary"
        binding.itemCategoryIncome.tvValue.text = "Lương"

        // 6. Reset Ví về trạng thái "Chọn tài khoản"
        val defaultWalletText = "Chọn tài khoản"
        val defaultWalletColor = "#BCBABA".toColorInt()

        // Xóa sạch bộ nhớ biến Ví
        selectedExpenseWalletId = ""
        selectedExpenseWalletName = ""
        selectedExpenseWalletIcon = ""

        selectedIncomeWalletId = ""
        selectedIncomeWalletName = ""
        selectedIncomeWalletIcon = ""

        selectedTransferSourceWalletId = ""
        selectedTransferSourceWalletName = ""
        selectedTransferSourceWalletIcon = ""
        selectedTransferDestWalletId = ""
        selectedTransferDestWalletName = ""
        selectedTransferDestWalletIcon = ""

        selectedLendWalletId = ""
        selectedLendWalletName = ""
        selectedLendWalletIcon = ""

        selectedBorrowWalletId = ""
        selectedBorrowWalletName = ""
        selectedBorrowWalletIcon = ""

        // Xóa trắng các biến kho lưu trữ
        savedNote = ""
        savedLendPerson = ""
        savedBorrowPerson = ""


        // Đưa UI của Ví về lại màu xám và in thường
        val textViewsToReset = listOf(
            binding.itemSourceExpense.tvValue,
            binding.itemDestinationIncome.tvValue,
            binding.itemSourceTransfer.tvValue,
            binding.itemDestinationTransfer.tvValue,
            binding.itemSourceLend.tvValue,
            binding.itemDestinationBorrow.tvValue
        )

        for (tv in textViewsToReset) {
            tv.text = defaultWalletText
            tv.setTextColor(defaultWalletColor)
            tv.setTypeface(null, Typeface.NORMAL)
        }
    }

    // Hàm phục hồi lại giao diện khi đi từ màn hình khác quay về
    private fun restoreUIState() {
        // 1. Phục hồi tên Danh mục
        binding.itemCategoryExpense.tvValue.text = selectedExpenseCategoryName
        binding.itemCategoryIncome.tvValue.text = selectedIncomeCategoryName

        // 2. Phục hồi Ngày tháng đang chọn
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val selectedDateString = sdf.format(Date(selectedDateInMillis))
        val todayString = sdf.format(Date())
        val displayString = if (selectedDateString == todayString) "Hôm nay" else selectedDateString

        binding.itemDateExpense.tvValue.text = displayString
        binding.itemDateIncome.tvValue.text = displayString
        binding.itemDateTransfer.tvValue.text = displayString
        binding.itemDateLend.tvValue.text = displayString
        binding.itemDateBorrow.tvValue.text = displayString

        // 3. Phục hồi trạng thái hiển thị của các Ví
        val activeColor = "#333333".toColorInt()

        if (selectedExpenseWalletId.isNotEmpty()) {
            binding.itemSourceExpense.tvValue.text = selectedExpenseWalletName
            binding.itemSourceExpense.tvValue.setTextColor(activeColor)
            binding.itemSourceExpense.tvValue.setTypeface(null, Typeface.BOLD)
        }
        if (selectedIncomeWalletId.isNotEmpty()) {
            binding.itemDestinationIncome.tvValue.text = selectedIncomeWalletName
            binding.itemDestinationIncome.tvValue.setTextColor(activeColor)
            binding.itemDestinationIncome.tvValue.setTypeface(null, Typeface.BOLD)
        }
        if (selectedTransferSourceWalletId.isNotEmpty()) {
            binding.itemSourceTransfer.tvValue.text = selectedTransferSourceWalletName
            binding.itemSourceTransfer.tvValue.setTextColor(activeColor)
            binding.itemSourceTransfer.tvValue.setTypeface(null, Typeface.BOLD)
        }
        if (selectedTransferDestWalletId.isNotEmpty()) {
            binding.itemDestinationTransfer.tvValue.text = selectedTransferDestWalletName
            binding.itemDestinationTransfer.tvValue.setTextColor(activeColor)
            binding.itemDestinationTransfer.tvValue.setTypeface(null, Typeface.BOLD)
        }
        if (selectedLendWalletId.isNotEmpty()) {
            binding.itemSourceLend.tvValue.text = selectedLendWalletName
            binding.itemSourceLend.tvValue.setTextColor(activeColor)
            binding.itemSourceLend.tvValue.setTypeface(null, Typeface.BOLD)
        }
        if (selectedBorrowWalletId.isNotEmpty()) {
            binding.itemDestinationBorrow.tvValue.text = selectedBorrowWalletName
            binding.itemDestinationBorrow.tvValue.setTextColor(activeColor)
            binding.itemDestinationBorrow.tvValue.setTypeface(null, Typeface.BOLD)
        }

        // 4. Phục hồi Ghi chú cho cả 5 Form
        binding.itemNoteExpense.etValue.setText(savedNote)
        binding.itemNoteIncome.etValue.setText(savedNote)
        binding.itemNoteTransfer.etValue.setText(savedNote)
        binding.itemNoteLend.etValue.setText(savedNote)
        binding.itemNoteBorrow.etValue.setText(savedNote)

        // 5. Phục hồi tên người vay / chủ nợ
        binding.itemNameLend.etValue.setText(savedLendPerson)
        binding.itemNameBorrow.etValue.setText(savedBorrowPerson)
    }


}