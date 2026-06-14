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
import com.example.appmoni.databinding.FragmentAddLimitBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.addCurrencyFormatter
import com.example.appmoni.ui.parseCurrencyValue
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.home.ManageLimitViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddLimitFragment : Fragment() {

    private var _binding: FragmentAddLimitBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ManageLimitViewModel

    // BIẾN LƯU TRỮ DỮ LIỆU
    private var currentLimit = LimitItem()

    // Biến lưu trữ thời gian
    private var startDateInMillis: Long = 0L
    private var endDateInMillis: Long = 0L

    // Bộ định dạng ngày tháng
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddLimitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ManageLimitViewModel::class.java]

        binding.edtLimitAmount.addCurrencyFormatter()

        if (startDateInMillis == 0L) {
            setupDefaultDates()
        } else {
            // Nếu giao diện bị vẽ lại (từ màn chọn danh mục quay về), tự động điền lại ngày đang chọn dở
            binding.tvStartDate.text = dateFormatter.format(startDateInMillis)
            binding.tvEndDate.text = dateFormatter.format(endDateInMillis)
        }

        restoreCategoryUI()

        setupListeners()
        setupFragmentResultListener()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.saveStatus.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                requireContext().showToast("Thêm hạn mức thành công!", ToastType.SUCCESS)
                viewModel.resetSaveStatus()
                findNavController().popBackStack()
            }
        }
    }

    // Hàm set ngày mặc định khi vừa mở màn hình
    private fun setupDefaultDates() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        // Setup Ngày bắt đầu (Mùng 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDateInMillis = calendar.timeInMillis
        binding.tvStartDate.text = dateFormatter.format(startDateInMillis)

        // Setup Ngày kết thúc (Ngày cuối tháng)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDateInMillis = calendar.timeInMillis
        binding.tvEndDate.text = dateFormatter.format(endDateInMillis)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Bấm chọn Ngày bắt đầu
        binding.layoutStartDate.setOnClickListener {
            showDatePicker(isStartDate = true)
        }

        // Bấm chọn Ngày kết thúc
        binding.layoutEndDate.setOnClickListener {
            showDatePicker(isStartDate = false)
        }

        // Bấm chọn Hạng mục chi
        binding.layoutSelectCategory.setOnClickListener {
            val bundle = Bundle().apply {
                val currentIds = ArrayList(currentLimit.categoryIds.ifEmpty { listOf("all") })
                putStringArrayList("EXISTING_CATEGORY_IDS", currentIds)
            }
            findNavController().navigate(R.id.action_addLimitFragment_to_selectCategoryLimitFragment, bundle)
        }

        // Bấm nút Lưu
        binding.btnSaveLimit.setOnClickListener {
            saveLimitToFirebase()
        }
    }

    // Hàm gọi bộ lịch MaterialDatePicker
    private fun showDatePicker(isStartDate: Boolean) {
        val title = if (isStartDate) "Chọn ngày bắt đầu" else "Chọn ngày kết thúc"
        val currentSelection = if (isStartDate) startDateInMillis else endDateInMillis

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(currentSelection)
            .build()

        // Nhấn ok
        datePicker.addOnPositiveButtonClickListener { selection ->
            if (isStartDate) {
                if (selection > endDateInMillis) {
                    requireContext().showToast("Ngày bắt đầu không được sau ngày kết thúc",
                        ToastType.WARNING)
                } else {
                    startDateInMillis = selection
                    binding.tvStartDate.text = dateFormatter.format(startDateInMillis)
                }
            } else {
                if (selection < startDateInMillis) {
                    requireContext().showToast("Ngày kết thúc không được trước ngày bắt đầu",
                        ToastType.WARNING)
                } else {
                    endDateInMillis = selection
                    binding.tvEndDate.text = dateFormatter.format(endDateInMillis)
                }
            }
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
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
                binding.tvSelectedCategory.setTextColor(android.graphics.Color.parseColor("#333333"))
                binding.ivCatIcon.setImageResource(R.drawable.ic_all_in)

                currentLimit.categoryIds = listOf("all")
                currentLimit.icon = "ic_all_in"
            } else {
                // Nếu có chọn một vài mục
                binding.tvSelectedCategory.text = "${selectedIds.size} hạng mục"
                binding.tvSelectedCategory.setTextColor(android.graphics.Color.parseColor("#333333"))

                val resId =
                    resources.getIdentifier(firstIconName, "drawable", requireContext().packageName)
                if (resId != 0) binding.ivCatIcon.setImageResource(resId)

                currentLimit.categoryIds = selectedIds.toList()
                currentLimit.icon = firstIconName
            }
        }
    }

    private fun restoreCategoryUI() {
        val selectedIds = currentLimit.categoryIds
        if (selectedIds.isEmpty() || selectedIds.contains("all")) {
            binding.tvSelectedCategory.text = "Tất cả hạng mục"
            binding.ivCatIcon.setImageResource(R.drawable.ic_all_in)
        } else {
            binding.tvSelectedCategory.text = "${selectedIds.size} hạng mục"
            val resId = resources.getIdentifier(currentLimit.icon, "drawable", requireContext().packageName)
            if (resId != 0) binding.ivCatIcon.setImageResource(resId)
        }
    }

    // Hàm gom dữ liệu và lưu lên Firebase
    private fun saveLimitToFirebase() {
        currentLimit.amount = binding.edtLimitAmount.text.toString().parseCurrencyValue()
        currentLimit.name = binding.edtLimitName.text.toString().trim()
        currentLimit.startDateInMillis = startDateInMillis
        currentLimit.endDateInMillis = endDateInMillis
        currentLimit.userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Validate
        if (currentLimit.amount <= 0) {
            requireContext().showToast("Vui lòng nhập số tiền hợp lệ", ToastType.WARNING)
            return
        }
        if (currentLimit.name.isEmpty()) {
            requireContext().showToast("Vui lòng nhập tên hạn mức", ToastType.WARNING)
            return
        }
        if (currentLimit.userId.isEmpty()) {
            requireContext().showToast("Lỗi: Không tìm thấy tài khoản người dùng!", ToastType.ERROR)
            return
        }

        viewModel.saveLimit(currentLimit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}