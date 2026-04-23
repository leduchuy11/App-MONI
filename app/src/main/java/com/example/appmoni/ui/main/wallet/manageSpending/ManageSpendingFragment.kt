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

        // Bắt sự kiện các nút bấm (Nút Back, Nút Thêm)
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
            // TẠM THỜI IN RA THÔNG BÁO. TÍNH NĂNG SỬA/XÓA SẼ CODE Ở BƯỚC SAU
            Toast.makeText(
                requireContext(),
                "Bấm vào ví: ${selectedWallet.name}",
                Toast.LENGTH_SHORT
            ).show()
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
                binding.rvWallets.visibility = View.INVISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.rvWallets.visibility = View.VISIBLE
            }
        }

        // Hóng thông báo lỗi (nếu có)
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                requireContext().showCustomToast("Lỗi: $error", R.drawable.avatar_app)
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
            // Chuyển sang màn hình Thêm/Sửa ví
            findNavController().navigate(R.id.action_manageSpendingFragment_to_addWalletFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}