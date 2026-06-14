package com.example.appmoni.ui.main.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.databinding.FragmentWalletBinding
import com.example.appmoni.viewmodel.wallet.SavingsViewModel
import com.example.appmoni.viewmodel.wallet.WalletViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat

class WalletFragment : Fragment() {
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private lateinit var walletViewModel: WalletViewModel
    private lateinit var savingsViewModel: SavingsViewModel

    private lateinit var spendingAdapter: WalletAccountAdapter
    private lateinit var savingsAdapter: WalletSavingsAdapter

    // Biến lưu trữ tiền để tính Tổng tài sản
    private var totalSpending = 0L
    private var totalSavings = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        walletViewModel = ViewModelProvider(this).get(WalletViewModel::class.java)
        savingsViewModel = ViewModelProvider(this).get(SavingsViewModel::class.java)

        setupClickListeners()
        setupRecyclerViews()
        setupObservers()

        // Tải dữ liệu từ Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            walletViewModel.loadWallets(userId, "spending")
            savingsViewModel.loadSavings(userId)
        }
    }

    private fun setupClickListeners() {
        val goToSpending = View.OnClickListener {
            findNavController().navigate(R.id.action_walletFragment_to_manageSpendingFragment)
        }
        binding.btnGoSpending.setOnClickListener(goToSpending)
        binding.cardSpendingAccounts.setOnClickListener(goToSpending)
        binding.cardEmptySpending.setOnClickListener(goToSpending)

        val goToSavings = View.OnClickListener {
            findNavController().navigate(R.id.action_walletFragment_to_manageSavingsFragment)
        }
        binding.btnGoSavings.setOnClickListener(goToSavings)
        binding.cardSavingsAccounts.setOnClickListener(goToSavings)
        binding.cardEmptySavings.setOnClickListener(goToSavings)
    }

    private fun setupRecyclerViews() {
        // 1. Cài đặt danh sách Ví chi tiêu
        binding.rvSpendingAccounts.layoutManager = LinearLayoutManager(requireContext())
        spendingAdapter = WalletAccountAdapter(emptyList()) { _ ->
            // Khi bấm vào 1 ví cụ thể -> Chuyển sang màn Quản lý chi tiêu
            findNavController().navigate(R.id.action_walletFragment_to_manageSpendingFragment)
        }
        binding.rvSpendingAccounts.adapter = spendingAdapter

        // 2. Cài đặt danh sách Sổ tiết kiệm (Sử dụng WalletSavingsAdapter mới tạo)
        binding.rvSavingsAccounts.layoutManager = LinearLayoutManager(requireContext())
        savingsAdapter = WalletSavingsAdapter(emptyList()) { _ ->
            // Khi bấm vào 1 sổ cụ thể -> Chuyển sang màn Quản lý sổ tiết kiệm
            findNavController().navigate(R.id.action_walletFragment_to_manageSavingsFragment)
        }
        binding.rvSavingsAccounts.adapter = savingsAdapter
    }

    private fun setupObservers() {
        // 1. Lắng nghe dữ liệu VÍ CHI TIÊU
        walletViewModel.walletList.observe(viewLifecycleOwner) { wallets ->
            if (wallets != null) {
                if (wallets.isEmpty()) {
                    binding.rvSpendingAccounts.visibility = View.GONE
                    binding.cardEmptySpending.visibility = View.VISIBLE
                } else {
                    binding.cardEmptySpending.visibility = View.GONE
                    binding.rvSpendingAccounts.visibility = View.VISIBLE
                    spendingAdapter.updateData(wallets)
                }

                // Tính tổng tiền ví chi tiêu
                totalSpending = wallets.sumOf { it.balance }
                updateTotalBalance()
            }
        }

        // 2. Lắng nghe dữ liệu SỔ TIẾT KIỆM
        savingsViewModel.savingsList.observe(viewLifecycleOwner) { savings ->
            if (savings != null) {
                if (savings.isEmpty()) {
                    binding.rvSavingsAccounts.visibility = View.GONE
                    binding.cardEmptySavings.visibility = View.VISIBLE
                } else {
                    binding.cardEmptySavings.visibility = View.GONE
                    binding.rvSavingsAccounts.visibility = View.VISIBLE
                    savingsAdapter.updateData(savings)
                }

                // Tính tổng tiền sổ tiết kiệm...
                totalSavings = savings.filter { it.status == "active" }.sumOf { it.amount }
                updateTotalBalance()
            }
        }
    }

    // Hàm cộng dồn và cập nhật Số dư hiện tại trên Card to cùng
    private fun updateTotalBalance() {
        val totalAssets = totalSpending + totalSavings

        val formatter = DecimalFormat("#,###")
        val formattedTotal = formatter.format(totalAssets).replace(",", ".") + " đ"
        binding.tvTotalBalance.text = formattedTotal
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}