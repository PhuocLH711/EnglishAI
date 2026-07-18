package adu.nttu.englishai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import adu.nttu.englishai.R;
import adu.nttu.englishai.activities.ImportVocabularyActivity;

public class ProfileFragment extends Fragment {

    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_LEARNING = "LEARNING";
    private static final String STATUS_LEARNED = "LEARNED";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextView tvProfileName;
    private TextView tvProfileEmail;

    private TextView tvTotalVocabulary;
    private TextView tvNotStartedCount;
    private TextView tvLearningCount;
    private TextView tvLearnedCount;
    private TextView tvFavoriteCount;

    private MaterialButton btnOpenImportVocabulary;

    /*
     * Chứa ID các từ đang tồn tại thật trong collection vocabularies.
     * Nhờ đó các document cũ như 1, 2, 5, 6, 7 sẽ không bị tính nhầm.
     */
    private final Set<String> validVocabularyIds = new HashSet<>();

    /*
     * Lưu tạm dữ liệu tiến độ hiện tại của người dùng.
     */
    private final Map<String, DocumentSnapshot> wordProgressMap =
            new HashMap<>();

    private ListenerRegistration vocabularyListener;
    private ListenerRegistration progressListener;

    public ProfileFragment() {
        // Constructor rỗng bắt buộc.
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_profile,
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
        setupEvents();
        loadUserInformation();
        startRealtimeStatistics();
    }

    private void initViews(View view) {
        tvProfileName =
                view.findViewById(R.id.tvProfileName);

        tvProfileEmail =
                view.findViewById(R.id.tvProfileEmail);

        tvTotalVocabulary =
                view.findViewById(R.id.tvTotalVocabulary);

        tvNotStartedCount =
                view.findViewById(R.id.tvNotStartedCount);

        tvLearningCount =
                view.findViewById(R.id.tvLearningCount);

        tvLearnedCount =
                view.findViewById(R.id.tvLearnedCount);

        tvFavoriteCount =
                view.findViewById(R.id.tvFavoriteCount);

        btnOpenImportVocabulary =
                view.findViewById(R.id.btnOpenImportVocabulary);
    }

    private void setupEvents() {
        btnOpenImportVocabulary.setOnClickListener(view -> {
            Intent intent = new Intent(
                    requireContext(),
                    ImportVocabularyActivity.class
            );

            startActivity(intent);
        });
    }

