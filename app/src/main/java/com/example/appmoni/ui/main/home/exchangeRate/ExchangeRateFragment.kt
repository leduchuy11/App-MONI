package com.example.appmoni.ui.main.home.exchangeRate

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentExchangeRateBinding
import com.example.appmoni.ui.addCurrencyFormatter
import com.example.appmoni.ui.parseCurrencyValue
import com.example.appmoni.viewmodel.home.ExchangeRateViewModel

class ExchangeRateFragment : Fragment() {
    private var _binding: FragmentExchangeRateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExchangeRateViewModel by viewModels()
    private lateinit var rateAdapter: ExchangeRateAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExchangeRateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvBaseCurrency.text = viewModel.currentBaseCode
        binding.tvBaseSymbol.text = viewModel.currentBaseSymbol

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        setFragmentResultListener("request_currency") { _, bundle ->
            val newCode = bundle.getString("code") ?: "VND"
            val newSymbol = bundle.getString("symbol") ?: "đ"
            val newName = bundle.getString("name") ?: "Việt Nam Đồng"

            binding.tvBaseCurrency.text = newCode
            binding.tvBaseSymbol.text = newSymbol

            viewModel.fetchRates(newCode, newSymbol, newName)
        }
    }

    private fun setupRecyclerView() {
        rateAdapter = ExchangeRateAdapter(0.0, "VND", "Việt Nam Đồng",false)
        binding.rvExchangeRates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rateAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Nút Đảo chiều công thức (Swap)
        binding.btnSwapRateMode.setOnClickListener {
            viewModel.toggleSwapMode()
        }

        // Bắt sự kiện nhập số tiền
        binding.etAmount.addCurrencyFormatter()
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().parseCurrencyValue().toDouble()

                viewModel.updateAmount(input)
            }
        })

        // Bắt sự kiện tìm kiếm
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Nút chọn loại tiền tệ
        binding.tvBaseCurrency.setOnClickListener {
            val bundle = Bundle().apply {
                putString("selected_code", viewModel.currentBaseCode)
            }
            findNavController().navigate(R.id.action_exchangeRateFragment_to_currencySelectionFragment, bundle)
        }
    }

    private fun observeViewModel() {
        viewModel.currencyList.observe(viewLifecycleOwner) { list ->
            rateAdapter.setData(
                list,
                viewModel.currentAmount,
                viewModel.currentBaseCode,
                viewModel.currentBaseName,
                viewModel.isSwapMode
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}