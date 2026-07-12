package adu.nttu.englishai.fragments;

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

public class QuizFragment extends Fragment {

    private TextView tvQuestion;
    private Button btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4;
    private List<Vocabulary> vocabularyList;
    private Vocabulary currentQuestionWord;

    public QuizFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        tvQuestion = view.findViewById(R.id.tvQuestion);
        btnAnswer1 = view.findViewById(R.id.btnAnswer1);
        btnAnswer2 = view.findViewById(R.id.btnAnswer2);
        btnAnswer3 = view.findViewById(R.id.btnAnswer3);
        btnAnswer4 = view.findViewById(R.id.btnAnswer4);

        vocabularyList = DataRepository.getInstance().getVocabularyList();

        generateNewQuestion();

        return view;
    }

    private void generateNewQuestion() {
        if (vocabularyList == null || vocabularyList.size() < 4) return;

        // Chọn ngẫu nhiên 1 từ làm câu hỏi
        Random random = new Random();
        currentQuestionWord = vocabularyList.get(random.nextInt(vocabularyList.size()));
        tvQuestion.setText("Nghĩa của từ '" + currentQuestionWord.getEnglishWord() + "' là gì?");

        // Tạo danh sách 4 đáp án (1 đúng, 3 sai)
        List<String> options = new ArrayList<>();
        options.add(currentQuestionWord.getVietnameseMeaning()); // Đáp án đúng

        while (options.size() < 4) {
            Vocabulary randomWord = vocabularyList.get(random.nextInt(vocabularyList.size()));
            if (!options.contains(randomWord.getVietnameseMeaning())) {
                options.add(randomWord.getVietnameseMeaning());
            }
        }

        // Xáo trộn vị trí 4 nút đáp án
        Collections.shuffle(options);

        btnAnswer1.setText("A. " + options.get(0));
        btnAnswer2.setText("B. " + options.get(1));
        btnAnswer3.setText("C. " + options.get(2));
        btnAnswer4.setText("D. " + options.get(3));

        // Gán sự kiện kiểm tra đáp án
        btnAnswer1.setOnClickListener(v -> checkAnswer(options.get(0)));
        btnAnswer2.setOnClickListener(v -> checkAnswer(options.get(1)));
        btnAnswer3.setOnClickListener(v -> checkAnswer(options.get(2)));
        btnAnswer4.setOnClickListener(v -> checkAnswer(options.get(3)));
    }

    private void checkAnswer(String selectedAnswer) {
        if (selectedAnswer.equals(currentQuestionWord.getVietnameseMeaning())) {
            Toast.makeText(getContext(), "Chính xác! 🎉 Tuyệt vời!", Toast.LENGTH_SHORT).show();
            generateNewQuestion(); // Tự động tạo câu hỏi mới khi trả lời đúng!
        } else {
            Toast.makeText(getContext(), "Sai rồi, chọn lại thử xem! 😅", Toast.LENGTH_SHORT).show();
        }
    }
}