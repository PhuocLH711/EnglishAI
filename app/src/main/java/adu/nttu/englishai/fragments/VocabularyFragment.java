package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.VocabularyAdapter;
import adu.nttu.englishai.models.Vocabulary;

public class VocabularyFragment extends Fragment {

    private Button btnFilterVocabulary;
    private TextView tvFilterStatus;
    private TextView tvVocabularySummary;
    private RecyclerView recyclerVocabulary;
    private SearchView searchVocabulary;
    private View emptyStateLayout;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private VocabularyAdapter vocabularyAdapter;

    private final List<Vocabulary> vocabularyList = new ArrayList<>();
    private final List<Vocabulary> filteredList = new ArrayList<>();

    private String selectedCategory = "Tất cả";
    private String selectedLevel = "Tất cả";
    private String currentKeyword = "";

    public VocabularyFragment() {
        // Constructor rỗng bắt buộc.
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_vocabulary,
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

        recyclerVocabulary =
                view.findViewById(R.id.recyclerVocabulary);

        searchVocabulary =
                view.findViewById(R.id.searchVocabulary);

        emptyStateLayout =
                view.findViewById(R.id.emptyStateLayout);

        btnFilterVocabulary =
                view.findViewById(R.id.btnFilterVocabulary);

        tvFilterStatus =
                view.findViewById(R.id.tvFilterStatus);

        tvVocabularySummary =
                view.findViewById(R.id.tvVocabularySummary);

        setupRecyclerView();
        setupSearch();
        setupFilterButton();

        searchVocabulary.setIconifiedByDefault(false);
        searchVocabulary.setIconified(false);
        searchVocabulary.clearFocus();

        updateEmptyStateVisibility();
        loadVocabularyFromFirestore();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (firebaseAuth != null
                && firestore != null
                && !vocabularyList.isEmpty()) {

            loadUserProgress();
        }
    }

    private void setupRecyclerView() {
        vocabularyAdapter = new VocabularyAdapter(
                filteredList,
                this::saveFavoriteStatus
        );

        recyclerVocabulary.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerVocabulary.setHasFixedSize(true);
        recyclerVocabulary.setAdapter(vocabularyAdapter);
    }

