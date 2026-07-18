package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import adu.nttu.englishai.R;

// =========================================================================
// REGISTER ACTIVITY: Màn hình Đăng ký tài khoản mới & Tạo hồ sơ Firestore
// =========================================================================
public class RegisterActivity extends AppCompatActivity {

    // Các thành phần giao diện (UI Components)
    private EditText edtName;
    private EditText edtEmail;
    private EditText edtPassword;
    private Button btnRegister;

    // Đối tượng quản lý xác thực (Auth) và Cơ sở dữ liệu đám mây (Firestore)
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Ánh xạ các biến Java với ID thẻ trong file XML
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);

        // 2. Khởi tạo đối tượng Firebase Auth và Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // 3. Gán sự kiện click cho nút Đăng ký
        btnRegister.setOnClickListener(view -> registerUser());
    }

    // HÀM QUAN TRỌNG: Kiểm tra dữ liệu đầu vào và thực hiện luồng đăng ký 3 bước nối tiếp
    private void registerUser() {
        // Lấy dữ liệu người dùng gõ vào và cắt bỏ khoảng trắng thừa ở đầu/cuối bằng .trim()
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // =========================================================================
        // BƯỚC 1: VALIDATION - Kiểm tra tính hợp lệ của dữ liệu đầu vào
        // =========================================================================

        // Kiểm tra họ tên không được để trống
        if (TextUtils.isEmpty(name)) {
            edtName.setError("Vui lòng nhập họ và tên");
            edtName.requestFocus(); // Đưa con trỏ chuột về ô họ tên
            return; // Dừng hàm ngay lập tức
        }

        // Kiểm tra email không được để trống
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }

        // Kiểm tra định dạng email chuẩn (phải có @, tên miền...) bằng Regular Expression
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        // Kiểm tra mật khẩu không được để trống
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }

        // Quy định bảo mật của Firebase Auth: Mật khẩu bắt buộc phải từ 6 ký tự trở lên
        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return;
        }

        // =========================================================================
        // BƯỚC 2: KHÓA GIAO DIỆN - Tránh bấm liên tục gửi nhiều request khi mạng chậm
        // =========================================================================
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        // =========================================================================
        // BƯỚC 3: GỌI FIREBASE AUTHENTICATION TẠO TÀI KHOẢN
        // =========================================================================
        // createUserWithEmailAndPassword: Gửi email & password lên server Google để tạo tài khoản
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        // Lấy đối tượng người dùng vừa đăng ký thành công
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {

                            // QUAN TRỌNG: Mặc định tài khoản mới tạo chỉ có Email/Pass, chưa có Tên.
                            // UserProfileChangeRequest dùng để nhét Họ Tên (name) vào thẳng trong Token Authentication
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name) // Gán tên người dùng vừa nhập vào đây
                                    .build();

                            // Gửi yêu cầu cập nhật Profile lên Firebase Auth
                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        // Sau khi cập nhật profile xong thì mới tiếp tục lưu vào Firestore và chuyển trang
                                        // Đây là kỹ thuật Chaining Asynchronous Tasks (Nối tiếp các tác vụ bất đồng bộ)
                                        saveUserToFirestore(
                                                firebaseUser.getUid(), // Mã ID duy nhất (UUID) của user trên Firebase
                                                name,
                                                email
                                        );
                                    });
                        } else {
                            resetButton(); // Mở khóa nút bấm lại nếu lỗi

                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Không lấy được thông tin người dùng",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } else {
                        resetButton(); // Mở khóa nút bấm lại cho người dùng thử lại

                        String errorMessage = "Đăng ký thất bại";

                        // Lấy câu lỗi chi tiết từ Firebase (ví dụ: Email đã tồn tại - The email address is already in use)
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        Toast.makeText(
                                RegisterActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // HÀM QUAN TRỌNG: Lưu thông tin chi tiết (Role, Thời gian tạo...) vào Cloud Firestore
    private void saveUserToFirestore(
            String userId,
            String name,
            String email
    ) {
        // Tạo một gói dữ liệu dạng Key-Value (Map) để đóng gói thông tin hồ sơ
        Map<String, Object> userData = new HashMap<>();

        userData.put("uid", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", "user"); // Phân quyền mặc định là học viên ("user")

        // FieldValue.serverTimestamp(): Lấy chính xác thời gian thực từ Máy chủ Google (Không dùng giờ điện thoại)
        userData.put("createdAt", FieldValue.serverTimestamp());

        // Ghi dữ liệu vào collection "users", với tên Document ID chính là UID của người dùng
        firestore.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    // KHI CẢ AUTH VÀ FIRESTORE ĐỀU THÀNH CÔNG -> HOÀN TẤT ĐĂNG KÝ
                    Toast.makeText(
                            RegisterActivity.this,
                            "Đăng ký thành công",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Chuyến xe Intent chuyển thẳng vào màn hình chính (MainActivity)
                    Intent intent = new Intent(
                            RegisterActivity.this,
                            MainActivity.class
                    );

                    // Xóa sạch lịch sử màn hình cũ (Backstack), ngăn người dùng bấm nút Back quay lại trang Register/Login
                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish(); // Đóng hoàn toàn RegisterActivity
                })
                .addOnFailureListener(exception -> {
                    resetButton(); // Mở khóa nút nếu lưu Firestore thất bại

                    Toast.makeText(
                            RegisterActivity.this,
                            "Đã tạo tài khoản nhưng lưu Firestore thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Hàm phụ: Mở khóa nút bấm và trả lại chữ "Đăng ký" khi có lỗi xảy ra
    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Đăng ký");
    }
}