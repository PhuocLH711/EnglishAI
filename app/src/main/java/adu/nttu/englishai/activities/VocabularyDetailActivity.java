package adu.nttu.englishai.activities;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import adu.nttu.englishai.R;

// =========================================================================
// VOCABULARY DETAIL ACTIVITY: Màn hình chi tiết từ vựng, luyện phát âm & lưu tiến độ
// =========================================================================
public class VocabularyDetailActivity extends AppCompatActivity {

    // Đối tượng kết nối Firebase Authentication và cơ sở dữ liệu Cloud Firestore
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // Biến lưu trạng thái học tập hiện tại của từ (Mặc định là "NOT_STARTED" - Chưa học)
    private String learningStatus = "NOT_STARTED";

    // Các thẻ hiển thị thông tin chi tiết trên màn hình (UI Components)
    private TextView tvEnglishWord;
    private TextView tvPronunciation;
    private TextView tvMeaning;
    private TextView tvExample;
    private TextView tvCategory;
    private TextView tvLevel;

    // Các nút thao tác và cờ trạng thái
    private Button btnLearned;
    private boolean isLearned;
    private Button btnSpeak;
    private Button btnFavoriteDetail;

    // Bộ máy chuyển đổi văn bản thành giọng nói (Text-To-Speech Engine)
    private TextToSpeech textToSpeech;

