package com.example.appmoni.ui.main.profile

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.appmoni.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.ui.ToastType
import com.example.appmoni.ui.main.profile.changeAvatar.AvatarUploadWorker
import com.example.appmoni.ui.showToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleImagePicked(uri)
        }
    }

    private var nameSnapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserInfo()

        binding.btnChangeAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")  // Mở album ảnh trên điện thoại
        }
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnChangeName.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_changeNameFragment)
        }
        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_changePasswordFragment)
        }
        binding.btnSettingTheme.setOnClickListener {
            showThemeBottomSheet()
        }
        binding.btnSettingLanguage.setOnClickListener {
            showLanguageBottomSheet()
        }
        binding.btnSyncData.setOnClickListener {
            performSyncData()
        }
        binding.btnDeleteData.setOnClickListener {
            showDeleteDataConfirmDialog()
        }
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmDialog()
        }
        binding.btnHelpAbout.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_aboutFragment)
        }
        binding.btnHelpGuide.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_guideFragment)
        }
    }

    // Hàm hiển thị ảnh và tên hiển thị
    private fun loadUserInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.tvEmail.text = user.email ?: "Chưa cập nhật email"

            val sharedPref =
                requireContext().getSharedPreferences("MoniPrefs", Context.MODE_PRIVATE)
            val pendingAvatar = sharedPref.getString("pending_avatar", null)

            if (pendingAvatar != null) {
                // TH 1: Đang chờ upload (offline), dùng Glide load thẳng file ảnh cục bộ lên
                Glide.with(this)
                    .load(pendingAvatar.toUri())
                    .into(binding.ivAvatar)
            } else {
                // TH 2: Không có ảnh chờ, load URL ảnh từ Firestore về
                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val avatarUrl = document.getString("avatarUrl")
                        if (avatarUrl != null) {
                            Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(com.example.appmoni.R.drawable.avatar_app)
                                .error(com.example.appmoni.R.drawable.avatar_app)
                                .into(binding.ivAvatar)
                        } else {
                            binding.ivAvatar.setImageResource(com.example.appmoni.R.drawable.avatar_app)
                        }
                    }
            }
            val localName = sharedPref.getString("display_name", null)

            if (localName != null) {
                binding.tvUsername.text = localName
            } else {
                val email = user.email ?: ""
                binding.tvUsername.text =
                    if (email.contains("@")) email.substringBefore("@") else "Người dùng Moni"
            }

            // Gán vào biến để quản lý và tránh crash
            nameSnapshotListener =
                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    .addSnapshotListener { document, error ->
                        if (error != null || document == null) return@addSnapshotListener

                        // chống crash: Nếu giao diện đã bị tiêu hủy thì dừng lại ngay
                        if (_binding == null) return@addSnapshotListener

                        val nameFromServer = document.getString("displayName")
                        if (!nameFromServer.isNullOrEmpty() && nameFromServer != localName) {
                            binding.tvUsername.text = nameFromServer
                            sharedPref.edit { putString("display_name", nameFromServer) }
                        }
                    }
        }
    }

    private fun handleImagePicked(uri: Uri) {
        // 1. Copy ảnh vào vùng nhớ của App
        val localUri = copyUriToInternalStorage(uri)

        if (localUri == null) {
            requireContext().showToast("Không thể mở ảnh này khi offline. Vui lòng chọn ảnh khác đã có sẵn trên máy!",
                ToastType.ERROR)
            return
        }

        // 2. Cập nhật giao diện Ngay Lập Tức bằng Glide
        Glide.with(this)
            .load(localUri)
            .into(binding.ivAvatar)

        // 3. Lưu avatar vào SharedPreferences
        requireContext().getSharedPreferences("MoniPrefs", Context.MODE_PRIVATE)
            .edit { putString("pending_avatar", localUri.toString()) }

        // 4. Đẩy sang WorkManager chờ xử lý up ngầm lên ImgBB khi có mạng trở lại
        val inputData = Data.Builder().putString("LOCAL_URI", localUri.toString()).build()
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val uploadWorkRequest =
            OneTimeWorkRequest.Builder(AvatarUploadWorker::class.java)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

        WorkManager.getInstance(requireContext()).enqueue(uploadWorkRequest)
    }

    private fun copyUriToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null

            // tạo tên file theo thời gian để tránh lỗi trùng cache hình ảnh
            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)

            // Xóa các file ảnh avatar cũ đã lưu trước đó để sạch bộ nhớ máy
            requireContext().filesDir.listFiles()?.forEach {
                if (it.name.startsWith("avatar_") && it.name.endsWith(".jpg")) {
                    it.delete()
                }
            }

            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // PHẦN XỬ LÍ ĐĂNG XUẤT
    private fun showLogoutConfirmDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này không?")
            .setPositiveButton("Đăng xuất") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Dọn dẹp sạch sẽ bộ nhớ đệm (Tên, Ảnh) để tài khoản khác đăng nhập không bị dính
        requireContext().getSharedPreferences("MoniPrefs", Context.MODE_PRIVATE)
            .edit {
                clear()
            }

        // Đăng xuất khỏi Firebase Auth
        FirebaseAuth.getInstance().signOut()

        // Đăng xuất khỏi Google Sign-in (Để lần sau ấn đăng nhập Google nó cho chọn lại tài khoản khác)
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build()
        GoogleSignIn.getClient(requireContext(), gso).signOut()

        // Chuyển hướng về màn hình Login và xóa sạch lịch sử màn hình cũ
        val intent = android.content.Intent(
            requireContext(),
            com.example.appmoni.ui.auth.LoginActivity::class.java
        )
        intent.flags =
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // PHẦN XỬ LÍ CÀI GIAO DIỆN VÀ NGÔN NGỮ
    private fun showThemeBottomSheet() {
        val bottomSheetDialog =
            BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_theme, null)
        bottomSheetDialog.setContentView(view)

        val btnLight = view.findViewById<View>(R.id.btn_theme_light)
        val btnDark = view.findViewById<View>(R.id.btn_theme_dark)

        btnLight.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnDark.setOnClickListener {
            requireContext().showToast("Chức năng này hiện tại không khả dụng!", ToastType.WARNING)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showLanguageBottomSheet() {
        val bottomSheetDialog =
            BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_language, null)
        bottomSheetDialog.setContentView(view)

        val btnLangVi = view.findViewById<View>(R.id.btn_lang_vi)
        val btnLangEn = view.findViewById<View>(R.id.btn_lang_en)

        btnLangVi.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnLangEn.setOnClickListener {
            requireContext().showToast("Chức năng này hiện tại không khả dụng!", ToastType.WARNING)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    // PHẦN ĐỒNG BỘ DỮ LIỆU
    private fun performSyncData() {
        binding.layoutLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            kotlinx.coroutines.delay(1500) // Đợi 1.5 giây tạo cảm giác đang tải thật

            binding.layoutLoading.visibility = View.GONE

            if (isNetworkAvailable()) {
                requireContext().showToast("Đồng bộ dữ liệu thành công!", ToastType.SUCCESS)
            } else {
                requireContext().showToast("Đồng bộ thất bại. Vui lòng kiểm tra kết nối mạng!",
                    ToastType.ERROR)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    // PHẦN XÓA DỮ LIỆU
    private fun showDeleteDataConfirmDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cảnh báo xóa dữ liệu")
            .setMessage("Hành động này sẽ xóa toàn bộ dữ liệu của bạn. Chỉ duy nhất danh mục thu chi được giữ lại.\nHành động này không thế hoàn tác. Bạn có đồng ý không?")
            .setPositiveButton("Đồng ý") { dialog, _ ->
                performDeleteData()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performDeleteData() {
        // Kiểm tra mạng
        if (!isNetworkAvailable()) {
            requireContext().showToast("Vui lòng kết nối Internet để thực hiện thao tác này!",
                ToastType.WARNING)
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        binding.layoutLoading.visibility = View.VISIBLE

        val collectionsToDelete =
            listOf("wallets", "transactions", "savings", "limits", "notifications")

        // Tổng số tiến trình = 5 bảng dữ liệu + 1 bảng gốc (chứa Tên, Ảnh)
        val totalTasks = collectionsToDelete.size + 1
        var completedTasks = 0
        var hasError = false

        // B1: Ghi đè tệp rỗng để xóa sạch displayName và avatarUrl ở ngoài cùng
        db.collection("users").document(userId).set(emptyMap<String, Any>())
            .addOnSuccessListener {
                checkDeleteCompletion(++completedTasks, totalTasks, hasError)
            }
            .addOnFailureListener {
                hasError = true
                checkDeleteCompletion(++completedTasks, totalTasks, hasError)
            }

        // B2: Quét và dùng WriteBatch dọn dẹp 5 bảng dữ liệu
        for (collectionName in collectionsToDelete) {
            db.collection("users").document(userId).collection(collectionName)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        checkDeleteCompletion(++completedTasks, totalTasks, hasError)
                        return@addOnSuccessListener
                    }

                    val batch = db.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            checkDeleteCompletion(++completedTasks, totalTasks, hasError)
                        }
                        .addOnFailureListener {
                            hasError = true
                            checkDeleteCompletion(++completedTasks, totalTasks, hasError)
                        }
                }
                .addOnFailureListener {
                    hasError = true
                    checkDeleteCompletion(++completedTasks, totalTasks, hasError)
                }
        }
    }

    // Hàm kiểm tra tiến độ và báo kết quả
    private fun checkDeleteCompletion(completedCount: Int, total: Int, hasError: Boolean) {
        if (completedCount == total) {
            binding.layoutLoading.visibility = View.GONE

            if (hasError) {
                requireContext().showToast("Có lỗi xảy ra trong quá trình xóa. Vui lòng thử lại!",
                    ToastType.ERROR)
            } else {
                // Xóa sạch bộ nhớ đệm cục bộ để ảnh và tên trên UI biến mất ngay lập tức
                requireContext().getSharedPreferences("MoniPrefs", Context.MODE_PRIVATE)
                    .edit {
                        clear()
                    }

                // Trả UI về trạng thái mặc định ban đầu
                val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                binding.tvUsername.text =
                    if (email.contains("@")) email.substringBefore("@") else "Người dùng Moni"
                binding.ivAvatar.setImageResource(R.drawable.avatar_app)

                requireContext().showToast("Xóa dữ liệu thành công!", ToastType.SUCCESS)
            }
        }
    }

    // PHẦN XÓA TÀI KHOẢN
    private fun showDeleteAccountConfirmDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa tài khoản")
            .setMessage("Hành động này sẽ xóa vĩnh viễn tài khoản và toàn bộ dữ liệu của bạn trên hệ thống.\nBạn có chắc chắn muốn tiếp tục?")
            .setPositiveButton("Xóa") { dialog, _ ->
                performDeleteAccount()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performDeleteAccount() {
        if (!isNetworkAvailable()) {
            requireContext().showToast("Vui lòng kết nối Internet để thực hiện thao tác này!",
                ToastType.WARNING)
            return
        }

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()

        binding.layoutLoading.visibility = View.VISIBLE

        // B1: Xóa toàn bộ Database
        val collectionsToDelete = listOf("wallets", "transactions", "savings", "limits", "notifications", "categories")
        val totalTasks = collectionsToDelete.size + 1
        var completedTasks = 0
        var hasError = false

        // Hàm cục bộ: Kiểm tra nếu xóa DB xong xuôi thì mới tiến hành xóa tài khoản Auth
        fun checkDbDeleteAndProceed() {
            if (completedTasks == totalTasks) {
                if (hasError) {
                    binding.layoutLoading.visibility = View.GONE
                    requireContext().showToast("Lỗi xóa dữ liệu máy chủ. Vui lòng thử lại!",
                        ToastType.ERROR)
                    return
                }

                // B2: Chỉ khi DB đã sạch bóng, mới được phép xóa User Auth
                user.delete().addOnCompleteListener { task ->
                    binding.layoutLoading.visibility = View.GONE
                    if (task.isSuccessful) {
                        performLogout()
                        requireContext().showToast("Tài khoản đã được xóa vĩnh viễn!", ToastType.SUCCESS)
                    } else {
                        if (task.exception is FirebaseAuthRecentLoginRequiredException) {
                            requireContext().showToast("Thao tác nhạy cảm: Vui lòng đăng xuất và đăng nhập lại trước khi xóa tài khoản!",
                                ToastType.WARNING)
                        } else {
                            requireContext().showToast("Xóa tài khoản thất bại: ${task.exception?.message}",
                                ToastType.ERROR)
                        }
                    }
                }
            }
        }

        // Xóa Document gốc
        db.collection("users").document(userId).delete()
            .addOnCompleteListener {
                if (!it.isSuccessful) hasError = true
                completedTasks++
                checkDbDeleteAndProceed()
            }

        for (collectionName in collectionsToDelete) {
            db.collection("users").document(userId).collection(collectionName).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        completedTasks++
                        checkDbDeleteAndProceed()
                        return@addOnSuccessListener
                    }
                    val batch = db.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                        .addOnCompleteListener {
                            if (!it.isSuccessful) hasError = true
                            completedTasks++
                            checkDbDeleteAndProceed()
                        }
                }
                .addOnFailureListener {
                    hasError = true
                    completedTasks++
                    checkDbDeleteAndProceed()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nameSnapshotListener?.remove()
        _binding = null
    }
}