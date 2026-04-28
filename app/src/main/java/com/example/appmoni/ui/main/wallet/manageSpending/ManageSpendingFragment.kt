package com.example.appmoni.ui.main.wallet.manageSpending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.databinding.FragmentManageSpendingBinding
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.wallet.WalletViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat

class ManageSpendingFragment : Fragment() {

    private var _binding: FragmentManageSpendingBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WalletViewModel
    private lateinit var adapter: ManageSpendingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageSpendingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(WalletViewModel::class.java)

        // Cài đặt danh sách (RecyclerView) và Adapter
        setupRecyclerView()

        // Lắng nghe dữ liệu từ ViewModel
        setupObservers()

        // Bắt sự kiện các nút bấm
        setupClickListeners()

        // Tải dữ liệu từ mây về (chỉ lấy loại "spending" - chi tiêu)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.loadWallets(userId, "spending")
        } else {
            requireContext().showCustomToast("Lỗi: Chưa đăng nhập!", R.drawable.avatar_app)
        }
    }

    private fun setupRecyclerView() {
        binding.rvWallets.layoutManager = LinearLayoutManager(requireContext())

        // Khởi tạo Adapter và lắng nghe sự kiện bấm nút 3 chấm
        adapter = ManageSpendingAdapter(emptyList()) { selectedWallet, view ->
            showWalletOptionsBottomSheet(selectedWallet)
        }

        binding.rvWallets.adapter = adapter
    }

    private fun setupObservers() {
        // Hóng danh sách ví
        viewModel.walletList.observe(viewLifecycleOwner) { wallets ->
            if (wallets.isEmpty()) {
                binding.tvEmptyWallet.visibility = View.VISIBLE
                binding.rvWallets.visibility = View.GONE
            } else {
                binding.tvEmptyWallet.visibility = View.GONE
                binding.rvWallets.visibility = View.VISIBLE
                adapter.updateData(wallets)
            }
            calculateAndDisplayTotalBalance(wallets)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvEmptyWallet.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        // Hóng thông báo lỗi (nếu có)
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                requireContext().showCustomToast("Lỗi: $error", R.drawable.avatar_app)
                viewModel.clearErrorMessage()
            }
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                requireContext().showCustomToast(message, R.drawable.avatar_app)
                viewModel.clearActionSuccess()
            }
        }
    }

    private fun calculateAndDisplayTotalBalance(wallets: List<WalletItem>) {
        var total = 0L
        for (wallet in wallets) {
            total += wallet.balance
        }

        val formatter = DecimalFormat("#,###")
        val formattedTotal = formatter.format(total).replace(",", ".") + " đ"
        binding.tvTotalAmount.text = formattedTotal
    }

    private fun setupClickListeners() {
        // Nút lùi về
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Nút thêm ví mới
        binding.fabAddWallet.setOnClickListener {
            findNavController().navigate(R.id.action_manageSpendingFragment_to_addWalletFragment)
        }
    }

    // Hàm show tùy chọn khi bấm vào 1 ví
    private fun showWalletOptionsBottomSheet(wallet: WalletItem) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_wallet_options, null)
        bottomSheetDialog.setContentView(view)

        // Cập nhật tên ví trên tiêu đề
        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tv_wallet_name_title)
        tvTitle.text = wallet.name

        val btnEdit = view.findViewById<View>(R.id.btn_edit_wallet)
        val btnAdjust = view.findViewById<View>(R.id.btn_adjust_balance)
        val btnArchive = view.findViewById<View>(R.id.btn_archive_wallet)
        val btnDelete = view.findViewById<View>(R.id.btn_delete_wallet)

        // LOGIC NGƯNG SỬ DỤNG (Chỉ cho phép khi số dư = 0)
        if (wallet.balance != 0L) {
            // Còn tiền -> Làm mờ nút đi
            btnArchive.alpha = 0.5f
            btnArchive.setOnClickListener {
                // Bấm vào thì hiện cảnh báo chứ không cho đi tiếp
                requireContext().showCustomToast(
                    "Số dư phải bằng 0đ mới có thể ngưng sử dụng!",
                    R.drawable.avatar_app
                )
            }
        } else {
            // Số dư = 0 -> Nút sáng lên bình thường
            btnArchive.alpha = 1.0f
            btnArchive.setOnClickListener {
                bottomSheetDialog.dismiss()
                // Hiện Dialog cảnh báo
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Ngưng sử dụng tài khoản")
                    .setMessage(" Khi đã ngưng sử dụng, bạn sẽ không thể thực hiện giao dịch hay mở lại tài khoản này nữa. Bạn có chắc chắn không?")
                    .setPositiveButton("Xác nhận") { dialog, _ ->
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId != null) {
                            viewModel.archiveWallet(userId, wallet.id)
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }

        btnEdit.setOnClickListener {
            bottomSheetDialog.dismiss()
            // Truyền dữ liệu ví và chuyển sang màn hình Sửa
            val bundle = Bundle().apply { putSerializable("walletItem", wallet) }
            findNavController().navigate(
                R.id.action_manageSpendingFragment_to_editWalletFragment,
                bundle
            )
        }

        btnAdjust.setOnClickListener {
            bottomSheetDialog.dismiss()
            // Truyền cục dữ liệu Ví và mở màn hình Điều chỉnh
            val bundle = Bundle().apply { putSerializable("walletItem", wallet) }
            findNavController().navigate(
                R.id.action_manageSpendingFragment_to_adjustBalanceFragment,
                bundle
            )
        }

        btnDelete.setOnClickListener {
            bottomSheetDialog.dismiss()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa tài khoản")
                .setMessage(" Nếu tài khoản này chưa phát sinh giao dịch mới có thể xóa và hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa tài khoản '${wallet.name}' không?")
                .setPositiveButton("Xóa") { dialog, _ ->
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        viewModel.deleteWalletSafely(userId, wallet.id)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        bottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}