package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import adu.nttu.englishai.R;

public class ProgressFragment extends Fragment {

    private static final int TOTAL_WORDS = 10;

    private TextView tvProgressPercent;
    private TextView tvProgressDetail;
    private TextView tvTotalWords;
    private TextView tvLearnedWords;
    private TextView tvFavoriteWords;
    private TextView tvNotLearnedWords;

    private ProgressBar progressLearning;
    private ProgressBar progressLoading;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    public ProgressFragment() {
        // Constructor rỗng bắt buộc
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_progress,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews(view);
        showDefaultData();
        loadProgressData();
    }

    private void initViews(View view) {
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        tvProgressDetail = view.findViewById(R.id.tvProgressDetail);
        tvTotalWords = view.findViewById(R.id.tvTotalWords);
        tvLearnedWords = view.findViewById(R.id.tvLearnedWords);
        tvFavoriteWords = view.findViewById(R.id.tvFavoriteWords);
        tvNotLearnedWords = view.findViewById(R.id.tvNotLearnedWords);

        progressLearning = view.findViewById(R.id.progressLearning);
        progressLoading = view.findViewById(R.id.progressLoading);
    }

    private void showDefaultData() {
        tvTotalWords.setText("Tổng số từ: " + TOTAL_WORDS);
        tvLearnedWords.setText("Đã học: 0");
        tvFavoriteWords.setText("Yêu thích: 0");
        tvNotLearnedWords.setText("Chưa học: " + TOTAL_WORDS);
        tvProgressPercent.setText("0%");
        tvProgressDetail.setText("0 / " + TOTAL_WORDS + " từ đã học");
        progressLearning.setProgress(0);
    }

    private void loadProgressData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    requireContext(),
                    "Bạn cần đăng nhập",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);

        String userId = currentUser.getUid();

        firestore.collection("users")
                .document(userId)
                .collection("learnedWords")
                .get()
                .addOnSuccessListener(learnedSnapshots -> {
                    int learnedCount = learnedSnapshots.size();

                    loadFavoriteCount(userId, learnedCount);
                })
                .addOnFailureListener(exception -> {
                    progressLoading.setVisibility(View.GONE);

                    Toast.makeText(
                            requireContext(),
                            "Không thể tải tiến độ: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadFavoriteCount(
            String userId,
            int learnedCount
    ) {
        firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnSuccessListener(favoriteSnapshots -> {
                    int favoriteCount = favoriteSnapshots.size();

                    updateProgressUI(
                            learnedCount,
                            favoriteCount
                    );

                    progressLoading.setVisibility(View.GONE);
                })
                .addOnFailureListener(exception -> {
                    updateProgressUI(learnedCount, 0);
                    progressLoading.setVisibility(View.GONE);
                });
    }

    private void updateProgressUI(
            int learnedCount,
            int favoriteCount
    ) {
        int safeLearnedCount =
                Math.min(learnedCount, TOTAL_WORDS);

        int notLearnedCount =
                Math.max(TOTAL_WORDS - safeLearnedCount, 0);

        int percent = 0;

        if (TOTAL_WORDS > 0) {
            percent =
                    safeLearnedCount * 100 / TOTAL_WORDS;
        }

        tvTotalWords.setText(
                "Tổng số từ: " + TOTAL_WORDS
        );

        tvLearnedWords.setText(
                "Đã học: " + safeLearnedCount
        );

        tvFavoriteWords.setText(
                "Yêu thích: " + favoriteCount
        );

        tvNotLearnedWords.setText(
                "Chưa học: " + notLearnedCount
        );

        tvProgressPercent.setText(
                percent + "%"
        );

        tvProgressDetail.setText(
                safeLearnedCount
                        + " / "
                        + TOTAL_WORDS
                        + " từ đã học"
        );

        progressLearning.setProgress(percent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (firebaseAuth != null
                && firestore != null
                && progressLoading != null) {

            loadProgressData();
        }
    }
}