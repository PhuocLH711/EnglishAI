package adu.nttu.englishai.fragments;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

// =========================================================================
// QUIZ FRAGMENT: Màn hình Trò chơi Trắc nghiệm Từ vựng
// =========================================================================
public class QuizFragment extends Fragment {

    private TextView tvQuestion;
    private Button btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4, btnNextQuestion;

    // View pháo hoa từ thư viện
    private nl.dionsegijn.konfetti.xml.KonfettiView konfettiView;

    // Biến lưu trữ âm thanh
    private MediaPlayer soundCorrect;
    private MediaPlayer soundWrong;

    private List<Vocabulary> vocabularyList;
    private Vocabulary currentQuestionWord;

    private boolean isQuestionAnswered = false;

    public QuizFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        View btnBackToStage = view.findViewById(R.id.btnBackToStage);
        if (btnBackToStage != null) {
            btnBackToStage.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        // Ánh xạ View
        tvQuestion = view.findViewById(R.id.tvQuestion);
        konfettiView = view.findViewById(R.id.konfettiView);
        btnAnswer1 = view.findViewById(R.id.btnAnswer1);
        btnAnswer2 = view.findViewById(R.id.btnAnswer2);
        btnAnswer3 = view.findViewById(R.id.btnAnswer3);
        btnAnswer4 = view.findViewById(R.id.btnAnswer4);
        btnNextQuestion = view.findViewById(R.id.btnNextQuestion);

        // 👉 KHỞI TẠO ÂM THANH
        soundCorrect = MediaPlayer.create(requireContext(), R.raw.correct);
        soundWrong = MediaPlayer.create(requireContext(), R.raw.wrong);

        vocabularyList = DataRepository.getInstance().getVocabularyList();

        generateNewQuestion();

        btnNextQuestion.setOnClickListener(v -> generateNewQuestion());

        return view;
    }

    private void generateNewQuestion() {
        if (vocabularyList == null || vocabularyList.size() < 4) return;

        isQuestionAnswered = false;
        resetButtons();
        btnNextQuestion.setVisibility(View.GONE);

        Random random = new Random();
        currentQuestionWord = vocabularyList.get(random.nextInt(vocabularyList.size()));
        tvQuestion.setText("Nghĩa của từ '" + currentQuestionWord.getEnglishWord() + "' là gì?");

        List<String> options = new ArrayList<>();
        options.add(currentQuestionWord.getVietnameseMeaning());

        while (options.size() < 4) {
            Vocabulary randomWord = vocabularyList.get(random.nextInt(vocabularyList.size()));
            if (!options.contains(randomWord.getVietnameseMeaning())) {
                options.add(randomWord.getVietnameseMeaning());
            }
        }

        Collections.shuffle(options);

        btnAnswer1.setText("A. " + options.get(0));
        btnAnswer2.setText("B. " + options.get(1));
        btnAnswer3.setText("C. " + options.get(2));
        btnAnswer4.setText("D. " + options.get(3));

        btnAnswer1.setOnClickListener(v -> checkAnswer(btnAnswer1, options.get(0)));
        btnAnswer2.setOnClickListener(v -> checkAnswer(btnAnswer2, options.get(1)));
        btnAnswer3.setOnClickListener(v -> checkAnswer(btnAnswer3, options.get(2)));
        btnAnswer4.setOnClickListener(v -> checkAnswer(btnAnswer4, options.get(3)));
    }

    private void checkAnswer(Button selectedButton, String selectedAnswer) {
        if (isQuestionAnswered) return;

        if (selectedAnswer.equals(currentQuestionWord.getVietnameseMeaning())) {
            // 👉 TRẢ LỜI ĐÚNG
            isQuestionAnswered = true;
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            selectedButton.setTextColor(Color.parseColor("#1B5E20"));
            lockAllButtons();
            btnNextQuestion.setVisibility(View.VISIBLE);
            // Cộng 10 XP lên Server
            updateProgressAndStreak(10);

            // Bật âm thanh Ting!
            if (soundCorrect != null) {
                soundCorrect.seekTo(0);
                soundCorrect.start();
            }

            // Gọi hàm bắn pháo hoa
            showConfettiAnimation();
            Toast.makeText(getContext(), "Chính xác! Bạn giỏi quá 🎉", Toast.LENGTH_SHORT).show();

        } else {
            // 👉 TRẢ LỜI SAI
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            selectedButton.setTextColor(Color.parseColor("#B71C1C"));
            selectedButton.setEnabled(false);

            // Bật âm thanh Tè te!
            if (soundWrong != null) {
                soundWrong.seekTo(0);
                soundWrong.start();
            }

            Toast.makeText(getContext(), "Chưa đúng rồi, bạn thử chọn lại xem! 😅", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // HÀM 3: BẮN PHÁO HOA KONFETTI CHUYÊN NGHIỆP
    // =========================================================================
    private void showConfettiAnimation() {
        // Cấu hình số lượng pháo (100 hạt) bắn ra trong 100 mili-giây
        nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig =
                new nl.dionsegijn.konfetti.core.emitter.Emitter(100L, java.util.concurrent.TimeUnit.MILLISECONDS).max(100);

        // Bắn!
        konfettiView.start(
                new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig)
                        .spread(360) // Bắn tỏa ra 360 độ
                        .colors(java.util.Arrays.asList(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED, Color.CYAN))
                        .setSpeedBetween(0f, 30f)
                        .position(new nl.dionsegijn.konfetti.core.Position.Relative(0.5, 0.3)) // Bắn từ giữa màn hình (nhích lên trên 1 tí)
                        .build()
        );
    }

    private void lockAllButtons() {
        btnAnswer1.setEnabled(false);
        btnAnswer2.setEnabled(false);
        btnAnswer3.setEnabled(false);
        btnAnswer4.setEnabled(false);
    }

    private void resetButtons() {
        Button[] buttons = {btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4};
        for (Button btn : buttons) {
            btn.setEnabled(true);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            btn.setTextColor(Color.parseColor("#333333"));
        }
    }

    // 👉 QUAN TRỌNG: Dọn dẹp RAM khi thoát Trò chơi để không bị giật lag
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (soundCorrect != null) {
            soundCorrect.release();
            soundCorrect = null;
        }
        if (soundWrong != null) {
            soundWrong.release();
            soundWrong = null;
        }
    }
    // Hàm đẩy điểm XP và tính toán Chuỗi lửa lên Firebase
    private void updateProgressAndStreak(int xpAmount) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        com.google.firebase.firestore.DocumentReference userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid());

        // Lấy ngày hôm nay và ngày hôm qua
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
                    newStreak = currentStreak; // Đã chơi hôm nay -> Giữ nguyên chuỗi
                } else if (yesterday.equals(lastActiveDate)) {
                    newStreak = currentStreak + 1; // Hôm qua có chơi, hôm nay chơi tiếp -> Cộng 1 ngày
                } else {
                    newStreak = 1; // Hôm qua quên không chơi hoặc mới chơi lần đầu -> Reset tính từ ngày 1
                }

                // Đẩy đồng loạt cả Điểm, Chuỗi lửa và Ngày điểm danh lên Server
                userRef.update(
                        "score", com.google.firebase.firestore.FieldValue.increment(xpAmount),
                        "streak", newStreak,
                        "lastActiveDate", today
                );
            }
        });
    }
}