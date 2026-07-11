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

public class VocabularyDetailActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextView tvEnglishWord;
    private TextView tvPronunciation;
    private TextView tvMeaning;
    private TextView tvExample;
    private TextView tvCategory;
    private TextView tvLevel;
    private Button btnLearned;
    private boolean isLearned;
    private Button btnSpeak;
    private Button btnFavoriteDetail;

    private TextToSpeech textToSpeech;

    private String vocabularyId;
    private String englishWord;
    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary_detail);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        MaterialToolbar toolbarDetail = findViewById(R.id.toolbarDetail);
        toolbarDetail.setNavigationOnClickListener(view -> finish());

        initViews();
        getVocabularyData();
        setupTextToSpeech();
        setupEvents();
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

    private void getVocabularyData() {
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

        tvEnglishWord.setText(englishWord);
        tvPronunciation.setText(pronunciation);
        tvMeaning.setText(meaning);
        tvExample.setText("Ví dụ: " + example);
        tvCategory.setText("Chủ đề: " + category);
        tvLevel.setText("Mức độ: " + level);

        checkFavoriteFromFirestore();
    }

    private void setupTextToSpeech() {

        textToSpeech = new TextToSpeech(
                this,
                status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        int result = textToSpeech.setLanguage(Locale.US);

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

    private void setupEvents() {
        btnLearned.setOnClickListener(view -> toggleLearned());
        btnSpeak.setOnClickListener(view -> speakWord());

        btnFavoriteDetail.setOnClickListener(view -> toggleFavorite());
    }

    private void speakWord() {
        if (textToSpeech != null
                && englishWord != null
                && !englishWord.isEmpty()) {

            textToSpeech.speak(
                    englishWord,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "vocabulary_word"
            );
        }

    }

    private void toggleLearned() {
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

        btnLearned.setEnabled(false);

        if (isLearned) {
            removeLearnedWord(currentUser.getUid());
        } else {
            saveLearnedWord(currentUser.getUid());
        }
    }
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
            removeFavorite(currentUser.getUid());
        } else {
            saveFavorite(currentUser.getUid());
        }
    }

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
                    isFavorite = documentSnapshot.exists();
                    updateFavoriteButton();
                })
                .addOnFailureListener(exception -> {
                    isFavorite = false;
                    updateFavoriteButton();
                });
    }

    private void updateFavoriteButton() {
        if (isFavorite) {
            btnFavoriteDetail.setText("★ Đã yêu thích");
        } else {
            btnFavoriteDetail.setText("☆ Thêm vào yêu thích");
        }
    }

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

    private void updateLearnedButton() {
        if (isLearned) {
            btnLearned.setText("✓ Đã học");
        } else {
            btnLearned.setText("✓ Đánh dấu đã học");
        }
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