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
import com.example.appmoni.data.model.wallet.WalletItem
import com.example.appmoni.databinding.FragmentWalletBinding
import com.example.appmoni.viewmodel.wallet.WalletViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat

class WalletFragment : Fragment() {
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WalletViewModel
    private lateinit var adapter: WalletAccountAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(WalletViewModel::class.java)

        setupRecyclerView()

        setupObservers()

        // tải dữ liệu từ Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.loadWallets(userId, "spending")
        }

        // Nhấn vào mũi tên để sang màn hình Quản lý chi tiêu
        binding.btnGoSpending.setOnClickListener {
            findNavController().navigate(R.id.action_walletFragment_to_manageSpendingFragment)
        }

    }

    private fun setupRecyclerView() {
        binding.rvSpendingAccounts.layoutManager = LinearLayoutManager(requireContext())
        adapter = WalletAccountAdapter(emptyList()) { _ ->
            // Khi bấm vào 1 ví bất kỳ -> Chuyển sang màn Quản lý chi tiêu
            findNavController().navigate(R.id.action_walletFragment_to_manageSpendingFragment)
        }

        binding.rvSpendingAccounts.adapter = adapter
    }

    private fun setupObservers() {
        // Hóng danh sách ví từ Firebase đổ về
        viewModel.walletList.observe(viewLifecycleOwner) { wallets ->
            if (wallets != null) {
                adapter.updateData(wallets)
                calculateAndDisplayTotalBalance(wallets)
            }
        }
    }

    // Hàm tính tổng tiền
    private fun calculateAndDisplayTotalBalance(wallets: List<WalletItem>) {
        var total = 0L
        for (wallet in wallets) {
            total += wallet.balance
        }

        val formatter = DecimalFormat("#,###")
        val formattedTotal = formatter.format(total).replace(",", ".") + " đ"
        binding.tvTotalBalance.text = formattedTotal
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}