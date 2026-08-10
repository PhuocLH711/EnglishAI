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

public class HomeFragment extends Fragment {

    private MaterialCardView cardStage1, cardStage2, cardStage3, cardStage4, cardStageChest;
    private Button btnStartDaily;
    private TextView tvBannerDescription;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Ánh xạ thông tin User
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        TextView tvHomeStreak = view.findViewById(R.id.tvHomeStreak);
        TextView tvHomeXP = view.findViewById(R.id.tvHomeXP);

        // 2. Ánh xạ các nút và thẻ Ải
        btnStartDaily = view.findViewById(R.id.btnStartDaily);
        tvBannerDescription = view.findViewById(R.id.tvBannerDescription); // Đã bắt được dòng chữ cần đổi!
        cardStage1 = view.findViewById(R.id.cardStage1);
        cardStage2 = view.findViewById(R.id.cardStage2);
        cardStage3 = view.findViewById(R.id.cardStage3);
        cardStage4 = view.findViewById(R.id.cardStage4);
        cardStageChest = view.findViewById(R.id.cardStageChest);

        // 3. Tải dữ liệu người dùng từ Firebase
        loadUserData(tvGreeting, tvHomeStreak, tvHomeXP);

        return view;
    }

    private void loadUserData(TextView tvGreeting, TextView tvHomeStreak, TextView tvHomeXP) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        SharedPreferences sharedPref = requireActivity().getSharedPreferences("EnglishAI_Prefs", Context.MODE_PRIVATE);
        String cachedName = sharedPref.getString("USER_REAL_NAME", null);

        // Set tạm tên cũ cho nhanh, tránh giật lag chờ mạng
        if (cachedName != null && !cachedName.isEmpty()) {
            tvGreeting.setText("Chào " + cachedName + "! 👋");
        } else {
            String email = currentUser.getEmail();
            String fallbackName = (email != null && email.contains("@")) ? email.substring(0, email.indexOf("@")) : "bạn";
            tvGreeting.setText("Chào " + fallbackName + "! 👋");
        }

        // Lắng nghe dữ liệu Realtime từ Firebase
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((document, error) -> {
                    if (!isAdded() || error != null || document == null || !document.exists()) return;

                    // Lấy tên
                    String realName = document.getString("name");
                    if (realName == null || realName.trim().isEmpty()) {
                        realName = document.getString("fullName");
                    }
                    if (realName != null && !realName.trim().isEmpty()) {
                        String finalName = realName.trim();
                        if (!finalName.equals(cachedName)) {
                            tvGreeting.setText("Chào " + finalName + "! 👋");
                            sharedPref.edit().putString("USER_REAL_NAME", finalName).apply();
                        }
                    }

                    // Lấy Điểm XP và Chuỗi ngày
                    Long scoreObj = document.getLong("score");
                    long realScore = (scoreObj != null) ? scoreObj : 0;
                    if (tvHomeXP != null) tvHomeXP.setText("💎 " + realScore);

                    Long streakObj = document.getLong("streak");
                    long realStreak = (streakObj != null) ? streakObj : 0;
                    if (tvHomeStreak != null) tvHomeStreak.setText("🔥 " + realStreak + " ngày");
                });
    }

    // =====================================================================
    // KIỂM TRA & CẬP NHẬT ẢI (CHẠY MỖI KHI VÀO LẠI TRANG CHỦ)
    // =====================================================================
    @Override
    public void onResume() {
        super.onResume();
        refreshStageLocks();
    }

    private void refreshStageLocks() {
        // Lấy Ải hiện tại (Mặc định là 1 nếu chưa có)
        int unlockedStage = requireActivity().getSharedPreferences("EnglishAI_Prefs", Context.MODE_PRIVATE)
                .getInt("UNLOCKED_STAGE", 1);

        // 1. Tự động đổi chữ Banner: Đang ở Ải nào thì chữ hiện Ải đó (Tối đa Ải 4)
        if (tvBannerDescription != null) {
            int displayStage = Math.min(unlockedStage, 4);
            tvBannerDescription.setText("Vượt qua Ải " + displayStage + " để giữ vững chuỗi ngày học chăm chỉ nhé!");
        }

        // 2. Nút "VƯỢT ẢI NGAY" tự động trỏ tới Ải cao nhất
        if (btnStartDaily != null) {
            btnStartDaily.setOnClickListener(v -> {
                if (unlockedStage == 1) openStageQuiz("Easy", "Ải 1: Khởi Động");
                else if (unlockedStage == 2) openStageQuiz("Medium", "Ải 2: Tăng Tốc");
                else if (unlockedStage == 3) openStageQuiz("Hard", "Ải 3: Bứt Phá");
                else openStageQuiz("Boss", "Ải 4: Trùm Cuối 👑");
            });
        }

        // 3. Khóa/Mở giao diện các thẻ Ải bên dưới bản đồ
        setupStage(cardStage1, 1, unlockedStage, "Easy", "Ải 1: Khởi Động");
        setupStage(cardStage2, 2, unlockedStage, "Medium", "Ải 2: Tăng Tốc");
        setupStage(cardStage3, 3, unlockedStage, "Hard", "Ải 3: Bứt Phá");
        setupStage(cardStage4, 4, unlockedStage, "Boss", "Ải 4: Trùm Cuối 👑");

        // 4. Xử lý phần thưởng Rương báu
        if (cardStageChest != null) {
            if (unlockedStage >= 5) {
                cardStageChest.setAlpha(1.0f);
                cardStageChest.setOnClickListener(v -> Toast.makeText(requireContext(), "🎁 Chúc mừng! Bạn đã phá đảo toàn bộ chiến dịch!", Toast.LENGTH_LONG).show());
            } else {
                cardStageChest.setAlpha(0.4f);
                cardStageChest.setOnClickListener(v -> Toast.makeText(requireContext(), "🔒 Đánh bại Trùm Cuối Ải 4 để mở rương!", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void setupStage(MaterialCardView card, int stageNumber, int unlockedStage, String difficulty, String name) {
        if (card == null) return;

        if (stageNumber <= unlockedStage) {
            card.setAlpha(1.0f); // Mở khóa: Sáng rõ
            card.setOnClickListener(v -> openStageQuiz(difficulty, name));
        } else {
            card.setAlpha(0.4f); // Khóa: Mờ đi
            card.setOnClickListener(v -> Toast.makeText(requireContext(), "🔒 Bạn phải hoàn thành Ải " + (stageNumber - 1) + " trước!", Toast.LENGTH_SHORT).show());
        }
    }

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
    }
}