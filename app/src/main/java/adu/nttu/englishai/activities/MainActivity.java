package adu.nttu.englishai.activities;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import adu.nttu.englishai.R;
import adu.nttu.englishai.fragments.GameFragment;
import adu.nttu.englishai.fragments.HomeFragment;
import adu.nttu.englishai.fragments.ProfileFragment;
import adu.nttu.englishai.fragments.ToeicFragment;
import adu.nttu.englishai.fragments.VocabularyFragment;
import adu.nttu.englishai.notifications.RandomLearningReminderWorker;
import adu.nttu.englishai.notifications.RandomReminderScheduler;

public class MainActivity extends AppCompatActivity {





    // =========================================================
    // NOTIFICATION PERMISSION
    // =========================================================
    private ActivityResultLauncher<String>
            notificationPermissionLauncher;

    // =========================================================
    // FIRESTORE
    // =========================================================
    private FirebaseFirestore db;




    // =========================================================
    // LIFECYCLE
    // =========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

// =====================================================
// KIỂM TRA TÀI KHOẢN ĐANG ĐĂNG NHẬP
// =====================================================
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {

            Log.d("ADMIN_CHECK",
                    "UID = " + currentUser.getUid());

            Log.d("ADMIN_CHECK",
                    "EMAIL = " + currentUser.getEmail());

        } else {

            Log.d("ADMIN_CHECK",
                    "CHUA DANG NHAP");
        }

// =====================================================
// 1. FIRESTORE
// =====================================================
        db = FirebaseFirestore.getInstance();

// =====================================================
// 2. ĐĂNG KÝ CALLBACK XIN QUYỀN NOTIFICATION
// =====================================================
        notificationPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {

                            if (isGranted) {

                                RandomReminderScheduler
                                        .scheduleNextReminder(
                                                MainActivity.this
                                        );

                                testNotificationNow();
                            }
                        }
                );

        // =====================================================
        // 2. CÀI ĐẶT THÔNG BÁO RANDOM
        // =====================================================
        setupRandomNotification();

        // =====================================================
        // 3. TEST NOTIFICATION NGAY
        // Chỉ dùng tạm để kiểm tra
        // =====================================================
        testNotificationNow();

        // =====================================================
        // 4. FIRESTORE
        // =====================================================
        db =
                FirebaseFirestore.getInstance();

        // =====================================================
        // 5. AI TUTOR FLOATING BUTTON
        // =====================================================
        setupDraggableAiTutor();

        // =====================================================
        // 6. BOTTOM NAVIGATION
        // =====================================================
        BottomNavigationView bottomNav =
                findViewById(
                        R.id.bottom_navigation
                );

        bottomNav.setOnItemSelectedListener(item -> {

            int itemId =
                    item.getItemId();

            // =================================================
            // HOME
            // =================================================
            if (itemId == R.id.nav_home) {

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new HomeFragment()
                        )
                        .commit();

                return true;
            }

            // =================================================
            // VOCABULARY
            // =================================================
            else if (itemId == R.id.nav_vocabulary) {

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new VocabularyFragment()
                        )
                        .commit();

                return true;
            }

            // =================================================
            // TOEIC
            // =================================================
            else if (itemId == R.id.nav_toeic) {

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new ToeicFragment()
                        )
                        .commit();

                return true;
            }

            // =================================================
            // GAME
            // =================================================
            else if (itemId == R.id.nav_game) {

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new GameFragment()
                        )
                        .commit();

                return true;
            }

            // =================================================
            // PROFILE / PROGRESS
            // =================================================
            else if (itemId == R.id.nav_progress) {

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new ProfileFragment()
                        )
                        .commit();

                return true;
            }

            return false;
        });

        // =====================================================
        // 7. MỞ APP → TRANG HOME
        // =====================================================
        if (savedInstanceState == null) {

            bottomNav.setSelectedItemId(
                    R.id.nav_home
            );
        }
    }

    // =========================================================================
    // CÀI ĐẶT HỆ THỐNG THÔNG BÁO RANDOM
    // =========================================================================
    private void setupRandomNotification() {

        // =========================================================
        // ANDROID 13+
        // Cần quyền POST_NOTIFICATIONS
        // =========================================================
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {

                // Đã có quyền
                // → tạo lịch random 1 / 2 / 3 / 5 ngày
                RandomReminderScheduler
                        .scheduleNextReminder(
                                this
                        );

            } else {

                // Chưa có quyền
                // → hỏi người dùng
                notificationPermissionLauncher
                        .launch(
                                Manifest.permission.POST_NOTIFICATIONS
                        );
            }

        } else {

            // Android < 13 không cần runtime permission
            RandomReminderScheduler
                    .scheduleNextReminder(
                            this
                    );
        }
    }

    // =========================================================================
    // TEST NOTIFICATION NGAY
    // =========================================================================
    private void testNotificationNow() {

        // Android 13+
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        OneTimeWorkRequest testNotificationRequest =
                new OneTimeWorkRequest.Builder(
                        RandomLearningReminderWorker.class
                )
                        .build();

        WorkManager
                .getInstance(this)
                .enqueue(
                        testNotificationRequest
                );
    }

    // =========================================================================
    // AI TUTOR - KÉO THẢ
    // =========================================================================
    private void setupDraggableAiTutor() {

        View layoutAiTutor =
                findViewById(
                        R.id.layoutAiTutor
                );

        if (layoutAiTutor == null) {
            return;
        }

        layoutAiTutor.setOnTouchListener(
                new View.OnTouchListener() {

                    private float dX;
                    private float dY;

                    private float startX;
                    private float startY;

                    private static final int CLICK_THRESHOLD =
                            15;

                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {

                        switch (event.getActionMasked()) {

                            // =================================================
                            // TOUCH DOWN
                            // =================================================
                            case MotionEvent.ACTION_DOWN:

                                dX =
                                        view.getX()
                                                - event.getRawX();

                                dY =
                                        view.getY()
                                                - event.getRawY();

                                startX =
                                        event.getRawX();

                                startY =
                                        event.getRawY();

                                return true;

                            // =================================================
                            // MOVE
                            // =================================================
                            case MotionEvent.ACTION_MOVE:

                                float newX =
                                        event.getRawX()
                                                + dX;

                                float newY =
                                        event.getRawY()
                                                + dY;

                                View parent =
                                        (View) view.getParent();

                                if (parent != null) {

                                    int parentWidth =
                                            parent.getWidth();

                                    int parentHeight =
                                            parent.getHeight();

                                    int bottomMargin =
                                            220;

                                    int topMargin =
                                            50;

                                    newX =
                                            Math.max(
                                                    0,
                                                    Math.min(
                                                            newX,
                                                            parentWidth
                                                                    - view.getWidth()
                                                    )
                                            );

                                    newY =
                                            Math.max(
                                                    topMargin,
                                                    Math.min(
                                                            newY,
                                                            parentHeight
                                                                    - view.getHeight()
                                                                    - bottomMargin
                                                    )
                                            );
                                }

                                view.setX(
                                        newX
                                );

                                view.setY(
                                        newY
                                );

                                return true;

                            // =================================================
                            // TOUCH UP
                            // =================================================
                            case MotionEvent.ACTION_UP:

                                float endX =
                                        event.getRawX();

                                float endY =
                                        event.getRawY();

                                float distance =
                                        (float) Math.hypot(
                                                endX - startX,
                                                endY - startY
                                        );

                                // =================================================
                                // CLICK
                                // =================================================
                                if (distance < CLICK_THRESHOLD) {

                                    view.performClick();

                                }

                                // =================================================
                                // DRAG → HÚT VỀ MÉP
                                // =================================================
                                else {

                                    View parentView =
                                            (View) view.getParent();

                                    if (parentView != null) {

                                        int parentWidth =
                                                parentView.getWidth();

                                        float center =
                                                parentWidth / 2f;

                                        float targetX =
                                                (
                                                        view.getX()
                                                                + view.getWidth() / 2f
                                                                < center
                                                )
                                                        ? 16f
                                                        : (
                                                        parentWidth
                                                        - view.getWidth()
                                                        - 16f
                                                );

                                        view.animate()
                                                .x(targetX)
                                                .setDuration(250)
                                                .start();
                                    }
                                }

                                return true;

                            default:

                                return false;
                        }
                    }
                }
        );

        // =========================================================
        // CLICK AI TUTOR
        // =========================================================
        layoutAiTutor.setOnClickListener(view -> {

            view.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(120)
                    .withEndAction(() -> {

                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .withEndAction(() -> {

                                    Intent intent =
                                            new Intent(
                                                    MainActivity.this,
                                                    AiTutorActivity.class
                                            );

                                    startActivity(
                                            intent
                                    );
                                })
                                .start();
                    })
                    .start();
        });
    }

    // =========================================================================
    // HIỆU ỨNG FLOATING
    // =========================================================================
    private void startIdleAnimation(
            ImageView imageView
    ) {

        ObjectAnimator animator =
                ObjectAnimator.ofFloat(
                        imageView,
                        "translationY",
                        0f,
                        -12f,
                        0f
                );

        animator.setDuration(
                1800
        );

        animator.setRepeatCount(
                ValueAnimator.INFINITE
        );

        animator.setRepeatMode(
                ValueAnimator.RESTART
        );

        animator.start();
    }
}