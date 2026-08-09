package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import adu.nttu.englishai.R;

// =========================================================================
// STAGE MISSION FRAGMENT: Màn hình Chờ chuẩn bị vượt Ải
// =========================================================================
public class StageMissionFragment extends Fragment {

    private String difficultyLevel = "Easy";
    private String stageName = "Ải 1: Khởi Động";

    public StageMissionFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stage_mission, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Nhận dữ liệu từ Bản Đồ
        if (getArguments() != null) {
            difficultyLevel = getArguments().getString("DIFFICULTY_LEVEL", "Easy");
            stageName = getArguments().getString("STAGE_NAME", "Ải 1: Khởi Động");
        }

        // 2. Gắn thông tin lên UI
        View btnBackToHome = view.findViewById(R.id.btnBackToStage);
        if (btnBackToHome != null) btnBackToHome.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        TextView tvStageTitle = view.findViewById(R.id.tvStageTitle);
        if (tvStageTitle != null) tvStageTitle.setText(stageName);

        TextView tvStageDifficulty = view.findViewById(R.id.tvStageDifficulty);
        if (tvStageDifficulty != null) tvStageDifficulty.setText("🔥 Cấp độ: " + getDifficultyLabel(difficultyLevel));

        // 3. Sự kiện bấm nút BẮT ĐẦU CHIẾN ĐẤU
        View btnStartBattle = view.findViewById(R.id.btnStartBattle);
        if (btnStartBattle != null) {
            btnStartBattle.setOnClickListener(v -> {
                // TẠM THỜI hiện thông báo. Bước tiếp theo chúng ta sẽ tạo StageGameplayFragment
                // để nhảy vào đấu trường liên hoàn ở đây.
                Toast.makeText(getContext(), "Đang tải Đấu Trường Liên Hoàn...", Toast.LENGTH_SHORT).show();

                // Ném vali chứa Độ khó vào Đấu Trường
                StageGameplayFragment gameplayFragment = new StageGameplayFragment();
                Bundle bundle = new Bundle();
                bundle.putString("DIFFICULTY_LEVEL", difficultyLevel);
                gameplayFragment.setArguments(bundle);

                // Kích hoạt chuyển vào Đấu Trường
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, gameplayFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
    }

    private String getDifficultyLabel(String level) {
        switch (level) {
            case "Medium": return "Vừa (Medium)";
            case "Hard": return "Khó (Hard)";
            case "Boss": return "Siêu Khó (Boss)";
            default: return "Dễ (Easy)";
        }
    }
}