    private void loadUserInformation() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            tvProfileName.setText("Khách");
            tvProfileEmail.setText("Chưa đăng nhập");
            resetStatistics();
            return;
        }

        String authName = currentUser.getDisplayName();
        String authEmail = currentUser.getEmail();

        if (authName == null || authName.trim().isEmpty()) {
            tvProfileName.setText("Học viên EnglishAI");
        } else {
            tvProfileName.setText(authName.trim());
        }

        tvProfileEmail.setText(
                authEmail == null ? "" : authEmail
        );

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!isAdded()) {
                        return;
                    }

                    String firestoreName = firstNonEmpty(
                            document.getString("name"),
                            document.getString("fullName"),
                            document.getString("username")
                    );

                    String firestoreEmail =
                            document.getString("email");

                    if (!firestoreName.isEmpty()) {
                        tvProfileName.setText(firestoreName);
                    }

                    if (firestoreEmail != null
                            && !firestoreEmail.trim().isEmpty()) {

                        tvProfileEmail.setText(
                                firestoreEmail.trim()
                        );
                    }
                });
    }

    /**
     * Bắt đầu nghe dữ liệu thật từ Firestore.
     */
    private void startRealtimeStatistics() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            resetStatistics();
            return;
        }

        stopRealtimeListeners();

        listenToVocabularyCollection();
        listenToWordProgress(currentUser.getUid());
    }

    /**
     * Nghe realtime collection vocabularies.
     */
    private void listenToVocabularyCollection() {
        vocabularyListener = firestore
                .collection("vocabularies")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (error != null) {
                        Toast.makeText(
                                requireContext(),
                                "Không tải được danh sách từ vựng",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    validVocabularyIds.clear();

                    if (snapshot != null) {
                        for (DocumentSnapshot document
                                : snapshot.getDocuments()) {

                            validVocabularyIds.add(
                                    document.getId()
                            );
                        }
                    }

                    calculateAndDisplayStatistics();
                });
    }

    /**
     * Nghe realtime tiến độ người dùng.
     */
    private void listenToWordProgress(String userId) {
        progressListener = firestore
                .collection("users")
                .document(userId)
                .collection("wordProgress")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (error != null) {
                        Toast.makeText(
                                requireContext(),
                                "Không tải được trạng thái từ vựng",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    wordProgressMap.clear();

                    if (snapshot != null) {
                        for (DocumentSnapshot document
                                : snapshot.getDocuments()) {

                            wordProgressMap.put(
                                    document.getId(),
                                    document
                            );
                        }
                    }

                    calculateAndDisplayStatistics();
                });
    }

    /**
     * Tính dữ liệu thật:
     *
     * apple   -> LEARNED + favorite
     * mother  -> LEARNING + favorite
     * teacher -> LEARNING
     */
    private void calculateAndDisplayStatistics() {
        int totalCount = validVocabularyIds.size();

        int notStartedCount = 0;
        int learningCount = 0;
        int learnedCount = 0;
        int favoriteCount = 0;

        /*
         * Chỉ duyệt ID từ thật trong vocabularies.
         * Không tính các document cũ như 1, 2, 5, 6, 7.
         */
        for (String vocabularyId : validVocabularyIds) {
            DocumentSnapshot progressDocument =
                    wordProgressMap.get(vocabularyId);

            if (progressDocument == null
                    || !progressDocument.exists()) {

                notStartedCount++;
                continue;
            }

            String status = firstNonEmpty(
                    progressDocument.getString(
                            "learningStatus"
                    ),
                    progressDocument.getString("status")
            );

            status = normalizeLearningStatus(status);

            switch (status) {
                case STATUS_LEARNING:
                    learningCount++;
                    break;

                case STATUS_LEARNED:
                    learnedCount++;
                    break;

                default:
                    notStartedCount++;
                    break;
            }

            Boolean favorite =
                    progressDocument.getBoolean("favorite");

            if (favorite != null && favorite) {
                favoriteCount++;
            }
        }

        updateStatistics(
                totalCount,
                notStartedCount,
                learningCount,
                learnedCount,
                favoriteCount
        );
    }

    private void updateStatistics(
            int totalCount,
            int notStartedCount,
            int learningCount,
            int learnedCount,
            int favoriteCount
    ) {
        if (!isAdded() || getView() == null) {
            return;
        }

        tvTotalVocabulary.setText(
                String.valueOf(totalCount)
        );

        tvNotStartedCount.setText(
                String.valueOf(notStartedCount)
        );

        tvLearningCount.setText(
                String.valueOf(learningCount)
        );

        tvLearnedCount.setText(
                String.valueOf(learnedCount)
        );

        tvFavoriteCount.setText(
                String.valueOf(favoriteCount)
        );
    }

    private void resetStatistics() {
        validVocabularyIds.clear();
        wordProgressMap.clear();

        updateStatistics(
                0,
                0,
                0,
                0,
                0
        );
    }

    private String normalizeLearningStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return STATUS_NOT_STARTED;
        }

        String normalized =
                status.trim().toUpperCase(Locale.ROOT);

        if ("LEARNED".equals(normalized)
                || "MASTERED".equals(normalized)) {

            return STATUS_LEARNED;
        }

        if ("LEARNING".equals(normalized)) {
            return STATUS_LEARNING;
        }

        return STATUS_NOT_STARTED;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null
                    && !value.trim().isEmpty()) {

                return value.trim();
            }
        }

        return "";
    }

    private void stopRealtimeListeners() {
        if (vocabularyListener != null) {
            vocabularyListener.remove();
            vocabularyListener = null;
        }

        if (progressListener != null) {
            progressListener.remove();
            progressListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        stopRealtimeListeners();
        super.onDestroyView();
    }
}