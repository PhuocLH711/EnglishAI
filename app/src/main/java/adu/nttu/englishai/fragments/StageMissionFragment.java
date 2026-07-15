package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import adu.nttu.englishai.R;

public class StageMissionFragment extends Fragment {

    private String stageName = "Ải Thử Thách";
    private String difficultyLevel = "Easy";

    public StageMissionFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stage_mission, container, false);

        // Nhận dữ liệu Tên ải & Độ khó từ Trang chủ
        if (getArguments() != null) {
            stageName = getArguments().getString("STAGE_NAME", "Ải Thử Thách");
            difficultyLevel = getArguments().getString("DIFFICULTY_LEVEL", "Easy");
        }

        // ================= 1. XỬ LÝ NÚT QUAY LẠI TRANG CHỦ =================
        View btnBackToHome = view.findViewById(R.id.btnBackToHome);
        if (btnBackToHome != null) {
            btnBackToHome.setOnClickListener(v -> {
                // Lệnh này giúp thoạt khỏi màn hình Ải và trượt ngược về đúng Bản đồ Trang chủ
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // ================= 2. HIỂN THỊ TIÊU ĐỀ & ĐỘ KHÓ =================
        TextView tvStageTitle = view.findViewById(R.id.tvStageTitle);
        TextView tvStageDifficulty = view.findViewById(R.id.tvStageDifficulty);

        if (tvStageTitle != null) tvStageTitle.setText(stageName);
        if (tvStageDifficulty != null) {
            tvStageDifficulty.setText("🔥 Cấp độ: " + difficultyLevel);
        }

        // ================= 3. ÁNH XẠ 4 NHIỆM VỤ =================
        MaterialCardView cardVocab = view.findViewById(R.id.cardMissionVocab);
        MaterialCardView cardSpeaking = view.findViewById(R.id.cardMissionSpeaking);
        MaterialCardView cardQuiz = view.findViewById(R.id.cardMissionQuiz);
        MaterialCardView cardMemory = view.findViewById(R.id.cardMissionMemory);

        // 1. Mở nhiệm vụ Từ vựng
        if (cardVocab != null) {
            cardVocab.setOnClickListener(v -> openSkillFragment(new VocabularyFragment(), "Từ vựng"));
        }

        // 2. Mở nhiệm vụ Luyện nói
        if (cardSpeaking != null) {
            cardSpeaking.setOnClickListener(v -> openSkillFragment(new SpeakingFragment(), "Luyện nói"));
        }

        // 3. Mở nhiệm vụ Trắc nghiệm Quiz
        if (cardQuiz != null) {
            cardQuiz.setOnClickListener(v -> openSkillFragment(new QuizFragment(), "Trắc nghiệm"));
        }

        // 4. Mở nhiệm vụ Lật thẻ Ghép cặp
        if (cardMemory != null) {
            cardMemory.setOnClickListener(v -> openSkillFragment(new MemoryMatchFragment(), "Siêu ghép cặp"));
        }

        return view;
    }

    private void openSkillFragment(Fragment fragment, String skillName) {
        Bundle bundle = new Bundle();
        bundle.putString("DIFFICULTY_LEVEL", difficultyLevel);
        bundle.putString("STAGE_NAME", stageName);
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        Toast.makeText(requireContext(), "Đang vào nhiệm vụ: " + skillName, Toast.LENGTH_SHORT).show();
    }
}