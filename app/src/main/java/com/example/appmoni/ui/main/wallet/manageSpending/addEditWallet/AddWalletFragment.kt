package com.example.appmoni.ui.main.wallet.manageSpending.addEditWallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.viewmodel.wallet.WalletViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.view.isVisible
import com.example.appmoni.databinding.FragmentAddWalletBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.showToast

class AddWalletFragment : Fragment() {

    private var _binding: FragmentAddWalletBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WalletViewModel

    // Hai biến này dùng để hứng data khi ấn nút Lưu
    private var currentWalletType = "cash"
    private var currentIconName = "ic_money"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(WalletViewModel::class.java)

        // NGHE DỮ LIỆU TỪ MÀN CHỌN NGÂN HÀNG/VÍ TRẢ VỀ
        setFragmentResultListener("requestKey_institution") { _, bundle ->
            val selectedName = bundle.getString("selectedShortName") ?: ""
            val selectedIcon = bundle.getString("selectedIcon") ?: ""

            viewModel.updateInstitutionInfo(selectedName, selectedIcon)
        }

        setupBalanceFormatter()
        setupClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveWallet.isEnabled = !isLoading
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                requireContext().showToast(message, ToastType.SUCCESS)
                viewModel.clearActionSuccess()
                findNavController().navigateUp()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                requireContext().showToast("Lỗi: $error", ToastType.ERROR)
                viewModel.clearErrorMessage()
            }
        }


        // Nghe Loại tài khoản (để thay đổi Text, Icon và bật/tắt các ô)
        viewModel.selectedAccountType.observe(viewLifecycleOwner) { type ->
            currentWalletType = type
            syncUIVisibility(type)
        }

        viewModel.accountTypeName.observe(viewLifecycleOwner) { typeName ->
            binding.tvAccountType.text = typeName
        }

        viewModel.accountTypeIcon.observe(viewLifecycleOwner) { typeIcon ->
            currentIconName = typeIcon
            val iconResId =
                resources.getIdentifier(typeIcon, "drawable", requireContext().packageName)
            if (iconResId != 0) binding.ivTypeIcon.setImageResource(iconResId)
        }

        // Nghe tổ chức tài chính (Ngân hàng/Ví)
        viewModel.selectedInstitutionName.observe(viewLifecycleOwner) { name ->
            if (name.isNotEmpty()) {
                binding.tvBankName.text = name
                binding.tvEwalletName.text = name
            } else {
                binding.tvBankName.text = "Chọn ngân hàng"
                binding.tvEwalletName.text = "Chọn loại ví điện tử"
            }
        }

        viewModel.selectedInstitutionIcon.observe(viewLifecycleOwner) { iconName ->
            if (iconName.isNotEmpty()) {
                currentIconName = iconName
                val iconResId =
                    resources.getIdentifier(iconName, "drawable", requireContext().packageName)
                if (iconResId != 0) {
                    binding.ivBankIcon.setImageResource(iconResId)
                    binding.ivEwalletIcon.setImageResource(iconResId)
                }
            } else {
                // Reset về icon mặc định
                binding.ivBankIcon.setImageResource(R.drawable.ic_add_ver2)
                binding.ivEwalletIcon.setImageResource(R.drawable.ic_add_ver2)
            }
        }
    }

    // Hàm chuyên lo việc Ẩn/Hiện các ô
    private fun syncUIVisibility(type: String) {
        binding.layoutAccountName.visibility = View.GONE
        binding.layoutSelectBank.visibility = View.GONE
        binding.layoutSelectEwallet.visibility = View.GONE

        when (type) {
            "cash" -> {
                binding.layoutAccountName.visibility = View.VISIBLE
            }

            "bank" -> {
                binding.layoutAccountName.visibility = View.VISIBLE
                binding.layoutSelectBank.visibility = View.VISIBLE
            }

            "ewallet" -> {
                binding.layoutAccountName.visibility = View.VISIBLE
                binding.layoutSelectEwallet.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutAccountType.setOnClickListener {
            showAccountTypeBottomSheet()
        }

        binding.btnSaveWallet.setOnClickListener {
            saveWallet()
        }

        binding.btnSelectBank.setOnClickListener {
            val bundle = Bundle().apply { putString("type", "bank") }
            findNavController().navigate(
                R.id.action_addWalletFragment_to_selectInstitutionFragment,
                bundle
            )
        }

        binding.btnSelectEwallet.setOnClickListener {
            val bundle = Bundle().apply { putString("type", "ewallet") }
            findNavController().navigate(
                R.id.action_addWalletFragment_to_selectInstitutionFragment,
                bundle
            )
        }
    }

    private fun showAccountTypeBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_account_type, null)
        bottomSheetDialog.setContentView(view)

        val btnCash = view.findViewById<View>(R.id.btn_type_cash)
        val btnBank = view.findViewById<View>(R.id.btn_type_bank)
        val btnEwallet = view.findViewById<View>(R.id.btn_type_ewallet)

        btnCash.setOnClickListener {
            viewModel.updateAccountType("cash", "Tiền mặt", "ic_money")
            bottomSheetDialog.dismiss()
        }

        btnBank.setOnClickListener {
            viewModel.updateAccountType("bank", "Tài khoản ngân hàng", "ic_bank")
            bottomSheetDialog.dismiss()
        }

        btnEwallet.setOnClickListener {
            viewModel.updateAccountType("ewallet", "Ví điện tử", "ic_e_wallet")
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun saveWallet() {
        val balanceRaw = binding.etInitialBalance.text.toString().replace(".", "")
        val balance = if (balanceRaw.isNotEmpty()) balanceRaw.toLong() else 0L

        var walletName = viewModel.accountTypeName.value ?: "Tiền mặt"
        if (binding.layoutAccountName.isVisible) {
            val inputName = binding.etWalletName.text.toString()
            if (inputName.isNotBlank()) walletName = inputName
        }

        var selectedBankOrEwalletName = ""
        if (currentWalletType == "bank" || currentWalletType == "ewallet") {
            selectedBankOrEwalletName = viewModel.selectedInstitutionName.value ?: ""
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val newWallet = WalletItem(
            id = "",
            name = walletName,
            balance = balance,
            iconName = currentIconName,
            type = "spending",
            accountType = currentWalletType,
            bankName = selectedBankOrEwalletName
        )

        viewModel.addWallet(userId, newWallet)
    }

    fun setupBalanceFormatter() {
        binding.etInitialBalance.addTextChangedListener(object : android.text.TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating) return
                isUpdating = true

                val originalText = s.toString()
                val cleanString = originalText.replace(".", "")

                if (cleanString.isNotEmpty()) {
                    try {
                        val parsed = cleanString.toLong()
                        val formatter = java.text.DecimalFormat("#,###")
                        val formattedString = formatter.format(parsed).replace(",", ".")

                        binding.etInitialBalance.setText(formattedString)
                        binding.etInitialBalance.setSelection(formattedString.length)
                    } catch (e: Exception) {
                    }
                }
                isUpdating = false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}