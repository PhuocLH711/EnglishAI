package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import adu.nttu.englishai.R;
import adu.nttu.englishai.firebase.FirebaseAuthManager;

public class RegisterActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================
    private EditText edtName;
    private EditText edtEmail;
    private EditText edtPassword;
    private EditText edtConfirmPassword;

    private MaterialButton btnRegister;
    private TextView txtGoToLogin;

    // =========================================================
    // FIREBASE
    // =========================================================
    private FirebaseAuthManager authManager;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        // =====================================================
        // 1. ÁNH XẠ VIEW
        // =====================================================
        edtName =
                findViewById(R.id.edtName);

        edtEmail =
                findViewById(R.id.edtEmail);

        edtPassword =
                findViewById(R.id.edtPassword);

        edtConfirmPassword =
                findViewById(R.id.edtConfirmPassword);

        btnRegister =
                findViewById(R.id.btnRegister);

        txtGoToLogin =
                findViewById(R.id.txtGoToLogin);

        // =====================================================
        // 2. FIREBASE
        // =====================================================
        authManager =
                new FirebaseAuthManager();

        firestore =
                FirebaseFirestore.getInstance();

        // =====================================================
        // 3. ĐĂNG KÝ
        // =====================================================
        btnRegister.setOnClickListener(
                view -> registerUser()
        );

        // =====================================================
        // 4. QUAY VỀ LOGIN
        // =====================================================
        txtGoToLogin.setOnClickListener(
                view -> finish()
        );
    }

    // =========================================================================
    // REGISTER
    // =========================================================================
    private void registerUser() {

        String name =
                edtName
                        .getText()
                        .toString()
                        .trim();

        String email =
                edtEmail
                        .getText()
                        .toString()
                        .trim();

        String password =
                edtPassword
                        .getText()
                        .toString()
                        .trim();

        String confirmPassword =
                edtConfirmPassword
                        .getText()
                        .toString()
                        .trim();

        // =====================================================
        // NAME
        // =====================================================
        if (TextUtils.isEmpty(name)) {

            edtName.setError(
                    "Vui lòng nhập họ và tên"
            );

            edtName.requestFocus();

            return;
        }

        // =====================================================
        // EMAIL
        // =====================================================
        if (TextUtils.isEmpty(email)) {

            edtEmail.setError(
                    "Vui lòng nhập email"
            );

            edtEmail.requestFocus();

            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            edtEmail.setError(
                    "Email không hợp lệ"
            );

            edtEmail.requestFocus();

            return;
        }

        // =====================================================
        // PASSWORD
        // =====================================================
        if (TextUtils.isEmpty(password)) {

            edtPassword.setError(
                    "Vui lòng nhập mật khẩu"
            );

            edtPassword.requestFocus();

            return;
        }

        if (password.length() < 6) {

            edtPassword.setError(
                    "Mật khẩu phải có ít nhất 6 ký tự"
            );

            edtPassword.requestFocus();

            return;
        }

        // =====================================================
        // CONFIRM PASSWORD
        // =====================================================
        if (TextUtils.isEmpty(confirmPassword)) {

            edtConfirmPassword.setError(
                    "Vui lòng xác nhận mật khẩu"
            );

            edtConfirmPassword.requestFocus();

            return;
        }

        if (!password.equals(confirmPassword)) {

            edtConfirmPassword.setError(
                    "Mật khẩu xác nhận không khớp"
            );

            edtConfirmPassword.requestFocus();

            return;
        }

        // =====================================================
        // LOADING
        // =====================================================
        setLoading(true);

        // =====================================================
        // FIREBASE AUTH
        // =====================================================
        authManager
                .registerWithEmail(
                        email,
                        password
                )
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        setLoading(false);

                        String errorMessage =
                                "Đăng ký thất bại";

                        if (task.getException() != null) {

                            errorMessage =
                                    task
                                            .getException()
                                            .getMessage();
                        }

                        Toast.makeText(
                                RegisterActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    FirebaseUser firebaseUser =
                            authManager
                                    .getCurrentUser();

                    if (firebaseUser == null) {

                        setLoading(false);

                        Toast.makeText(
                                RegisterActivity.this,
                                "Không lấy được thông tin người dùng",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    // =================================================
                    // DISPLAY NAME
                    // =================================================
                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest
                                    .Builder()
                                    .setDisplayName(name)
                                    .build();

                    firebaseUser
                            .updateProfile(profileUpdates)
                            .addOnCompleteListener(updateTask -> {

                                saveUserToFirestore(
                                        firebaseUser,
                                        name,
                                        email
                                );
                            });
                });
    }

    // =========================================================================
    // SAVE USER TO FIRESTORE
    // =========================================================================
    private void saveUserToFirestore(
            FirebaseUser firebaseUser,
            String name,
            String email
    ) {

        String userId =
                firebaseUser.getUid();

        Map<String, Object> userData =
                new HashMap<>();

        userData.put(
                "uid",
                userId
        );

        userData.put(
                "name",
                name
        );

        userData.put(
                "email",
                email
        );

        userData.put(
                "role",
                "user"
        );

        userData.put(
                "authProvider",
                "password"
        );

        userData.put(
                "score",
                0
        );

        userData.put(
                "streak",
                0
        );

        userData.put(
                "emailVerified",
                false
        );

        userData.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        firestore
                .collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(unused -> {

                    // Firestore OK
                    // → gửi email verification
                    sendVerificationEmail();
                })
                .addOnFailureListener(exception -> {

                    setLoading(false);

                    Toast.makeText(
                            RegisterActivity.this,
                            "Đã tạo tài khoản nhưng lưu Firestore thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================================
    // SEND VERIFICATION EMAIL
    // =========================================================================
    private void sendVerificationEmail() {

        FirebaseUser user =
                authManager
                        .getCurrentUser();

        if (user == null) {

            setLoading(false);

            Toast.makeText(
                    RegisterActivity.this,
                    "Không tìm thấy tài khoản để gửi email xác minh",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // CHỈ GỌI 1 LẦN
        Task<Void> verificationTask =
                authManager.sendVerificationEmail();

        if (verificationTask == null) {

            setLoading(false);

            Toast.makeText(
                    RegisterActivity.this,
                    "Không thể gửi email xác minh",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        verificationTask
                .addOnCompleteListener(task -> {

                    setLoading(false);

                    if (task.isSuccessful()) {

                        // Đăng xuất để bắt buộc xác minh trước khi login
                        authManager.logout();

                        Toast.makeText(
                                RegisterActivity.this,
                                "Tạo tài khoản thành công. "
                                        + "Vui lòng kiểm tra email để xác minh tài khoản.",
                                Toast.LENGTH_LONG
                        ).show();

                        goToLoginActivity();

                    } else {

                        String error =
                                "Tạo tài khoản thành công nhưng không gửi được email xác minh.";

                        if (task.getException() != null) {

                            error =
                                    task
                                            .getException()
                                            .getMessage();
                        }

                        Toast.makeText(
                                RegisterActivity.this,
                                error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // =========================================================================
    // LOGIN ACTIVITY
    // =========================================================================
    private void goToLoginActivity() {

        Intent intent =
                new Intent(
                        RegisterActivity.this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================================
    // LOADING
    // =========================================================================
    private void setLoading(
            boolean loading
    ) {

        btnRegister.setEnabled(
                !loading
        );

        if (loading) {

            btnRegister.setText(
                    "Đang tạo tài khoản..."
            );

        } else {

            btnRegister.setText(
                    "Tạo tài khoản"
            );
        }
    }
}