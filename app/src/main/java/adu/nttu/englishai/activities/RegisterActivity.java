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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import adu.nttu.englishai.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName;
    private EditText edtEmail;
    private EditText edtPassword;
    private Button btnRegister;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ giao diện
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);

        // Khởi tạo Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(view -> registerUser());
    }

    private void registerUser() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Kiểm tra họ tên
        if (TextUtils.isEmpty(name)) {
            edtName.setError("Vui lòng nhập họ và tên");
            edtName.requestFocus();
            return;
        }

        // Kiểm tra email
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        // Kiểm tra mật khẩu
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            saveUserToFirestore(
                                    firebaseUser.getUid(),
                                    name,
                                    email
                            );
                        } else {
                            resetButton();

                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Không lấy được thông tin người dùng",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } else {
                        resetButton();

                        String errorMessage = "Đăng ký thất bại";

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

    private void saveUserToFirestore(
            String userId,
            String name,
            String email
    ) {
        Map<String, Object> userData = new HashMap<>();

        userData.put("uid", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", "user");
        userData.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            RegisterActivity.this,
                            "Đăng ký thành công",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(
                            RegisterActivity.this,
                            MainActivity.class
                    );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(exception -> {
                    resetButton();

                    Toast.makeText(
                            RegisterActivity.this,
                            "Đã tạo tài khoản nhưng lưu Firestore thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Đăng ký");
    }
}