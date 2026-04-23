package com.example.appmoni.data.model.wallet

data class WalletItem(
    var id: String = "",
    var name: String = "",
    var balance: Long = 0L,
    var iconName: String = "",
    var type: String = "spending",       // Mặc định là tài khoản chi tiêu
    var accountType: String = "cash",   // Phân biệt Tiền mặt / Ngân hàng / Ví điện tử
    var bankName: String = ""
) {
    // Hàm phụ trợ để lấy ID của Icon từ tên chuỗi
    fun getIconResource(context: android.content.Context): Int {
        return context.resources.getIdentifier(iconName, "drawable", context.packageName)
    }
}