    private void loadVocabularyFromFirestore() {
        firestore.collection("vocabularies")
                .get()
                .addOnSuccessListener(snapshot -> {
                    vocabularyList.clear();

                    for (DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        Vocabulary vocabulary =
                                createVocabularyFromDocument(document);

                        String englishWord =
                                vocabulary.getEnglishWord();

                        if (englishWord != null
                                && !englishWord.trim().isEmpty()) {

                            vocabularyList.add(vocabulary);
                        }
                    }

                    Collections.sort(
                            vocabularyList,
                            (first, second) ->
                                    safeLower(first.getEnglishWord())
                                            .compareTo(
                                                    safeLower(
                                                            second.getEnglishWord()
                                                    )
                                            )
                    );

                    loadUserProgress();
                })
                .addOnFailureListener(exception -> {
                    vocabularyList.clear();
                    filteredList.clear();

                    vocabularyAdapter.notifyDataSetChanged();
                    updateVocabularySummary();
                    updateEmptyStateVisibility();

                    Toast.makeText(
                            requireContext(),
                            "Không tải được từ vựng: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private Vocabulary createVocabularyFromDocument(
            DocumentSnapshot document
    ) {
        Vocabulary vocabulary = new Vocabulary();

        vocabulary.setId(document.getId());

        vocabulary.setEnglishWord(
                firstNonEmpty(
                        document.getString("englishWord"),
                        document.getString("word"),
                        document.getString("english"),
                        document.getString("name")
                )
        );

        vocabulary.setVietnameseMeaning(
                firstNonEmpty(
                        document.getString("vietnameseMeaning"),
                        document.getString("meaning"),
                        document.getString("vietnamese")
                )
        );

        vocabulary.setPronunciation(
                firstNonEmpty(
                        document.getString("pronunciation"),
                        document.getString("phonetic")
                )
        );

        vocabulary.setWordType(
                firstNonEmpty(
                        document.getString("wordType"),
                        document.getString("type"),
                        document.getString("partOfSpeech")
                )
        );

        vocabulary.setExample(
                firstNonEmpty(
                        document.getString("example"),
                        document.getString("exampleSentence")
                )
        );

        vocabulary.setExampleMeaning(
                firstNonEmpty(
                        document.getString("exampleMeaning"),
                        document.getString("exampleVietnamese")
                )
        );

        vocabulary.setCategory(
                firstNonEmpty(
                        document.getString("category"),
                        "Khác"
                )
        );

        vocabulary.setLevel(
                firstNonEmpty(
                        document.getString("level"),
                        "Easy"
                )
        );

        vocabulary.setImageUrl(
                firstNonEmpty(
                        document.getString("imageUrl"),
                        document.getString("image")
                )
        );

        vocabulary.setFavorite(false);
        vocabulary.setLearningStatus(
                Vocabulary.STATUS_NOT_STARTED
        );

        return vocabulary;
    }

    private void loadUserProgress() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            resetProgressToDefault();
            applyCurrentFilter();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wordProgress")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, DocumentSnapshot> progressMap =
                            new HashMap<>();

                    for (DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        progressMap.put(
                                document.getId(),
                                document
                        );
                    }

                    for (Vocabulary vocabulary
                            : vocabularyList) {

                        vocabulary.setFavorite(false);
                        vocabulary.setLearningStatus(
                                Vocabulary.STATUS_NOT_STARTED
                        );

                        DocumentSnapshot progress =
                                progressMap.get(vocabulary.getId());

                        if (progress == null) {
                            continue;
                        }

                        Boolean favorite =
                                firstBoolean(
                                        progress.getBoolean("favorite"),
                                        progress.getBoolean("isFavorite"),
                                        progress.getBoolean("isFav")
                                );

                        String status =
                                firstNonEmpty(
                                        progress.getString("learningStatus"),
                                        progress.getString("status")
                                );

                        vocabulary.setFavorite(
                                favorite != null && favorite
                        );

                        vocabulary.setLearningStatus(
                                normalizeStatus(status)
                        );
                    }

                    applyCurrentFilter();
                })
                .addOnFailureListener(exception -> {
                    resetProgressToDefault();
                    applyCurrentFilter();

                    Toast.makeText(
                            requireContext(),
                            "Không tải được tiến độ học.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void resetProgressToDefault() {
        for (Vocabulary vocabulary : vocabularyList) {
            vocabulary.setFavorite(false);
            vocabulary.setLearningStatus(
                    Vocabulary.STATUS_NOT_STARTED
            );
        }
    }

    private void saveFavoriteStatus(
            Vocabulary vocabulary,
            boolean newState
    ) {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            vocabulary.setFavorite(!newState);
            vocabularyAdapter.notifyDataSetChanged();

            Toast.makeText(
                    requireContext(),
                    "Bạn cần đăng nhập để lưu yêu thích.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String vocabularyId = vocabulary.getId();

        if (vocabularyId == null
                || vocabularyId.trim().isEmpty()) {

            vocabulary.setFavorite(!newState);
            vocabularyAdapter.notifyDataSetChanged();

            Toast.makeText(
                    requireContext(),
                    "Từ vựng chưa có ID hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> progressData =
                new HashMap<>();

        progressData.put("vocabularyId", vocabularyId);
        progressData.put("favorite", newState);
        progressData.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wordProgress")
                .document(vocabularyId)
                .set(progressData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    vocabularyAdapter.notifyDataSetChanged();

                    Toast.makeText(
                            requireContext(),
                            newState
                                    ? "Đã thêm vào yêu thích"
                                    : "Đã bỏ yêu thích",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    vocabulary.setFavorite(!newState);
                    vocabularyAdapter.notifyDataSetChanged();

                    Toast.makeText(
                            requireContext(),
                            "Không lưu được yêu thích: "
                                    + exception.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void setupSearch() {
        searchVocabulary.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {
                        currentKeyword =
                                query == null ? "" : query;

                        applyCurrentFilter();
                        searchVocabulary.clearFocus();

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {
                        currentKeyword =
                                newText == null ? "" : newText;

                        applyCurrentFilter();
                        return true;
                    }
                }
        );
    }

    private void setupFilterButton() {
        btnFilterVocabulary.setOnClickListener(
                view -> showFilterDialog()
        );
    }

    private void showFilterDialog() {
        View dialogView = LayoutInflater
                .from(requireContext())
                .inflate(
                        R.layout.dialog_filter_vocabulary,
                        null
                );

        Spinner spinnerCategory =
                dialogView.findViewById(
                        R.id.spinnerCategory
                );

        Spinner spinnerLevel =
                dialogView.findViewById(
                        R.id.spinnerLevel
                );

        String[] categories =
                buildCategoryArray();

        String[] levels = {
                "Tất cả",
                "Easy",
                "Medium",
                "Hard"
        };

        spinnerCategory.setAdapter(
                createSpinnerAdapter(categories)
        );

        spinnerLevel.setAdapter(
                createSpinnerAdapter(levels)
        );

        spinnerCategory.setSelection(
                findArrayPosition(
                        categories,
                        selectedCategory
                )
        );

        spinnerLevel.setSelection(
                findArrayPosition(
                        levels,
                        selectedLevel
                )
        );

        AlertDialog dialog =
                new AlertDialog.Builder(requireContext())
                        .setTitle("Lọc từ vựng")
                        .setView(dialogView)
                        .setPositiveButton(
                                "Áp dụng",
                                (currentDialog, which) -> {
                                    selectedCategory =
                                            spinnerCategory
                                                    .getSelectedItem()
                                                    .toString();

                                    selectedLevel =
                                            spinnerLevel
                                                    .getSelectedItem()
                                                    .toString();

                                    updateFilterStatus();
                                    applyCurrentFilter();
                                }
                        )
                        .setNeutralButton(
                                "Xóa lọc",
                                (currentDialog, which) -> {
                                    selectedCategory = "Tất cả";
                                    selectedLevel = "Tất cả";

                                    updateFilterStatus();
                                    applyCurrentFilter();
                                }
                        )
                        .setNegativeButton(
                                "Hủy",
                                null
                        )
                        .create();

        dialog.setOnShowListener(unused -> {
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setTextColor(
                    requireContext().getColor(
                            android.R.color.holo_purple
                    )
            );

            dialog.getButton(
                    AlertDialog.BUTTON_NEUTRAL
            ).setTextColor(
                    requireContext().getColor(
                            android.R.color.holo_purple
                    )
            );

            dialog.getButton(
                    AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(
                    requireContext().getColor(
                            android.R.color.darker_gray
                    )
            );
        });

        dialog.show();
    }

    private ArrayAdapter<String> createSpinnerAdapter(
            String[] values
    ) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        values
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        return adapter;
    }

    private String[] buildCategoryArray() {
        Set<String> categorySet =
                new LinkedHashSet<>();

        categorySet.add("Tất cả");

        List<String> categories =
                new ArrayList<>();

        for (Vocabulary vocabulary : vocabularyList) {
            String category =
                    vocabulary.getCategory();

            if (category == null
                    || category.trim().isEmpty()) {

                continue;
            }

            String cleanedCategory =
                    category.trim();

            boolean alreadyExists = false;

            for (String existingCategory : categories) {
                if (existingCategory.equalsIgnoreCase(
                        cleanedCategory
                )) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                categories.add(cleanedCategory);
            }
        }

        Collections.sort(
                categories,
                String.CASE_INSENSITIVE_ORDER
        );

        categorySet.addAll(categories);

        return categorySet.toArray(new String[0]);
    }

    private void applyCurrentFilter() {
        filteredList.clear();

        String searchText =
                safeLower(currentKeyword);

        for (Vocabulary vocabulary : vocabularyList) {
            String englishWord =
                    safeLower(
                            vocabulary.getEnglishWord()
                    );

            String vietnameseMeaning =
                    safeLower(
                            vocabulary.getVietnameseMeaning()
                    );

            String pronunciation =
                    safeLower(
                            vocabulary.getPronunciation()
                    );

            String category =
                    safeLower(
                            vocabulary.getCategory()
                    );

            String level =
                    safeLower(
                            vocabulary.getLevel()
                    );

            boolean matchesKeyword =
                    searchText.isEmpty()
                            || englishWord.contains(searchText)
                            || vietnameseMeaning.contains(searchText)
                            || pronunciation.contains(searchText)
                            || category.contains(searchText)
                            || level.contains(searchText);

            boolean matchesCategory =
                    "Tất cả".equals(selectedCategory)
                            || category.equals(
                            safeLower(selectedCategory)
                    );

            boolean matchesLevel =
                    "Tất cả".equals(selectedLevel)
                            || level.equals(
                            safeLower(selectedLevel)
                    );

            if (matchesKeyword
                    && matchesCategory
                    && matchesLevel) {

                filteredList.add(vocabulary);
            }
        }

        updateVocabularySummary();
        vocabularyAdapter.notifyDataSetChanged();
        updateEmptyStateVisibility();
    }

    private void updateVocabularySummary() {
        if (tvVocabularySummary == null) {
            return;
        }

        int count = filteredList.size();

        tvVocabularySummary.setText(
                count + " từ vựng đang chờ bạn"
        );
    }

    private void updateFilterStatus() {
        boolean hasFilter =
                !"Tất cả".equals(selectedCategory)
                        || !"Tất cả".equals(selectedLevel);

        if (!hasFilter) {
            tvFilterStatus.setVisibility(View.GONE);
            btnFilterVocabulary.setText("Lọc");
            return;
        }

        tvFilterStatus.setVisibility(View.VISIBLE);

        StringBuilder statusBuilder =
                new StringBuilder();

        if (!"Tất cả".equals(selectedCategory)) {
            statusBuilder.append("📚 ")
                    .append(selectedCategory);
        }

        if (!"Tất cả".equals(selectedLevel)) {
            if (statusBuilder.length() > 0) {
                statusBuilder.append("  •  ");
            }

            statusBuilder.append("⭐ ")
                    .append(selectedLevel);
        }

        tvFilterStatus.setText(
                statusBuilder.toString()
        );

        btnFilterVocabulary.setText("Đang lọc");
    }

    private void updateEmptyStateVisibility() {
        if (filteredList.isEmpty()) {
            recyclerVocabulary.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerVocabulary.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private int findArrayPosition(
            String[] values,
            String selectedValue
    ) {
        if (values == null
                || selectedValue == null) {

            return 0;
        }

        for (int index = 0;
             index < values.length;
             index++) {

            if (values[index].equals(selectedValue)) {
                return index;
            }
        }

        return 0;
    }

    private String safeLower(
            String value
    ) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
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

    private Boolean firstBoolean(
            Boolean... values
    ) {
        if (values == null) {
            return null;
        }

        for (Boolean value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String normalizeStatus(
            String status
    ) {
        if (status == null
                || status.trim().isEmpty()) {

            return Vocabulary.STATUS_NOT_STARTED;
        }

        String normalized =
                status.trim().toUpperCase(Locale.ROOT);

        if ("MASTERED".equals(normalized)
                || "LEARNED".equals(normalized)) {

            return Vocabulary.STATUS_LEARNED;
        }

        if ("LEARNING".equals(normalized)) {
            return Vocabulary.STATUS_LEARNING;
        }

        return Vocabulary.STATUS_NOT_STARTED;
    }
}