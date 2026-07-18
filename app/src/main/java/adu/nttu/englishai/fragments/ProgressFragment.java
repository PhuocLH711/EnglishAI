package adu.nttu.englishai.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
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
import adu.nttu.englishai.activities.LoginActivity;

public class ProgressFragment extends Fragment {

    private static final String TAG = "PROGRESS_ERROR";

    private static final String STATUS_NOT_STARTED =
            "NOT_STARTED";

    private static final String STATUS_LEARNING =
            "LEARNING";

    private static final String STATUS_LEARNED =
            "LEARNED";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvMasteredCount;
    private TextView tvLearningCount;
    private TextView tvNotStartedCount;
    private TextView tvFavoriteCount;

    private final Set<String> validVocabularyIds =
            new HashSet<>();

    private final Map<String, DocumentSnapshot> progressMap =
            new HashMap<>();

    private final Map<String, DocumentSnapshot> vocabularyMap =
            new HashMap<>();

    private ListenerRegistration vocabularyListener;
    private ListenerRegistration progressListener;

    public ProgressFragment() {
        // Constructor rỗng bắt buộc.
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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        loadUserInformation(view);
        setupStatisticCardEvents(view);
        setupSettingsButton(view);

        startRealtimeVocabularyStats();
    }

    private void initViews(View view) {
        tvMasteredCount =
                view.findViewById(R.id.tvMasteredCount);

        tvLearningCount =
                view.findViewById(R.id.tvLearningCount);

        tvNotStartedCount =
                view.findViewById(R.id.tvNotStartedCount);

        tvFavoriteCount =
                view.findViewById(R.id.tvFavoriteCount);
    }

