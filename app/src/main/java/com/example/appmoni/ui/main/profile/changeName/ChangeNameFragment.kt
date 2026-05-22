package com.example.appmoni.ui.main.profile.changeName

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appmoni.R
import com.example.appmoni.databinding.FragmentChangeNameBinding
import com.example.appmoni.ui.showCustomToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChangeNameFragment : Fragment() {

    private var _binding: FragmentChangeNameBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangeNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentName()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
        binding.btnSave.setOnClickListener { saveNewName() }
    }

    private fun loadCurrentName() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // B1: Hiện tên bằng phần đầu Email (Cho offline lần đầu)
        val email = user.email ?: ""
        val defaultName = if (email.contains("@")) email.substringBefore("@") else ""
        binding.etName.setText(defaultName)

        // B2: Chọc lên Firestore lấy dữ liệu (nếu có mạng hoặc có cache)
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("displayName")
                if (!name.isNullOrEmpty()) {
                    binding.etName.setText(name)
                }
            }
            .addOnFailureListener {
            }
    }

    private fun saveNewName() {
        val newName = binding.etName.text.toString().trim()
        if (newName.isEmpty()) {
            binding.tilName.error = "Tên hiển thị không được để trống"
            binding.etName.requestFocus()
            return
        }
        binding.tilName.error = null

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val data = hashMapOf("displayName" to newName)
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .set(data, SetOptions.merge())

        requireContext().showCustomToast("Đã lưu tên hiển thị!", R.drawable.avatar_app)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}