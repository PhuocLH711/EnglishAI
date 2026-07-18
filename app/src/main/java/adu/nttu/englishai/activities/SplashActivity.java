package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import adu.nttu.englishai.R;

// =========================================================================
// SPLASH ACTIVITY: Màn hình chào khởi động & Tự động kiểm tra phiên đăng nhập
// =========================================================================
public class SplashActivity extends AppCompatActivity {

    // Khoảng thời gian trễ của màn hình chào: 1500 miligiây (tương đương 1.5 giây)
    // Giúp màn hình logo hiện lên vừa đủ để người dùng nhận diện thương hiệu app
    private static final long SPLASH_DELAY = 1500;

    // Đối tượng quản lý xác thực tài khoản từ Firebase
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Khởi tạo bộ máy xác thực Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // =========================================================================
        // CƠ CHẾ ĐẾM NGƯỢC THỜI GIAN (TIMER HƠN 1.5 GIÂY)
        // =========================================================================
        // Looper.getMainLooper(): Chỉ định rõ ràng rằng hành động sau khi đếm ngược
        // phải được thực thi ngay trên Luồng chính (Main/UI Thread) để chuyển màn hình an toàn.
        // postDelayed: Lệnh đếm ngược đúng 1.5 giây (SPLASH_DELAY) rồi tự động gọi hàm checkLoginStatus()
        new Handler(Looper.getMainLooper()).postDelayed(
                this::checkLoginStatus,
                SPLASH_DELAY
        );
    }

    // HÀM QUAN TRỌNG: Kiểm tra trạng thái đăng nhập để phân luồng người dùng
    private void checkLoginStatus() {
        // getCurrentUser(): Lấy thông tin tài khoản đang đăng nhập hiện tại lưu trong bộ nhớ đệm (Cache token)
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        Intent intent;

        // =========================================================================
        // PHÂN LUỒNG LOGIC (AUTO-LOGIN)
        // =========================================================================
        if (currentUser != null) {
            // Trường hợp 1: Biến currentUser KHÁC null nghĩa là người dùng đã đăng nhập từ trước và chưa bấm Đăng xuất.
            // -> Kích hoạt tính năng Đăng nhập tự động (Auto-Login), đưa thẳng vào màn hình chính MainActivity
            intent = new Intent(
                    SplashActivity.this,
                    MainActivity.class
            );
        } else {
            // Trường hợp 2: Biến currentUser BẰNG null nghĩa là đây là lần đầu mở app hoặc đã đăng xuất trước đó.
            // -> Điều hướng người dùng sang màn hình Đăng nhập LoginActivity để xác thực
            intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );
        }

        // Kích hoạt chuyến xe Intent để chuyển sang màn hình tiếp theo
        startActivity(intent);

        // QUAN TRỌNG: Đóng và tiêu hủy hoàn toàn màn hình SplashActivity này khỏi bộ nhớ RAM (Backstack)
        // Đảm bảo khi người dùng ở màn hình trong mà bấm nút Trở về (Back) của điện thoại,
        // app sẽ thoát ra màn hình chính của điện thoại luôn chứ không bao giờ quay lại màn hình chào này nữa.
        finish();
    }
}