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

// =========================================================================
// PROFILE FRAGMENT: Màn hình Hồ sơ cá nhân & Thống kê tiến độ Real-time
// =========================================================================
public class ProfileFragment extends Fragment {

    // Hằng số định danh trạng thái học tập chuẩn hóa
    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_LEARNING = "LEARNING";
    private static final String STATUS_LEARNED = "LEARNED";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // Các thành phần giao diện (UI Components)
    private TextView tvProfileName;
    private TextView tvProfileEmail;

    private TextView tvTotalVocabulary;
    private TextView tvNotStartedCount;
    private TextView tvLearningCount;
    private TextView tvLearnedCount;
    private TextView tvFavoriteCount;

    private MaterialButton btnOpenImportVocabulary;

    /*
     * CƠ CHẾ BẢO VỆ TOÀN VẸN DỮ LIỆU (DATA INTEGRITY & GHOST RECORD PREVENTER):
     * Chứa ID các từ đang tồn tại thật trong collection vocabularies.
     * Nhờ đó các document cũ như 1, 2, 5, 6, 7 sẽ không bị tính nhầm.
     * - Dùng Set<String> (HashSet) giúp tìm kiếm (contains) với độ phức tạp thời gian O(1) cực kỳ siêu tốc!
     */
    private final Set<String> validVocabularyIds = new HashSet<>();

    /*
     * Lưu tạm dữ liệu tiến độ hiện tại của người dùng vào bộ nhớ RAM.
     * - Dùng Map<String, DocumentSnapshot> để tra cứu tiến độ của một từ vựng chỉ mất O(1) thời gian.
     */
    private final Map<String, DocumentSnapshot> wordProgressMap =
            new HashMap<>();

    // Các biến lưu trữ bộ lắng nghe Realtime của Firebase (dùng để hủy lắng nghe khi rời màn hình)
    private ListenerRegistration vocabularyListener;
    private ListenerRegistration progressListener;

    public ProfileFragment() {
        // Constructor rỗng bắt buộc cho Fragment theo chuẩn Android.
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Bơm (inflate) bản thiết kế XML fragment_profile thành đối tượng View
        return inflater.inflate(
                R.layout.fragment_profile,
                container,
                false
        );
    }

    // =========================================================================
    // HÀM KHỞI TẠO LOGIC SAU KHI GIAO DIỆN ĐÃ TẠO XONG (ON VIEW CREATED)
    // =========================================================================
    // Kỹ thuật chuẩn Android: Xử lý ánh xạ và gọi API ở onViewCreated để đảm bảo View đã tồn tại an toàn
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

    // Ánh xạ các biến Java với ID thẻ trong XML
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

    // Gán sự kiện cho nút Mở trang Nhập dữ liệu từ vựng (ImportVocabularyActivity)
    private void setupEvents() {
        btnOpenImportVocabulary.setOnClickListener(view -> {
            Intent intent = new Intent(
                    requireContext(),
                    ImportVocabularyActivity.class
            );

            startActivity(intent);
        });
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 1: TẢI THÔNG TIN HỒ SƠ NGƯỜI DÙNG (TWO-TIER LOADING)
    // =========================================================================
    private void loadUserInformation() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            tvProfileName.setText("Khách");
            tvProfileEmail.setText("Chưa đăng nhập");
            resetStatistics();
            return;
        }

        // Bước 1: HIỂN THỊ TỨC THÌ từ bộ nhớ đệm Authentication (Không cần chờ mạng)
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

        // Bước 2: TRUY VẤN SÂU lên Firestore để lấy thông tin mới nhất (Nếu có thay đổi từ Admin/Thiết bị khác)
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    // LẬP TRÌNH PHÒNG VỆ (UI Defensive Check):
                    // Kiểm tra xem Fragment này còn gắn với Activity không.
                    // Nếu người dùng vừa mở Profile rồi lập tức bấm nút Back thoát ra trước khi mạng tải xong,
                    // việc gọi setText() sẽ gây lỗi crash app (IllegalStateException). Lệnh !isAdded() giúp chặn đứng lỗi này!
                    if (!isAdded()) {
                        return;
                    }

                    // Sử dụng hàm tiện ích firstNonEmpty để quét cả 3 tên cột phổ biến (Tín năng chịu lỗi NoSQL)
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

    // =========================================================================
    // HÀM QUAN TRỌNG 2: BẮT ĐẦU ĐỒNG BỘ SỐ LIỆU THỜI GIAN THỰC (REALTIME STATS)
    // =========================================================================
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

        // Hủy các listener cũ nếu có trước khi tạo mới để tránh bị nhân đôi luồng lắng nghe
        stopRealtimeListeners();