    private void loadUserInformation(View view) {
        TextView tvProfileName =
                view.findViewById(R.id.tvProfileName);

        TextView tvProfileEmail =
                view.findViewById(R.id.tvProfileEmail);

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {
            if (tvProfileName != null) {
                tvProfileName.setText("Khách");
            }

            if (tvProfileEmail != null) {
                tvProfileEmail.setText("Chưa đăng nhập");
            }

            return;
        }

        String email = currentUser.getEmail();
        String displayName = currentUser.getDisplayName();

        if (tvProfileEmail != null) {
            tvProfileEmail.setText(
                    email == null ? "" : email
            );
        }

        if (tvProfileName != null) {
            if (displayName != null
                    && !displayName.trim().isEmpty()) {

                tvProfileName.setText(
                        displayName.trim()
                );

            } else if (email != null
                    && !email.trim().isEmpty()) {

                String fallbackName =
                        email.contains("@")
                                ? email.substring(
                                0,
                                email.indexOf("@")
                        )
                                : email;

                if (!fallbackName.isEmpty()) {
                    fallbackName =
                            fallbackName.substring(0, 1)
                                    .toUpperCase(Locale.ROOT)
                                    + fallbackName.substring(1);
                }

                tvProfileName.setText(fallbackName);
            }
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!isAdded()) {
                        return;
                    }

                    String savedName = firstNonEmpty(
                            document.getString("name"),
                            document.getString("fullName"),
                            document.getString("username")
                    );

                    String savedEmail =
                            document.getString("email");

                    if (tvProfileName != null
                            && !savedName.isEmpty()) {

                        tvProfileName.setText(savedName);
                    }

                    if (tvProfileEmail != null
                            && savedEmail != null
                            && !savedEmail.trim().isEmpty()) {

                        tvProfileEmail.setText(
                                savedEmail.trim()
                        );
                    }
                });
    }

    private void setupStatisticCardEvents(View view) {
        MaterialCardView cardMastered =
                view.findViewById(R.id.cardMastered);

        MaterialCardView cardLearning =
                view.findViewById(R.id.cardLearning);

        MaterialCardView cardNotStarted =
                view.findViewById(R.id.cardNotStarted);

        MaterialCardView cardFavorite =
                view.findViewById(R.id.cardFavorite);

        if (cardMastered != null) {
            cardMastered.setOnClickListener(
                    clickedView ->
                            showWordListBottomSheet(
                                    "🟢 Từ vựng đã thuộc",
                                    "learned"
                            )
            );
        }

        if (cardLearning != null) {
            cardLearning.setOnClickListener(
                    clickedView ->
                            showWordListBottomSheet(
                                    "🟡 Từ vựng đang học",
                                    "learning"
                            )
            );
        }

        if (cardNotStarted != null) {
            cardNotStarted.setOnClickListener(
                    clickedView ->
                            showWordListBottomSheet(
                                    "⚪ Từ vựng chưa học",
                                    "not_started"
                            )
            );
        }

        if (cardFavorite != null) {
            cardFavorite.setOnClickListener(
                    clickedView ->
                            showWordListBottomSheet(
                                    "❤️ Từ vựng yêu thích",
                                    "favorite"
                            )
            );
        }
    }

    private void setupSettingsButton(View view) {
        MaterialCardView btnSettings =
                view.findViewById(R.id.btnSettings);

        if (btnSettings != null) {
            btnSettings.setOnClickListener(
                    clickedView ->
                            showSettingsBottomSheet()
            );
        }
    }

    /**
     * Bắt đầu nghe dữ liệu thật từ Firestore.
     */
    private void startRealtimeVocabularyStats() {
        if (db == null || mAuth == null) {
            return;
        }

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {
            updateStatisticViews(
                    0,
                    0,
                    0,
                    0
            );

            return;
        }

        stopRealtimeListeners();

        listenToVocabularyCollection();
        listenToUserProgress(
                currentUser.getUid()
        );
    }

    /**
     * Đọc danh sách từ thật từ collection vocabularies.
     */
    private void listenToVocabularyCollection() {
        vocabularyListener =
                db.collection("vocabularies")
                        .addSnapshotListener(
                                (snapshot, error) -> {
                                    if (!isAdded()) {
                                        return;
                                    }

                                    if (error != null) {
                                        Log.e(
                                                TAG,
                                                "Không đọc được vocabularies",
                                                error
                                        );

                                        Toast.makeText(
                                                requireContext(),
                                                "Không tải được danh sách từ vựng",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        return;
                                    }

                                    validVocabularyIds.clear();
                                    vocabularyMap.clear();

                                    if (snapshot != null) {
                                        for (
                                                DocumentSnapshot document
                                                : snapshot.getDocuments()
                                        ) {
                                            validVocabularyIds.add(
                                                    document.getId()
                                            );

                                            vocabularyMap.put(
                                                    document.getId(),
                                                    document
                                            );
                                        }
                                    }

                                    calculateVocabularyStatistics();
                                }
                        );
    }

    /**
     * Đọc tiến độ thật của tài khoản hiện tại.
     */
    private void listenToUserProgress(
            String userId
    ) {
        progressListener =
                db.collection("users")
                        .document(userId)
                        .collection("wordProgress")
                        .addSnapshotListener(
                                (snapshot, error) -> {
                                    if (!isAdded()) {
                                        return;
                                    }

                                    if (error != null) {
                                        Log.e(
                                                TAG,
                                                "Không đọc được wordProgress",
                                                error
                                        );

                                        Toast.makeText(
                                                requireContext(),
                                                "Không tải được tiến độ từ vựng",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        return;
                                    }

                                    progressMap.clear();

                                    if (snapshot != null) {
                                        for (
                                                DocumentSnapshot document
                                                : snapshot.getDocuments()
                                        ) {
                                            progressMap.put(
                                                    document.getId(),
                                                    document
                                            );
                                        }
                                    }

                                    calculateVocabularyStatistics();
                                }
                        );
    }

    /**
     * Đếm:
     * - Đã thuộc
     * - Đang học
     * - Chưa học
     * - Yêu thích
     */
    private void calculateVocabularyStatistics() {
        int masteredCount = 0;
        int learningCount = 0;
        int notStartedCount = 0;
        int favoriteCount = 0;

        /*
         * Chỉ tính các từ đang tồn tại thật trong vocabularies.
         * Những document cũ như 1, 2, 5, 6, 7 sẽ không được tính
         * nếu chúng không còn tồn tại trong vocabularies.
         */
        for (String vocabularyId : validVocabularyIds) {
            DocumentSnapshot progressDocument =
                    progressMap.get(vocabularyId);

            if (progressDocument == null
                    || !progressDocument.exists()) {

                notStartedCount++;
                continue;
            }

            String status = firstNonEmpty(
                    progressDocument.getString(
                            "learningStatus"
                    ),
                    progressDocument.getString(
                            "status"
                    )
            );

            String normalizedStatus =
                    normalizeLearningStatus(status);

            if (STATUS_LEARNED.equals(
                    normalizedStatus
            )) {
                masteredCount++;

            } else if (STATUS_LEARNING.equals(
                    normalizedStatus
            )) {
                learningCount++;

            } else {
                notStartedCount++;
            }

            Boolean favorite =
                    progressDocument.getBoolean(
                            "favorite"
                    );

            if (favorite != null && favorite) {
                favoriteCount++;
            }
        }

        updateStatisticViews(
                masteredCount,
                learningCount,
                notStartedCount,
                favoriteCount
        );
    }

    private void updateStatisticViews(
            int mastered,
            int learning,
            int notStarted,
            int favorite
    ) {
        if (!isAdded() || getView() == null) {
            return;
        }

        if (tvMasteredCount != null) {
            tvMasteredCount.setText(
                    String.valueOf(mastered)
            );
        }

        if (tvLearningCount != null) {
            tvLearningCount.setText(
                    String.valueOf(learning)
            );
        }

        if (tvNotStartedCount != null) {
            tvNotStartedCount.setText(
                    String.valueOf(notStarted)
            );
        }

        if (tvFavoriteCount != null) {
            tvFavoriteCount.setText(
                    String.valueOf(favorite)
            );
        }
    }

    /**
     * Hiển thị danh sách từ tương ứng khi bấm vào card.
     */
    private void showWordListBottomSheet(
            String title,
            String filterType
    ) {
        if (getContext() == null) {
            return;
        }

        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        requireContext()
                );

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_word_list,
                        null
                );

        dialog.setContentView(dialogView);

        TextView tvDialogTitle =
                dialogView.findViewById(
                        R.id.tvDialogTitle
                );

        TextView btnCloseDialog =
                dialogView.findViewById(
                        R.id.btnCloseDialog
                );

        LinearLayout layoutWordContainer =
                dialogView.findViewById(
                        R.id.layoutWordContainer
                );

        if (tvDialogTitle != null) {
            tvDialogTitle.setText(title);
        }

        if (btnCloseDialog != null) {
            btnCloseDialog.setOnClickListener(
                    clickedView ->
                            dialog.dismiss()
            );
        }

        if (layoutWordContainer != null) {
            layoutWordContainer.removeAllViews();

            int matchCount = 0;

            for (String vocabularyId
                    : validVocabularyIds) {

                DocumentSnapshot vocabularyDocument =
                        vocabularyMap.get(vocabularyId);

                DocumentSnapshot progressDocument =
                        progressMap.get(vocabularyId);

                String status =
                        STATUS_NOT_STARTED;

                boolean favorite = false;

                if (progressDocument != null
                        && progressDocument.exists()) {

                    status = normalizeLearningStatus(
                            firstNonEmpty(
                                    progressDocument.getString(
                                            "learningStatus"
                                    ),
                                    progressDocument.getString(
                                            "status"
                                    )
                            )
                    );

                    Boolean favoriteValue =
                            progressDocument.getBoolean(
                                    "favorite"
                            );

                    favorite =
                            favoriteValue != null
                                    && favoriteValue;
                }

                boolean matches =
                        matchesFilter(
                                filterType,
                                status,
                                favorite
                        );

                if (!matches
                        || vocabularyDocument == null) {

                    continue;
                }

                String word = firstNonEmpty(
                        vocabularyDocument.getString(
                                "englishWord"
                        ),
                        vocabularyDocument.getString(
                                "word"
                        ),
                        progressDocument == null
                                ? null
                                : progressDocument.getString(
                                "englishWord"
                        )
                );

                String meaning = firstNonEmpty(
                        vocabularyDocument.getString(
                                "vietnameseMeaning"
                        ),
                        vocabularyDocument.getString(
                                "meaning"
                        ),
                        progressDocument == null
                                ? null
                                : progressDocument.getString(
                                "vietnameseMeaning"
                        )
                );

                String pronunciation =
                        firstNonEmpty(
                                vocabularyDocument.getString(
                                        "pronunciation"
                                ),
                                vocabularyDocument.getString(
                                        "phonetic"
                                ),
                                progressDocument == null
                                        ? null
                                        : progressDocument.getString(
                                        "pronunciation"
                                )
                        );

                if (!word.isEmpty()) {
                    addWordCardToContainer(
                            layoutWordContainer,
                            word,
                            pronunciation,
                            meaning
                    );

                    matchCount++;
                }
            }

            if (matchCount == 0) {
                showEmptyMessage(
                        layoutWordContainer
                );
            }
        }

        dialog.show();
    }

    private boolean matchesFilter(
            String filterType,
            String status,
            boolean favorite
    ) {
        if ("favorite".equals(filterType)) {
            return favorite;
        }

        if ("learned".equals(filterType)) {
            return STATUS_LEARNED.equals(status);
        }

        if ("learning".equals(filterType)) {
            return STATUS_LEARNING.equals(status);
        }

        if ("not_started".equals(filterType)) {
            return STATUS_NOT_STARTED.equals(status);
        }

        return false;
    }

    private void addWordCardToContainer(
            LinearLayout container,
            String word,
            String phonetic,
            String meaning
    ) {
        if (getContext() == null) {
            return;
        }

        MaterialCardView card =
                new MaterialCardView(
                        requireContext()
                );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                dpToPx(12)
        );

        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(
                Color.parseColor("#F8F9FA")
        );

        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(
                Color.parseColor("#E9ECEF")
        );

        LinearLayout innerLayout =
                new LinearLayout(
                        requireContext()
                );

        innerLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        innerLayout.setPadding(
                dpToPx(16),
                dpToPx(14),
                dpToPx(16),
                dpToPx(14)
        );

        TextView tvWord =
                new TextView(requireContext());

        String wordTitle = word;

        if (phonetic != null
                && !phonetic.trim().isEmpty()) {

            wordTitle += "  " + phonetic.trim();
        }

        tvWord.setText(wordTitle);
        tvWord.setTextSize(17f);
        tvWord.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        tvWord.setTextColor(
                Color.parseColor("#1A73E8")
        );

        TextView tvMeaning =
                new TextView(requireContext());

        tvMeaning.setText(
                meaning == null
                        || meaning.trim().isEmpty()
                        ? "👉 Chưa có nghĩa tiếng Việt"
                        : "👉 Nghĩa: " + meaning.trim()
        );

        tvMeaning.setTextSize(14f);
        tvMeaning.setTextColor(
                Color.parseColor("#37474F")
        );

        tvMeaning.setPadding(
                0,
                dpToPx(6),
                0,
                0
        );

        innerLayout.addView(tvWord);
        innerLayout.addView(tvMeaning);

        card.addView(innerLayout);
        container.addView(card);
    }

    private void showEmptyMessage(
            LinearLayout container
    ) {
        if (getContext() == null
                || container == null) {

            return;
        }

        TextView tvEmpty =
                new TextView(requireContext());

        tvEmpty.setText(
                "📭 Chưa có từ vựng nào trong mục này.\n"
                        + "Hãy qua trang Từ vựng để học nhé!"
        );

        tvEmpty.setTextSize(15f);
        tvEmpty.setTextColor(
                Color.parseColor("#757575")
        );

        tvEmpty.setGravity(Gravity.CENTER);

        tvEmpty.setPadding(
                dpToPx(20),
                dpToPx(40),
                dpToPx(20),
                dpToPx(40)
        );

        container.addView(tvEmpty);
    }

    private String normalizeLearningStatus(
            String status
    ) {
        if (status == null
                || status.trim().isEmpty()) {

            return STATUS_NOT_STARTED;
        }

        String normalized =
                status.trim()
                        .toUpperCase(Locale.ROOT);

        if ("LEARNED".equals(normalized)
                || "MASTERED".equals(normalized)) {

            return STATUS_LEARNED;
        }

        if ("LEARNING".equals(normalized)) {
            return STATUS_LEARNING;
        }

        return STATUS_NOT_STARTED;
    }

    private String firstNonEmpty(
            String... values
    ) {
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

    private int dpToPx(int dp) {
        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(dp * density);
    }

    private void showSettingsBottomSheet() {
        if (getContext() == null) {
            return;
        }

        BottomSheetDialog dialog =
                new BottomSheetDialog(
                        requireContext()
                );

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_settings,
                        null
                );

        dialog.setContentView(dialogView);

        LinearLayout itemAvatar =
                dialogView.findViewById(
                        R.id.itemChangeAvatar
                );

        LinearLayout itemInfo =
                dialogView.findViewById(
                        R.id.itemUpdateInfo
                );

        LinearLayout itemHelp =
                dialogView.findViewById(
                        R.id.itemHelp
                );

        Button dialogBtnLogout =
                dialogView.findViewById(
                        R.id.dialogBtnLogout
                );

        if (itemAvatar != null) {
            itemAvatar.setOnClickListener(
                    clickedView -> {
                        Toast.makeText(
                                requireContext(),
                                "Tính năng đổi ảnh đang được cập nhật",
                                Toast.LENGTH_SHORT
                        ).show();

                        dialog.dismiss();
                    }
            );
        }

        if (itemInfo != null) {
            itemInfo.setOnClickListener(
                    clickedView -> {
                        Toast.makeText(
                                requireContext(),
                                "Tính năng sửa thông tin đang được cập nhật",
                                Toast.LENGTH_SHORT
                        ).show();

                        dialog.dismiss();
                    }
            );
        }

        if (itemHelp != null) {
            itemHelp.setOnClickListener(
                    clickedView -> {
                        Toast.makeText(
                                requireContext(),
                                "Hãy qua trang Từ vựng để bắt đầu học",
                                Toast.LENGTH_LONG
                        ).show();

                        dialog.dismiss();
                    }
            );
        }

        if (dialogBtnLogout != null) {
            dialogBtnLogout.setOnClickListener(
                    clickedView -> {
                        dialog.dismiss();

                        if (mAuth != null) {
                            mAuth.signOut();
                        }

                        Intent intent = new Intent(
                                requireActivity(),
                                LoginActivity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        requireActivity().finish();
                    }
            );
        }

        dialog.show();
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