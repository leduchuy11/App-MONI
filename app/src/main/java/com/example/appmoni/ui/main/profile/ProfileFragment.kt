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
import com.example.appmoni.ui.showCustomToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.ui.main.profile.changeAvatar.AvatarUploadWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.ListenerRegistration

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
    }

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
            requireContext().showCustomToast(
                "Không thể mở ảnh này khi offline. Vui lòng chọn ảnh khác đã có sẵn trên máy!",
                com.example.appmoni.R.drawable.avatar_app
            )
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

    private fun showThemeBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_theme, null)
        bottomSheetDialog.setContentView(view)

        val btnLight = view.findViewById<View>(R.id.btn_theme_light)
        val btnDark = view.findViewById<View>(R.id.btn_theme_dark)

        btnLight.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnDark.setOnClickListener {
            requireContext().showCustomToast("Chức năng này hiện tại không khả dụng!", R.drawable.avatar_app)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showLanguageBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_language, null)
        bottomSheetDialog.setContentView(view)

        val btnLangVi = view.findViewById<View>(R.id.btn_lang_vi)
        val btnLangEn = view.findViewById<View>(R.id.btn_lang_en)

        btnLangVi.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnLangEn.setOnClickListener {
            requireContext().showCustomToast("Chức năng này hiện tại không khả dụng!", R.drawable.avatar_app)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nameSnapshotListener?.remove()
        _binding = null
    }
}