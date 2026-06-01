# 💰 Moni - Ứng dụng Quản lý Tài chính Cá nhân

Moni là một ứng dụng di động thông minh giúp người dùng theo dõi thu chi, quản lý ví tiền và xây dựng thói quen tài chính cá nhân lành mạnh. Ứng dụng được thiết kế với tiêu chí: Nhanh chóng, Trực quan và Mượt mà.

## 🌟 Tính năng nổi bật

* **Ghi chép giao dịch nhanh chóng:** Thêm khoản thu/chi, phân loại danh mục chỉ với vài thao tác.
* **Nhắc nhở thông minh:** Hệ thống thông báo cục bộ tự động nhắc nhở người dùng ghi chép vào các khung giờ cố định trong ngày.
* **Giao diện trực quan, hiện đại:** Sử dụng Floating Bottom Navigation với hiệu ứng trượt mượt mà. Phản hồi người dùng thân thiện qua hệ thống Custom Toast UI.
* **Đồng bộ đám mây:** Dữ liệu an toàn và đồng bộ thời gian thực.

## 🛠 Công nghệ sử dụng

* **Ngôn ngữ:** Kotlin
* **Kiến trúc:** MVVM (Model-View-ViewModel)
* **Giao diện:** ViewBinding, Navigation Component
* **Backend & Database:** Firebase Authentication (Google Sign-in) & Cloud Firestore
* **Xử lý nền:** AlarmManager & BroadcastReceiver để lên lịch thông báo

## 📸 Ảnh chụp màn hình

<p align="center">
  <img src="https://github.com/user-attachments/assets/228595d6-7a5e-446a-9f48-bb6ec0e28725" width="220" margin="10"/>
  <img src="https://github.com/user-attachments/assets/6db5af80-4133-4575-b9f6-33907ef52f6d" width="220" margin="10"/>
  <img src="https://github.com/user-attachments/assets/26680a03-2fd1-44fa-a0fa-4921f0e3425e" width="220" margin="10"/>
  <img src="https://github.com/user-attachments/assets/3b3473f0-96f7-4222-a3e5-8d0eea37668a" width="220" margin="10"/>
</p>

## 🚀 Hướng dẫn trải nghiệm & Cài đặt
Note: Dự án này chỉ áp dụng với điện thoại có hệ điều hành android

**Cách 1: Cài đặt nhanh lên điện thoại (Dành cho Người dùng & Nhà tuyển dụng)**
*Đây là cách nhanh nhất để bạn có thể tải và dùng thử app Moni ngay trên điện thoại Android của mình.*
1. Tại trang GitHub này, bạn hãy tìm và tải về file có đuôi **`.apk`** (ví dụ: `App_Moni.apk`).
2. Mở file vừa tải trên điện thoại Android để tiến hành cài đặt. 
3. *(Lưu ý: Nếu điện thoại hỏi, bạn hãy cấp quyền "Cho phép cài đặt ứng dụng từ nguồn không xác định" nhé).*
4. Mở ứng dụng Moni, đăng nhập và bắt đầu trải nghiệm!

**Cách 2: Chạy mã nguồn (Dành cho Lập trình viên)**
*Nếu bạn muốn xem cấu trúc code và chạy thử dự án trên phần mềm.*
1. Tải toàn bộ dự án này về máy (Bấm nút **Code** màu xanh lá ở trên cùng -> Chọn **Download ZIP**) và giải nén ra.
2. Mở phần mềm **Android Studio**, chọn **Open** và trỏ tới thư mục vừa giải nén.
3. Chờ vài phút để phần mềm tự động tải các cấu hình cần thiết.
4. Bấm nút **Run** (Hình tam giác màu xanh) để chạy ứng dụng lên máy ảo hoặc điện thoại thật.
*(Lưu ý bảo mật: File cấu hình Firebase `google-services.json` có thể không được đính kèm public, bạn vui lòng liên hệ trực tiếp để được cung cấp quyền truy cập Database nếu cần).*
