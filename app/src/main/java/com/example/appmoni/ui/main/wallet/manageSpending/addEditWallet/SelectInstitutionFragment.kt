package com.example.appmoni.ui.main.wallet.manageSpending.addEditWallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.data.model.wallet.FinancialInstitution
import com.example.appmoni.databinding.FragmentSelectInstitutionBinding
import com.example.appmoni.viewmodel.wallet.WalletViewModel

class SelectInstitutionFragment : Fragment() {

    private var _binding: FragmentSelectInstitutionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: WalletViewModel

    private lateinit var adapter: InstitutionAdapter
    private var fullList: List<FinancialInstitution> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectInstitutionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(WalletViewModel::class.java)

        // Nhận cờ phân loại (bank hoặc ewallet) từ màn hình trước
        val selectionType = arguments?.getString("type") ?: "bank"

        // Đổi tiêu đề dựa trên loại
        binding.tvTitle.text = if (selectionType == "bank") "Chọn ngân hàng" else "Chọn ví điện tử"

        // Khởi tạo RecyclerView với danh sách rỗng ban đầu
        setupRecyclerView()

        // Lắng nghe dữ liệu từ ViewModel
        viewModel.institutionList.observe(viewLifecycleOwner) { list ->
            fullList = list
            adapter.updateData(list) // Cập nhật lại danh sách trên màn hình
        }

        // ViewModel đi lấy dữ liệu
        viewModel.loadInstitutions(selectionType)

        // Cài đặt thanh tìm kiếm và nút Back
        setupSearch()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.rvInstitutions.layoutManager = LinearLayoutManager(requireContext())

        adapter = InstitutionAdapter(fullList) { selectedItem ->
            // KHI NGƯỜI DÙNG BẤM CHỌN: Gửi dữ liệu về lại màn hình AddEdit
            setFragmentResult(
                "requestKey_institution",
                bundleOf(
                    "selectedShortName" to selectedItem.shortName,
                    "selectedIcon" to selectedItem.iconName
                )
            )
            findNavController().navigateUp()
        }

        binding.rvInstitutions.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()

                // Nếu thanh tìm kiếm trống thì hiện tất cả, nếu có chữ thì lọc
                if (query.isEmpty()) {
                    adapter.updateData(fullList)
                } else {
                    val filteredList = fullList.filter {
                        it.shortName.lowercase().contains(query) ||
                                it.fullName.lowercase().contains(query)
                    }
                    adapter.updateData(filteredList)
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}