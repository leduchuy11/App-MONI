package com.example.appmoni.data.model.wallet

import java.io.Serializable

data class SavingsItem(
    var id: String = "",                    // ID của sổ trên Firebase
    var userId: String = "",                // ID của người dùng

    // 1. THÔNG TIN CƠ BẢN
    var amount: Long = 0L,                  // Số tiền gốc gửi vào
    var name: String = "",                  // Tên sổ
    var bankName: String = "",              // Tên ngân hàng gửi
    var bankIcon: String = "",              // Tên file icon ngân hàng

    // 2. THÔNG TIN LÃI SUẤT & KỲ HẠN
    var depositDateInMillis: Long = 0L,     // Ngày bắt đầu gửi
    var termMonths: Int = 0,                // Số tháng kỳ hạn
    var interestRate: Double = 0.0,         // Lãi suất %/năm
    var earlyWithdrawalRate: Double = 0.1,  // Lãi suất rút trước hạn
    var interestPaymentType: String = "Cuối kỳ", // Hình thức trả lãi

    // 3. NGUỒN TIỀN & LƯU VẾT
    var sourceWalletId: String = "",        // ID của Ví đã trừ tiền (Bỏ trống nếu không chọn)
    var sourceWalletName: String = "",      // Tên Ví đã trừ tiền
    var note: String = "",

    // 4. TRẠNG THÁI SỔ
    var status: String = "active",          // Trạng thái: "active" (Đang gửi) hoặc "closed" (Đã tất toán)
    var createdDateInMillis: Long = System.currentTimeMillis() // Ngày thao tác tạo sổ trên app
) : Serializable