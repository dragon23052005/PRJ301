# Kế Hoạch Phát Triển & Code Tóm Tắt (School Bus Management System)

Đây là bản tóm tắt lộ trình phát triển dự án Quản lý Xe buýt Trường học dựa trên kiến trúc Java Servlet, JDBC và JSP. 

## 1. Công nghệ & Kiến trúc Hệ thống
- **Nền tảng Controller:** Java Servlets
- **Views (Giao diện):** JSP (JavaServer Pages), JSTL, HTML/CSS/JS (Bootstrap)
- **Tương tác Cơ sở dữ liệu:** JDBC (Data Access Object - DAO Pattern)
- **Cơ sở dữ liệu:** SQL Server (`SchoolBusDB`)
- **Web Server:** Apache Tomcat

## 2. Tổ chức Source Code (Cấu trúc thư mục)
Để dự án dễ bảo trì và chia việc, hãy tuân thủ cấu trúc chuẩn:
- **`model/` (Entities):** Chứa các class tương ứng với bảng DB (`User`, `Route`, `Vehicle`, `Student`, `Trip`, `Attendance`, `AbsenceNotification`...)
- **`dal/` (Data Access Layer):** Chứa các thao tác CRUD tới DB (vd: `UserDAO`, `TripDAO`, `VehicleDAO`, `AttendanceDAO`).
- **`servlet/` (Controllers):** Chia theo từng phân quyền (Role)
  - `servlet.auth.` (LoginServlet, RegisterServlet, LogoutServlet...)
  - `servlet.admin.` (Quản lý users, tuyến xe, phân công...)
  - `servlet.monitor.` (MonitorDashboard, Chấm điểm danh...)
  - `servlet.parent.` (Đăng ký học sinh, xin nghỉ...)
  - `servlet.driver.` (Cập nhật lộ trình, trạng thái xe...)
- **`web/WEB-INF/views/`**: Chia thư mục `.jsp` theo role (`admin`, `monitor`, `parent`, `driver`, `common`).

---

## 3. Lộ trình Triển khai (4 Giai đoạn Chính)

### 🔴 Giai đoạn 1: Nền tảng & Xác Thực (Authentication Base)
*Giai đoạn này đảm bảo hệ thống có thể hoạt động ở mức cơ bản, người dùng có thể đăng nhập và định tuyến đúng trang.*
1. **Database:** Cài đặt SQL Server, chạy `database.sql`. Cấu hình `DBContext` kết nối ổn định.
2. **Auth Flow:**
   - Xây dựng `LoginServlet`: Validate mật khẩu (SHA-1 theo DB gốc). 
   - Check Cookie (`Remember me`).
   - Route (chuyển hướng) người dùng tới Homepage tùy theo `role` (Admin -> Admin Dashboard, Monitor -> Monitor Dashboard...).
3. **Common UI:** Setup Header, `sidebar.jsp`, Footer chung cho layout. Tính năng phân quyền hiển thị (JSTL `<c:if>`).

### 🔴 Giai đoạn 2: Module Admin (Dữ liệu Lõi)
*Phải có cái này thì các role khác mới có dữ liệu để thao tác.*
1. **Quản lý Users & Học sinh (Students):** Thêm, sửa, xóa phụ huynh, quản lý, tài xế.
2. **Quản lý Tuyến đường (Routes) & Điểm dừng (Stops):** CRUD tuyến đường.
3. **Quản lý Xe (Vehicles) & Phân công:** Gán Driver, Monitor và Route vào một Vehicle.
4. **Tạo Lịch Trình (Trips):** Admin tạo hoặc tự động sinh chuyến đi (`SCHEDULED`) theo ngày dựa trên Route/Vehicle.

### 🔴 Giai đoạn 3: Module Nghiệp vụ Giám sát (Monitor & Driver)
*Đóng vai trò điều phối hoạt động thật của tuyến xe buýt.*
1. **Monitor (Giám sát viên):**
   - **Màn hình chính (`MonitorDashboardServlet`):** Xem xe được phân công, lấy danh sách Trips hôm nay.
   - **Tính năng Điểm danh (Attendance):** Màn hình lấy danh sách Học sinh (`Students`) trên chuyến. Cập nhật `PRESENT`, `ABSENT`.
