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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import adu.nttu.englishai.R;
import adu.nttu.englishai.activities.LoginActivity;

// =========================================================================
// PROFILE FRAGMENT: Hồ sơ cá nhân + Thống kê Real-time + Quản lý tài khoản
// =========================================================================
public class ProfileFragment extends Fragment {

    private static final String TAG = "PROFILE_FRAGMENT";

    // Các hằng số định nghĩa trạng thái học tập của từ vựng
    private static final String STATUS_NOT_STARTED = "NOT_STARTED"; // Chưa học
    private static final String STATUS_LEARNING = "LEARNING";       // Đang học
    private static final String STATUS_LEARNED = "LEARNED";         // Đã thuộc (Mastered)

    // Các đối tượng kết nối Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // Các thành phần giao diện (UI) hiển thị thông tin người dùng
    private TextView tvProfileName;
    private TextView tvProfileEmail;

    // Các thành phần giao diện hiển thị số liệu thống kê
    private TextView tvTotalVocabulary;
    private TextView tvNotStartedCount;
    private TextView tvLearningCount;
    private TextView tvLearnedCount;
    private TextView tvFavoriteCount;

    // Bộ nhớ đệm (RAM Cache) để lưu trữ và tính toán dữ liệu Real-time
    private final Set<String> validVocabularyIds = new HashSet<>();
    private final Map<String, DocumentSnapshot> vocabularyMap = new HashMap<>();
    private final Map<String, DocumentSnapshot> progressMap = new HashMap<>();

    private ListenerRegistration vocabularyListener;
    private ListenerRegistration progressListener;

    public ProfileFragment() {

    }

   // Khởi tạo và nạp giao diện XML (fragment_profile) vào bộ nhớ.
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

