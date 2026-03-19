package servlet.auth;

import dal.UserDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import util.PasswordUtil;
import java.io.IOException;

/**
 * ForgotPasswordServlet – Xử lý chức năng Quên mật khẩu.
 *
 * URL: /forgot-password
 *
 * GET /forgot-password → Hiển thị form nhập email
 * POST /forgot-password → Xử lý yêu cầu đặt lại mật khẩu
 *
 * Luồng quên mật khẩu (đơn giản hóa, không gửi email):
 * Bước 1: Người dùng nhập email đăng ký
 * Bước 2: Hệ thống kiểm tra email có tồn tại không
 * Bước 3: Người dùng được chuyển sang bước đặt mật khẩu mới
 * Bước 4: Người dùng nhập mật khẩu mới + xác nhận
 * Bước 5: Hệ thống cập nhật mật khẩu trong DB
 * Bước 6: Redirect về trang login với thông báo thành công
 *
 * ⚠️ Lưu ý bảo mật:
 * Hệ thống hiện tại KHÔNG gửi email xác minh (không cần cấu hình SMTP).
 * Đây là phiên bản đơn giản phù hợp cho môi trường phát triển/học tập.
 * Trong thực tế, cần thêm token ngẫu nhiên gửi qua email để xác minh.
 *
 * Session tạm thời:
 * Sau khi xác minh email thành công, lưu email vào session attribute
 * "resetEmail" để bước 2 biết được đang reset cho ai.
 * Session attribute này bị xóa sau khi đổi mật khẩu thành công.
 */
public class ForgotPasswordServlet extends HttpServlet {

    /** Độ dài tối thiểu của mật khẩu mới */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * GET: Hiển thị form quên mật khẩu.
     * Nếu session có "resetEmail" → cho thấy bước 2 (đặt mật khẩu mới).
     * Nếu không → hiển thị bước 1 (nhập email).
     *
     * Tham số GET:
     * ?step=1 → Bước 1: nhập email (mặc định)
     * ?step=2 → Bước 2: đặt mật khẩu mới (chỉ hợp lệ khi có session resetEmail)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Nếu đã đăng nhập → không cần quên mật khẩu
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("loggedUser") != null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String step = request.getParameter("step");

        // Chỉ cho phép vào bước 2 nếu đã xác minh email ở bước 1
        if ("2".equals(step)) {
            HttpSession currentSession = request.getSession(false);
            if (currentSession == null || currentSession.getAttribute("resetEmail") == null) {
                // Chưa xác minh email → quay lại bước 1
                response.sendRedirect(request.getContextPath() + "/forgot-password");
                return;
            }
            request.setAttribute("step", "2");
        } else {
            request.setAttribute("step", "1");
        }

        request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
    }

    /**
     * POST: Xử lý form quên mật khẩu.
     *
     * Hai hành động dựa trên tham số "step":
     * step=1 → Xác minh email → Chuyển sang bước 2
     * step=2 → Đặt mật khẩu mới → Redirect về login
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String step = request.getParameter("step");

        if ("2".equals(step)) {
            handleResetPassword(request, response);
        } else {
            handleVerifyEmail(request, response);
        }
    }

    /**
     * Bước 1: Xác minh email.
     *
     * Kiểm tra email có tồn tại trong hệ thống không.
     * Nếu có → lưu vào session "resetEmail" → redirect sang bước 2.
     * Nếu không → hiển thị lỗi.
     *
     * ⚠️ Bảo mật: Để tránh lộ thông tin, có thể dùng thông báo chung
     * "Nếu email tồn tại, chúng tôi sẽ gửi hướng dẫn" thay vì
     * xác nhận email có/không có. (Bản này dùng thông báo rõ ràng cho dễ demo)
     */
    private void handleVerifyEmail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = trimOrEmpty(request.getParameter("email"));

        if (email.isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập địa chỉ email!");
            request.setAttribute("step", "1");
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
            return;
        }

        UserDAO userDAO = new UserDAO();

        if (userDAO.findByEmail(email) == null) {
            // Email không tồn tại hoặc tài khoản đã bị khóa
            request.setAttribute("error",
                    "Email này chưa được đăng ký trong hệ thống hoặc tài khoản đã bị khóa!");
            request.setAttribute("step", "1");
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
            return;
        }

        // Email hợp lệ → lưu vào session để dùng ở bước 2
        HttpSession session = request.getSession(true);
        session.setAttribute("resetEmail", email);
        session.setMaxInactiveInterval(10 * 60); // Session reset hết hạn sau 10 phút

        // Chuyển sang bước 2 (đặt mật khẩu mới)
        response.sendRedirect(request.getContextPath() + "/forgot-password?step=2");
    }

    /**
     * Bước 2: Đặt mật khẩu mới.
     *
     * Kiểm tra mật khẩu mới hợp lệ, sau đó cập nhật vào DB.
     * Xóa session "resetEmail" sau khi hoàn tất để ngăn reset lại lần nữa.
     */
    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Kiểm tra session hợp lệ
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("resetEmail") == null) {
            response.sendRedirect(request.getContextPath() + "/forgot-password");
            return;
        }

        String email = (String) session.getAttribute("resetEmail");
        String newPassword = trimOrEmpty(request.getParameter("newPassword"));
        String confirmPassword = trimOrEmpty(request.getParameter("confirmPassword"));

        // Validation mật khẩu mới
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            request.setAttribute("error", "Vui lòng điền đầy đủ mật khẩu mới!");
            request.setAttribute("step", "2");
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
            return;
        }

        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            request.setAttribute("error",
                    "Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự!");
            request.setAttribute("step", "2");
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "Mật khẩu mới và xác nhận mật khẩu không khớp!");
            request.setAttribute("step", "2");
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
            return;
        }

        // Cập nhật mật khẩu mới (hash SHA-1 trước khi lưu)
        UserDAO userDAO = new UserDAO();
        boolean updateSuccess = userDAO.resetPasswordByEmail(email, PasswordUtil.sha1(newPassword));

        if (updateSuccess) {
            // Thành công → xóa session resetEmail, redirect về login
            session.removeAttribute("resetEmail");
            response.sendRedirect(request.getContextPath() + "/login?passwordReset=true");
        } else {
            // Lỗi DB (hiếm gặp)
            request.setAttribute("error", "Đặt lại mật khẩu thất bại. Vui lòng thử lại!");
            request.setAttribute("step", "2");
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
        }
    }

    /**
     * Lấy giá trị parameter, trim khoảng trắng, trả về chuỗi rỗng nếu null.
     */
    private String trimOrEmpty(String value) {
        return (value != null) ? value.trim() : "";
    }
}
