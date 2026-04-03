package com.example.appmoni.ui.main.home.manageCategory.addAndEdit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmoni.R
import com.example.appmoni.data.record.DefaultCategories
import com.example.appmoni.databinding.FragmentAddEditCategoryBinding
import com.example.appmoni.ui.showCustomToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class AddEditCategoryFragment : Fragment() {

    private var _binding: FragmentAddEditCategoryBinding? = null
    private val binding get() = _binding!!

    // Biến lưu trạng thái
    private var categoryId: String? = null
    private var categoryType: String = "expense" // Mặc định là chi

    private var selectedIconName: String = "ic_category_salary" // Biến lưu icon

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nhận dữ liệu truyền từ màn Quản lý sang
        categoryId = arguments?.getString("categoryId")
        categoryType = arguments?.getString("categoryType") ?: "expense"

        // Thiết lập giao diện theo Logic
        setupUI()

        // Xử lý các nút bấm
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            saveCategory()
        }

        binding.btnDelete.setOnClickListener {
            // Tạo hộp thoại xác nhận
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa danh mục?")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục này không?")
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss() // Đóng hộp thoại nếu bấm Hủy
                }
                .setPositiveButton("Xóa") { _, _ ->
                    // Nếu xác nhận Xóa, tiến hành gọi Firebase
                    deleteCategoryFromFirebase()
                }
                .show()
        }

        binding.layoutSelectIcon.setOnClickListener {
            showIconBottomSheet()
        }
    }

    private fun setupUI() {
        // Xử lý theo Loại (Thu / Chi)
        if (categoryType == "income") {
            binding.layoutCategoryGroup.visibility = View.GONE // Thu tiền thì ẩn Nhóm
        } else {
            binding.layoutCategoryGroup.visibility = View.VISIBLE // Chi tiền thì hiện Nhóm
            setupDropdownGroup() // Đổ danh sách nhóm mặc định vào Dropdown
        }

        // Xử lý theo Hành động (Thêm / Sửa)
        val titleType = if (categoryType == "income") "mục thu" else "mục chi"

        if (categoryId == null) {
            // Kịch bản Thêm
            binding.tvTitle.text = "Thêm $titleType"
            binding.btnDelete.visibility = View.GONE // Ẩn thùng rác
        } else {
            // Kịch bản Sửa
            binding.tvTitle.text = "Sửa $titleType"
            binding.btnDelete.visibility = View.VISIBLE // Hiện thùng rác

            // Gọi Firebase kéo dữ liệu cũ về đắp lên UI
            loadOldDataFromFirebase(categoryId!!)
        }
    }

    private fun setupDropdownGroup() {
        // Lấy danh sách tên Nhóm duy nhất từ bộ DefaultCategories
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
            requireContext().showCustomToast(
                "Vui lòng nhập tên danh mục!",
                R.drawable.avatar_app
            )
            return
        }
        if (categoryType == "expense" && group.isEmpty()) {
            requireContext().showCustomToast(
                "Vui lòng chọn hoặc nhập tên nhóm!",
                R.drawable.avatar_app
            )
            return
        }

        // Khởi tạo Auth và Firestore để lấy UserId
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return // Nếu chưa đăng nhập thì thoát luôn
        val db = FirebaseFirestore.getInstance()

        // Trỏ đúng vào kho dữ liệu của User này
        val categoryRef = db.collection("users").document(userId).collection("categories")

        if (categoryId == null) {
            // ======== KỊCH BẢN THÊM MỚI ========
            // Tạo một ID mới ngẫu nhiên trước
            val newDocRef = categoryRef.document()

            // Đóng gói dữ liệu (Có kèm theo ID để lát nữa ra ngoài ManageFragment dễ bắt sự kiện sửa)
            val categoryData = hashMapOf(
                "id" to newDocRef.id, // Lưu kèm ID
                "name" to name,
                "group" to group,
                "type" to categoryType,
                "iconName" to selectedIconName
            )

            newDocRef.set(categoryData)
                .addOnSuccessListener {
                    requireContext().showCustomToast(
                        "Đã lưu danh mục thành công!",
                        R.drawable.avatar_app
                    )
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    requireContext().showCustomToast(
                        "Lỗi khi lưu: ${e.message}",
                        R.drawable.avatar_app
                    )
                }
        } else {
            // ======== KỊCH BẢN CẬP NHẬT (SỬA) ========
            val categoryData = hashMapOf(
                "id" to categoryId!!, // Giữ nguyên ID cũ
                "name" to name,
                "group" to group,
                "type" to categoryType,
                "iconName" to selectedIconName
            )

            categoryRef.document(categoryId!!)
                .update(categoryData as Map<String, Any>)
                .addOnSuccessListener {
                    requireContext().showCustomToast(
                        "Cập nhật thành công!",
                        R.drawable.avatar_app
                    )
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    requireContext().showCustomToast(
                        "Lỗi khi cập nhật: ${e.message}",
                        R.drawable.avatar_app
                    )
                }
        }
    }

    // Gọi Firebase kéo dữ liệu cũ về đắp lên UI
    private fun loadOldDataFromFirebase(id: String) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Trỏ đúng vào kho của User
        db.collection("users").document(userId).collection("categories").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: ""
                    val group = document.getString("group") ?: ""
                    val iconName = document.getString("iconName") ?: "ic_category_salary"

                    binding.edtCategoryName.setText(name)
                    binding.actCategoryGroup.setText(group, false)

                    val iconRes = requireContext().resources.getIdentifier(
                        iconName, "drawable", requireContext().packageName
                    )

                    if (iconRes != 0) {
                        binding.ivSelectedIcon.setImageResource(iconRes)
                        selectedIconName = iconName
                    }
                }
            }
            .addOnFailureListener { e ->
                requireContext().showCustomToast(
                    "Lỗi tải dữ liệu cũ: ${e.message}",
                    R.drawable.avatar_app
                )
            }
    }

    // Hàm xóa danh mục
    private fun deleteCategoryFromFirebase() {
        if (categoryId == null) return

        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        requireContext().showCustomToast(
            "Đang xóa...",
            R.drawable.avatar_app
        )

        // Trỏ đúng vào kho của User để xóa
        db.collection("users").document(userId).collection("categories").document(categoryId!!)
            .delete()
            .addOnSuccessListener {
                requireContext().showCustomToast(
                    "Đã xóa danh mục!",
                    R.drawable.avatar_app
                )
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                requireContext().showCustomToast(
                    "Lỗi khi xóa: ${e.message}",
                    R.drawable.avatar_app
                )
            }
    }

    private fun showIconBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        // Nạp giao diện bảng trượt từ file XML
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_icons, null)
        val rvIcons = view.findViewById<RecyclerView>(R.id.rv_icons)

        // Chuẩn bị danh sách 55 icon
        val iconList = listOf(
            R.drawable.ic_category_breakfast,
            R.drawable.ic_category_dining_out,
            R.drawable.ic_category_dinner,
            R.drawable.ic_category_lunch,
            R.drawable.ic_category_cafe,
            R.drawable.ic_category_grocery,
            R.drawable.ic_category_toys,
            R.drawable.ic_category_tuition,
            R.drawable.ic_category_books,
            R.drawable.ic_category_milk,
            R.drawable.ic_category_pocket_money,
            R.drawable.ic_category_electricity,
            R.drawable.ic_category_landline,
            R.drawable.ic_category_mobile,
            R.drawable.ic_category_gas,
            R.drawable.ic_category_internet,
            R.drawable.ic_category_water,
            R.drawable.ic_category_maid,
            R.drawable.ic_category_tv,
            R.drawable.ic_category_insurance,
            R.drawable.ic_category_parking,
            R.drawable.ic_category_car_wash,
            R.drawable.ic_category_maintenance,
            R.drawable.ic_category_taxi,
            R.drawable.ic_category_car_gas,
            R.drawable.ic_category_gift_giving,
            R.drawable.ic_category_wedding,
            R.drawable.ic_category_funeral,
            R.drawable.ic_category_visit,
            R.drawable.ic_category_travel,
            R.drawable.ic_category_beauty,
            R.drawable.ic_category_cosmetics,
            R.drawable.ic_category_movies_music,
            R.drawable.ic_category_entertainment,
            R.drawable.ic_category_transfer_fee,
            R.drawable.ic_category_furniture,
            R.drawable.ic_category_home_repair,
            R.drawable.ic_category_house_rent,
            R.drawable.ic_category_networking,
            R.drawable.ic_category_education,
            R.drawable.ic_category_medical,
            R.drawable.ic_category_sports,
            R.drawable.ic_category_medicine,
            R.drawable.ic_category_shoes,
            R.drawable.ic_category_accessories,
            R.drawable.ic_category_clothes,
            R.drawable.ic_category_cash_out,
            R.drawable.ic_category_salary,
            R.drawable.ic_category_bonus,
            R.drawable.ic_category_gift,
            R.drawable.ic_category_interest,
            R.drawable.ic_category_saving_interest,
            R.drawable.ic_category_borrow,
            R.drawable.ic_category_debt_collection,
            R.drawable.ic_category_other
        )

        // Mỗi dòng hiển thị 5 icon
        rvIcons.layoutManager = GridLayoutManager(requireContext(), 5)

        // Gắn Adapter và bắt sự kiện khi click vào 1 icon
        rvIcons.adapter = IconAdapter(iconList) { selectedIconRes ->
            // Đổi hình trên giao diện
            binding.ivSelectedIcon.setImageResource(selectedIconRes)

            // Dịch từ mã số sang tên file để chuẩn bị lưu firebase
            selectedIconName = requireContext().resources.getResourceEntryName(selectedIconRes)

            // Đóng bảng
            dialog.dismiss()
        }

        // Hiển thị bảng trượt lên màn hình
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}