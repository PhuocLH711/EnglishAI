package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

public class ProgressFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvTotalWords;
    private TextView tvLearnedCount, tvLearningCount, tvNotStartedCount;
    private TextView tvFoodProgress, tvAnimalProgress, tvSchoolProgress, tvTechProgress;
    private ProgressBar progressFood, progressAnimal, progressSchool, progressTech;
    private Button btnLogoutProfile;

    private List<Vocabulary> vocabularyList;

    public ProgressFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        // 1. Ánh xạ giao diện
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvTotalWords = view.findViewById(R.id.tvTotalWords);

        tvLearnedCount = view.findViewById(R.id.tvLearnedCount);
        tvLearningCount = view.findViewById(R.id.tvLearningCount);
        tvNotStartedCount = view.findViewById(R.id.tvNotStartedCount);

        btnLogoutProfile = view.findViewById(R.id.btnLogoutProfile);

        vocabularyList = DataRepository.getInstance().getVocabularyList();

        // 2. Load thông tin tài khoản
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            tvProfileEmail.setText(email != null ? email : "Đang hoạt động");
            String name = (email != null && email.contains("@")) ? email.substring(0, email.indexOf("@")) : "Học viên";
            tvProfileName.setText(name.toUpperCase());

            // Tải tiến độ thực từ Firebase
            loadRealtimeProgress(user.getUid());
        } else {
            tvProfileName.setText("HỌC VIÊN ENGLISH AI");
            tvProfileEmail.setText("Chưa đăng nhập");
            calculateAndDisplayStats();
        }

        // 3. Xử lý nút Đăng xuất
        if (btnLogoutProfile != null) {
            btnLogoutProfile.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(requireContext(), "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show();
                requireActivity().finish();
            });
        }

        return view;
    }

    // Tải trạng thái học tập từ Firebase để thống kê chính xác tuyệt đối
    private void loadRealtimeProgress(String userId) {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .collection("wordProgress")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Reset trạng thái mặc định
                    for (Vocabulary v : vocabularyList) {
                        v.setLearningStatus("NOT_STARTED");
                    }
                    // Cập nhật trạng thái thật từ database
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String wordId = doc.getId();
                        String status = doc.getString("status");
                        for (Vocabulary v : vocabularyList) {
                            if (v.getId().equals(wordId)) {
                                v.setLearningStatus(status != null ? status : "NOT_STARTED");
                                break;
                            }
                        }
                    }
                    calculateAndDisplayStats();
                })
                .addOnFailureListener(e -> calculateAndDisplayStats());
    }

    // Thuật toán đếm và phân loại tiến độ siêu cụ thể
    private void calculateAndDisplayStats() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        int total = vocabularyList.size();
        tvTotalWords.setText(total + " từ");

        int learned = 0, learning = 0, notStarted = 0;
        int foodTotal = 0, foodLearned = 0;
        int animalTotal = 0, animalLearned = 0;
        int schoolTotal = 0, schoolLearned = 0;
        int techTotal = 0, techLearned = 0;

        for (Vocabulary v : vocabularyList) {
            String status = v.getLearningStatus() != null ? v.getLearningStatus() : "NOT_STARTED";
            boolean isLearned = "LEARNED".equals(status);

            // Đếm trạng thái chung
            if (isLearned) learned++;
            else if ("LEARNING".equals(status)) learning++;
            else notStarted++;

            // Phân loại theo Chủ đề (Category)
            String category = v.getCategory() != null ? v.getCategory() : "";
            if (category.equalsIgnoreCase("Food")) {
                foodTotal++;
                if (isLearned) foodLearned++;
            } else if (category.equalsIgnoreCase("Animals")) {
                animalTotal++;
                if (isLearned) animalLearned++;
            } else if (category.equalsIgnoreCase("School")) {
                schoolTotal++;
                if (isLearned) schoolLearned++;
            } else {
                // Các chủ đề còn lại (Tech, Travel, Adjectives, Places...)
                techTotal++;
                if (isLearned) techLearned++;
            }
        }

        // Cập nhật số liệu Trạng thái lên UI
        tvLearnedCount.setText(String.valueOf(learned));
        tvLearningCount.setText(String.valueOf(learning));
        tvNotStartedCount.setText(String.valueOf(notStarted));

        // Cập nhật thanh tiến độ từng Chủ đề
        updateProgressBar(progressFood, tvFoodProgress, foodLearned, foodTotal);
        updateProgressBar(progressAnimal, tvAnimalProgress, animalLearned, animalTotal);
        updateProgressBar(progressSchool, tvSchoolProgress, schoolLearned, schoolTotal);
        updateProgressBar(progressTech, tvTechProgress, techLearned, techTotal);
    }

    private void updateProgressBar(ProgressBar bar, TextView label, int learned, int total) {
        int percent = total > 0 ? (int) (((float) learned / total) * 100) : 0;
        if (bar != null) bar.setProgress(percent);
        if (label != null) label.setText(percent + "% (" + learned + "/" + total + ")");
    }
}