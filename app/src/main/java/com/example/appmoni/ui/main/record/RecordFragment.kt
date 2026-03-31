package com.example.appmoni.ui.main.record

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentRecordBinding
import com.example.appmoni.databinding.LayoutPopupTransactionBinding
import com.example.appmoni.ui.main.record.category.IncomeCategoryFragment
import com.example.appmoni.ui.showCustomToast
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    // BIẾN TRẠNG THÁI & LƯU TRỮ
    private var currentSelectingType: String = "" // Cây cờ ghi nhớ xuất phát điểm
    private var currentTransactionType: String = "expense"

    // 2 biến lưu ô lựa chọn danh mục mặc định của loai chi tiền và thu tiền
    private var selectedExpenseCategoryId: String = "exp_1"
    private var selectedIncomeCategoryId: String = "inc_1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Lắng nghe kết quả từ CategoryFragment
        setupCategoryResultListener()
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

        // Phục hồi lại đúng Tab đang chọn trước khi đi sang màn hình khác
        updateTransactionTypeUI(currentTransactionType)

        setupTransactionTypeSelector()
        setupFormDate()
        setupFormCategory()
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
            findNavController().navigate(R.id.action_recordFragment_to_categoryFragment,bundle)
        }

        // Bấm từ form THU TIỀN
        binding.itemCategoryIncome.root.setOnClickListener {
            currentSelectingType = "income" // Cắm cờ
            val bundle = bundleOf("transaction_type" to "income")
            findNavController().navigate(R.id.action_recordFragment_to_categoryFragment,bundle)
        }
    }

    // Hàm lắng nghe kết quả từ CategoryFragment
    private fun setupCategoryResultListener() {
        setFragmentResultListener("REQUEST_KEY_CATEGORY") { _, bundle ->

            val selectedId = bundle.getString("selected_id") ?: return@setFragmentResultListener
            val selectedName = bundle.getString("selected_name")
            val returnedType = bundle.getString("selected_type")

            if (currentSelectingType.isNotEmpty() && currentSelectingType != returnedType) {
                val typeName = if (currentSelectingType == "expense") "Chi tiền" else "Thu tiền"
                requireContext().showCustomToast(
                    "Vui lòng chọn đúng danh mục cho $typeName!",
                    R.drawable.avatar_app
                )
                return@setFragmentResultListener
            }

            // ĐIỀN CHỮ VÀ LƯU LẠI ID MỚI
            if (currentSelectingType == "expense") {
                binding.itemCategoryExpense.tvValue.text = selectedName
                selectedExpenseCategoryId = selectedId // Lưu ID Chi tiền
            } else if (currentSelectingType == "income") {
                binding.itemCategoryIncome.tvValue.text = selectedName
                selectedIncomeCategoryId = selectedId // Lưu ID Thu tiền
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
    }

}