        // Khởi tạo các dịch vụ Firebase Auth và Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        // Ánh xạ
        initViews(view);
        setupEvents(view);
        loadUserInformation();
        startRealtimeStatistics();
    }

    private void initViews(View view) {
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);

        tvTotalVocabulary = view.findViewById(R.id.tvTotalVocabulary);
        tvNotStartedCount = view.findViewById(R.id.tvNotStartedCount);
        tvLearningCount = view.findViewById(R.id.tvLearningCount);

        // Sử dụng ID chuẩn từ giao diện Progress: R.id.tvMasteredCount
        tvLearnedCount = view.findViewById(R.id.tvMasteredCount);

        tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount);
    }

        // Hàm cài đặt sự kiện click cho các thẻ thống kê và nút cài đặt.
    private void setupEvents(View view) {
        // 1. Sự kiện bấm vào các Thẻ thống kê -> Mở BottomSheet danh sách từ vựng theo trạng thái tương ứng
        MaterialCardView cardMastered = view.findViewById(R.id.cardMastered);
        MaterialCardView cardLearning = view.findViewById(R.id.cardLearning);
        MaterialCardView cardNotStarted = view.findViewById(R.id.cardNotStarted);
        MaterialCardView cardFavorite = view.findViewById(R.id.cardFavorite);

        if (cardMastered != null) {
            cardMastered.setOnClickListener(v -> showWordListBottomSheet("🟢 Từ vựng đã thuộc", "learned"));
        }
        if (cardLearning != null) {
            cardLearning.setOnClickListener(v -> showWordListBottomSheet("🟡 Từ vựng đang học", "learning"));
        }
        if (cardNotStarted != null) {
            cardNotStarted.setOnClickListener(v -> showWordListBottomSheet("⚪ Từ vựng chưa học", "not_started"));
        }
        if (cardFavorite != null) {
            cardFavorite.setOnClickListener(v -> showWordListBottomSheet("❤️ Từ vựng yêu thích", "favorite"));
        }

        // 2. Sự kiện bấm Nút Cài đặt -> Mở BottomSheet Cài đặt & Đăng xuất
        MaterialCardView btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showSettingsBottomSheet());
        }
    }

    // =========================================================================
    // TẢI THÔNG TIN HỒ SƠ & TỰ ĐỘNG TẠO TÊN FALLBACK
    // =========================================================================
    private void loadUserInformation() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        // Nếu chưa đăng nhập -> Hiển thị chế độ Khách và reset thống kê về 0
        if (currentUser == null) {
            if (tvProfileName != null) tvProfileName.setText("Khách");
            if (tvProfileEmail != null) tvProfileEmail.setText("Chưa đăng nhập");
            resetStatistics();
            return;
        }

        String authName = currentUser.getDisplayName();
        String authEmail = currentUser.getEmail();

        if (tvProfileEmail != null) {
            tvProfileEmail.setText(authEmail == null ? "" : authEmail);
        }

        // Xử lý Fallback Tên hiển thị ngay lập tức từ dữ liệu Auth
        if (tvProfileName != null) {
            if (authName != null && !authName.trim().isEmpty()) {
                tvProfileName.setText(authName.trim());
            } else if (authEmail != null && !authEmail.trim().isEmpty()) {
                // Cắt phần tên trước @ của Email để làm tên tạm (vd: test@gmail.com -> Test)
                String fallbackName = authEmail.contains("@") ? authEmail.substring(0, authEmail.indexOf("@")) : authEmail;
                if (!fallbackName.isEmpty()) {
                    fallbackName = fallbackName.substring(0, 1).toUpperCase(Locale.ROOT) + fallbackName.substring(1);
                }
                tvProfileName.setText(fallbackName);
            } else {
                tvProfileName.setText("Học viên EnglishAI");
            }
        }

        // Tải thêm dữ liệu chính xác nhất từ Firestore
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!isAdded()) return; // Kiểm tra Fragment còn gắn với Activity không

                    // Thử tìm tên theo các trường phổ biến: name, fullName, username
                    String firestoreName = firstNonEmpty(
                            document.getString("name"),
                            document.getString("fullName"),
                            document.getString("username")
                    );
                    String firestoreEmail = document.getString("email");

                    if (tvProfileName != null && !firestoreName.isEmpty()) {
                        tvProfileName.setText(firestoreName);
                    }
                    if (tvProfileEmail != null && firestoreEmail != null && !firestoreEmail.trim().isEmpty()) {
                        tvProfileEmail.setText(firestoreEmail.trim());
                    }
                });
    }

    // =========================================================================
    // LẮNG NGHE DỮ LIỆU THỜI GIAN THỰC
    // =========================================================================
    private void startRealtimeStatistics() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            resetStatistics();
            return;
        }

        stopRealtimeListeners(); // Dừng các listener cũ trước khi tạo mới để tránh trùng lặp
        listenToVocabularyCollection();
        listenToWordProgress(currentUser.getUid());
    }

        // Hàm lắng nghe collection vocabularies từ Firestore.
    private void listenToVocabularyCollection() {
        vocabularyListener = firestore.collection("vocabularies")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        Log.e(TAG, "Lỗi tải danh sách từ vựng", error);
                        return;
                    }

                    validVocabularyIds.clear();
                    vocabularyMap.clear();

                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            validVocabularyIds.add(document.getId());
                            vocabularyMap.put(document.getId(), document);
                        }
                    }
                    calculateAndDisplayStatistics(); // Tính toán lại số liệu ngay khi có dữ liệu mới
                });
    }

        // Hàm lắng nghe sub-collection wordProgress của học viên từ Firestore.
    private void listenToWordProgress(String userId) {
        progressListener = firestore.collection("users")
                .document(userId)
                .collection("wordProgress")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        Log.e(TAG, "Lỗi tải tiến độ học viên", error);
                        return;
                    }

                    progressMap.clear();

                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            progressMap.put(document.getId(), document);
                        }
                    }
                    calculateAndDisplayStatistics(); // Tính toán lại số liệu ngay khi tiến độ thay đổi
                });
    }

    // =========================================================================
    // THUẬT TOÁN INNER-JOIN TRONG RAM & TÍNH TOÁN THỐNG KÊ
    // =========================================================================

    private void calculateAndDisplayStatistics() {
        int totalCount = validVocabularyIds.size();
        int notStartedCount = 0;
        int learningCount = 0;
        int learnedCount = 0;
        int favoriteCount = 0;

        // Duyệt qua tất cả ID từ vựng hợp lệ có trong hệ thống
        for (String vocabularyId : validVocabularyIds) {
            DocumentSnapshot progressDocument = progressMap.get(vocabularyId);

            // Nếu user chưa có bản ghi tiến độ của từ này -> Tính là Chưa học
            if (progressDocument == null || !progressDocument.exists()) {
                notStartedCount++;
                continue;
            }

            // Lấy trạng thái học, ưu tiên trường "learningStatus" rồi đến "status"
            String status = firstNonEmpty(
                    progressDocument.getString("learningStatus"),
                    progressDocument.getString("status")
            );
            status = normalizeLearningStatus(status); // Chuẩn hóa chuỗi trạng thái

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
            Boolean favorite = progressDocument.getBoolean("favorite");
            if (favorite != null && favorite) {
                favoriteCount++;
            }
        }

        updateStatistics(totalCount, notStartedCount, learningCount, learnedCount, favoriteCount);
    }

      // Hàm cập nhật các số liệu thống kê đã tính toán lên các TextView trên màn hình.
    private void updateStatistics(int total, int notStarted, int learning, int learned, int favorite) {
        if (!isAdded() || getView() == null) return;

        if (tvTotalVocabulary != null) tvTotalVocabulary.setText(String.valueOf(total));
        if (tvNotStartedCount != null) tvNotStartedCount.setText(String.valueOf(notStarted));
        if (tvLearningCount != null) tvLearningCount.setText(String.valueOf(learning));
        if (tvLearnedCount != null) tvLearnedCount.setText(String.valueOf(learned));
        if (tvFavoriteCount != null) tvFavoriteCount.setText(String.valueOf(favorite));
    }

  // Hàm xóa trắng bộ nhớ đệm và đặt tất cả số liệu thống kê về 0 (dùng khi đăng xuất hoặc lỗi).
    private void resetStatistics() {
        validVocabularyIds.clear();
        vocabularyMap.clear();
        progressMap.clear();
        updateStatistics(0, 0, 0, 0, 0);
    }

    // =========================================================================
    // HỘP THOẠI BOTTOM SHEET: HIỂN THỊ DANH SÁCH TỪ VỰNG TỪ RAM
    // =========================================================================
    private void showWordListBottomSheet(String title, String filterType) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_word_list, null);
        dialog.setContentView(dialogView);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
        LinearLayout layoutWordContainer = dialogView.findViewById(R.id.layoutWordContainer);

        if (tvDialogTitle != null) tvDialogTitle.setText(title);
        if (btnCloseDialog != null) btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        if (layoutWordContainer != null) {
            layoutWordContainer.removeAllViews(); // Xóa các view cũ trước khi render danh sách mới
            int matchCount = 0;

            // Lọc và render từng từ vựng thỏa mãn điều kiện
            for (String vocabularyId : validVocabularyIds) {
                DocumentSnapshot vocabularyDocument = vocabularyMap.get(vocabularyId);
                DocumentSnapshot progressDocument = progressMap.get(vocabularyId);

                String status = STATUS_NOT_STARTED;
                boolean favorite = false;

                if (progressDocument != null && progressDocument.exists()) {
                    status = normalizeLearningStatus(firstNonEmpty(
                            progressDocument.getString("learningStatus"),
                            progressDocument.getString("status")
                    ));
                    Boolean favVal = progressDocument.getBoolean("favorite");
                    favorite = (favVal != null && favVal);
                }

                // Nếu từ vựng không khớp với bộ lọc -> Bỏ qua
                if (!matchesFilter(filterType, status, favorite) || vocabularyDocument == null) {
                    continue;
                }

                // Lấy từ tiếng Anh, phiên âm và nghĩa tiếng Việt
                String word = firstNonEmpty(
                        vocabularyDocument.getString("englishWord"),
                        vocabularyDocument.getString("word")
                );
                String meaning = firstNonEmpty(
                        vocabularyDocument.getString("vietnameseMeaning"),
                        vocabularyDocument.getString("meaning")
                );
                String pronunciation = firstNonEmpty(
                        vocabularyDocument.getString("pronunciation"),
                        vocabularyDocument.getString("phonetic")
                );

                if (!word.isEmpty()) {
                    addWordCardToContainer(layoutWordContainer, word, pronunciation, meaning);
                    matchCount++;
                }
            }

            // Nếu không có từ nào khớp -> Hiển thị thông báo danh sách trống
            if (matchCount == 0) {
                showEmptyMessage(layoutWordContainer);
            }
        }
        dialog.show();
    }

   // Hàm kiểm tra xem một từ vựng có thỏa mãn điều kiện của bộ lọc đang chọn hay không.
    private boolean matchesFilter(String filterType, String status, boolean favorite) {
        if ("favorite".equals(filterType)) return favorite;
        if ("learned".equals(filterType)) return STATUS_LEARNED.equals(status);
        if ("learning".equals(filterType)) return STATUS_LEARNING.equals(status);
        if ("not_started".equals(filterType)) return STATUS_NOT_STARTED.equals(status);
        return false;
    }

   // Hàm tạo giao diện Thẻ từ vựng (MaterialCardView) bằng code Java và thêm vào danh sách BottomSheet.
    private void addWordCardToContainer(LinearLayout container, String word, String phonetic, String meaning) {
        if (getContext() == null) return;

        // Khởi tạo thẻ CardView bên ngoài
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(Color.parseColor("#F8F9FA"));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(Color.parseColor("#E9ECEF"));

        // Khởi tạo Layout chứa nội dung bên trong thẻ
        LinearLayout innerLayout = new LinearLayout(requireContext());
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        // Dòng 1: Từ vựng Tiếng Anh + Phiên âm
        TextView tvWord = new TextView(requireContext());
        String wordTitle = word + (phonetic != null && !phonetic.trim().isEmpty() ? "  " + phonetic.trim() : "");
        tvWord.setText(wordTitle);
        tvWord.setTextSize(17f);
        tvWord.setTypeface(null, android.graphics.Typeface.BOLD);
        tvWord.setTextColor(Color.parseColor("#1A73E8"));

        // Dòng 2: Nghĩa Tiếng Việt
        TextView tvMeaning = new TextView(requireContext());
        tvMeaning.setText(meaning == null || meaning.trim().isEmpty() ? "👉 Chưa có nghĩa tiếng Việt" : "👉 Nghĩa: " + meaning.trim());
        tvMeaning.setTextSize(14f);
        tvMeaning.setTextColor(Color.parseColor("#37474F"));
        tvMeaning.setPadding(0, dpToPx(6), 0, 0);

        // Ghép các thành phần lại và thêm vào container chính
        innerLayout.addView(tvWord);
        innerLayout.addView(tvMeaning);
        card.addView(innerLayout);
        container.addView(card);
    }

    // Hàm hiển thị thông báo giao diện khi danh sách từ vựng lọc được bị trống.
    private void showEmptyMessage(LinearLayout container) {
        if (getContext() == null || container == null) return;

        TextView tvEmpty = new TextView(requireContext());
        tvEmpty.setText("📭 Chưa có từ vựng nào trong mục này.\nHãy qua trang Từ vựng để học nhé!");
        tvEmpty.setTextSize(15f);
        tvEmpty.setTextColor(Color.parseColor("#757575"));
        tvEmpty.setGravity(Gravity.CENTER);
        tvEmpty.setPadding(dpToPx(20), dpToPx(40), dpToPx(20), dpToPx(40));
        container.addView(tvEmpty);
    }

    // =========================================================================
    // HỘP THOẠI CÀI ĐẶT & ĐĂNG XUẤT AN TOÀN
    // =========================================================================
    private void showSettingsBottomSheet() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        dialog.setContentView(dialogView);

        LinearLayout itemAvatar = dialogView.findViewById(R.id.itemChangeAvatar);
        LinearLayout itemInfo = dialogView.findViewById(R.id.itemUpdateInfo);
        LinearLayout itemHelp = dialogView.findViewById(R.id.itemHelp);
        Button dialogBtnLogout = dialogView.findViewById(R.id.dialogBtnLogout);

        if (itemAvatar != null) {
            itemAvatar.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Tính năng đổi ảnh đang được cập nhật", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }
        if (itemInfo != null) {
            itemInfo.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Tính năng sửa thông tin đang được cập nhật", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }
        if (itemHelp != null) {
            itemHelp.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Hãy qua trang Từ vựng để bắt đầu học", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            });
        }

        // Xử lý sự kiện bấm nút Đăng xuất
        if (dialogBtnLogout != null) {
            dialogBtnLogout.setOnClickListener(v -> {
                dialog.dismiss();
                if (firebaseAuth != null) firebaseAuth.signOut(); // Đăng xuất khỏi Firebase Auth

                // Chuyển hướng về màn hình Đăng nhập và xóa hết lịch sử các màn hình trước đó
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }
        dialog.show();
    }

    // =========================================================================
    // CÁC HÀM TIỆN ÍCH & QUẢN LÝ VÒNG ĐỜI
    // =========================================================================
   // Hàm tiện ích: Chuẩn hóa chuỗi trạng thái học từ DB về các hằng số chuẩn của Fragment
    private String normalizeLearningStatus(String status) {
        if (status == null || status.trim().isEmpty()) return STATUS_NOT_STARTED;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("LEARNED".equals(normalized) || "MASTERED".equals(normalized)) return STATUS_LEARNED;
        if ("LEARNING".equals(normalized)) return STATUS_LEARNING;
        return STATUS_NOT_STARTED;
    }

    // Hàm tiện ích: Trả về giá trị chuỗi đầu tiên không bị null hoặc rỗng trong danh sách các chuỗi truyền vào.
    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    //Hàm tiện ích: Chuyển đổi đơn vị đo lường từ dp  sang px
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Hàm hủy bỏ (remove) các Listener lắng nghe Realtime từ Firebase
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

    // Hàm vòng đời Fragment: Được gọi khi View của Fragment bị hủy (VD: Chuyển tab hoặc tắt app).
    @Override
    public void onDestroyView() {
        stopRealtimeListeners();
        super.onDestroyView();
    }
}