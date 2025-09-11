Video demo : [https://drive.google.com/drive/u/0/folders/15k7qFY0U9PfqdHs_cKPQLNqPZvoE3jWp]
## 1. ĐĂNG KÝ TÀI KHOẢN

### 1.1. Các bước đăng ký

1. Truy cập trang chủ, nhấn nút **"Đăng ký"** hoặc truy cập trực tiếp đường dẫn `/register`.
2. Nhập đầy đủ thông tin:
   - Họ tên
   - Email (bắt buộc, duy nhất)
   - Mật khẩu (tối thiểu 6 ký tự)
   - Xác nhận mật khẩu.
3. Nhấn **"Đăng ký"** để gửi thông tin.

### 1.2. Xác thực tài khoản qua OTP

1. Sau khi đăng ký, hệ thống sẽ gửi mã OTP (6 số) về email bạn vừa đăng ký.
2. Truy cập trang `/verify-otp` (hệ thống tự chuyển hướng hoặc bạn vào link trong email).
3. Nhập mã OTP vào ô xác thực, nhấn **"Xác nhận"**.
4. Nếu OTP hợp lệ, tài khoản sẽ được kích hoạt. Nếu chưa nhận được OTP, nhấn **"Gửi lại mã OTP"**.

> **Lưu ý:**
> - OTP có hiệu lực trong 5 phút.
> - Nếu quá thời gian, hãy yêu cầu gửi lại mã mới.

## 2. ĐĂNG NHẬP & QUÊN MẬT KHẨU

### 2.1. Đăng nhập

1. Truy cập `/login`.
2. Nhập email và mật khẩu đã đăng ký.
3. Nhấn **"Đăng nhập"**.
4. Nếu thông tin đúng và tài khoản đã xác thực, bạn sẽ được chuyển đến trang chủ.

### 2.2. Quên mật khẩu

1. Tại trang đăng nhập, nhấn **"Quên mật khẩu?"** hoặc truy cập `/forgot-password`.
2. Nhập email đã đăng ký, nhấn **"Gửi mã  OTP"**.
3. Kiểm tra email, nhập OTP đặt lại mật khẩu.
4. Nhập mật khẩu mới và xác nhận.

## 3. QUẢN LÝ HỒ SƠ CÁ NHÂN

### 3.1. Xem thông tin cá nhân

- Truy cập `/profile` hoặc chọn **"Thông tin"** trên thanh điều hướng.
- Xem các thông tin: Họ tên, email, số điện thoại, địa chỉ, ngày sinh.

### 3.2. Cập nhật thông tin

1. Tại trang hồ sơ, nhấn **"Chỉnh sửa"**.
2. Thay đổi các trường mong muốn.
3. Nhấn **"Lưu thay đổi"**.
4. Hệ thống sẽ thông báo thành công hoặc lỗi (nếu có).

### 3.3. Đổi mật khẩu

- Thường nằm trong phần chỉnh sửa hồ sơ hoặc mục riêng.
- Nhập mật khẩu cũ, mật khẩu mới, xác nhận lại mật khẩu mới.
- Nhấn **"Đổi mật khẩu"**.

---

## 4. GÓI DỊCH VỤ

### 4.1. Xem các gói dịch vụ

- Truy cập `/services` hoặc chọn **"Gói dịch vụ"** trên menu.
- Hệ thống hiển thị các gói: **Cơ bản**, **Mở rộng**, **Nâng cao**.
- Mỗi gói có mô tả, tính năng, giá, thời hạn sử dụng.

### 4.2. Đăng ký/Mua gói dịch vụ

1. Nhấn nút **"VNPAY"** tại gói mong muốn.
2. Thực hiện thanh toán theo hướng dẫn.
3. Sau khi thanh toán thành công, gói dịch vụ sẽ được kích hoạt cho tài khoản.




## 5. XEM LỊCH SỬ GIAO DỊCH 

### Bước 1: Đăng nhập vào hệ thống
- Truy cập trang `/login`.
- Nhập email và mật khẩu, nhấn **"Đăng nhập"**.

### Bước 2: Truy cập chức năng lịch sử giao dịch
- Sau khi đăng nhập, trên thanh menu chọn **"Giao dịch"** hoặc truy cập trực tiếp đường dẫn `/user_transactions`.

### Bước 3: Xem danh sách giao dịch
- Hệ thống sẽ hiển thị danh sách các giao dịch của bạn, bao gồm:
  - Mã giao dịch
  - Loại giao dịch (nạp tiền, mua dịch vụ, ...)
  - Số tiền
  - Trạng thái giao dịch (thành công, thất bại, đang xử lý)

### Bước 4: Xuất báo cáo

- Người dùng có thể xuất báo cáo giao dịch ra file Excel để lưu trữ hoặc phân tích.

> **Lưu ý:**
> - Chỉ xem được các giao dịch của chính tài khoản đang đăng nhập.
---


## 6. QUẢN TRỊ HỆ THỐNG (ADMIN)

### 6.1. Quản lý tài khoản người dùng

1. Truy cập `/admin/account_management`.
2. Xem danh sách tài khoản, tìm kiếm theo tên/email.
3. Thêm mới tài khoản: Nhấn **"Thêm tài khoản"**, nhập thông tin, lưu lại.
4. Sửa thông tin: Nhấn **"Chỉnh sửa"** tại tài khoản cần sửa.
5. Xóa tài khoản: Nhấn **"Xóa"** tại tài khoản cần xóa.

### 6.2. Quản lý gói dịch vụ

- Truy cập `/servicepacks` để thêm, sửa, xóa các gói dịch vụ.
- Thêm thành viên vào gói tại `/servicepacks/add-member`.

### 6.3. Quản lý nhật ký hệ thống

- Truy cập `/log` để xem lịch sử thao tác, hoạt động của người dùng.

### 6.4. Thống kê giao dịch

- Truy cập `/thongkegiaodich` để xem biểu đồ, thống kê tổng quan về giao dịch.

### 6.5. Xuất báo cáo

- Quản trị viên có thể xuất báo cáo giao dịch ra file Excel để lưu trữ hoặc phân tích.

---

## 7. HỖ TRỢ & LIÊN HỆ

- **Gói Cơ bản:** Hỗ trợ qua email.
- **Gói Mở rộng:** Hỗ trợ ưu tiên qua email và chat.
- **Gói Nâng cao:** Hỗ trợ 24/7 qua email, chat, điện thoại.
- Truy cập phần **"Liên hệ"** trên trang chủ hoặc gửi email tới bộ phận hỗ trợ.

---

## 8. BẢO MẬT & QUYỀN TRUY CẬP

- Hệ thống sử dụng xác thực OTP, mã hóa mật khẩu, phân quyền truy cập:
  - **Admin:** Quản lý toàn bộ hệ thống.
  - **Client:** Người dùng thông thường.
  - **Admin dịch vụ:** Quản lý các gói dịch vụ.
- Đăng xuất an toàn, tự động hủy phiên khi đăng xuất.
- Chỉ tài khoản đã xác thực mới sử dụng đầy đủ chức năng.
- Đăng xuất sau khi sử dụng trên thiết bị công cộng.
- Nếu phát hiện bất thường, liên hệ ngay bộ phận hỗ trợ.