    // Biến lưu trữ dữ liệu của từ vựng được truyền từ màn hình danh sách sang
    private String vocabularyId;
    private String englishWord;
    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary_detail);

        // Khởi tạo các dịch vụ Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Cấu hình thanh tiêu đề (Toolbar) và nút Mũi tên quay lại -> Gọi finish() để đóng màn hình
        MaterialToolbar toolbarDetail = findViewById(R.id.toolbarDetail);
        toolbarDetail.setNavigationOnClickListener(view -> finish());

        // Khởi tạo tuần tự: Giao diện -> Lấy dữ liệu Intent -> Bộ phát âm TTS -> Sự kiện nút bấm
        initViews();
        getVocabularyData();
        setupTextToSpeech();
        setupEvents();
    }

    // Ánh xạ các biến Java với ID thẻ trong file layout XML
    private void initViews() {
        tvEnglishWord = findViewById(R.id.tvDetailEnglishWord);
        tvPronunciation = findViewById(R.id.tvDetailPronunciation);
        tvMeaning = findViewById(R.id.tvDetailMeaning);
        tvExample = findViewById(R.id.tvDetailExample);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvLevel = findViewById(R.id.tvDetailLevel);

        btnSpeak = findViewById(R.id.btnSpeak);
        btnFavoriteDetail = findViewById(R.id.btnFavoriteDetail);
        btnLearned = findViewById(R.id.btnLearned);
    }

    // HÀM QUAN TRỌNG: Nhận dữ liệu từ vựng được truyền qua từ màn hình danh sách (VocabularyFragment)
    private void getVocabularyData() {
        // getIntent().getStringExtra(): Lấy các chuỗi dữ liệu đã được đóng gói trong Intent ở màn hình trước
        vocabularyId = getIntent().getStringExtra("id");
        englishWord = getIntent().getStringExtra("englishWord");

        String pronunciation =
                getIntent().getStringExtra("pronunciation");

        String meaning =
                getIntent().getStringExtra("vietnameseMeaning");

        String example =
                getIntent().getStringExtra("example");

        String category =
                getIntent().getStringExtra("category");

        String level =
                getIntent().getStringExtra("level");

        // Đẩy dữ liệu vừa lấy được hiển thị lên các thẻ TextView trên giao diện
        tvEnglishWord.setText(englishWord);
        tvPronunciation.setText(pronunciation);
        tvMeaning.setText(meaning);
        tvExample.setText("Ví dụ: " + example);
        tvCategory.setText("Chủ đề: " + category);
        tvLevel.setText("Mức độ: " + level);

        // Kiểm tra xem từ này người dùng đã học hay chưa từ cơ sở dữ liệu đám mây
        checkLearningStatusFromFirestore();
    }

    // Cấu hình bộ máy đọc giọng nói tiếng Anh (Text-to-Speech)
    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(
                this,
                status -> {
                    // Kiểm tra xem hệ thống Android có khởi tạo bộ TTS thành công hay không
                    if (status == TextToSpeech.SUCCESS) {
                        // Thiết lập ngôn ngữ đọc là Tiếng Anh chuẩn Mỹ (Locale.US)
                        int result = textToSpeech.setLanguage(Locale.US);

                        // Nếu điện thoại người dùng chưa tải gói ngôn ngữ Anh hoặc không hỗ trợ -> Báo lỗi
                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                            Toast.makeText(
                                    this,
                                    "Thiết bị chưa hỗ trợ giọng tiếng Anh",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    } else {
                        Toast.makeText(
                                this,
                                "Không thể khởi tạo phát âm",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // Gán sự kiện click cho các nút bấm trên giao diện
    private void setupEvents() {
        btnSpeak.setOnClickListener(view -> speakWord());             // Bấm loa để phát âm
        btnFavoriteDetail.setOnClickListener(view -> toggleFavorite()); // Thêm/Bỏ yêu thích
        btnLearned.setOnClickListener(view -> changeLearningStatus()); // Chuyển đổi trạng thái học
    }

    // Hàm thực hiện đọc từ tiếng Anh ra loa điện thoại
    private void speakWord() {
        if (textToSpeech != null
                && englishWord != null
                && !englishWord.isEmpty()) {

            // QUEUE_FLUSH: Nếu đang đọc từ trước đó, lập tức dừng lại và xóa hàng đợi để đọc ngay từ mới này
            textToSpeech.speak(
                    englishWord,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "vocabulary_word"
            );
        }
    }

    // HÀM QUAN TRỌNG: Chuyển đổi vòng lặp trạng thái học (Chưa học -> Đang học -> Đã học)
    private void changeLearningStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Bạn cần đăng nhập",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (vocabularyId == null || vocabularyId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy ID từ vựng",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // State Machine: Logic xoay vòng trạng thái mỗi lần người dùng bấm nút
        if ("NOT_STARTED".equals(learningStatus)) {
            learningStatus = "LEARNING";     // Chưa học -> Chuyển thành Đang học
        } else if ("LEARNING".equals(learningStatus)) {
            learningStatus = "LEARNED";      // Đang học -> Chuyển thành Đã học
        } else {
            learningStatus = "NOT_STARTED";  // Đã học -> Reset về Chưa học
        }

        // Lưu trạng thái mới lên cơ sở dữ liệu Cloud Firestore
        saveLearningStatus(currentUser.getUid());
    }

    // Hàm phụ: Lưu từ vựng vào bộ sưu tập "learnedWords" (Hệ thống tracking song song)
    private void saveLearnedWord(String userId) {
        Map<String, Object> learnedData = new HashMap<>();

        learnedData.put("id", vocabularyId);
        learnedData.put("englishWord", englishWord);
        learnedData.put(
                "vietnameseMeaning",
                tvMeaning.getText().toString()
        );
        learnedData.put(
                "pronunciation",
                tvPronunciation.getText().toString()
        );
        learnedData.put("learned", true);
        learnedData.put("learnedAt", System.currentTimeMillis());

        // Lưu vào đường dẫn: users -> {userId} -> learnedWords -> {vocabularyId}
        firestore.collection("users")
                .document(userId)
                .collection("learnedWords")
                .document(vocabularyId)
                .set(learnedData)
                .addOnSuccessListener(unused -> {
                    isLearned = true;
                    updateLearnedButton();
                    btnLearned.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Đã đánh dấu từ đã học",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    btnLearned.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Lưu trạng thái thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // HÀM QUAN TRỌNG: Ghi hoặc xóa trạng thái học tập trong bộ sưu tập "wordProgress" trên Cloud
    private void saveLearningStatus(String userId) {
        btnLearned.setEnabled(false); // Khóa nút trong lúc chờ mạng, tránh bấm liên tục

        // TỐI ƯU CƠ SỞ DỮ LIỆU: Nếu chuyển về trạng thái "Chưa học" -> Xóa luôn tài liệu khỏi Firestore
        // Giúp giải phóng dung lượng đám mây, không lưu trữ những dữ liệu không cần thiết
        if ("NOT_STARTED".equals(learningStatus)) {
            firestore.collection("users")
                    .document(userId)
                    .collection("wordProgress")
                    .document(vocabularyId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        updateLearningButton();
                        btnLearned.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Đã chuyển về chưa học",
                                Toast.LENGTH_SHORT
                        ).show();
                    })
                    .addOnFailureListener(exception -> {
                        btnLearned.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Cập nhật thất bại: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });

            return;
        }

        // Nếu là "LEARNING" hoặc "LEARNED" -> Đóng gói dữ liệu và ghi lên Firestore
        Map<String, Object> data = new HashMap<>();

        data.put("id", vocabularyId);
        data.put("englishWord", englishWord);
        data.put("vietnameseMeaning", tvMeaning.getText().toString());
        data.put("pronunciation", tvPronunciation.getText().toString());
        data.put("status", learningStatus);
        data.put("updatedAt", System.currentTimeMillis());

        // Ghi đè hoặc tạo mới tài liệu tại đường dẫn: users -> {userId} -> wordProgress -> {vocabularyId}
        firestore.collection("users")
                .document(userId)
                .collection("wordProgress")
                .document(vocabularyId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    updateLearningButton();
                    btnLearned.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Đã cập nhật trạng thái",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    btnLearned.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Cập nhật thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Truy vấn Cloud Firestore xem trước đó người dùng đã học từ này tới giai đoạn nào rồi
    private void checkLearningStatusFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null
                || vocabularyId == null
                || vocabularyId.isEmpty()) {

            learningStatus = "NOT_STARTED";
            updateLearningButton();
            return;
        }

        // Gọi truy vấn vào subcollection wordProgress của user hiện tại
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wordProgress")
                .document(vocabularyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status =
                                documentSnapshot.getString("status");

                        if (status != null) {
                            learningStatus = status;
                        } else {
                            learningStatus = "NOT_STARTED";
                        }
                    } else {
                        learningStatus = "NOT_STARTED";
                    }

                    updateLearningButton(); // Vẽ lại nút bấm theo dữ liệu tải về
                })
                .addOnFailureListener(exception -> {
                    learningStatus = "NOT_STARTED";
                    updateLearningButton();
                });
    }

    // Cập nhật giao diện của nút Trạng thái học (Đổi chữ và màu sắc icon)
    private void updateLearningButton() {
        switch (learningStatus) {
            case "LEARNING":
                btnLearned.setText("🟡 Đang học");
                break;

            case "LEARNED":
                btnLearned.setText("🟢 Đã học");
                break;

            default:
                btnLearned.setText("⚪ Chưa học");
                break;
        }
    }

    // Hàm phụ: Xóa từ khỏi bộ sưu tập "learnedWords"
    private void removeLearnedWord(String userId) {
        firestore.collection("users")
                .document(userId)
                .collection("learnedWords")
                .document(vocabularyId)
                .delete()
                .addOnSuccessListener(unused -> {
                    isLearned = false;
                    updateLearnedButton();
                    btnLearned.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Đã bỏ trạng thái đã học",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    btnLearned.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Không thể cập nhật trạng thái: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Xử lý logic đảo ngược trạng thái Yêu thích (Đang thích thì bỏ, chưa thích thì thêm)
    private void toggleFavorite() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Bạn cần đăng nhập",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (vocabularyId == null || vocabularyId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy ID từ vựng",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnFavoriteDetail.setEnabled(false);

        if (isFavorite) {
            removeFavorite(currentUser.getUid()); // Nếu đang yêu thích -> Gọi hàm xóa
        } else {
            saveFavorite(currentUser.getUid());   // Nếu chưa -> Gọi hàm lưu lên Cloud
        }
    }

    // Lưu từ vựng vào danh sách Yêu thích trên Cloud Firestore
    private void saveFavorite(String userId) {
        Map<String, Object> favoriteData = new HashMap<>();

        favoriteData.put("id", vocabularyId);
        favoriteData.put("englishWord", englishWord);
        favoriteData.put(
                "vietnameseMeaning",
                tvMeaning.getText().toString()
        );
        favoriteData.put(
                "pronunciation",
                tvPronunciation.getText().toString()
        );
        favoriteData.put("favorite", true);
        favoriteData.put("savedAt", System.currentTimeMillis());

        // Lưu vào đường dẫn: users -> {userId} -> favorites -> {vocabularyId}
        firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document(vocabularyId)
                .set(favoriteData)
                .addOnSuccessListener(unused -> {
                    isFavorite = true;
                    updateFavoriteButton();
                    btnFavoriteDetail.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Đã thêm vào yêu thích",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    btnFavoriteDetail.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Lưu yêu thích thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Xóa từ khỏi danh sách Yêu thích trên Cloud Firestore
    private void removeFavorite(String userId) {
        firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document(vocabularyId)
                .delete()
                .addOnSuccessListener(unused -> {
                    isFavorite = false;
                    updateFavoriteButton();
                    btnFavoriteDetail.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Đã bỏ yêu thích",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    btnFavoriteDetail.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Bỏ yêu thích thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Kiểm tra trạng thái yêu thích từ Firestore (Dành cho tracking hệ favorites)
    private void checkFavoriteFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null
                || vocabularyId == null
                || vocabularyId.isEmpty()) {

            isFavorite = false;
            updateFavoriteButton();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .document(vocabularyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isFavorite = documentSnapshot.exists(); // Nếu tài liệu tồn tại -> đã yêu thích
                    updateFavoriteButton();
                })
                .addOnFailureListener(exception -> {
                    isFavorite = false;
                    updateFavoriteButton();
                });
    }

    // Cập nhật câu chữ hiển thị trên nút Yêu thích
    private void updateFavoriteButton() {
        if (isFavorite) {
            btnFavoriteDetail.setText("★ Đã yêu thích");
        } else {
            btnFavoriteDetail.setText("☆ Thêm vào yêu thích");
        }
    }

    // Kiểm tra trạng thái đã học từ Firestore (Dành cho tracking hệ learnedWords)
    private void checkLearnedFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null
                || vocabularyId == null
                || vocabularyId.isEmpty()) {

            isLearned = false;
            updateLearnedButton();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("learnedWords")
                .document(vocabularyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLearned = documentSnapshot.exists();
                    updateLearnedButton();
                })
                .addOnFailureListener(exception -> {
                    isLearned = false;
                    updateLearnedButton();
                });
    }

    // Cập nhật giao diện nút đã học (của hệ thống tracking song song)
    private void updateLearnedButton() {
        if (isLearned) {
            btnLearned.setText("✓ Đã học");
        } else {
            btnLearned.setText("✓ Đánh dấu đã học");
        }
    }

    // =========================================================================
    // VÒNG ĐỜI ACTIVITY (LIFECYCLE) - CỰC KỲ QUAN TRỌNG ĐỂ CHỐNG LỖI BỘ NHỚ
    // =========================================================================
    @Override
    protected void onDestroy() {
        // Khi người dùng thoát khỏi màn hình chi tiết này -> Dừng lập tức bộ đọc giọng nói
        // Giải phóng bộ nhớ RAM và ngăn lỗi âm thanh vẫn tự phát ngầm khi đã thoát app
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }
}