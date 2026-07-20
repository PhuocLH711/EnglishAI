package adu.nttu.englishai.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import adu.nttu.englishai.R;
import adu.nttu.englishai.fragments.GameFragment;
import adu.nttu.englishai.fragments.HomeFragment;
import adu.nttu.englishai.fragments.ProfileFragment;
import adu.nttu.englishai.fragments.VocabularyFragment;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Cấu hình bong bóng AI
        setupDraggableAiTutor();

        BottomNavigationView bottomNav =
                findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new HomeFragment()
                        )
                        .commit();

                return true;

            } else if (itemId == R.id.nav_vocabulary) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new VocabularyFragment()
                        )
                        .commit();

                return true;

            } else if (itemId == R.id.nav_game) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new GameFragment()
                        )
                        .commit();

                return true;

            } else if (itemId == R.id.nav_progress) {
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

        // Khi mở app lần đầu, hiển thị Trang chủ
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /**
     * Hàm cấu hình logic di chuyển kéo thả tự do cho nút AI Tutor
     * Tự động hút về mép trái hoặc phải gần nhất khi người dùng buông tay.
     */
    private void setupDraggableAiTutor() {
        View layoutAiTutor = findViewById(R.id.layoutAiTutor);
        if (layoutAiTutor == null) return;

        // Xử lý sự kiện vuốt chạm di chuyển (Touch Event)
        layoutAiTutor.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private float startX, startY;
            private static final int CLICK_THRESHOLD = 15; // Ngưỡng pixel để phân biệt Bấm nhẹ hay Kéo di chuyển

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // Ghi lại tọa độ ban đầu khi ngón tay vừa chạm vào màn hình
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Tính toán vị trí mới dựa trên đường di chuyển của ngón tay
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Ràng buộc không cho nút AI bị kéo văng ra ngoài mép màn hình
                        View parent = (View) view.getParent();
                        if (parent != null) {
                            int parentWidth = parent.getWidth();
                            int parentHeight = parent.getHeight();
                            int bottomMargin = 220; // Trừ hao chiều cao của Bottom Navigation
                            int topMargin = 50;     // Trừ hao thanh thông báo hệ thống phía trên

                            newX = Math.max(0, Math.min(newX, parentWidth - view.getWidth()));
                            newY = Math.max(topMargin, Math.min(newY, parentHeight - view.getHeight() - bottomMargin));
                        }

                        // Cập nhật vị trí tức thời cho nút AI
                        view.setX(newX);
                        view.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Khi người dùng nhấc ngón tay ra khỏi màn hình
                        float endX = event.getRawX();
                        float endY = event.getRawY();
                        float distance = (float) Math.hypot(endX - startX, endY - startY);

                        if (distance < CLICK_THRESHOLD) {
                            // Nếu khoảng cách di chuyển cực nhỏ -> Hiểu là hành động CLICK (Bấm nhẹ)
                            view.performClick();
                        } else {
                            // Nếu khoảng cách lớn -> Hiểu là hành động KÉO THẢ -> Tự động trượt mượt mà về mép rìa gần nhất
                            View parentView = (View) view.getParent();
                            if (parentView != null) {
                                int parentWidth = parentView.getWidth();
                                float center = parentWidth / 2f;

                                // Nếu tâm nút nằm lệch bên trái trục giữa thì hút về mép trái (cách 16dp), ngược lại hút về bên phải
                                float targetX = (view.getX() + view.getWidth() / 2f < center)
                                        ? 16f
                                        : (parentWidth - view.getWidth() - 16f);

                                // Thực hiện hiệu ứng trượt mượt mà trong 250 miligiây
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
        });

        // Xử lý hành động BẤM VÀO nút AI: Giữ nguyên hiệu ứng nhún nhảy phóng to thu nhỏ xịn sò của bạn!
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
                                    // Sau khi nhún nhảy xong thì chuyển sang màn hình cuộc hội thoại AI
                                    Intent intent = new Intent(
                                            MainActivity.this,
                                            AiTutorActivity.class
                                    );
                                    startActivity(intent);
                                })
                                .start();
                    })
                    .start();
        });
    }

    /**
     * Hàm tạo hiệu ứng nhún nhảy lơ lửng tại chỗ cho hình ảnh nhân vật AI (nếu dùng sau này)
     */
    private void startIdleAnimation(ImageView imageView) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                imageView,
                "translationY",
                0f,
                -12f,
                0f
        );

        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.start();
    }
}