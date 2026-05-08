package com.example.appmoni.ui.main.wallet.manageSavings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmoni.R
import com.example.appmoni.data.model.wallet.SavingsItem
import com.example.appmoni.databinding.FragmentManageSavingsBinding
import com.example.appmoni.ui.showCustomToast
import com.example.appmoni.viewmodel.wallet.SavingsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat

class ManageSavingsFragment : Fragment() {

    private var _binding: FragmentManageSavingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var savingsViewModel: SavingsViewModel
    private lateinit var adapter: ManageSavingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savingsViewModel = ViewModelProvider(this).get(SavingsViewModel::class.java)

        setupListeners()
        setupRecyclerView()
        setupObservers()

        // Lấy danh sách Sổ tiết kiệm từ Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            binding.progressBar.visibility = View.VISIBLE
            savingsViewModel.loadSavings(userId)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabAddSavings.setOnClickListener {
            findNavController().navigate(R.id.action_manageSavingsFragment_to_addSavingsFragment)
        }
    }

    private fun setupRecyclerView() {
        binding.rvSavingsList.layoutManager = LinearLayoutManager(requireContext())

        adapter = ManageSavingsAdapter(
            savingsList = emptyList(),
            onItemClick = { selectedSavings ->
                navigateToDetail(selectedSavings)
            },
            onMenuClick = { selectedSavings ->
                showSavingsOptionsBottomSheet(selectedSavings)
            }
        )

        binding.rvSavingsList.adapter = adapter
    }

    private fun navigateToDetail(savings: SavingsItem) {
        val bundle = Bundle().apply {
            putSerializable("savings_item", savings)
        }
        findNavController().navigate(
            R.id.action_manageSavingsFragment_to_detailSavingsFragment,
            bundle
        )
    }

    private fun showSavingsOptionsBottomSheet(savings: SavingsItem) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_savings_menu, null)
        bottomSheetDialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tv_sheet_title)
        tvTitle.text = savings.name

        // Bắt sự kiện các nút
        view.findViewById<View>(R.id.btn_view_detail).setOnClickListener {
            bottomSheetDialog.dismiss()
            navigateToDetail(savings)
        }

        view.findViewById<View>(R.id.btn_settle).setOnClickListener {
            bottomSheetDialog.dismiss()

            val bundle = Bundle().apply {
                putSerializable("savings_item", savings)
            }

            findNavController().navigate(
                R.id.action_manageSavingsFragment_to_settleSavingsFragment,
                bundle
            )
        }

        view.findViewById<View>(R.id.btn_edit).setOnClickListener {
            bottomSheetDialog.dismiss()
            val bundle = Bundle().apply {
                putSerializable("savings_item", savings)
            }
            findNavController().navigate(
                R.id.action_manageSavingsFragment_to_editSavingsFragment,
                bundle
            )
        }

        view.findViewById<View>(R.id.btn_delete).setOnClickListener {
            bottomSheetDialog.dismiss()
            showDeleteConfirmationDialog(savings)
        }

        bottomSheetDialog.show()
    }

    private fun showDeleteConfirmationDialog(savings: SavingsItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa sổ tiết kiệm")
            .setMessage("Bạn có chắc chắn muốn xóa '${savings.name}'?  \nLưu ý: Mọi giao dịch liên quan sẽ bị thu hồi và tiền sẽ được hoàn về ví nguồn.")
            .setPositiveButton("Xóa") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                savingsViewModel.deleteSavings(savings)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupObservers() {
        savingsViewModel.savingsList.observe(viewLifecycleOwner) { list ->
            binding.progressBar.visibility = View.GONE

            if (list.isNullOrEmpty()) {
                binding.cardEmptySavings.visibility = View.VISIBLE
                binding.rvSavingsList.visibility = View.GONE
                binding.tvTotalSavingsAmount.text = "0 đ"
            } else {
                binding.cardEmptySavings.visibility = View.GONE
                binding.rvSavingsList.visibility = View.VISIBLE

                adapter.updateData(list)
                calculateTotal(list)
            }
        }
        savingsViewModel.deleteStatus.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.progressBar.visibility = View.GONE
                result.onSuccess { msg ->
                    requireContext().showCustomToast(msg, R.drawable.avatar_app)
                }.onFailure { e ->
                    requireContext().showCustomToast("Lỗi: ${e.message}", R.drawable.avatar_app)
                }
                savingsViewModel.resetDeleteStatus()
            }
        }
    }

    private fun calculateTotal(list: List<SavingsItem>) {
        var totalAmount = 0L
        for (item in list) {
            if (item.status == "active") {
                totalAmount += item.amount
            }
        }
        val formatter = DecimalFormat("#,###")
        val formattedTotal = formatter.format(totalAmount).replace(",", ".") + " đ"
        binding.tvTotalSavingsAmount.text = formattedTotal
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}