        // Lắng nghe song song 2 nguồn dữ liệu
        listenToVocabularyCollection();
        listenToWordProgress(currentUser.getUid());
    }

    /**
     * Nghe realtime collection vocabularies.
     */
    private void listenToVocabularyCollection() {
        // addSnapshotListener: Thay vì gọi .get() 1 lần, lệnh này duy trì kết nối mạng liên tục với Firebase.
        // Bất cứ khi nào có từ vựng được thêm mới, sửa, hoặc xóa trên Cloud, block code này sẽ lập tức tự động chạy lại!
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

                            // Gom toàn bộ ID từ vựng đang có thật trong hệ thống vào Set
                            validVocabularyIds.add(
                                    document.getId()
                            );
                        }
                    }

                    // Tự động tính toán và vẽ lại số liệu lên màn hình
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

                            // Đưa toàn bộ tài liệu tiến độ của user vào Map theo cặp: Key=vocabularyId, Value=DocumentSnapshot
                            wordProgressMap.put(
                                    document.getId(),
                                    document
                            );
                        }
                    }

                    // Tự động tính toán và vẽ lại số liệu lên màn hình
                    calculateAndDisplayStatistics();
                });
    }

    // =========================================================================
    // HÀM QUAN TRỌNG NHẤT: THUẬT TOÁN KẾT HỢP DỮ LIỆU IN-MEMORY (RAM INNER JOIN)
    // =========================================================================
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
         * THUẬT TOÁN ĐỐI CHIẾU SIÊU TỐC (O(N) Complexity):
         * Chỉ duyệt qua các ID từ vựng ĐANG TỒN TẠI THẬT trong validVocabularyIds.
         * LÝ DO: Trong quá trình học, học viên có thể từng học các từ cũ (ví dụ ID 1, 2, 5...)
         * nhưng sau đó Admin đã xóa các từ đó khỏi hệ thống. Nếu duyệt trực tiếp bảng wordProgress
         * thì sẽ bị đếm nhầm những "tài liệu ma" (ghost records) đó.
         * Cách thiết kế này giúp số liệu thống kê luôn chính xác tuyệt đối 100%!
         */
        for (String vocabularyId : validVocabularyIds) {
            // Tra cứu O(1) trong Map bộ nhớ RAM
            DocumentSnapshot progressDocument =
                    wordProgressMap.get(vocabularyId);

            // Nếu người dùng chưa từng tương tác với từ này -> Tính là "Chưa học"
            if (progressDocument == null
                    || !progressDocument.exists()) {

                notStartedCount++;
                continue;
            }

            // Lấy trạng thái học tập (Bao dung cả 2 trường hợp tên cột: learningStatus hoặc status)
            String status = firstNonEmpty(
                    progressDocument.getString(
                            "learningStatus"
                    ),
                    progressDocument.getString("status")
            );

            // Chuẩn hóa chuỗi về dạng viết hoa chuẩn mực
            status = normalizeLearningStatus(status);

            // Phân loại đếm số lượng
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

            // Kiểm tra trạng thái yêu thích
            Boolean favorite =
                    progressDocument.getBoolean("favorite");

            if (favorite != null && favorite) {
                favoriteCount++;
            }
        }

        // Đẩy số liệu đã tính toán ra giao diện
        updateStatistics(
                totalCount,
                notStartedCount,
                learningCount,
                learnedCount,
                favoriteCount
        );
    }

    // Cập nhật các thẻ con số thống kê trên màn hình
    private void updateStatistics(
            int totalCount,
            int notStartedCount,
            int learningCount,
            int learnedCount,
            int favoriteCount
    ) {
        // Kiểm tra an toàn: Đảm bảo Fragment vẫn đang gắn với Activity và View chưa bị hủy
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

    // Đặt lại toàn bộ số liệu về 0 (Dùng khi đăng xuất hoặc chưa có dữ liệu)
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

    // Chuẩn hóa các biến thể chuỗi trạng thái từ NoSQL về 3 dạng chuẩn của hệ thống
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

    // Hàm tiện ích: Trả về chuỗi không rỗng đầu tiên tìm thấy trong danh sách tham số (Varargs)
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

    // =========================================================================
    // QUẢN LÝ VÒNG ĐỜI & CHỐNG RÒ RỈ BỘ NHỚ (MEMORY LEAK PREVENTION)
    // =========================================================================
    private void stopRealtimeListeners() {
        if (vocabularyListener != null) {
            vocabularyListener.remove(); // Hủy kết nối lắng nghe Realtime với Firebase
            vocabularyListener = null;
        }

        if (progressListener != null) {
            progressListener.remove();
            progressListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        /*
         * LỆNH CỰC KỲ QUAN TRỌNG TRONG FRAGMENT:
         * Ngay khi người dùng rời khỏi màn hình Hồ sơ (ví dụ bấm sang tab Trang chủ hay Trò chơi),
         * ta BẮT BUỘC phải gọi stopRealtimeListeners() để ngắt kết nối với máy chủ Firebase.
         * NẾU KHÔNG GỌI: Các listener này sẽ tiếp tục chạy ngầm vô tận phía sau,
         * gây hao pin, tốn lưu lượng mạng internet, rò rỉ bộ nhớ RAM (Memory Leak),
         * và thậm chí gây CRASH APP nếu mạng tải về và cố gắng cập nhật giao diện đã bị tiêu hủy!
         */
        stopRealtimeListeners();
        super.onDestroyView();
    }
}