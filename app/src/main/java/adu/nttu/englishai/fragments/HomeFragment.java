package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import adu.nttu.englishai.R;

// =========================================================================
// HOME FRAGMENT: Màn hình Trang chủ chào đón & Bản đồ chọn Ải thử thách
// =========================================================================
public class HomeFragment extends Fragment {

    public HomeFragment() {}

    // =========================================================================
    // HÀM TẠO GIAO DIỆN VÀ GÁN SỰ KIỆN (ON CREATE VIEW)
    // =========================================================================
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Bơm (inflate) bản thiết kế XML fragment_home thành đối tượng View thực tế
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // =========================================================================
        // 1. HIỂN THỊ TÊN THẬT TỪ FIREBASE (SMART NAME RESOLUTION)
        // =========================================================================
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        // Lấy thông tin tài khoản đang đăng nhập hiện tại từ bộ nhớ đệm Firebase Auth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String finalName = "";
            /*
             * KỸ THUẬT PHÒNG VỆ VÀ THÂN THIỆN VỚI NGƯỜI DÙNG (Fallback Handling):
             * - Ưu tiên 1: Lấy Tên hiển thị (DisplayName) được lưu trong profile.
             * - Ưu tiên 2: Nếu người dùng đăng ký nhanh bằng Email mà chưa kịp cập nhật tên,
             *   hệ thống sẽ tự động cắt phần chuỗi nằm trước dấu "@" trong email (ví dụ: "nam123@gmail.com" -> "nam123").
             * - Sau đó, dùng substring và toUpperCase để tự động Viết Hoa Chữ Cái Đầu ("nam123" -> "Nam123"),
             *   giúp lời chào hiển thị lên giao diện luôn trang trọng, lịch sự và đẹp mắt.
             */
            if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                finalName = user.getDisplayName().trim();
            } else if (user.getEmail() != null) {
                String email = user.getEmail();
                String fallbackName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
                if (!fallbackName.isEmpty()) {
                    finalName = fallbackName.substring(0, 1).toUpperCase() + fallbackName.substring(1);
                }
            }
            // Đẩy lời chào lên thẻ TextView
            if (tvGreeting != null && !finalName.isEmpty()) {
                tvGreeting.setText("Chào " + finalName + "! 👋");
            }
        }

        // =========================================================================
        // 2. ÁNH XẠ CÁC ẢI THỬ THÁCH (STAGE CARDS MAPPING)
        // =========================================================================
        Button btnStartDaily = view.findViewById(R.id.btnStartDaily);
        MaterialCardView cardStage1 = view.findViewById(R.id.cardStage1);
        MaterialCardView cardStage2 = view.findViewById(R.id.cardStage2);
        MaterialCardView cardStage3 = view.findViewById(R.id.cardStage3);
        MaterialCardView cardStage4 = view.findViewById(R.id.cardStage4);
        MaterialCardView cardStageChest = view.findViewById(R.id.cardStageChest);

        // Nút Học Ngay hàng ngày -> Mặc định mở Ải 1 (Dễ)
        if (btnStartDaily != null) {
            btnStartDaily.setOnClickListener(v -> openStageQuiz("Easy", "Ải 1: Khởi Động"));
        }

        // Bấm Ải 1 -> Gửi độ khó "Easy" (Dễ)
        if (cardStage1 != null) {
            cardStage1.setOnClickListener(v -> openStageQuiz("Easy", "Ải 1: Khởi Động"));
        }

        // Bấm Ải 2 -> Gửi độ khó "Medium" (Vừa)
        if (cardStage2 != null) {
            cardStage2.setOnClickListener(v -> openStageQuiz("Medium", "Ải 2: Tăng Tốc"));
        }

        // Bấm Ải 3 -> Gửi độ khó "Hard" (Khó)
        if (cardStage3 != null) {
            cardStage3.setOnClickListener(v -> openStageQuiz("Hard", "Ải 3: Bứt Phá"));
        }

        // Bấm Ải 4 -> Gửi độ khó "Boss" (Siêu Khó - Trùm cuối)
        if (cardStage4 != null) {
            cardStage4.setOnClickListener(v -> openStageQuiz("Boss", "Ải 4: Trùm Cuối 👑"));
        }

        // Bấm Ải 5 -> Rương quà tặng (Khóa thử thách)
        if (cardStageChest != null) {
            cardStageChest.setOnClickListener(v -> {
                // Hiển thị thông báo hướng dẫn người dùng vượt qua 4 ải trước
                Toast.makeText(requireContext(), "🎁 Hãy vượt qua toàn bộ 4 Ải để mở khóa Rương Báu nhé!", Toast.LENGTH_LONG).show();
            });
        }

        return view;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG: ĐIỀU HƯỚNG & TRUYỀN THAM SỐ ĐỘ KHÓ (STAGE ROUTER)
    // =========================================================================
    /**
     * Hàm mở Trạm Nhiệm Vụ Ải (Đầy đủ 4 kỹ năng: Từ vựng, Nói, Quiz, Lật thẻ)
     */
    private void openStageQuiz(String difficulty, String stageName) {
        StageMissionFragment missionFragment = new StageMissionFragment();

        /*
         * KỸ THUẬT ĐÓNG GÓI DỮ LIỆU GIỮA CÁC FRAGMENT (BUNDLE PASSING):
         * - Không dùng biến static hay truyền qua hàm tạo (constructor),
         *   mà đóng gói dữ liệu vào một đối tượng Bundle bằng putString().
         * - setArguments(bundle): Gắn gói dữ liệu vào Fragment mới.
         *   Kỹ thuật này giúp hệ thống Android tự động bảo toàn tham số (Độ khó & Tên ải)
         *   kể cả khi ứng dụng bị thu hồi bộ nhớ hay người dùng xoay ngang màn hình.
         */
        Bundle bundle = new Bundle();
        bundle.putString("DIFFICULTY_LEVEL", difficulty); // "Easy", "Medium", "Hard", "Boss"
        bundle.putString("STAGE_NAME", stageName);
        missionFragment.setArguments(bundle);

        // Chuyển sang màn hình Trạm nhiệm vụ và lưu giao diện Trang chủ vào ngăn xếp bộ nhớ (Backstack)
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, missionFragment)
                .addToBackStack(null) // Cho phép bấm nút Back trên điện thoại quay lại bản đồ Trang chủ
                .commit();

        // Hiển thị Toast chào mừng ngắn gọn tạo hứng thú học tập
        Toast.makeText(requireContext(), "Chào mừng vào " + stageName, Toast.LENGTH_SHORT).show();
    }
}