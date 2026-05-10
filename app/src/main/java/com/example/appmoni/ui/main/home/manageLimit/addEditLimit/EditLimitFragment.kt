package com.example.appmoni.ui.main.home.manageLimit.addEditLimit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.limit.LimitItem
import com.example.appmoni.databinding.FragmentEditLimitBinding
import com.example.appmoni.ui.addCurrencyFormatter
import com.example.appmoni.ui.parseCurrencyValue
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.home.ManageLimitViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EditLimitFragment : Fragment() {

    private var _binding: FragmentEditLimitBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ManageLimitViewModel
    private var currentLimit: LimitItem? = null

    // BIẾN LƯU TRỮ THỜI GIAN
    private var startDateInMillis: Long = 0L
    private var endDateInMillis: Long = 0L

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditLimitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ManageLimitViewModel::class.java]

        binding.edtLimitAmount.addCurrencyFormatter()

        if (currentLimit == null) {
            currentLimit = arguments?.getParcelable("limit_item")
            if (currentLimit == null) {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return
            }

            // Khởi tạo ngày tháng cho biến lưu trữ ở lần đầu tiên
            startDateInMillis = currentLimit!!.startDateInMillis
            endDateInMillis = currentLimit!!.endDateInMillis
        }

        // Điền dữ liệu từ currentLimit
        bindExistingData()

        setupListeners()
        setupFragmentResultListener()
        observeViewModel()
    }

    private fun bindExistingData() {
        val limit = currentLimit!!

        binding.edtLimitAmount.setText(limit.amount.toString())
        binding.edtLimitName.setText(limit.name)

        val categoryText = if (limit.categoryIds.contains("all")) {
            "Tất cả hạng mục"
        } else {
            "${limit.categoryIds.size} hạng mục đã chọn"
        }
        binding.tvSelectedCategory.text = categoryText

        val resId = requireContext().resources.getIdentifier(
            limit.icon,
            "drawable",
            requireContext().packageName
        )
        if (resId != 0) binding.ivCatIcon.setImageResource(resId)

        val walletText = if (limit.walletId == "all") "Tất cả tài khoản" else ""
        binding.tvWalletName.text = walletText

        binding.tvStartDate.text = dateFormatter.format(startDateInMillis)
        binding.tvEndDate.text = dateFormatter.format(endDateInMillis)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.layoutSelectCategory.setOnClickListener {
            val bundle = Bundle().apply {
                val currentIds = ArrayList(currentLimit?.categoryIds ?: listOf("all"))
                putStringArrayList("EXISTING_CATEGORY_IDS", currentIds)
            }

            findNavController().navigate(
                R.id.action_editLimitFragment_to_selectCategoryLimitFragment,
                bundle
            )
        }

        binding.layoutStartDate.setOnClickListener {
            showDatePicker(isStartDate = true)
        }
        binding.layoutEndDate.setOnClickListener {
            showDatePicker(isStartDate = false)
        }

        // Nút Lưu
        binding.btnSaveLimit.setOnClickListener {
            saveEditedLimit()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val title = if (isStartDate) "Chọn ngày bắt đầu" else "Chọn ngày kết thúc"
        val currentSelection = if (isStartDate) startDateInMillis else endDateInMillis

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(currentSelection)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            if (isStartDate) {
                if (selection > endDateInMillis) {
                    Toast.makeText(
                        requireContext(),
                        "Ngày bắt đầu không được sau ngày kết thúc",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    startDateInMillis = selection
                    binding.tvStartDate.text = dateFormatter.format(startDateInMillis)
                }
            } else {
                if (selection < startDateInMillis) {
                    Toast.makeText(
                        requireContext(),
                        "Ngày kết thúc không được trước ngày bắt đầu",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    endDateInMillis = selection
                    binding.tvEndDate.text = dateFormatter.format(endDateInMillis)
                }
            }
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun saveEditedLimit() {
        val finalAmount = binding.edtLimitAmount.text.toString().parseCurrencyValue()
        val strName = binding.edtLimitName.text.toString().trim()

        if (finalAmount <= 0) {
            requireContext().showCustomToast("Vui lòng nhập số tiền hợp lệ", R.drawable.avatar_app)
            return
        }
        if (strName.isEmpty()) {
            requireContext().showCustomToast("Vui lòng nhập tên hạn mức", R.drawable.avatar_app)
            return
        }

        // Cập nhật lại các trường
        currentLimit?.apply {
            amount = finalAmount
            name = strName
            startDateInMillis = this@EditLimitFragment.startDateInMillis
            endDateInMillis = this@EditLimitFragment.endDateInMillis
        }

        viewModel.updateLimit(currentLimit!!)
    }

    // Hàm lắng nghe dữ liệu từ màn SelectCategoryLimit truyền về
    private fun setupFragmentResultListener() {
        setFragmentResultListener("REQUEST_KEY_CATEGORY") { _, bundle ->
            val selectedIds = bundle.getStringArrayList("SELECTED_IDS") ?: arrayListOf()
            val firstIconName = bundle.getString("FIRST_ICON_NAME") ?: "ic_all_in"
            val isSelectAll = bundle.getBoolean("IS_SELECT_ALL")

            if (selectedIds.isEmpty() || isSelectAll) {
                // Nếu chọn tất cả hoặc không chọn gì
                binding.tvSelectedCategory.text = "Tất cả hạng mục"
                binding.ivCatIcon.setImageResource(R.drawable.ic_all_in)

                currentLimit?.categoryIds = listOf("all")
                currentLimit?.icon = "ic_all_in"
            } else {
                // Nếu có chọn một vài mục
                binding.tvSelectedCategory.text = "${selectedIds.size} hạng mục đã chọn"

                val resId =
                    resources.getIdentifier(firstIconName, "drawable", requireContext().packageName)
                if (resId != 0) binding.ivCatIcon.setImageResource(resId)

                currentLimit?.categoryIds = selectedIds.toList()
                currentLimit?.icon = firstIconName
            }
        }
    }

    private fun observeViewModel() {
        viewModel.saveStatus.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(requireContext(), "Đã cập nhật hạn mức!", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveStatus()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}