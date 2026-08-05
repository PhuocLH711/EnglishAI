package adu.nttu.englishai.fragments;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.google.firebase.firestore.FirebaseFirestore;

import adu.nttu.englishai.R;

// =========================================================================
// HOME FRAGMENT: Màn hình Trang chủ & Bản đồ chọn Ải thử thách
// =========================================================================
public class HomeFragment extends Fragment {

    public HomeFragment() {}

    // =========================================================================
    // HÀM TẠO GIAO DIỆN
    // =========================================================================
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // =========================================================================
        // 1. ĐỒNG BỘ REAL-TIME: TÊN, ĐIỂM XP VÀ CHUỖI LỬA
        // =========================================================================
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        TextView tvHomeStreak = view.findViewById(R.id.tvHomeStreak);
        TextView tvHomeXP = view.findViewById(R.id.tvHomeXP);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            SharedPreferences sharedPref = requireActivity().getSharedPreferences("EnglishAI_Prefs", Context.MODE_PRIVATE);
            String cachedName = sharedPref.getString("USER_REAL_NAME", null);

            if (cachedName != null && !cachedName.isEmpty()) {
                tvGreeting.setText("Chào " + cachedName + "!");
            } else {
                String email = currentUser.getEmail();
                String fallbackName = (email != null && email.contains("@")) ? email.substring(0, email.indexOf("@")) : "bạn";
                tvGreeting.setText("Chào " + fallbackName + "!");
            }

            // 👉 SỬ DỤNG SnapshotListener ĐỂ NHẢY SỐ NGAY LẬP TỨC KHI VỪA THOÁT GAME
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .addSnapshotListener((document, error) -> {
                        if (!isAdded() || error != null || document == null || !document.exists()) return;

                        // 1. Đồng bộ Tên
                        String realName = document.getString("name");
                        if (realName == null || realName.trim().isEmpty()) {
                            realName = document.getString("fullName");
                        }
                        if (realName != null && !realName.trim().isEmpty()) {
                            String finalName = realName.trim();
                            if (!finalName.equals(cachedName)) {
                                tvGreeting.setText("Chào " + finalName + "!");
                                sharedPref.edit().putString("USER_REAL_NAME", finalName).apply();
                            }
                        }

                        // 2. Đồng bộ Điểm XP và Chuỗi lửa (Nhảy số tức thì)
                        Long scoreObj = document.getLong("score");
                        long realScore = (scoreObj != null) ? scoreObj : 0;
                        if (tvHomeXP != null) tvHomeXP.setText("💎 " + realScore);

                        Long streakObj = document.getLong("streak");
                        long realStreak = (streakObj != null) ? streakObj : 0;
                        if (tvHomeStreak != null) tvHomeStreak.setText("🔥 " + realStreak + " ngày");
                    });
        }

        // =========================================================================
        // 2. ÁNH XẠ CÁC ẢI THỬ THÁCH
        // =========================================================================
        Button btnStartDaily = view.findViewById(R.id.btnStartDaily);
        MaterialCardView cardStage1 = view.findViewById(R.id.cardStage1);
        MaterialCardView cardStage2 = view.findViewById(R.id.cardStage2);
        MaterialCardView cardStage3 = view.findViewById(R.id.cardStage3);
        MaterialCardView cardStage4 = view.findViewById(R.id.cardStage4);
        MaterialCardView cardStageChest = view.findViewById(R.id.cardStageChest);

        if (btnStartDaily != null) {
            btnStartDaily.setOnClickListener(v -> openStageQuiz("Easy", "Ải 1: Khởi Động"));
        }

        if (cardStage1 != null) {
            cardStage1.setOnClickListener(v -> openStageQuiz("Easy", "Ải 1: Khởi Động"));
        }

        if (cardStage2 != null) {
            cardStage2.setOnClickListener(v -> openStageQuiz("Medium", "Ải 2: Tăng Tốc"));
        }

        if (cardStage3 != null) {
            cardStage3.setOnClickListener(v -> openStageQuiz("Hard", "Ải 3: Bứt Phá"));
        }

        if (cardStage4 != null) {
            cardStage4.setOnClickListener(v -> openStageQuiz("Boss", "Ải 4: Trùm Cuối 👑"));
        }

        if (cardStageChest != null) {
            cardStageChest.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "🎁 Hãy vượt qua toàn bộ 4 Ải để mở khóa Rương Báu nhé!", Toast.LENGTH_LONG).show();
            });
        }

        return view;
    }

    // =========================================================================
    // HÀM ĐIỀU HƯỚNG & TRUYỀN THAM SỐ ĐỘ KHÓ
    // =========================================================================
    private void openStageQuiz(String difficulty, String stageName) {
        StageMissionFragment missionFragment = new StageMissionFragment();

        Bundle bundle = new Bundle();
        bundle.putString("DIFFICULTY_LEVEL", difficulty);
        bundle.putString("STAGE_NAME", stageName);
        missionFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, missionFragment)
                .addToBackStack(null)
                .commit();

        Toast.makeText(requireContext(), "Chào mừng vào " + stageName, Toast.LENGTH_SHORT).show();
    }
}