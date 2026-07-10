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

public class LoginActivity extends AppCompatActivity {

    private EditText edtLoginEmail;
    private EditText edtLoginPassword;
    private Button btnLogin;
    private TextView txtGoToRegister;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtLoginEmail = findViewById(R.id.edtLoginEmail);
        edtLoginPassword = findViewById(R.id.edtLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);

        firebaseAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(view -> loginUser());

        txtGoToRegister.setOnClickListener(view -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = edtLoginEmail.getText().toString().trim();
        String password = edtLoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtLoginEmail.setError("Vui lòng nhập email");
            edtLoginEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtLoginEmail.setError("Email không hợp lệ");
            edtLoginEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtLoginPassword.setError("Vui lòng nhập mật khẩu");
            edtLoginPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Đăng nhập thành công",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                LoginActivity.this,
                                MainActivity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Đăng nhập");

                        String message = "Đăng nhập thất bại";

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