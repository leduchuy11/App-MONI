package com.example.appmoni.ui.main.home.manageLimit

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
import com.example.appmoni.data.model.limit.LimitItem
import com.example.appmoni.databinding.FragmentManageLimitBinding
import com.example.appmoni.viewmodel.home.ManageLimitViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.appmoni.databinding.LayoutBottomSheetLimitOptionsBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ManageLimitFragment : Fragment() {

    private var _binding: FragmentManageLimitBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ManageLimitAdapter
    private lateinit var viewModel: ManageLimitViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageLimitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ManageLimitViewModel::class.java]

        setupRecyclerView()
        setupListeners()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        viewModel.loadLimits(userId)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.limitList.observe(viewLifecycleOwner) { list ->
            updateUI(list)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            requireContext().showToast(msg, ToastType.ERROR)
        }
    }

    private fun setupRecyclerView() {
        adapter = ManageLimitAdapter(
            onItemClick = { limit ->
                val bundle = Bundle().apply {
                    putParcelable("limit_item", limit)
                }

                findNavController().navigate(
                    R.id.action_manageLimitFragment_to_limitDetailFragment,
                    bundle
                )
            },
            onMoreOptionsClick = { limit ->
                showBottomSheetOptions(limit)
            }
        )

        binding.rvLimits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLimits.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddLimit.setOnClickListener {
            findNavController().navigate(R.id.action_manageLimitFragment_to_addLimitFragment)
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updateUI(limitList: List<LimitItem>) {
        if (limitList.isEmpty()) {
            binding.cardEmptyState.visibility = View.VISIBLE
            binding.rvLimits.visibility = View.GONE
        } else {
            binding.cardEmptyState.visibility = View.GONE
            binding.rvLimits.visibility = View.VISIBLE
            adapter.submitData(limitList)
        }
    }

    private fun showBottomSheetOptions(limit: LimitItem) {
        val dialog = BottomSheetDialog(requireContext())

        val sheetBinding = LayoutBottomSheetLimitOptionsBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvLimitTitle.text = limit.name

        sheetBinding.layoutDetail.setOnClickListener {
            dialog.dismiss()

            val bundle = Bundle().apply {
                putParcelable("limit_item", limit)
            }

            findNavController().navigate(
                R.id.action_manageLimitFragment_to_limitDetailFragment,
                bundle
            )
        }

        sheetBinding.layoutEdit.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply {
                putParcelable("limit_item", limit)
            }
            findNavController().navigate(R.id.action_manageLimitFragment_to_editLimitFragment, bundle)
        }

        sheetBinding.layoutDelete.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmDialog(limit)
        }

        // Hiển thị lên màn hình
        dialog.show()
    }

    private fun showDeleteConfirmDialog(limit: LimitItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa hạn mức")
            .setMessage("Bạn có chắc chắn muốn xóa hạn mức '${limit.name}' không? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteLimit(limit)
                requireContext().showToast("Đã xóa hạn mức", ToastType.SUCCESS)
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}