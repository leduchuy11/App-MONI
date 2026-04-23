package com.example.appmoni.ui.main.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentManageSpendingBinding
import com.example.appmoni.databinding.FragmentWalletBinding

class WalletFragment : Fragment() {
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nhấn vào mũi tên để sang màn hình Quản lý chi tiêu
        binding.btnGoSpending.setOnClickListener {
            findNavController().navigate(R.id.action_walletFragment_to_manageSpendingFragment)
        }

        // Bấm vào nguyên cả cái thẻ Card cũng chuyển trang được
        binding.cardSpendingAccounts.setOnClickListener {
            findNavController().navigate(R.id.action_walletFragment_to_manageSpendingFragment)
        }
    }

}