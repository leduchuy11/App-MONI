package com.example.appmoni.data.repository.category

import com.example.appmoni.data.model.category.CategoryExpenseItem
import com.example.appmoni.data.model.category.CategoryIncomeItem
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class CategoryRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // 1. Lấy danh sách danh mục từ mây (Dùng chung cho cả Thu và Chi)
    // Chỉ cần truyền chữ "expense" hoặc "income" vào biến type
    fun getCategoriesByType(userId: String, type: String): Task<QuerySnapshot> {
        return db.collection("users").document(userId).collection("categories")
            .whereEqualTo("type", type)
            .get()
    }

    // 2. Đẩy danh sách Mặc định của Mục CHI lên mây
    fun createDefaultExpenseCategories(
        userId: String,
        items: List<CategoryExpenseItem>
    ): Task<Void> {
        val batch = db.batch()
        val categoryRef = db.collection("users").document(userId).collection("categories")

        for (item in items) {
            val docRef = categoryRef.document(item.id)
            batch.set(docRef, item)
        }

        return batch.commit()
    }

    // 3. Đẩy danh sách Mặc định của Mục THU lên mây
    fun createDefaultIncomeCategories(userId: String, items: List<CategoryIncomeItem>): Task<Void> {
        val batch = db.batch()
        val categoryRef = db.collection("users").document(userId).collection("categories")

        for (item in items) {
            val docRef = categoryRef.document(item.id)
            batch.set(docRef, item)
        }

        return batch.commit()
    }

    // 4. Lấy chi tiết một danh mục để sửa
    fun getCategoryById(userId: String, categoryId: String): Task<DocumentSnapshot> {
        return db.collection("users").document(userId).collection("categories").document(categoryId).get()
    }

    // 5. Thêm mới một danh mục
    fun addCategory(userId: String, categoryData: HashMap<String, String>): Task<Void> {
        val categoryRef = db.collection("users").document(userId).collection("categories")
        val newDocRef = categoryRef.document()

        // Gắn ID tự tạo vào Map dữ liệu
        categoryData["id"] = newDocRef.id
        return newDocRef.set(categoryData)
    }

    // 6. Cập nhật (sửa) danh mục
    fun updateCategory(userId: String, categoryId: String, categoryData: HashMap<String, String>): Task<Void> {
        return db.collection("users").document(userId).collection("categories").document(categoryId)
            .update(categoryData as Map<String, Any>)
    }

    // 7. Xóa danh mục
    fun deleteCategory(userId: String, categoryId: String): Task<Void> {
        return db.collection("users").document(userId).collection("categories").document(categoryId).delete()
    }
}