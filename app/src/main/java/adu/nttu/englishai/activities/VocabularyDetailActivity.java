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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import adu.nttu.englishai.R;

public class VocabularyDetailActivity extends AppCompatActivity {

    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_LEARNING = "LEARNING";
    private static final String STATUS_LEARNED = "LEARNED";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextView tvEnglishWord;
    private TextView tvPronunciation;
    private TextView tvMeaning;
    private TextView tvExample;
    private TextView tvCategory;
    private TextView tvLevel;

    private Button btnSpeak;
    private Button btnFavoriteDetail;
    private Button btnLearned;

    private TextToSpeech textToSpeech;

    private String vocabularyId = "";
    private String englishWord = "";
    private String vietnameseMeaning = "";
    private String pronunciation = "";

    private String learningStatus = STATUS_NOT_STARTED;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary_detail);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        getVocabularyData();
        setupTextToSpeech();
        setupEvents();

        loadWordProgress();
    }

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

    private void setupToolbar() {
        MaterialToolbar toolbarDetail =
                findViewById(R.id.toolbarDetail);

        toolbarDetail.setNavigationOnClickListener(
                view -> finish()
        );
    }

    private void getVocabularyData() {
        vocabularyId = safeText(
                getIntent().getStringExtra("id")
        );

        englishWord = safeText(
                getIntent().getStringExtra("englishWord")
        );

        vietnameseMeaning = safeText(
                getIntent().getStringExtra("vietnameseMeaning")
        );

        pronunciation = safeText(
                getIntent().getStringExtra("pronunciation")
        );

        String example = safeText(
                getIntent().getStringExtra("example")
        );

        String category = safeText(
                getIntent().getStringExtra("category")
        );

        String level = safeText(
                getIntent().getStringExtra("level")
        );

        tvEnglishWord.setText(englishWord);
        tvPronunciation.setText(pronunciation);
        tvMeaning.setText(vietnameseMeaning);

        tvExample.setText(
                example.isEmpty()
                        ? "Ví dụ: Chưa có ví dụ"
                        : "Ví dụ: " + example
        );

        tvCategory.setText(
                category.isEmpty()
                        ? "Chủ đề: Khác"
                        : "Chủ đề: " + category
        );

        tvLevel.setText(
                level.isEmpty()
                        ? "Mức độ: Easy"
                        : "Mức độ: " + level
        );
    }

    private void setupEvents() {
        btnSpeak.setOnClickListener(
                view -> speakWord()
        );

        btnFavoriteDetail.setOnClickListener(
                view -> toggleFavorite()
        );

        btnLearned.setOnClickListener(
                view -> changeLearningStatus()
        );
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(
                this,
                status -> {
                    if (status != TextToSpeech.SUCCESS) {
                        Toast.makeText(
                                this,
                                "Không thể khởi tạo phát âm",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    int result = textToSpeech.setLanguage(
                            Locale.US
                    );

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                        Toast.makeText(
                                this,
                                "Thiết bị chưa hỗ trợ giọng tiếng Anh",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void speakWord() {
        if (textToSpeech == null
                || englishWord.isEmpty()) {

            return;
        }

        textToSpeech.speak(
                englishWord,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "vocabulary_word"
        );
    }

    /**
     * Đọc trạng thái học và yêu thích từ cùng một document.
     */
    private void loadWordProgress() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null
                || vocabularyId.isEmpty()) {

            learningStatus = STATUS_NOT_STARTED;
            isFavorite = false;

            updateLearningButton();
            updateFavoriteButton();

            return;
        }

        setButtonsEnabled(false);

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wordProgress")
                .document(vocabularyId)
                .get()
                .addOnSuccessListener(document -> {
                    readProgressDocument(document);

                    updateLearningButton();
                    updateFavoriteButton();
                    setButtonsEnabled(true);
                })
                .addOnFailureListener(exception -> {
                    learningStatus = STATUS_NOT_STARTED;
                    isFavorite = false;

                    updateLearningButton();
                    updateFavoriteButton();
                    setButtonsEnabled(true);

                    Toast.makeText(
                            this,
                            "Không tải được trạng thái từ vựng",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void readProgressDocument(
            DocumentSnapshot document
    ) {
        if (!document.exists()) {
            learningStatus = STATUS_NOT_STARTED;
            isFavorite = false;
            return;
        }

        String savedStatus =
                document.getString("learningStatus");

        // Hỗ trợ dữ liệu cũ đang lưu với tên status.
        if (savedStatus == null
                || savedStatus.trim().isEmpty()) {

            savedStatus = document.getString("status");
        }

        learningStatus =
                normalizeLearningStatus(savedStatus);

        Boolean savedFavorite =
                document.getBoolean("favorite");

        isFavorite =
                savedFavorite != null && savedFavorite;
    }

    private void changeLearningStatus() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Bạn cần đăng nhập",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (vocabularyId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy ID từ vựng",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String oldStatus = learningStatus;

        if (STATUS_NOT_STARTED.equals(learningStatus)) {
            learningStatus = STATUS_LEARNING;

        } else if (STATUS_LEARNING.equals(learningStatus)) {
            learningStatus = STATUS_LEARNED;

        } else {
            learningStatus = STATUS_NOT_STARTED;
        }

        updateLearningButton();
        setButtonsEnabled(false);

        Map<String, Object> progressData =
                createBaseProgressData();

        progressData.put(
                "learningStatus",
                learningStatus
        );

        // Ghi thêm status để tương thích code cũ.
        progressData.put(
                "status",
                learningStatus
        );

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wordProgress")
                .document(vocabularyId)
                .set(progressData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setButtonsEnabled(true);

                    Toast.makeText(
                            this,
                            getLearningStatusMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    learningStatus = oldStatus;

                    updateLearningButton();
                    setButtonsEnabled(true);

                    Toast.makeText(
                            this,
                            "Cập nhật trạng thái thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void toggleFavorite() {
        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Bạn cần đăng nhập",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (vocabularyId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy ID từ vựng",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        boolean oldFavoriteState = isFavorite;
        isFavorite = !isFavorite;

        updateFavoriteButton();
        setButtonsEnabled(false);

        Map<String, Object> progressData =
                createBaseProgressData();

        progressData.put(
                "favorite",
                isFavorite
        );

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("wordProgress")
                .document(vocabularyId)
                .set(progressData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setButtonsEnabled(true);

                    Toast.makeText(
                            this,
                            isFavorite
                                    ? "Đã thêm vào yêu thích"
                                    : "Đã bỏ yêu thích",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    isFavorite = oldFavoriteState;

                    updateFavoriteButton();
                    setButtonsEnabled(true);

                    Toast.makeText(
                            this,
                            "Cập nhật yêu thích thất bại: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private Map<String, Object> createBaseProgressData() {
        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "vocabularyId",
                vocabularyId
        );

        data.put(
                "id",
                vocabularyId
        );

        data.put(
                "englishWord",
                englishWord
        );

        data.put(
                "vietnameseMeaning",
                vietnameseMeaning
        );

        data.put(
                "pronunciation",
                pronunciation
        );

        data.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        return data;
    }

    private void updateLearningButton() {
        switch (learningStatus) {
            case STATUS_LEARNING:
                btnLearned.setText("🟡 Đang học");
                break;

            case STATUS_LEARNED:
                btnLearned.setText("🟢 Đã học");
                break;

            default:
                btnLearned.setText("⚪ Chưa học");
                break;
        }
    }

    private void updateFavoriteButton() {
        btnFavoriteDetail.setText(
                isFavorite
                        ? "★ Đã yêu thích"
                        : "☆ Thêm vào yêu thích"
        );
    }

    private String getLearningStatusMessage() {
        switch (learningStatus) {
            case STATUS_LEARNING:
                return "Đã chuyển sang đang học";

            case STATUS_LEARNED:
                return "Đã chuyển sang đã học";

            default:
                return "Đã chuyển về chưa học";
        }
    }

    private String normalizeLearningStatus(
            String status
    ) {
        if (status == null
                || status.trim().isEmpty()) {

            return STATUS_NOT_STARTED;
        }

        String normalized =
                status.trim().toUpperCase(Locale.ROOT);

        if ("MASTERED".equals(normalized)
                || "LEARNED".equals(normalized)) {

            return STATUS_LEARNED;
        }

        if ("LEARNING".equals(normalized)) {
            return STATUS_LEARNING;
        }

        return STATUS_NOT_STARTED;
    }

    private void setButtonsEnabled(
            boolean enabled
    ) {
        btnFavoriteDetail.setEnabled(enabled);
        btnLearned.setEnabled(enabled);
    }

    private String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }
}