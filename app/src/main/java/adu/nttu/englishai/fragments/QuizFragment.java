package adu.nttu.englishai.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
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

public class QuizFragment extends Fragment {

    private TextView tvQuestion, tvConfetti;
    private Button btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4, btnNextQuestion;
    private List<Vocabulary> vocabularyList;
    private Vocabulary currentQuestionWord;
    private boolean isQuestionAnswered = false;

    public QuizFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        tvQuestion = view.findViewById(R.id.tvQuestion);
        tvConfetti = view.findViewById(R.id.tvConfetti);
        btnAnswer1 = view.findViewById(R.id.btnAnswer1);
        btnAnswer2 = view.findViewById(R.id.btnAnswer2);
        btnAnswer3 = view.findViewById(R.id.btnAnswer3);
        btnAnswer4 = view.findViewById(R.id.btnAnswer4);
        btnNextQuestion = view.findViewById(R.id.btnNextQuestion);

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
        tvConfetti.setVisibility(View.GONE);

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
        if (isQuestionAnswered) return; // Nếu đã trả lời đúng rồi thì không cho bấm nữa

        if (selectedAnswer.equals(currentQuestionWord.getVietnameseMeaning())) {
            isQuestionAnswered = true;

            // 1. Tông màu xanh ngọc dịu mắt (#E8F5E9) + Chữ xanh đậm (#1B5E20)
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            selectedButton.setTextColor(Color.parseColor("#1B5E20"));

            // 2. Khóa tất cả lại vì đã chọn đúng
            lockAllButtons();

            // 3. Hiện nút "Tiếp theo" nhỏ gọn góc phải
            btnNextQuestion.setVisibility(View.VISIBLE);

            // 4. Bắn hiệu ứng chúc mừng
            showConfettiAnimation();
            Toast.makeText(getContext(), "Chính xác! Bạn giỏi quá 🎉", Toast.LENGTH_SHORT).show();

        } else {
            // Tông màu đỏ hồng dịu mắt (#FFEBEE) + Chữ đỏ đậm (#B71C1C)
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            selectedButton.setTextColor(Color.parseColor("#B71C1C"));

            // CHỈ khóa nút vừa chọn sai, các nút khác VẪN MỞ để chọn lại!
            selectedButton.setEnabled(false);

            // Lắc nhẹ nút hoặc báo lỗi
            Toast.makeText(getContext(), "Chưa đúng rồi, bạn thử chọn lại xem! 😅", Toast.LENGTH_SHORT).show();
        }
    }

    private void showConfettiAnimation() {
        tvConfetti.setVisibility(View.VISIBLE);

        // Hiệu ứng phóng to và mờ dần
        ScaleAnimation scale = new ScaleAnimation(0.2f, 1.5f, 0.2f, 1.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        AlphaAnimation alpha = new AlphaAnimation(1f, 0f);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        set.setDuration(1200);

        tvConfetti.startAnimation(set);

        // Ẩn đi sau khi bay xong
        tvConfetti.postDelayed(() -> tvConfetti.setVisibility(View.GONE), 1200);
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
}