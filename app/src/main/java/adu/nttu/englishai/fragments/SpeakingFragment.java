package adu.nttu.englishai.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

public class SpeakingFragment extends Fragment {

    private TextView tvTargetWord, tvTargetPhonetic, tvTargetMeaning, tvUserSpeech;
    private MaterialCardView btnMic;
    private View btnNextWord;
    private nl.dionsegijn.konfetti.xml.KonfettiView konfettiView;

    private MediaPlayer soundCorrect, soundWrong;
    private List<Vocabulary> vocabularyList;
    private Vocabulary currentWord;
    private boolean isAnsweredCorrectly = false;

    // Trình khởi chạy xin quyền Micro
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startVoiceRecognition();
                else Toast.makeText(getContext(), "Cần cấp quyền Micro để thu âm!", Toast.LENGTH_SHORT).show();
            });

    // Trình khởi chạy Gọi API thu âm của Google
    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> voiceResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (voiceResults != null && !voiceResults.isEmpty()) {
                        String spokenText = voiceResults.get(0);
                        checkPronunciation(spokenText);
                    }
                }
            });

    public SpeakingFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_speaking, container, false);

        View btnBackToStage = view.findViewById(R.id.btnBackToStage);
        if (btnBackToStage != null) {
            btnBackToStage.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        tvTargetWord = view.findViewById(R.id.tvTargetWord);
        tvTargetPhonetic = view.findViewById(R.id.tvTargetPhonetic);
        tvTargetMeaning = view.findViewById(R.id.tvTargetMeaning);
        tvUserSpeech = view.findViewById(R.id.tvUserSpeech);
        btnMic = view.findViewById(R.id.btnMic);
        btnNextWord = view.findViewById(R.id.btnNextWord);
        konfettiView = view.findViewById(R.id.konfettiView);

        soundCorrect = MediaPlayer.create(requireContext(), R.raw.correct);
        soundWrong = MediaPlayer.create(requireContext(), R.raw.wrong);

        vocabularyList = DataRepository.getInstance().getVocabularyList();

        loadNewWord();

        btnNextWord.setOnClickListener(v -> loadNewWord());

        btnMic.setOnClickListener(v -> {
            if (isAnsweredCorrectly) return;
            // Kiểm tra quyền Micro trước khi thu âm
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });

        return view;
    }

    private void loadNewWord() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        isAnsweredCorrectly = false;
        btnNextWord.setVisibility(View.GONE);
        tvUserSpeech.setText("Bấm Micro để bắt đầu đọc...");
        tvUserSpeech.setTextColor(Color.parseColor("#9E9E9E"));
        btnMic.setCardBackgroundColor(Color.parseColor("#F44336")); // Đỏ đỏ khích lệ

        currentWord = vocabularyList.get(new Random().nextInt(vocabularyList.size()));
        tvTargetWord.setText(currentWord.getEnglishWord());
        tvTargetPhonetic.setText(currentWord.getPronunciation() != null ? currentWord.getPronunciation() : "");
        tvTargetMeaning.setText(currentWord.getVietnameseMeaning());
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // Ép AI Google nghe tiếng Anh
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy đọc to từ: " + currentWord.getEnglishWord());

        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Thiết bị của bạn không hỗ trợ Google Speech!", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPronunciation(String spokenText) {
        tvUserSpeech.setText("Bạn đọc: \"" + spokenText + "\"");

        // Chuẩn hóa chuỗi (xóa khoảng trắng thừa, đưa về in thường) để so sánh
        String target = currentWord.getEnglishWord().trim().toLowerCase();
        String spoken = spokenText.trim().toLowerCase();

        // Kiểm tra xem từ người dùng đọc có chứa từ gốc không
        if (spoken.contains(target)) {
            // 👉 ĐỌC ĐÚNG
            isAnsweredCorrectly = true;
            tvUserSpeech.setTextColor(Color.parseColor("#2E7D32"));
            tvUserSpeech.setText("Bạn đọc: \"" + spokenText + "\" - Chính xác! 🌟");

            btnMic.setCardBackgroundColor(Color.parseColor("#4CAF50")); // Chuyển mic sang xanh lá
            btnNextWord.setVisibility(View.VISIBLE);

            if (soundCorrect != null) { soundCorrect.seekTo(0); soundCorrect.start(); }
            showConfettiAnimation();

            // Cộng 15 XP vì bài tập Nói khó hơn!
            updateProgressAndStreak(15);

        } else {
            // 👉 ĐỌC SAI
            tvUserSpeech.setTextColor(Color.parseColor("#C62828"));
            tvUserSpeech.setText("Bạn đọc: \"" + spokenText + "\" - Gần đúng rồi, đọc lại nhé! 💪");

            if (soundWrong != null) { soundWrong.seekTo(0); soundWrong.start(); }
        }
    }

    // Hàm bắn pháo hoa
    private void showConfettiAnimation() {
        nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig =
                new nl.dionsegijn.konfetti.core.emitter.Emitter(100L, java.util.concurrent.TimeUnit.MILLISECONDS).max(100);
        konfettiView.start(
                new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig).spread(360)
                        .colors(java.util.Arrays.asList(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED, Color.CYAN))
                        .setSpeedBetween(0f, 30f).position(new nl.dionsegijn.konfetti.core.Position.Relative(0.5, 0.3)).build()
        );
    }

    // Hàm đẩy điểm XP và tính toán Chuỗi lửa lên Firebase (Giống y 2 màn kia)
    private void updateProgressAndStreak(int xpAmount) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        com.google.firebase.firestore.DocumentReference userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid());

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        String today = sdf.format(calendar.getTime());
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1);
        String yesterday = sdf.format(calendar.getTime());

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String lastActiveDate = documentSnapshot.getString("lastActiveDate");
                Long currentStreakObj = documentSnapshot.getLong("streak");
                long currentStreak = (currentStreakObj != null) ? currentStreakObj : 0;
                long newStreak;
                if (today.equals(lastActiveDate)) {
                    newStreak = currentStreak;
                } else if (yesterday.equals(lastActiveDate)) {
                    newStreak = currentStreak + 1;
                } else {
                    newStreak = 1;
                }
                userRef.update(
                        "score", com.google.firebase.firestore.FieldValue.increment(xpAmount),
                        "streak", newStreak,
                        "lastActiveDate", today
                );
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (soundCorrect != null) { soundCorrect.release(); soundCorrect = null; }
        if (soundWrong != null) { soundWrong.release(); soundWrong = null; }
    }
}