package adu.nttu.englishai.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

// =========================================================================
// FIREBASE AUTH MANAGER
// Quản lý tập trung các chức năng xác thực của EnglishAI
// =========================================================================
public class FirebaseAuthManager {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // =========================================================================
    // ĐĂNG NHẬP EMAIL + PASSWORD
    // =========================================================================
    public Task<AuthResult> loginWithEmail(
            String email,
            String password
    ) {

        return firebaseAuth
                .signInWithEmailAndPassword(
                        email,
                        password
                );
    }

    // =========================================================================
    // ĐĂNG KÝ EMAIL + PASSWORD
    // =========================================================================
    public Task<AuthResult> registerWithEmail(
            String email,
            String password
    ) {

        return firebaseAuth
                .createUserWithEmailAndPassword(
                        email,
                        password
                );
    }

    // =========================================================================
    // QUÊN MẬT KHẨU
    // Firebase gửi link đặt lại mật khẩu về email
    // =========================================================================
    public Task<Void> sendPasswordResetEmail(
            String email
    ) {

        firebaseAuth.setLanguageCode("vi");

        return firebaseAuth
                .sendPasswordResetEmail(email);
    }

    // =========================================================================
    // GỬI EMAIL XÁC MINH
    // =========================================================================
    public Task<Void> sendVerificationEmail() {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {
            return null;
        }

        // Email Firebase ưu tiên tiếng Việt
        firebaseAuth.setLanguageCode("vi");

        return user.sendEmailVerification();
    }

    // =========================================================================
    // KIỂM TRA EMAIL ĐÃ XÁC MINH CHƯA
    // =========================================================================
    public boolean isEmailVerified() {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        return user != null
                && user.isEmailVerified();
    }

    // =========================================================================
    // RELOAD USER
    // Dùng khi cần cập nhật trạng thái xác minh email mới nhất từ Firebase
    // =========================================================================
    public Task<Void> reloadCurrentUser() {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {
            return null;
        }

        return user.reload();
    }

    // =========================================================================
    // GOOGLE ID TOKEN → FIREBASE
    // Sẽ sử dụng khi tích hợp Google Sign-In
    // =========================================================================
    public Task<AuthResult> loginWithGoogleToken(
            String idToken
    ) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null
                );

        return firebaseAuth
                .signInWithCredential(
                        credential
                );
    }

    // =========================================================================
    // LẤY USER HIỆN TẠI
    // =========================================================================
    public FirebaseUser getCurrentUser() {

        return firebaseAuth
                .getCurrentUser();
    }

    // =========================================================================
    // KIỂM TRA CÓ PHIÊN ĐĂNG NHẬP KHÔNG
    // =========================================================================
    public boolean isLoggedIn() {

        return firebaseAuth
                .getCurrentUser() != null;
    }

    // =========================================================================
    // ĐĂNG XUẤT
    // =========================================================================
    public void logout() {

        firebaseAuth.signOut();
    }
}