2. **Driver (Tài xế):**
   - Xem lịch chạy ngày hôm nay.
   - Bấm cập nhật trạng thái chuyến: Bắt đầu (`DEPARTED`), Kết thúc (`COMPLETED`).
   - Gửi báo cáo hư hỏng xe (`Vehicle Reports`).

### 🔴 Giai đoạn 4: Module Phụ Huynh (Parents) & Hoàn thiện
1. **Màn hình Phụ huynh:**
   - Xem thông tin xe buýt của con em (Biển số xe, số điện thoại Monitor).
   - Đăng ký Tuyến/Điểm đón (`Registrations`).
   - Tính năng báo vắng (`Absence Notifications`): Xin vắng phép cho ngày cụ thể (`NOTIFIED_ABSENT`).
2. **Refactor & Testing:**
   - Sửa lỗi mapping URL Tomcat (vd: lỗi `/ProjectGroup4/ProjectGroup4`).
   - Ráp JSTL cho các logic frontend phòng tránh Data null.
   - Xử lý Audit Trails (người tạo, người cập nhật).

---

## 4. Cách tổ chức phân chia công việc trong nhóm (Dành cho nhóm 5 người)
Dưới đây là phương án phân chia chi tiết để đảm bảo khối lượng công việc cân bằng và tính tích hợp cao:

- **Người 1: Trưởng nhóm & Hạ tầng (Integrator)**
    - Thiết lập cấu trúc Project, cấu hình `DBContext`.
    - Xây dựng Module **Authentication** (Login, Logout, Cookie, Phân quyền Filter).
    - Thiết kế **Layout chung** (Header, Sidebar, Footer) và xử lý lỗi Deployment/Tomcat.
    - DAO: `UserDAO` (phần xác thực).

- **Người 2: Admin - Quản lý Nhân sự**
    - CRUD **Users**: Quản lý tài khoản Admin, Driver, Monitor, Parent.
    - CRUD **Students**: Quản lý thông tin học sinh và liên kết với Phụ huynh.
    - Xây dựng màn hình Profile cá nhân.
    - DAO: `UserDAO` (phần CRUD), `StudentDAO`.

- **Người 3: Admin - Quản lý Vận hành**
    - CRUD **Routes** & **Stops** (Tuyến đường & Điểm dừng).
    - CRUD **Vehicles** (Xe buýt).
    - Logic **Phân công (Assignments)** & **Giờ chạy (Trips)**: Tự động sinh lịch trình hàng ngày.
    - DAO: `RouteDAO`, `VehicleDAO`, `AssignmentDAO`.

- **Người 4: Module Giám sát (Monitor)**
    - **Monitor Dashboard**: Hiển thị danh sách chuyến đi trong ngày.
    - **Attendance (Điểm danh)**: Cập nhật trạng thái học sinh (Present/Absent) theo thời gian thực.
    - Xử lý logic đón/trả học sinh trên xe.
    - DAO: `TripDAO`, `AttendanceDAO`.

- **Người 5: Module Phụ huynh & Tài xế (End-users)**
    - **Parent Portal**: Theo dõi xe của con, xem lịch sử đi học.
    - **Absence**: Phụ huynh báo nghỉ cho con.
    - **Driver UI**: Tài xế cập nhật trạng thái xe (Bắt đầu/Hoàn thành).
    - **Kiểm thử (QA)**: Kiểm tra luồng dữ liệu giữa các Role và sửa lỗi UI.
    - DAO: `AbsenceDAO`.

## 5. Các Checklist Quan trọng để phòng Bug (Dành cho Coder)
- [ ] Mọi Servlet phải check Session (Nếu chưa log in, redirect ra Login).
- [ ] Khi redirect bằng `request.getRequestDispatcher`, lưu ý dùng đúng đường dẫn nội bộ (bắt đầu bằng `/WEB-INF/...`).
- [ ] Luôn sử dụng `PreparedStatement` thay vì `Statement` trong các file DAO để chống SQL Injection.
- [ ] Close Connection, ResultSet trong khối `finally` ở lớp DAO.
