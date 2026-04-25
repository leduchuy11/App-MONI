package com.example.appmoni.ui.main.record.selectWallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.databinding.FragmentSelectWalletBinding
import com.example.appmoni.ui.main.wallet.WalletAccountAdapter
import com.example.appmoni.viewmodel.wallet.WalletViewModel
import com.google.firebase.auth.FirebaseAuth

class SelectWalletFragment : Fragment() {
    private var _binding: FragmentSelectWalletBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WalletViewModel
    private lateinit var adapter: WalletAccountAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(WalletViewModel::class.java)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Cài đặt RecyclerView
        binding.rvWallets.layoutManager = LinearLayoutManager(requireContext())
        adapter = WalletAccountAdapter(emptyList()) { selectedWallet ->
            // Đóng gói dữ liệu gửi về
            val result = bundleOf(
                "selected_wallet_id" to selectedWallet.id,
                "selected_wallet_name" to selectedWallet.name,
                "selected_wallet_icon" to selectedWallet.iconName
            )
            setFragmentResult("REQUEST_KEY_WALLET", result)

            findNavController().popBackStack()
        }
        binding.rvWallets.adapter = adapter

        // Lắng nghe và đổ dữ liệu
        viewModel.walletList.observe(viewLifecycleOwner) { wallets ->
            binding.progressBar.visibility = View.GONE

            if (wallets.isNullOrEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.cardWallets.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.cardWallets.visibility = View.VISIBLE
                adapter.updateData(wallets)
            }
        }

        // Lắng nghe trạng thái Loading từ ViewModel
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.cardWallets.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.GONE
            }
        }

        // Gọi Firebase lấy danh sách ví đang hoạt động (type: spending)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.loadWallets(userId, "spending")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}