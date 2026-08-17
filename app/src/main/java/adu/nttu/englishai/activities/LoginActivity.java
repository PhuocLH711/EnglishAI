package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import adu.nttu.englishai.R;
import adu.nttu.englishai.firebase.FirebaseAuthManager;
import adu.nttu.englishai.firebase.GoogleSignInManager;

public class LoginActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================
    private EditText edtLoginEmail;
    private EditText edtLoginPassword;

    private MaterialButton btnLogin;
    private MaterialButton btnGoogleLogin;

    private TextView txtForgotPassword;
    private TextView txtGoToRegister;

    // =========================================================
    // FIREBASE
    // =========================================================
    private FirebaseAuthManager authManager;
    private FirebaseFirestore firestore;

    // =========================================================
    // GOOGLE
    // =========================================================
    private GoogleSignInManager googleSignInManager;

    // =========================================================
    // LIFECYCLE
    // =========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // =====================================================
        // 1. ÁNH XẠ VIEW
        // =====================================================
        edtLoginEmail =
                findViewById(R.id.edtLoginEmail);

        edtLoginPassword =
                findViewById(R.id.edtLoginPassword);

        btnLogin =
                findViewById(R.id.btnLogin);

        btnGoogleLogin =
                findViewById(R.id.btnGoogleLogin);

        txtForgotPassword =
                findViewById(R.id.txtForgotPassword);

        txtGoToRegister =
                findViewById(R.id.txtGoToRegister);

        // =====================================================
        // 2. FIREBASE
        // =====================================================
        authManager =
                new FirebaseAuthManager();

        firestore =
                FirebaseFirestore.getInstance();

        // =====================================================
        // 3. GOOGLE SIGN-IN MANAGER
        // =====================================================
        googleSignInManager =
                new GoogleSignInManager(this);

        // =====================================================
        // 4. ĐĂNG NHẬP EMAIL
        // =====================================================
        btnLogin.setOnClickListener(
                view -> loginUser()
        );

        // =====================================================
        // 5. GOOGLE LOGIN
        // =====================================================
        btnGoogleLogin.setOnClickListener(
                view -> startGoogleSignIn()
        );

        // =====================================================
        // 6. QUÊN MẬT KHẨU
        // =====================================================
        txtForgotPassword.setOnClickListener(
                view -> showForgotPasswordDialog()
        );

        // =====================================================
        // 7. CHUYỂN SANG REGISTER
        // =====================================================
        txtGoToRegister.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            RegisterActivity.class
                    );

            startActivity(intent);
        });
    }

    // =========================================================================
    // GOOGLE SIGN-IN
    // =========================================================================
    private void startGoogleSignIn() {

        btnGoogleLogin.setEnabled(false);

        btnGoogleLogin.setText(
                "Đang kết nối Google..."
        );

        googleSignInManager.signIn(
                new GoogleSignInManager.GoogleSignInCallback() {

                    @Override
                    public void onSuccess(String idToken) {

                        firebaseAuthWithGoogle(
                                idToken
                        );
                    }

                    @Override
                    public void onError(String message) {

                        resetGoogleButton();

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================================
    // GOOGLE TOKEN → FIREBASE AUTH
    // =========================================================================
    private void firebaseAuthWithGoogle(
            String idToken
    ) {

        authManager
                .loginWithGoogleToken(idToken)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        resetGoogleButton();

                        String error =
                                "Đăng nhập Google thất bại";

                        if (task.getException() != null) {

                            error =
                                    task
                                            .getException()
                                            .getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                error,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    FirebaseUser user =
                            authManager
                                    .getCurrentUser();

                    if (user == null) {

                        resetGoogleButton();

                        Toast.makeText(
                                LoginActivity.this,
                                "Không lấy được thông tin tài khoản Google",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    // Kiểm tra user đã có Firestore hay chưa
                    createGoogleUserIfNeeded(
                            user
                    );
                });
    }

    // =========================================================================
    // GOOGLE USER → FIRESTORE
    // =========================================================================
    private void createGoogleUserIfNeeded(
            FirebaseUser user
    ) {

        firestore
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {

                    // =========================================================
                    // USER GOOGLE ĐÃ TỒN TẠI
                    // =========================================================
                    if (snapshot.exists()) {

                        resetGoogleButton();

                        Toast.makeText(
                                LoginActivity.this,
                                "Đăng nhập Google thành công",
                                Toast.LENGTH_SHORT
                        ).show();

                        goToMainActivity();

                        return;
                    }

                    // =========================================================
                    // USER GOOGLE MỚI
                    // =========================================================
                    Map<String, Object> userData =
                            new HashMap<>();

                    userData.put(
                            "uid",
                            user.getUid()
                    );

                    userData.put(
                            "name",
                            user.getDisplayName() != null
                                    ? user.getDisplayName()
                                    : "Người dùng EnglishAI"
                    );

                    userData.put(
                            "email",
                            user.getEmail() != null
                                    ? user.getEmail()
                                    : ""
                    );

                    userData.put(
                            "role",
                            "user"
                    );

                    userData.put(
                            "authProvider",
                            "google"
                    );

                    userData.put(
                            "emailVerified",
                            true
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
                            "createdAt",
                            FieldValue.serverTimestamp()
                    );

                    firestore
                            .collection("users")
                            .document(user.getUid())
                            .set(userData)
                            .addOnSuccessListener(unused -> {

                                resetGoogleButton();

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Chào mừng bạn đến với EnglishAI!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                goToMainActivity();
                            })
                            .addOnFailureListener(e -> {

                                resetGoogleButton();

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Đăng nhập Google thành công nhưng "
                                                + "không tạo được hồ sơ: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {

                    resetGoogleButton();

                    Toast.makeText(
                            LoginActivity.this,
                            "Không thể kiểm tra dữ liệu người dùng: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================================
    // RESET GOOGLE BUTTON
    // =========================================================================
    private void resetGoogleButton() {

        btnGoogleLogin.setEnabled(true);

        btnGoogleLogin.setText(
                "G   Tiếp tục với Google"
        );
    }

    // =========================================================================
    // LOGIN EMAIL + PASSWORD
    // =========================================================================
    private void loginUser() {

        String email =
                edtLoginEmail
                        .getText()
                        .toString()
                        .trim();

        String password =
                edtLoginPassword
                        .getText()
                        .toString()
                        .trim();

        // =========================================================
        // EMAIL VALIDATION
        // =========================================================
        if (TextUtils.isEmpty(email)) {

            edtLoginEmail.setError(
                    "Vui lòng nhập email"
            );

            edtLoginEmail.requestFocus();

            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            edtLoginEmail.setError(
                    "Email không hợp lệ"
            );

            edtLoginEmail.requestFocus();

            return;
        }

        // =========================================================
        // PASSWORD VALIDATION
        // =========================================================
        if (TextUtils.isEmpty(password)) {

            edtLoginPassword.setError(
                    "Vui lòng nhập mật khẩu"
            );

            edtLoginPassword.requestFocus();

            return;
        }

        // =========================================================
        // LOADING
        // =========================================================
        setLoginLoading(true);

        // =========================================================
        // FIREBASE EMAIL LOGIN
        // =========================================================
        authManager
                .loginWithEmail(
                        email,
                        password
                )
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        setLoginLoading(false);

                        String message =
                                "Đăng nhập thất bại";

                        if (task.getException() != null) {

                            message =
                                    task
                                            .getException()
                                            .getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    FirebaseUser user =
                            authManager
                                    .getCurrentUser();

                    if (user == null) {

                        setLoginLoading(false);

                        Toast.makeText(
                                LoginActivity.this,
                                "Không lấy được thông tin tài khoản",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    // =========================================================
                    // RELOAD ĐỂ LẤY TRẠNG THÁI VERIFY MỚI NHẤT
                    // =========================================================
                    user.reload()
                            .addOnCompleteListener(reloadTask -> {

                                FirebaseUser refreshedUser =
                                        authManager
                                                .getCurrentUser();

                                if (refreshedUser == null) {

                                    setLoginLoading(false);

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Không thể cập nhật trạng thái tài khoản",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                // =================================================
                                // CHƯA XÁC MINH EMAIL
                                // =================================================
                                if (!refreshedUser.isEmailVerified()) {

                                    setLoginLoading(false);

                                    showEmailNotVerifiedDialog();

                                    return;
                                }

                                // =================================================
                                // ĐÃ XÁC MINH
                                // =================================================
                                updateEmailVerifiedInFirestore(
                                        refreshedUser
                                );
                            });
                });
    }

    // =========================================================================
    // UPDATE EMAIL VERIFIED TRÊN FIRESTORE
    // =========================================================================
    private void updateEmailVerifiedInFirestore(
            FirebaseUser user
    ) {

        firestore
                .collection("users")
                .document(user.getUid())
                .update(
                        "emailVerified",
                        true
                )
                .addOnCompleteListener(task -> {

                    setLoginLoading(false);

                    Toast.makeText(
                            LoginActivity.this,
                            "Đăng nhập thành công",
                            Toast.LENGTH_SHORT
                    ).show();

                    goToMainActivity();
                });
    }

    // =========================================================================
    // DIALOG EMAIL CHƯA XÁC MINH
    // =========================================================================
    private void showEmailNotVerifiedDialog() {

        View dialogView =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.dialog_email_not_verified,
                                null
                        );

        TextView tvVerificationEmail =
                dialogView.findViewById(
                        R.id.tvVerificationEmail
                );

        MaterialButton btnCheckVerified =
                dialogView.findViewById(
                        R.id.btnCheckVerified
                );

        MaterialButton btnResendVerification =
                dialogView.findViewById(
                        R.id.btnResendVerification
                );

        TextView txtCloseVerification =
                dialogView.findViewById(
                        R.id.txtCloseVerification
                );

        FirebaseUser currentUser =
                authManager
                        .getCurrentUser();

        if (currentUser != null
                && currentUser.getEmail() != null) {

            tvVerificationEmail.setText(
                    currentUser.getEmail()
            );
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

        dialog.setCanceledOnTouchOutside(false);

        // =========================================================
        // TÔI ĐÃ XÁC MINH
        // =========================================================
        btnCheckVerified.setOnClickListener(view -> {

            FirebaseUser user =
                    authManager.getCurrentUser();

            if (user == null) {

                Toast.makeText(
                        LoginActivity.this,
                        "Không tìm thấy tài khoản",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            btnCheckVerified.setEnabled(false);

            btnCheckVerified.setText(
                    "Đang kiểm tra..."
            );

            user.reload()
                    .addOnCompleteListener(task -> {

                        btnCheckVerified.setEnabled(true);

                        btnCheckVerified.setText(
                                "Tôi đã xác minh"
                        );

                        if (!task.isSuccessful()) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Không thể kiểm tra trạng thái xác minh",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        FirebaseUser refreshedUser =
                                authManager
                                        .getCurrentUser();

                        if (refreshedUser != null
                                && refreshedUser.isEmailVerified()) {

                            dialog.dismiss();

                            updateEmailVerifiedInFirestore(
                                    refreshedUser
                            );

                        } else {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Email vẫn chưa được xác minh. "
                                            + "Hãy mở email và xác minh trước.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        // =========================================================
        // GỬI LẠI EMAIL XÁC MINH
        // =========================================================
        btnResendVerification.setOnClickListener(view -> {

            FirebaseUser user =
                    authManager
                            .getCurrentUser();

            if (user == null) {

                Toast.makeText(
                        LoginActivity.this,
                        "Không tìm thấy tài khoản",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            btnResendVerification.setEnabled(false);

            btnResendVerification.setText(
                    "Đang gửi..."
            );

            authManager
                    .sendVerificationEmail()
                    .addOnCompleteListener(task -> {

                        btnResendVerification.setEnabled(true);

                        btnResendVerification.setText(
                                "Gửi lại email xác minh"
                        );

                        if (task.isSuccessful()) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Đã gửi lại email xác minh đến "
                                            + user.getEmail(),
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {

                            String error =
                                    "Không thể gửi lại email xác minh";

                            if (task.getException() != null) {

                                error =
                                        task
                                                .getException()
                                                .getMessage();
                            }

                            Toast.makeText(
                                    LoginActivity.this,
                                    error,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        // =========================================================
        // ĐỂ SAU
        // =========================================================
        txtCloseVerification.setOnClickListener(view -> {

            authManager.logout();

            dialog.dismiss();
        });

        dialog.show();

        // =========================================================
        // STYLE DIALOG
        // =========================================================
        if (dialog.getWindow() != null) {

            dialog
                    .getWindow()
                    .setBackgroundDrawableResource(
                            android.R.color.transparent
                    );

            int width =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels
                                    * 0.90
                    );

            dialog
                    .getWindow()
                    .setLayout(
                            width,
                            android.view.ViewGroup
                                    .LayoutParams
                                    .WRAP_CONTENT
                    );
        }
    }

    // =========================================================================
    // DIALOG QUÊN MẬT KHẨU
    // =========================================================================
    private void showForgotPasswordDialog() {

        View dialogView =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.dialog_forgot_password,
                                null
                        );

        TextInputEditText edtResetEmail =
                dialogView.findViewById(
                        R.id.edtResetEmail
                );

        MaterialButton btnCancelReset =
                dialogView.findViewById(
                        R.id.btnCancelReset
                );

        MaterialButton btnSendResetEmail =
                dialogView.findViewById(
                        R.id.btnSendResetEmail
                );

        String currentEmail =
                edtLoginEmail
                        .getText()
                        .toString()
                        .trim();

        if (!TextUtils.isEmpty(currentEmail)) {

            edtResetEmail.setText(
                    currentEmail
            );

            edtResetEmail.setSelection(
                    currentEmail.length()
            );
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

        dialog.setCanceledOnTouchOutside(false);

        // =========================================================
        // HỦY
        // =========================================================
        btnCancelReset.setOnClickListener(
                view -> dialog.dismiss()
        );

        // =========================================================
        // GỬI EMAIL RESET
        // =========================================================
        btnSendResetEmail.setOnClickListener(view -> {

            String email =
                    edtResetEmail
                            .getText()
                            .toString()
                            .trim();

            if (TextUtils.isEmpty(email)) {

                edtResetEmail.setError(
                        "Vui lòng nhập email"
                );

                edtResetEmail.requestFocus();

                return;
            }

            if (!Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()) {

                edtResetEmail.setError(
                        "Email không hợp lệ"
                );

                edtResetEmail.requestFocus();

                return;
            }

            btnSendResetEmail.setEnabled(false);

            btnSendResetEmail.setText(
                    "Đang gửi..."
            );

            authManager
                    .sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {

                        btnSendResetEmail.setEnabled(true);

                        btnSendResetEmail.setText(
                                "Gửi email"
                        );

                        if (task.isSuccessful()) {

                            dialog.dismiss();

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Đã gửi liên kết đặt lại mật khẩu. "
                                            + "Vui lòng kiểm tra email.",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {

                            String error =
                                    "Không thể gửi email đặt lại mật khẩu";

                            if (task.getException() != null) {

                                error =
                                        task
                                                .getException()
                                                .getMessage();
                            }

                            Toast.makeText(
                                    LoginActivity.this,
                                    error,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        dialog.show();

        // =========================================================
        // STYLE DIALOG
        // =========================================================
        if (dialog.getWindow() != null) {

            dialog
                    .getWindow()
                    .setBackgroundDrawableResource(
                            android.R.color.transparent
                    );

            int width =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels
                                    * 0.90
                    );

            dialog
                    .getWindow()
                    .setLayout(
                            width,
                            android.view.ViewGroup
                                    .LayoutParams
                                    .WRAP_CONTENT
                    );
        }
    }

    // =========================================================================
    // MAIN
    // =========================================================================
    private void goToMainActivity() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================================
    // LOGIN LOADING
    // =========================================================================
    private void setLoginLoading(
            boolean loading
    ) {

        btnLogin.setEnabled(
                !loading
        );

        btnGoogleLogin.setEnabled(
                !loading
        );

        if (loading) {

            btnLogin.setText(
                    "Đang đăng nhập..."
            );

        } else {

            btnLogin.setText(
                    "Đăng nhập"
            );
        }
    }
}