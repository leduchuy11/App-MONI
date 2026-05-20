package com.example.appmoni.ui.main.home.exportData

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentExportDataBinding
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.home.ExportDataViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExportDataFragment : Fragment() {

    private var _binding: FragmentExportDataBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExportDataViewModel by viewModels()

    private var startDate: Long = 0L
    private var endDate: Long = 0L
    private var targetEmail: String = ""

    private val sdfOutput = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDefaultDates()
        setupListeners()
        observeViewModel()
    }

    private fun setDefaultDates() {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        endDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startDate = calendar.timeInMillis

        updateDateTextViews()
    }

    private fun updateDateTextViews() {
        binding.tvDateFrom.text = "Từ: ${sdfOutput.format(Date(startDate))}"
        binding.tvDateTo.text = "Đến: ${sdfOutput.format(Date(endDate))}"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.layoutDateFrom.setOnClickListener { showDatePicker(isStartDate = true) }
        binding.layoutDateTo.setOnClickListener { showDatePicker(isStartDate = false) }

        binding.btnExportExcel.setOnClickListener {
            showEmailDialog()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = if (isStartDate) startDate else endDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val sel = Calendar.getInstance()
                if (isStartDate) {
                    sel.set(year, month, dayOfMonth, 0, 0, 0)
                    if (sel.timeInMillis > endDate) {
                        requireContext().showCustomToast(
                            "Ngày bắt đầu không được lớn hơn ngày kết thúc",
                            R.drawable.avatar_app
                        )
                        return@DatePickerDialog
                    }
                    startDate = sel.timeInMillis
                } else {
                    sel.set(year, month, dayOfMonth, 23, 59, 59)
                    if (sel.timeInMillis < startDate) {
                        requireContext().showCustomToast(
                            "Ngày kết thúc không được nhỏ hơn ngày bắt đầu",
                            R.drawable.avatar_app
                        )
                        return@DatePickerDialog
                    }
                    endDate = sel.timeInMillis
                }
                updateDateTextViews()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showEmailDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_export_email)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val btnClose = dialog.findViewById<ImageView>(R.id.btn_close_dialog)
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btn_confirm_export)
        val edtEmail = dialog.findViewById<TextInputEditText>(R.id.edt_email)

        // Tự động điền email của tài khoản đang đăng nhập
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.email != null) {
            edtEmail.setText(currentUser.email)
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            if (email.isEmpty()) {
                requireContext().showCustomToast("Vui lòng nhập email!", R.drawable.avatar_app)
                return@setOnClickListener
            }

            targetEmail = email
            dialog.dismiss()

            requireContext().showCustomToast("Đang xử lý...", R.drawable.avatar_app)

            val userId = currentUser?.uid ?: return@setOnClickListener
            viewModel.getTransactionsForExport(userId, startDate, endDate)
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewModel.exportData.observe(viewLifecycleOwner) { transactions ->
            if (transactions == null) return@observe

            if (transactions.isEmpty()) {
                Toast.makeText(
                    context,
                    "Không có giao dịch nào trong khoảng thời gian này!",
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            // Gọi Helper tạo file Excel
            val excelFile =
                ExcelExportHelper.exportToExcel(requireContext(), transactions, startDate, endDate)

            if (excelFile != null) {
                sendEmailWithAttachment(excelFile, targetEmail)
            } else {
                Toast.makeText(context, "Đã có lỗi xảy ra khi tạo file Excel!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Gọi Intent mở app Gmail và đính kèm file
    private fun sendEmailWithAttachment(file: File, email: String) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email)) // Người nhận
                putExtra(Intent.EXTRA_SUBJECT, "Báo cáo tài chính Moni")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Chào bạn,\n\nĐây là file Excel báo cáo thu chi từ ứng dụng Moni mà bạn vừa yêu cầu xuất.\n\nTrân trọng,\nĐội ngũ Moni."
                )
                putExtra(Intent.EXTRA_STREAM, fileUri) // File đính kèm
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Cho phép Gmail đọc file này
            }

            startActivity(Intent.createChooser(intent, "Chọn ứng dụng gửi mail"))
        } catch (e: Exception) {
            Toast.makeText(context, "Không tìm thấy ứng dụng hỗ trợ gửi mail!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}