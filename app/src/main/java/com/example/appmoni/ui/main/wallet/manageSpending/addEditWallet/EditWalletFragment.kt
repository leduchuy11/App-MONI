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
import com.example.appmoni.databinding.FragmentEditWalletBinding
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.wallet.WalletViewModel
import com.google.firebase.auth.FirebaseAuth

class EditWalletFragment : Fragment() {

    private var _binding: FragmentEditWalletBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WalletViewModel
    private var currentWallet: WalletItem? = null

    // Biến lưu trạng thái icon và bank đang được chọn lại
    private var currentIconName = ""
    private var currentBankName = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(WalletViewModel::class.java)

        // 1. NHẬN DỮ LIỆU TỪ MÀN HÌNH TRƯỚC VÀ ĐỔ LÊN UI
        currentWallet = arguments?.getSerializable("walletItem") as? WalletItem
        currentWallet?.let { wallet ->
            currentIconName = wallet.iconName
            currentBankName = wallet.bankName
            fillDataToUI(wallet)
        }

        // 2. LẮNG NGHE KHI NGƯỜI DÙNG ĐI CHỌN LẠI NGÂN HÀNG TRẢ VỀ
        setFragmentResultListener("requestKey_institution") { _, bundle ->
            currentBankName = bundle.getString("selectedShortName") ?: ""
            currentIconName = bundle.getString("selectedIcon") ?: ""

            // Cập nhật lại UI tạm thời
            binding.tvBankName.text = currentBankName
            binding.tvEwalletName.text = currentBankName
            val iconResId =
                resources.getIdentifier(currentIconName, "drawable", requireContext().packageName)
            if (iconResId != 0) {
                binding.ivBankIcon.setImageResource(iconResId)
                binding.ivEwalletIcon.setImageResource(iconResId)
            }
        }

        setupClickListeners()
        setupObservers()
    }

    private fun fillDataToUI(wallet: WalletItem) {
        // Đổ dữ liệu Tên và Số dư
        binding.etWalletName.setText(wallet.name)
        val formatter = java.text.DecimalFormat("#,###")
        val formattedBalance = formatter.format(wallet.balance).replace(",", ".")
        binding.etInitialBalance.setText(formattedBalance)

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

        // Hiển thị layout Ngân hàng/Ví điện tử tương ứng với loại tài khoản cũ
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
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Nếu là Bank thì cho phép bấm nút chọn lại Bank
        binding.btnSelectBank.setOnClickListener {
            val bundle = Bundle().apply { putString("type", "bank") }
            findNavController().navigate(
                R.id.action_editWalletFragment_to_selectInstitutionFragment,
                bundle
            )
        }

        // Nếu là Ví thì cho phép bấm nút chọn lại Ví
        binding.btnSelectEwallet.setOnClickListener {
            val bundle = Bundle().apply { putString("type", "ewallet") }
            findNavController().navigate(
                R.id.action_editWalletFragment_to_selectInstitutionFragment,
                bundle
            )
        }

        binding.btnSaveWallet.setOnClickListener {
            saveEditedWallet()
        }
    }

    private fun saveEditedWallet() {
        if (currentWallet == null) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val newName = binding.etWalletName.text.toString().trim()
        val finalName = if (newName.isNotEmpty()) newName else currentWallet!!.name

        // Copy ra một object mới với các thông tin đã sửa
        val updatedWallet = currentWallet!!.copy(
            name = finalName,
            iconName = currentIconName,
            bankName = currentBankName
            // Balance và AccountType giữ nguyên không đổi
        )

        viewModel.updateWalletInfo(userId, updatedWallet)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveWallet.isEnabled = !isLoading
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                requireContext().showCustomToast(message, R.drawable.avatar_app)
                viewModel.clearActionSuccess()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}