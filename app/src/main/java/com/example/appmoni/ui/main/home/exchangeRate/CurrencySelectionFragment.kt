package com.example.appmoni.ui.main.home.exchangeRate

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.data.repository.exchangeRate.CurrencyHelper
import com.example.appmoni.databinding.FragmentCurrencySelectionBinding
import com.example.appmoni.ui.removeAccents

class CurrencySelectionFragment : Fragment() {
    private var _binding: FragmentCurrencySelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CurrencySelectionAdapter
    private var allCurrencies = CurrencyHelper.getSupportedCurrencies()
    private var currentSelectedCode = "VND"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrencySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("selected_code")?.let {
            currentSelectedCode = it
        }

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = CurrencySelectionAdapter(currentSelectedCode) { selectedItem ->
            setFragmentResult(
                "request_currency", bundleOf(
                    "code" to selectedItem.code,
                    "symbol" to selectedItem.symbol,
                    "name" to selectedItem.name
                )
            )
            findNavController().navigateUp()
        }
        binding.rvCurrencies.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCurrencies.adapter = adapter
        adapter.setData(allCurrencies)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase().removeAccents()

                if (query.isEmpty()) {
                    adapter.setData(allCurrencies)
                } else {
                    val filtered = allCurrencies.filter {
                        it.code.lowercase().contains(query) ||
                                it.name.lowercase().removeAccents().contains(query) ||
                                it.symbol.lowercase().contains(query)
                    }
                    adapter.setData(filtered)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}