package com.example.appmoni.data.record

object DefaultCategories {

    // KHO DANH SÁCH THU TIỀN MẶC ĐỊNH
    fun getIncomeCategories(): List<CategoryIncomeItem> {
        return listOf(
            CategoryIncomeItem("inc_1", "Lương", "ic_category_salary", "income"),
            CategoryIncomeItem("inc_2", "Thưởng", "ic_category_bonus", "income"),
            CategoryIncomeItem("inc_3", "Được cho/tặng", "ic_category_gift", "income"),
            CategoryIncomeItem("inc_4", "Tiền lãi", "ic_category_interest", "income"),
            CategoryIncomeItem("inc_5", "Lãi tiết kiệm", "ic_category_saving_interest", "income"),
            CategoryIncomeItem("inc_6", "Đi vay", "ic_category_borrow", "income"),
            CategoryIncomeItem("inc_7", "Thu nợ", "ic_category_debt_collection", "income"),
            CategoryIncomeItem("inc_8", "Thu nhập khác", "ic_category_other", "income")

        )
    }

    // KHO DANH SÁCH CHI TIỀN MẶC ĐỊNH
    fun getExpenseCategories(): List<CategoryExpenseItem> {
        return listOf(
            // NHÓM ĂN UỐNG
            CategoryExpenseItem("exp_1", "Ăn sáng", "ic_category_breakfast", "Ăn uống"),
            CategoryExpenseItem("exp_2", "Ăn tiệm", "ic_category_dining_out", "Ăn uống"),
            CategoryExpenseItem("exp_3", "Ăn tối", "ic_category_dinner", "Ăn uống"),
            CategoryExpenseItem("exp_4", "Ăn trưa", "ic_category_lunch", "Ăn uống"),
            CategoryExpenseItem("exp_5", "Cafe", "ic_category_cafe", "Ăn uống"),
            CategoryExpenseItem("exp_6", "Đi chợ/siêu thị", "ic_category_grocery", "Ăn uống"),

            // NHÓM CON CÁI
            CategoryExpenseItem("exp_7", "Đồ chơi", "ic_category_toys", "Con cái"),
            CategoryExpenseItem("exp_8", "Học phí", "ic_category_tuition", "Con cái"),
            CategoryExpenseItem("exp_9", "Sách vở", "ic_category_books", "Con cái"),
            CategoryExpenseItem("exp_10", "Sữa", "ic_category_milk", "Con cái"),
            CategoryExpenseItem("exp_11", "Tiền tiêu vặt", "ic_category_pocket_money", "Con cái"),

            // NHÓM DỊCH VỤ SINH HOẠT
            CategoryExpenseItem("exp_12", "Điện", "ic_category_electricity", "Dịch vụ sinh hoạt"),
            CategoryExpenseItem("exp_13", "Điện thoại cố định", "ic_category_landline", "Dịch vụ sinh hoạt"),
            CategoryExpenseItem("exp_14", "Điện thoại di động", "ic_category_mobile", "Dịch vụ sinh hoạt"),
            CategoryExpenseItem("exp_15", "Gas", "ic_category_gas", "Dịch vụ sinh hoạt"),
            CategoryExpenseItem("exp_16", "Internet", "ic_category_internet", "Dịch vụ sinh hoạt"),
            CategoryExpenseItem("exp_17", "Nước", "ic_category_water", "Dịch vụ sinh hoạt"),
            CategoryExpenseItem("exp_18", "Thuê người giúp việc", "ic_category_maid", "Dịch vụ sinh hoạt"),
            CategoryExpenseItem("exp_19", "Truyền hình", "ic_category_tv", "Dịch vụ sinh hoạt"),

            // NHÓM ĐI LẠI
            CategoryExpenseItem("exp_20", "Bảo hiểm xe", "ic_category_insurance", "Đi lại"),
            CategoryExpenseItem("exp_21", "Gửi xe", "ic_category_parking", "Đi lại"),
            CategoryExpenseItem("exp_22", "Rửa xe", "ic_category_car_wash", "Đi lại"),
            CategoryExpenseItem("exp_23", "Sửa chữa, bảo dưỡng xe", "ic_category_maintenance", "Đi lại"),
            CategoryExpenseItem("exp_24", "Taxi/thuê xe", "ic_category_taxi", "Đi lại"),
            CategoryExpenseItem("exp_25", "Xăng xe", "ic_category_car_gas", "Đi lại"),

            // NHÓM HIẾU HỈ
            CategoryExpenseItem("exp_26", "Biếu tặng", "ic_category_gift_giving", "Hiếu hỉ"),
            CategoryExpenseItem("exp_27", "Cưới xin", "ic_category_wedding", "Hiếu hỉ"),
            CategoryExpenseItem("exp_28", "Ma chay", "ic_category_funeral", "Hiếu hỉ"),
            CategoryExpenseItem("exp_29", "Thăm hỏi", "ic_category_visit", "Hiếu hỉ"),

            // NHÓM HƯỞNG THỤ
            CategoryExpenseItem("exp_30", "Du lịch", "ic_category_travel", "Hưởng thụ"),
            CategoryExpenseItem("exp_31", "Làm đẹp", "ic_category_beauty", "Hưởng thụ"),
            CategoryExpenseItem("exp_32", "Mỹ phẩm", "ic_category_cosmetics", "Hưởng thụ"),
            CategoryExpenseItem("exp_33", "Phim ảnh ca nhạc", "ic_category_movies_music", "Hưởng thụ"),
            CategoryExpenseItem("exp_34", "Vui chơi giải trí", "ic_category_entertainment", "Hưởng thụ"),

            // NHÓM NGÂN HÀNG
            CategoryExpenseItem("exp_35", "Phí chuyển khoản", "ic_category_transfer_fee", "Ngân hàng"),

            // NHÓM NHÀ CỬA
            CategoryExpenseItem("exp_36", "Mua sắm đồ đạc", "ic_category_furniture", "Nhà cửa"),
            CategoryExpenseItem("exp_37", "Sửa chữa nhà cửa", "ic_category_home_repair", "Nhà cửa"),
            CategoryExpenseItem("exp_38", "Thuê nhà", "ic_category_house_rent", "Nhà cửa"),

            // NHÓM PHÁT TRIỂN BẢN THÂN
            CategoryExpenseItem("exp_39", "Giao lưu, quan hệ", "ic_category_networking", "Phát triển bản thân"),
            CategoryExpenseItem("exp_40", "Học hành", "ic_category_education", "Phát triển bản thân"),

            // (BỔ SUNG MỚI) NHÓM SỨC KHỎE
            CategoryExpenseItem("exp_41", "Khám chữa bệnh", "ic_category_medical", "Sức khỏe"),
            CategoryExpenseItem("exp_42", "Thể thao", "ic_category_sports", "Sức khỏe"),
            CategoryExpenseItem("exp_43", "Thuốc men", "ic_category_medicine", "Sức khỏe"),

            // (BỔ SUNG MỚI) NHÓM TRANG PHỤC
            CategoryExpenseItem("exp_44", "Giày dép", "ic_category_shoes", "Trang phục"),
            CategoryExpenseItem("exp_45", "Phụ kiện khác", "ic_category_accessories", "Trang phục"),
            CategoryExpenseItem("exp_46", "Quần áo", "ic_category_clothes", "Trang phục"),

            // NHÓM KHÁC
            CategoryExpenseItem("exp_47", "Tiền ra", "ic_category_cash_out", "Khác")
        )
    }
}