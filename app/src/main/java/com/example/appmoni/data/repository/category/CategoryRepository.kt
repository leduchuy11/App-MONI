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

    fun createAllDefaultCategories(
        userId: String,
        expenseItems: List<CategoryExpenseItem>,
        incomeItems: List<CategoryIncomeItem>
    ): Task<Void> {
        val batch = db.batch()
        val categoryRef = db.collection("users").document(userId).collection("categories")

        // Nạp mục Chi vào gói hàng
        for (item in expenseItems) {
            batch.set(categoryRef.document(item.id), item)
        }
        // Nạp mục Thu vào chung gói hàng
        for (item in incomeItems) {
            batch.set(categoryRef.document(item.id), item)
        }

        // Đẩy lên mây trong 1 lần duy nhất
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

    fun listenToCategoriesByType(
        userId: String,
        type: String,
        onResult: (List<CategoryExpenseItem>) -> Unit
    ) {
        db.collection("users").document(userId).collection("categories")
            .whereEqualTo("type", type)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.toObjects(CategoryExpenseItem::class.java)
                onResult(list)
            }
    }
}