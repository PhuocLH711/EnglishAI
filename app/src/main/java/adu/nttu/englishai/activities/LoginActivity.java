package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import adu.nttu.englishai.R;

// =========================================================================
// LOGIN ACTIVITY: Màn hình Đăng nhập tài khoản bằng Firebase Authentication
// =========================================================================
public class LoginActivity extends AppCompatActivity {

    // Các thành phần giao diện (UI Components)
    private EditText edtLoginEmail;
    private EditText edtLoginPassword;
    private Button btnLogin;
    private TextView txtGoToRegister;

    // Đối tượng quản lý xác thực của Firebase (Bộ máy kiểm tra tài khoản/mật khẩu trên Cloud)
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ các biến Java với ID thẻ trong file XML
        edtLoginEmail = findViewById(R.id.edtLoginEmail);
        edtLoginPassword = findViewById(R.id.edtLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);

        // 2. Khởi tạo đối tượng Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // 3. Gán sự kiện cho nút Đăng nhập -> Gọi hàm xử lý loginUser()
        btnLogin.setOnClickListener(view -> loginUser());

        // 4. Gán sự kiện cho dòng chữ "Chưa có tài khoản? Đăng ký ngay" -> Chuyển sang trang RegisterActivity
        txtGoToRegister.setOnClickListener(view -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }

    // HÀM QUAN TRỌNG: Kiểm tra dữ liệu đầu vào (Validation) và gửi yêu cầu đăng nhập lên Firebase
    private void loginUser() {
        // Lấy dữ liệu người dùng nhập vào và cắt bỏ khoảng trắng thừa ở đầu/cuối bằng .trim()
        String email = edtLoginEmail.getText().toString().trim();
        String password = edtLoginPassword.getText().toString().trim();

        // =========================================================================
        // BƯỚC 1: VALIDATION - Kiểm tra tính hợp lệ của dữ liệu trước khi gửi lên mạng
        // =========================================================================

        // Kiểm tra xem email có bị để trống không
        if (TextUtils.isEmpty(email)) {
            edtLoginEmail.setError("Vui lòng nhập email");
            edtLoginEmail.requestFocus(); // Đưa con trỏ chuột về lại ô email
            return; // Dừng hàm ngay lập tức, không chạy code bên dưới
        }

        // Kiểm tra định dạng email chuẩn (phải có @, tên miền .com, .vn...) bằng biểu thức chính quy (Regular Expression)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtLoginEmail.setError("Email không hợp lệ");
            edtLoginEmail.requestFocus();
            return;
        }

        // Kiểm tra xem mật khẩu có bị để trống không
        if (TextUtils.isEmpty(password)) {
            edtLoginPassword.setError("Vui lòng nhập mật khẩu");
            edtLoginPassword.requestFocus();
            return;
        }

        // =========================================================================
        // BƯỚC 2: KHÓA GIAO DIỆN - Tránh người dùng bấm liên tục khi mạng chậm
        // =========================================================================
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // =========================================================================
        // BƯỚC 3: GỌI FIREBASE AUTHENTICATION ĐỂ XÁC THỰC
        // =========================================================================
        // signInWithEmailAndPassword: Lệnh gửi email & password lên server Google kiểm tra
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // addOnCompleteListener: Lắng nghe kết quả trả về từ server (Thành công hoặc Thất bại)
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Đăng nhập thành công",
                                Toast.LENGTH_SHORT
                        ).show();

                        // Tạo chuyến xe Intent chuyển sang màn hình chính (MainActivity)
                        Intent intent = new Intent(
                                LoginActivity.this,
                                MainActivity.class
                        );

                        // QUAN TRỌNG: Xóa sạch ngăn xếp màn hình cũ (Backstack)
                        // Giúp khi vào MainActivity, nếu người dùng bấm nút Back (Trở về) trên điện thoại,
                        // app sẽ thoát luôn chứ KHÔNG quay ngược lại màn hình Login nữa
                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish(); // Đóng hoàn toàn LoginActivity
                    } else {
                        // Nếu đăng nhập thất bại (sai pass, tài khoản không tồn tại, lỗi mạng...)
                        // -> Mở khóa nút bấm lại cho người dùng thử lại
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Đăng nhập");

                        String message = "Đăng nhập thất bại";

                        // Lấy câu thông báo lỗi chi tiết từ Firebase trả về (ví dụ: Wrong password, User not found...)
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}