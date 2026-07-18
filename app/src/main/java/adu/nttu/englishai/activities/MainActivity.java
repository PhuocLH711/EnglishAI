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
import adu.nttu.englishai.fragments.ProgressFragment;
import adu.nttu.englishai.fragments.VocabularyFragment;

// =========================================================================
// MAIN ACTIVITY: Màn hình chính chứa thanh điều hướng và nút Gia sư AI
// =========================================================================
public class MainActivity extends AppCompatActivity {

    // Biến kết nối cơ sở dữ liệu đám mây Firestore
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Khởi tạo đối tượng kết nối cơ sở dữ liệu Firestore
        db = FirebaseFirestore.getInstance();

        // 2. Kích hoạt tính năng kéo thả linh hoạt cho bong bóng AI Tutor (Gia sư AI)
        setupDraggableAiTutor();

        // =========================================================================
        // 3. XỬ LÝ THANH ĐIỀU HƯỚNG DƯỚI CÙNG (BOTTOM NAVIGATION)
        // =========================================================================
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Lắng nghe sự kiện người dùng bấm chọn vào từng tab trên thanh công cụ
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Dùng FragmentManager để thay thế (replace) vùng chứa fragment_container bằng Fragment tương ứng
            if (itemId == R.id.nav_home) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit(); // Lệnh commit() bắt buộc phải có để thực thi việc chuyển màn hình
                return true;

            } else if (itemId == R.id.nav_vocabulary) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new VocabularyFragment())
                        .commit();
                return true;

            } else if (itemId == R.id.nav_progress) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ProgressFragment())
                        .commit();
                return true;

            } else if (itemId == R.id.nav_game) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new GameFragment())
                        .commit();
                return true;
            }

            return false;
        });

        // =========================================================================
        // 4. KIỂM TRA VÀ HIỂN THỊ TAB MẶC ĐỊNH
        // =========================================================================
        // savedInstanceState == null: Nghĩa là app vừa được mở lên lần đầu tiên (chưa bị xoay màn hình hay khởi động lại)
        // -> Tự động chọn tab Trang chủ (Home) làm màn hình mặc định
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

        // =========================================================================
        // XỬ LÝ SỰ KIỆN CHẠM VÀ KÉO TRƯỢT TRÊN MÀN HÌNH (TOUCH EVENT)
        // =========================================================================
        layoutAiTutor.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;         // Khoảng cách chênh lệch giữa điểm chạm và gốc tọa độ của nút
            private float startX, startY; // Tọa độ lúc bắt đầu đặt ngón tay vào
            private static final int CLICK_THRESHOLD = 15; // Ngưỡng 15 pixel để phân biệt Bấm nhẹ hay Kéo di chuyển

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // 1. KHI NGÓN TAY VỪA CHẠM VÀO NÚT
                        // Ghi lại tọa độ ban đầu để tính toán độ dịch chuyển
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // 2. KHI NGÓN TAY DI CHUYỂN TRÊN MÀN HÌNH
                        // Tính toán tọa độ (X, Y) mới theo hướng kéo của ngón tay
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // RÀNG BUỘC TỌA ĐỘ (Boundary Clamping): Không cho nút AI bị kéo văng ra ngoài mép màn hình
                        View parent = (View) view.getParent();
                        if (parent != null) {
                            int parentWidth = parent.getWidth();
                            int parentHeight = parent.getHeight();
                            int bottomMargin = 220; // Trừ hao chiều cao của Bottom Navigation bên dưới
                            int topMargin = 50;     // Trừ hao thanh thông báo hệ thống (Status Bar) phía trên

                            // Math.max và Math.min giúp ép tọa độ luôn nằm trong khung an toàn từ 0 đến chiều rộng/cao tối đa
                            newX = Math.max(0, Math.min(newX, parentWidth - view.getWidth()));
                            newY = Math.max(topMargin, Math.min(newY, parentHeight - view.getHeight() - bottomMargin));
                        }

                        // Cập nhật vị trí tức thời cho nút AI di chuyển theo tay
                        view.setX(newX);
                        view.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // 3. KHI NGÓN TAY NHẤC RA KHỎI MÀN HÌNH (BUÔNG TAY)
                        float endX = event.getRawX();
                        float endY = event.getRawY();
                        // Dùng định lý Pytago (Math.hypot) để tính tổng quãng đường ngón tay đã di chuyển
                        float distance = (float) Math.hypot(endX - startX, endY - startY);

                        if (distance < CLICK_THRESHOLD) {
                            // Nếu di chuyển < 15 pixel -> Hiểu là hành động CLICK (Bấm chạm nhẹ)
                            view.performClick(); // Gọi lệnh click để kích hoạt sự kiện setOnClickListener bên dưới
                        } else {
                            // Nếu di chuyển >= 15 pixel -> Hiểu là hành động KÉO THẢ -> Tự động trượt hút về mép màn hình
                            View parentView = (View) view.getParent();
                            if (parentView != null) {
                                int parentWidth = parentView.getWidth();
                                float center = parentWidth / 2f; // Tìm đường trung tâm chia đôi màn hình

                                // Nếu tâm của nút nằm lệch nửa bên trái thì hút về mép trái (cách lề 16dp)
                                // Nếu nằm lệch nửa bên phải thì hút về mép phải
                                float targetX = (view.getX() + view.getWidth() / 2f < center)
                                        ? 16f
                                        : (parentWidth - view.getWidth() - 16f);

                                // Tạo hiệu ứng hoạt hình (Animation) trượt mượt mà về đích trong 250 miligiây
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

        // =========================================================================
        // XỬ LÝ SỰ KIỆN BẤM CHẠM (CLICK EVENT) VÀ CHUYỂN TRANG
        // =========================================================================
        layoutAiTutor.setOnClickListener(view -> {
            // Bước 1: Phóng to nút lên 115% trong 120 miligiây (Tạo cảm giác nhún nhảy phản hồi)
            view.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(120)
                    .withEndAction(() -> {
                        // Bước 2: Thu nhỏ về lại kích thước gốc 100%
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .withEndAction(() -> {
                                    // Bước 3: Sau khi hiệu ứng nhún nhảy kết thúc -> Chuyển sang màn hình Gia sư AI
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
     * Hàm tạo hiệu ứng hoạt hình lơ lửng tại chỗ cho hình ảnh nhân vật AI (Idle Animation)
     * Dùng ObjectAnimator để nhấp nhô lên xuống vô tận theo trục Y
     */
    private void startIdleAnimation(ImageView imageView) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                imageView,
                "translationY", // Dịch chuyển theo chiều dọc
                0f,             // Vị trí ban đầu
                -12f,           // Bay lên cao 12 pixel
                0f              // Hạ về vị trí cũ
        );

        animator.setDuration(1800);                       // Một vòng bay lên hạ xuống mất 1.8 giây
        animator.setRepeatCount(ValueAnimator.INFINITE);  // Lặp lại vô tận
        animator.setRepeatMode(ValueAnimator.RESTART);    // Hết vòng thì bắt đầu lại
        animator.start(); // Kích hoạt hiệu ứng
    }
}