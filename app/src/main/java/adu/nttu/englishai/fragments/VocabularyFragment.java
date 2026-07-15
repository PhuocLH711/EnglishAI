package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.VocabularyAdapter;
import adu.nttu.englishai.models.Vocabulary;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class VocabularyFragment extends Fragment {
    private Button btnFilterVocabulary;
    private TextView tvFilterStatus;

    private String selectedCategory = "Tất cả";
    private String selectedLevel = "Tất cả";
    private String currentKeyword = "";
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private RecyclerView recyclerVocabulary;
    private SearchView searchVocabulary;

    // 1. Khai báo thêm biến khung thông báo Empty State
    private View emptyStateLayout;

    private VocabularyAdapter vocabularyAdapter;

    private final List<Vocabulary> vocabularyList = new ArrayList<>();
    private final List<Vocabulary> filteredList = new ArrayList<>();

    public VocabularyFragment() {
        // Constructor rỗng bắt buộc cho Fragment
    }

    @Override
    public void onResume() {
        super.onResume();

        if (firebaseAuth != null && firestore != null) {
            loadLearningStatus();
        }
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
        recyclerVocabulary = view.findViewById(R.id.recyclerVocabulary);
        searchVocabulary = view.findViewById(R.id.searchVocabulary);

        // 2. Ánh xạ ID từ file XML
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        btnFilterVocabulary =
                view.findViewById(R.id.btnFilterVocabulary);

        tvFilterStatus =
                view.findViewById(R.id.tvFilterStatus);

        searchVocabulary.setIconifiedByDefault(false);
        searchVocabulary.setIconified(false);
        searchVocabulary.clearFocus();
        createSampleVocabulary();

        filteredList.addAll(vocabularyList);

        vocabularyAdapter = new VocabularyAdapter(filteredList);

        recyclerVocabulary.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerVocabulary.setHasFixedSize(true);
        recyclerVocabulary.setAdapter(vocabularyAdapter);

        setupSearch();
        loadLearningStatus();
        setupFilterButton();

        // Kiểm tra trạng thái hiển thị lần đầu
        updateEmptyStateVisibility();
    }

    private void loadLearningStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wordProgress")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (Vocabulary vocabulary : vocabularyList) {
                        vocabulary.setLearningStatus("NOT_STARTED");
                    }

                    for (DocumentSnapshot document
                            : queryDocumentSnapshots.getDocuments()) {

                        String vocabularyId = document.getId();
                        String status = document.getString("status");

                        for (Vocabulary vocabulary : vocabularyList) {
                            if (vocabulary.getId().equals(vocabularyId)) {
                                vocabulary.setLearningStatus(
                                        status != null
                                                ? status
                                                : "NOT_STARTED"
                                );
                                break;
                            }
                        }
                    }

                    vocabularyAdapter.notifyDataSetChanged();
                    updateEmptyStateVisibility();
                })
                .addOnFailureListener(exception -> {
                    for (Vocabulary vocabulary : vocabularyList) {
                        vocabulary.setLearningStatus("NOT_STARTED");
                    }

                    vocabularyAdapter.notifyDataSetChanged();
                    updateEmptyStateVisibility();
                });
    }

    private void createSampleVocabulary() {
        vocabularyList.clear();

        vocabularyList.add(new Vocabulary(
                "1",
                "Apple",
                "Quả táo",
                "/ˈæp.əl/",
                "I eat an apple every day.",
                "Food",
                "Easy"
        ));

        vocabularyList.add(new Vocabulary(
                "2",
                "Banana",
                "Quả chuối",
                "/bəˈnɑː.nə/",
                "The banana is yellow.",
                "Food",
                "Easy"
        ));

        vocabularyList.add(new Vocabulary(
                "3",
                "Dog",
                "Con chó",
                "/dɒɡ/",
                "The dog is friendly.",
                "Animals",
                "Easy"
        ));

        vocabularyList.add(new Vocabulary(
                "4",
                "Cat",
                "Con mèo",
                "/kæt/",
                "The cat is sleeping.",
                "Animals",
                "Easy"
        ));

        vocabularyList.add(new Vocabulary(
                "5",
                "Teacher",
                "Giáo viên",
                "/ˈtiː.tʃər/",
                "My teacher is very kind.",
                "School",
                "Easy"
        ));

        vocabularyList.add(new Vocabulary(
                "6",
                "Student",
                "Học sinh",
                "/ˈstjuː.dənt/",
                "She is a good student.",
                "School",
                "Easy"
        ));

        vocabularyList.add(new Vocabulary(
                "7",
                "Airplane",
                "Máy bay",
                "/ˈeə.pleɪn/",
                "The airplane is flying.",
                "Travel",
                "Medium"
        ));

        vocabularyList.add(new Vocabulary(
                "8",
                "Beautiful",
                "Xinh đẹp",
                "/ˈbjuː.tɪ.fəl/",
                "The flower is beautiful.",
                "Adjectives",
                "Medium"
        ));

        vocabularyList.add(new Vocabulary(
                "9",
                "Hospital",
                "Bệnh viện",
                "/ˈhɒs.pɪ.təl/",
                "He works at a hospital.",
                "Places",
                "Medium"
        ));

        vocabularyList.add(new Vocabulary(
                "10",
                "Computer",
                "Máy tính",
                "/kəmˈpjuː.tər/",
                "I use a computer for studying.",
                "Technology",
                "Easy"
        ));
    }

    private void setupSearch() {
        searchVocabulary.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {
                        currentKeyword = query;
                        filterVocabulary(currentKeyword);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {
                        currentKeyword = newText;
                        filterVocabulary(currentKeyword);
                        return true;
                    }
                }
        );
    }
    private void setupFilterButton() {
        btnFilterVocabulary.setOnClickListener(view ->
                showFilterDialog()
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

        String[] categories = {
                "Tất cả",
                "Food",
                "Animals",
                "School",
                "Travel",
                "Adjectives",
                "Places",
                "Technology"
        };

        String[] levels = {
                "Tất cả",
                "Easy",
                "Medium",
                "Hard"
        };

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categories
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> levelAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        levels
                );

        levelAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerLevel.setAdapter(levelAdapter);

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

        new AlertDialog.Builder(requireContext())
                .setTitle("Lọc từ vựng")
                .setView(dialogView)
                .setPositiveButton(
                        "Áp dụng",
                        (dialog, which) -> {
                            selectedCategory =
                                    spinnerCategory
                                            .getSelectedItem()
                                            .toString();

                            selectedLevel =
                                    spinnerLevel
                                            .getSelectedItem()
                                            .toString();

                            updateFilterStatus();
                            filterVocabulary(currentKeyword);
                        }
                )
                .setNeutralButton(
                        "Xóa lọc",
                        (dialog, which) -> {
                            selectedCategory = "Tất cả";
                            selectedLevel = "Tất cả";

                            updateFilterStatus();
                            filterVocabulary(currentKeyword);
                        }
                )
                .setNegativeButton("Hủy", null)
                .show();
    }

    private int findArrayPosition(
            String[] values,
            String selectedValue
    ) {
        for (int index = 0;
             index < values.length;
             index++) {

            if (values[index].equals(selectedValue)) {
                return index;
            }
        }

        return 0;
    }
    private void filterVocabulary(String keyword) {
        filteredList.clear();

        String searchText =
                keyword == null
                        ? ""
                        : keyword.trim()
                          .toLowerCase(Locale.ROOT);

        for (Vocabulary vocabulary
                : vocabularyList) {

            String englishWord =
                    safeLower(
                            vocabulary.getEnglishWord()
                    );

            String vietnameseMeaning =
                    safeLower(
                            vocabulary.getVietnameseMeaning()
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
                            || category.contains(searchText)
                            || level.contains(searchText);

            boolean matchesCategory =
                    selectedCategory.equals("Tất cả")
                            || category.equals(
                            selectedCategory.toLowerCase(
                                    Locale.ROOT
                            )
                    );

            boolean matchesLevel =
                    selectedLevel.equals("Tất cả")
                            || level.equals(
                            selectedLevel.toLowerCase(
                                    Locale.ROOT
                            )
                    );

            if (matchesKeyword
                    && matchesCategory
                    && matchesLevel) {

                filteredList.add(vocabulary);
            }
        }

        vocabularyAdapter.notifyDataSetChanged();
    }
    private String safeLower(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT);
    }
    private void updateFilterStatus() {
        boolean hasFilter =
                !selectedCategory.equals("Tất cả")
                        || !selectedLevel.equals("Tất cả");

        if (!hasFilter) {
            tvFilterStatus.setVisibility(View.GONE);
            btnFilterVocabulary.setText("Lọc");
            return;
        }

        tvFilterStatus.setVisibility(View.VISIBLE);

        tvFilterStatus.setText(
                "Chủ đề: "
                        + selectedCategory
                        + " · Độ khó: "
                        + selectedLevel
        );

        btnFilterVocabulary.setText("Đang lọc");
    }
    // Hàm phụ trợ để ẩn/hiện Empty State UI
    private void updateEmptyStateVisibility() {
        if (emptyStateLayout != null && recyclerVocabulary != null) {
            if (filteredList.isEmpty()) {
                recyclerVocabulary.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            } else {
                recyclerVocabulary.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
            }
        }
    }
}