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

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

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

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .addSnapshotListener((document, error) -> {
                        if (!isAdded() || error != null || document == null || !document.exists()) return;

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

                        Long scoreObj = document.getLong("score");
                        long realScore = (scoreObj != null) ? scoreObj : 0;
                        if (tvHomeXP != null) tvHomeXP.setText("💎 " + realScore);

                        Long streakObj = document.getLong("streak");
                        long realStreak = (streakObj != null) ? streakObj : 0;
                        if (tvHomeStreak != null) tvHomeStreak.setText("🔥 " + realStreak + " ngày");
                    });
        }

        btnStartDaily = view.findViewById(R.id.btnStartDaily);
        cardStage1 = view.findViewById(R.id.cardStage1);
        cardStage2 = view.findViewById(R.id.cardStage2);
        cardStage3 = view.findViewById(R.id.cardStage3);
        cardStage4 = view.findViewById(R.id.cardStage4);
        cardStageChest = view.findViewById(R.id.cardStageChest);

        return view;
    }

    // 👉 ĐƯA LOGIC KHÓA ẢI VÀO ONRESUME ĐỂ NÓ CẬP NHẬT NGAY LẬP TỨC KHI VỪA CHIẾN THẮNG TRỞ VỀ
    @Override
    public void onResume() {
        super.onResume();
        refreshStageLocks();
    }

    private void refreshStageLocks() {
        // Lấy tiến độ mở khóa (Nếu chưa chơi bao giờ thì mặc định mở Ải 1)
        int unlockedStage = requireActivity().getSharedPreferences("EnglishAI_Prefs", Context.MODE_PRIVATE)
                .getInt("UNLOCKED_STAGE", 1);

        setupStage(cardStage1, 1, unlockedStage, "Easy", "Ải 1: Khởi Động");
        setupStage(cardStage2, 2, unlockedStage, "Medium", "Ải 2: Tăng Tốc");
        setupStage(cardStage3, 3, unlockedStage, "Hard", "Ải 3: Bứt Phá");
        setupStage(cardStage4, 4, unlockedStage, "Boss", "Ải 4: Trùm Cuối 👑");

        // Nút Vượt Ải Ngay sẽ thông minh tự động trỏ vào Ải cao nhất bạn đã mở
        if (btnStartDaily != null) {
            btnStartDaily.setOnClickListener(v -> {
                if (unlockedStage == 1) openStageQuiz("Easy", "Ải 1: Khởi Động");
                else if (unlockedStage == 2) openStageQuiz("Medium", "Ải 2: Tăng Tốc");
                else if (unlockedStage == 3) openStageQuiz("Hard", "Ải 3: Bứt Phá");
                else openStageQuiz("Boss", "Ải 4: Trùm Cuối 👑");
            });
        }

        // Logic của Rương báu
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

    // Hàm phụ trợ Khóa/Mở Giao Diện
    private void setupStage(MaterialCardView card, int stageNumber, int unlockedStage, String difficulty, String name) {
        if (card == null) return;

        if (stageNumber <= unlockedStage) {
            // 👉 ĐÃ MỞ KHÓA: Sáng rực rỡ, cho phép ấn vào
            card.setAlpha(1.0f);
            card.setOnClickListener(v -> openStageQuiz(difficulty, name));
        } else {
            // 👉 ĐANG KHÓA: Làm mờ đi 60%, bấm vào sẽ báo lỗi
            card.setAlpha(0.4f);
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