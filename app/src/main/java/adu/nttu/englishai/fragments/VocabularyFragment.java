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
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

// =========================================================================
// VOCABULARY FRAGMENT: Màn hình Từ điển Từ vựng, Tìm kiếm & Lọc nâng cao
// =========================================================================
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

    // Các biến lưu trạng thái lọc hiện tại
    private String selectedCategory = "Tất cả";
    private String selectedLevel = "Tất cả";
    private String currentKeyword = "";

    public VocabularyFragment() {

    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Bơm bản thiết kế XML fragment_vocabulary thành đối tượng View
        return inflater.inflate(
                R.layout.fragment_vocabulary,
                container,
                false
        );
    }

    // =========================================================================
    // HÀM KHỞI TẠO LOGIC SAU KHI GIAO DIỆN ĐÃ TẠO XONG
    // =========================================================================
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

        // Cấu hình SearchView: Mở rộng ô tìm kiếm sẵn nhưng bỏ focus để bàn phím không tự bật lên gây choáng màn hình
        searchVocabulary.setIconifiedByDefault(false);
        searchVocabulary.setIconified(false);
        searchVocabulary.clearFocus();

        updateEmptyStateVisibility();

        loadVocabularyFromFirestore();
    }

    // =========================================================================
    // ĐỒNG BỘ TIẾN ĐỘ KHI QUAY LẠI MÀN HÌNH
    // =========================================================================
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
        // Khởi tạo Adapter với danh sách đã lọc và truyền Callback xử lý thả tim
        vocabularyAdapter = new VocabularyAdapter(
                filteredList,
                this::saveFavoriteStatus
        );

        recyclerVocabulary.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        // Tối ưu hóa hiệu năng: Báo cho Android biết kích thước các thẻ là cố định để cuộn mượt hơn
        recyclerVocabulary.setHasFixedSize(true);
        recyclerVocabulary.setAdapter(vocabularyAdapter);
    }

    // =========================================================================
    // HÀM 1: TẢI DANH SÁCH TỪ VỰNG CHUNG
    // =========================================================================
    private void loadVocabularyFromFirestore() {
        firestore.collection("vocabularies")
                .get()
                .addOnSuccessListener(snapshot -> {
                    vocabularyList.clear();

                    for (DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        // Chuyển đổi dữ liệu thô từ Firestore thành đối tượng Vocabulary
                        Vocabulary vocabulary =
                                createVocabularyFromDocument(document);

                        String englishWord =
                                vocabulary.getEnglishWord();

                        // Lập trình phòng vệ: Chỉ thêm các từ vựng có chuỗi tiếng Anh hợp lệ
                        if (englishWord != null
                                && !englishWord.trim().isEmpty()) {

                            vocabularyList.add(vocabulary);
                        }
                    }

                    // Sắp xếp danh sách từ vựng theo bảng chữ cái A-Z (không phân biệt hoa thường)
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
                    DataRepository.getInstance().setVocabularyList(vocabularyList);
                    // Khi đã có danh sách từ chung -> Gọi tiếp hàm tải tiến độ học cá nhân
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

    // =========================================================================
    // HÀM 2: PARSE DỮ LIỆU BAO DUNG
    // =========================================================================
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
                        "Khác" // Giá trị mặc định nếu thiếu
                )
        );

        vocabulary.setLevel(
                firstNonEmpty(
                        document.getString("level"),
                        "Easy" // Giá trị mặc định nếu thiếu
                )
        );

        vocabulary.setImageUrl(
                firstNonEmpty(
                        document.getString("imageUrl"),
                        document.getString("image")
                )
        );

        // Gán trạng thái ban đầu mặc định là chưa học và chưa yêu thích
        vocabulary.setFavorite(false);
        vocabulary.setLearningStatus(
                Vocabulary.STATUS_NOT_STARTED
        );

        return vocabulary;
    }

    // =========================================================================
    // HÀM 3: GHÉP NỐI DỮ LIỆU CÁ NHÂN VÀO TỪ VỰNG CHUNG
    // =========================================================================
    private void loadUserProgress() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        // Nếu khách chưa đăng nhập -> Đặt tất cả về mặc định và áp dụng bộ lọc hiển thị
        if (currentUser == null) {
            resetProgressToDefault();
            applyCurrentFilter();
            return;
        }

        // Truy vấn vào subcollection riêng của user
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wordProgress")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, DocumentSnapshot> progressMap =
                            new HashMap<>();

                    // Đưa toàn bộ tài liệu tiến độ vào Map với Key = ID từ vựng để tra cứu O(1)
                    for (DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        progressMap.put(
                                document.getId(),
                                document
                        );
                    }

                    /*
                     * INNER-JOIN TRONG BỘ NHỚ RAM:
                     * Duyệt qua toàn bộ danh sách từ vựng chung (vocabularyList),
                     * tra cứu trong progressMap xem người dùng có tiến độ học của từ này chưa.
                     * Nếu có -> Cập nhật trạng thái "Đang học/Đã học" và cờ "Yêu thích" vào đối tượng Vocabulary.
                     */
                    for (Vocabulary vocabulary
                            : vocabularyList) {

                        vocabulary.setFavorite(false);
                        vocabulary.setLearningStatus(
                                Vocabulary.STATUS_NOT_STARTED
                        );

                        DocumentSnapshot progress =
                                progressMap.get(vocabulary.getId());

                        if (progress == null) {
                            continue; // Chưa từng tương tác -> Bỏ qua, giữ nguyên
                        }

                        // Quét lấy trạng thái yêu thích
                        Boolean favorite =
                                firstBoolean(
                                        progress.getBoolean("favorite"),
                                        progress.getBoolean("isFavorite"),
                                        progress.getBoolean("isFav")
                                );

                        // Quét lấy trạng thái học tập
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

                    // Sau khi đã ghép nối đủ dữ liệu -> Chạy hàm lọc để hiển thị ra màn hình
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

    // =========================================================================
    // HÀM 4: LƯU YÊU THÍCH BẰNG KỸ THUẬT MERGE
    // =========================================================================
    private void saveFavoriteStatus(
            Vocabulary vocabulary,
            boolean newState
    ) {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            // Lỗi chưa đăng nhập -> Hoàn tác lại giao diện thả tim
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
                    // Nếu lỗi mạng -> Hoàn tác màu ngôi sao lại như cũ
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

    // =========================================================================
    // CẤU HÌNH TÌM KIẾM TRONG THỜI GIAN THỰC
    // =========================================================================
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
                        searchVocabulary.clearFocus(); // Ẩn bàn phím khi bấm Enter/Tìm

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {
                        // Kích hoạt lọc lập tức mỗi khi người dùng gõ hoặc xóa từng ký tự
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

    // =========================================================================
    // HÀM 5: HỘP THOẠI LỌC NÂNG CAO
    // =========================================================================
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

        // Tự động xây dựng danh sách Chủ đề từ kho dữ liệu đang có
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

        // Gán vị trí mặc định trên Spinner bằng với bộ lọc đang chọn hiện tại
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

        // Trang điểm màu chữ cho các nút trong AlertDialog theo chuẩn Material
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

    // =========================================================================
    // HÀM 6: TẠO DANH SÁCH CHỦ ĐỀ KHÔNG TRÙNG LẶP
    // =========================================================================
    private String[] buildCategoryArray() {
        // LinkedHashSet: Vừa giữ đúng thứ tự thêm vào, vừa tự động loại bỏ phần tử trùng lặp
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

        // Sắp xếp các chủ đề theo bảng chữ cái A-Z cho học viên dễ tìm
        Collections.sort(
                categories,
                String.CASE_INSENSITIVE_ORDER
        );

        categorySet.addAll(categories);

        return categorySet.toArray(new String[0]);
    }

    // =========================================================================
    // HÀM QUAN TRỌNG: THUẬT TOÁN LỌC TỔNG HỢP
    // =========================================================================
    private void applyCurrentFilter() {
        filteredList.clear();

        String searchText =
                safeLower(currentKeyword);

        /*
         * THUẬT TOÁN LỌC O(N) TỐI ƯU HIỆU NĂNG:
         * Duyệt qua toàn bộ từ vựng và kiểm tra đồng thời 3 điều kiện (AND Logic):
         * 1. matchesKeyword: Từ khóa tìm kiếm có nằm trong tiếng Anh, tiếng Việt, phiên âm, chủ đề, hoặc cấp độ không?
         * 2. matchesCategory: Có trùng với Chủ đề đang chọn trong Spinner không?
         * 3. matchesLevel: Có trùng với Cấp độ (Easy/Medium/Hard) đang chọn không?
         * -> Chỉ từ nào thỏa mãn CẢ 3 điều kiện mới được đưa vào filteredList để vẽ ra màn hình!
         */
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

        // Cập nhật số lượng tổng, báo Adapter vẽ lại, và kiểm tra bật/tắt Màn hình trống
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

    // Cập nhật câu nhãn hiển thị trạng thái lọc
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

    // Quản lý hiển thị Màn hình trống khi không tìm thấy kết quả
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

    // Hàm tiện ích: Đưa chuỗi về chữ thường an toàn với chuẩn quốc tế Locale.ROOT
    private String safeLower(
            String value
    ) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    // Hàm tiện ích: Trả về chuỗi không rỗng đầu tiên tìm thấy
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

    // Hàm tiện ích: Trả về giá trị Boolean không null đầu tiên
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