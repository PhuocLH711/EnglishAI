package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import adu.nttu.englishai.R;

public class SplashActivity extends AppCompatActivity {

    // Tổng thời gian Splash
    private static final long SPLASH_DELAY = 1800;

    private FirebaseAuth firebaseAuth;

    private View splashContent;
    private View progressSplash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // =========================================================
        // FIREBASE AUTH
        // =========================================================
        firebaseAuth = FirebaseAuth.getInstance();

        // =========================================================
        // ÁNH XẠ VIEW
        // =========================================================
        splashContent = findViewById(R.id.splashContent);
        progressSplash = findViewById(R.id.progressSplash);

        // =========================================================
        // HIỆU ỨNG KHI MỞ APP
        // =========================================================
        playEnterAnimation();

        // =========================================================
        // CHỜ SPLASH CHẠY XONG
        // =========================================================
        new Handler(Looper.getMainLooper()).postDelayed(
                this::checkLoginStatus,
                SPLASH_DELAY
        );
    }

    // =============================================================
    // HIỆU ỨNG LOGO XUẤT HIỆN
    // =============================================================
    private void playEnterAnimation() {

        if (splashContent == null) {
            return;
        }

        // Trạng thái ban đầu
        splashContent.setAlpha(0f);
        splashContent.setScaleX(0.88f);
        splashContent.setScaleY(0.88f);
        splashContent.setTranslationY(30f);

        // Logo xuất hiện
        splashContent.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(650)
                .start();

        // Thanh loading xuất hiện sau logo một chút
        if (progressSplash != null) {

            progressSplash.setAlpha(0f);

            progressSplash.animate()
                    .alpha(1f)
                    .setStartDelay(300)
                    .setDuration(400)
                    .start();
        }
    }

    // =============================================================
    // KIỂM TRA TRẠNG THÁI ĐĂNG NHẬP
    // =============================================================
    private void checkLoginStatus() {

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        final Intent intent;

        if (currentUser != null) {

            // Đã đăng nhập
            intent = new Intent(
                    SplashActivity.this,
                    MainActivity.class
            );

        } else {

            // Chưa đăng nhập
            intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );
        }

        // Chạy animation kết thúc trước
        playExitAnimation(() -> {

            startActivity(intent);

            // Fade sang màn hình tiếp theo
            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );

            // Không cho quay ngược về Splash
            finish();
        });
    }

    // =============================================================
    // HIỆU ỨNG KHI SPLASH KẾT THÚC
    // =============================================================
    private void playExitAnimation(Runnable onFinished) {

        if (splashContent == null) {

            onFinished.run();
            return;
        }

        // ---------------------------------------------------------
        // THANH LOADING BIẾN MẤT
        // ---------------------------------------------------------
        if (progressSplash != null) {

            progressSplash.animate()
                    .alpha(0f)
                    .translationY(8f)
                    .setDuration(180)
                    .start();
        }

        // ---------------------------------------------------------
        // LOGO PHÓNG NHẸ + ĐI LÊN + MỜ DẦN
        // ---------------------------------------------------------
        splashContent.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .translationY(-20f)
                .alpha(0f)
                .setDuration(420)
                .withEndAction(onFinished)
                .start();
    }

    // =============================================================
    // DỌN ANIMATION KHI ACTIVITY BỊ HỦY
    // =============================================================
    @Override
    protected void onDestroy() {

        if (splashContent != null) {
            splashContent.animate().cancel();
        }

        if (progressSplash != null) {
            progressSplash.animate().cancel();
        }

        super.onDestroy();
    }
}