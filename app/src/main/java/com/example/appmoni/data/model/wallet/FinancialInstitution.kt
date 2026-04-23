package com.example.appmoni.data.model.wallet

data class FinancialInstitution(
    val id: String,
    val iconName: String,
    val shortName: String, // Tên viết tắt (VD: "ACB", "Momo")
    val fullName: String   // Tên đầy đủ (VD: "Ngân hàng TMCP Á Châu")
)