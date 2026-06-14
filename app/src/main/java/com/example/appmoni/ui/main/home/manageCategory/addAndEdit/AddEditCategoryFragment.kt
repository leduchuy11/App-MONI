package com.example.appmoni.ui.main.home.manageCategory.addAndEdit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.model.category.DefaultCategories
import com.example.appmoni.databinding.FragmentAddEditCategoryBinding
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.showToast
import com.example.appmoni.viewmodel.record.AddEditCategoryViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class AddEditCategoryFragment : Fragment() {

    private var _binding: FragmentAddEditCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddEditCategoryViewModel

    // Biến lưu trạng thái
    private var categoryId: String? = null
    private var categoryType: String = "expense"
    private var selectedIconName: String = "ic_category_salary"
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AddEditCategoryViewModel::class.java)

        // Lấy UserId để dùng chung
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Nhận dữ liệu truyền từ màn Quản lý sang
        categoryId = arguments?.getString("categoryId")
        categoryType = arguments?.getString("categoryType") ?: "expense"

        // Lắng nghe sự kiện
        setupObservers()

        // Thiết lập giao diện theo Logic
        setupUI()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            saveCategory()
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa danh mục?")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục này không?")
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Xóa") { _, _ ->
                    if (currentUserId != null && categoryId != null) {
                        viewModel.deleteCategory(currentUserId!!, categoryId!!)
                    }
                }
                .show()
        }

        binding.layoutSelectIcon.setOnClickListener {
            showIconBottomSheet()
        }
    }

    // HÀM NGỒI HÓNG KẾT QUẢ TỪ VIEWMODEL
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnDelete.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                requireContext().showToast("Lỗi: $error", ToastType.ERROR)
            }
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { message ->
            requireContext().showToast(message, ToastType.SUCCESS)
            findNavController().popBackStack()
        }

        // Hóng Data cũ trả về khi bấm vào tính năng "Sửa"
        viewModel.categoryToEdit.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                binding.edtCategoryName.setText(item.name)
                binding.actCategoryGroup.setText(item.group, false)

                val iconRes = requireContext().resources.getIdentifier(
                    item.iconName, "drawable", requireContext().packageName
                )

                if (iconRes != 0) {
                    binding.ivSelectedIcon.setImageResource(iconRes)
                    selectedIconName = item.iconName
                }
            }
        }
    }

    private fun setupUI() {
        if (categoryType == "income") {
            binding.layoutCategoryGroup.visibility = View.GONE
        } else {
            binding.layoutCategoryGroup.visibility = View.VISIBLE
            setupDropdownGroup()
        }

        val titleType = if (categoryType == "income") "mục thu" else "mục chi"

        if (categoryId == null) {
            // Thêm
            binding.tvTitle.text = "Thêm $titleType"
            binding.btnDelete.visibility = View.GONE
        } else {
            //  Sửa
            binding.tvTitle.text = "Sửa $titleType"
            binding.btnDelete.visibility = View.VISIBLE

            // Gọi viewmodel để kéo dữ liệu cũ về
            if (currentUserId != null) {
                viewModel.loadCategoryForEdit(currentUserId!!, categoryId!!)
            }
        }
    }

    private fun setupDropdownGroup() {
        val groupList = DefaultCategories.getExpenseCategories().map { it.group }.distinct()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            groupList
        )
        binding.actCategoryGroup.setAdapter(adapter)
    }

    private fun saveCategory() {
        val name = binding.edtCategoryName.text.toString().trim()
        val group = binding.actCategoryGroup.text.toString().trim()

        // Validate dữ liệu
        if (name.isEmpty()) {
            requireContext().showToast("Vui lòng nhập tên danh mục!", ToastType.WARNING)
            return
        }
        if (categoryType == "expense" && group.isEmpty()) {
            requireContext().showToast("Vui lòng chọn hoặc nhập tên nhóm!", ToastType.WARNING)
            return
        }

        // GIAO VIỆC CHO VIEWMODEL LƯU DATA
        if (currentUserId != null) {
            viewModel.saveCategory(
                userId = currentUserId!!,
                categoryId = categoryId,
                name = name,
                group = group,
                type = categoryType,
                iconName = selectedIconName
            )
        }
    }

    private fun showIconBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_icons, null)
        val rvIcons = view.findViewById<RecyclerView>(R.id.rv_icons)

        val iconList = listOf(
            R.drawable.ic_category_breakfast, R.drawable.ic_category_dining_out,
            R.drawable.ic_category_dinner, R.drawable.ic_category_lunch,
            R.drawable.ic_category_cafe, R.drawable.ic_category_grocery,
            R.drawable.ic_category_toys, R.drawable.ic_category_tuition,
            R.drawable.ic_category_books, R.drawable.ic_category_milk,
            R.drawable.ic_category_pocket_money, R.drawable.ic_category_electricity,
            R.drawable.ic_category_landline, R.drawable.ic_category_mobile,
            R.drawable.ic_category_gas, R.drawable.ic_category_internet,
            R.drawable.ic_category_water, R.drawable.ic_category_maid,
            R.drawable.ic_category_tv, R.drawable.ic_category_insurance,
            R.drawable.ic_category_parking, R.drawable.ic_category_car_wash,
            R.drawable.ic_category_maintenance, R.drawable.ic_category_taxi,
            R.drawable.ic_category_car_gas, R.drawable.ic_category_gift_giving,
            R.drawable.ic_category_wedding, R.drawable.ic_category_funeral,
            R.drawable.ic_category_visit, R.drawable.ic_category_travel,
            R.drawable.ic_category_beauty, R.drawable.ic_category_cosmetics,
            R.drawable.ic_category_movies_music, R.drawable.ic_category_entertainment,
            R.drawable.ic_category_transfer_fee, R.drawable.ic_category_furniture,
            R.drawable.ic_category_home_repair, R.drawable.ic_category_house_rent,
            R.drawable.ic_category_networking, R.drawable.ic_category_education,
            R.drawable.ic_category_medical, R.drawable.ic_category_sports,
            R.drawable.ic_category_medicine, R.drawable.ic_category_shoes,
            R.drawable.ic_category_accessories, R.drawable.ic_category_clothes,
            R.drawable.ic_category_cash_out, R.drawable.ic_category_salary,
            R.drawable.ic_category_bonus, R.drawable.ic_category_gift,
            R.drawable.ic_category_interest, R.drawable.ic_category_saving_interest,
            R.drawable.ic_category_borrow, R.drawable.ic_category_debt_collection,
            R.drawable.ic_category_other
        )

        rvIcons.layoutManager = GridLayoutManager(requireContext(), 5)
        rvIcons.adapter = IconAdapter(iconList) { selectedIconRes ->
            binding.ivSelectedIcon.setImageResource(selectedIconRes)
            selectedIconName = requireContext().resources.getResourceEntryName(selectedIconRes)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}