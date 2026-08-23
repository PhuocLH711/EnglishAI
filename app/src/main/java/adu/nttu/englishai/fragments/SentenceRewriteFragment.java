package adu.nttu.englishai.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.SentenceRewriteExercise;
import adu.nttu.englishai.repositories.SentenceRewriteRepository;

public class SentenceRewriteFragment extends Fragment {

    // Mỗi lượt chỉ làm tối đa 10 câu
    private static final int QUESTIONS_PER_ROUND = 10;

    private final SentenceRewriteRepository repository =
            new SentenceRewriteRepository();

    private final List<SentenceRewriteExercise> exercises =
            new ArrayList<>();

    private TextView tvProgress;
    private TextView tvXp;
    private TextView tvOriginalSentence;
    private TextView tvPrompt;
    private TextView tvResultTitle;
    private TextView tvHint;
    private TextView tvExplanation;
    private TextView tvTopic;
    private TextView tvLevel;

    private EditText edtAnswer;

    private Button btnCheck;
    private Button btnNext;
    private View btnBack;

    private View layoutResult;
    private ProgressBar progressBar;
    private ProgressBar progressQuestions;

    private int currentIndex = 0;
    private int totalXp = 0;

    // Chỉ true khi câu đã trả lời ĐÚNG
    private boolean answeredCorrectly = false;

    // Số lần trả lời sai ở câu hiện tại
    private int wrongAttempts = 0;

    public SentenceRewriteFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_sentence_rewrite,
                container,
                false
        );

        bindViews(view);
        bindEvents();
        loadExercises();

        return view;
    }

    private void bindViews(View view) {

        tvProgress =
                view.findViewById(R.id.tvProgress);

        tvXp =
                view.findViewById(R.id.tvXp);

        tvOriginalSentence =
                view.findViewById(R.id.tvOriginalSentence);

        tvPrompt =
                view.findViewById(R.id.tvPrompt);

        tvResultTitle =
                view.findViewById(R.id.tvResultTitle);

        tvHint =
                view.findViewById(R.id.tvHint);

        tvExplanation =
                view.findViewById(R.id.tvExplanation);

        tvTopic =
                view.findViewById(R.id.tvTopic);

        tvLevel =
                view.findViewById(R.id.tvLevel);

        edtAnswer =
                view.findViewById(R.id.edtAnswer);

        btnCheck =
                view.findViewById(R.id.btnCheck);

        btnNext =
                view.findViewById(R.id.btnNext);

        btnBack =
                view.findViewById(R.id.btnBack);

        layoutResult =
                view.findViewById(R.id.layoutResult);

        progressBar =
                view.findViewById(R.id.progressBar);

        progressQuestions =
                view.findViewById(R.id.progressQuestions);
    }

    private void bindEvents() {

        btnBack.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        btnCheck.setOnClickListener(v ->
                checkAnswer()
        );

        btnNext.setOnClickListener(v ->
                showNextQuestion()
        );
    }

    // =========================================================
    // TẢI BÀI TỪ FIRESTORE
    // Repository đã shuffle danh sách, Fragment chỉ lấy 10 câu đầu.
    // =========================================================
    private void loadExercises() {

        progressBar.setVisibility(View.VISIBLE);
        btnCheck.setEnabled(false);

        repository.getForGame(
                new SentenceRewriteRepository.ListCallback() {

                    @Override
                    public void onSuccess(
                            List<SentenceRewriteExercise> list
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        progressBar.setVisibility(View.GONE);

                        exercises.clear();

                        int limit =
                                Math.min(
                                        QUESTIONS_PER_ROUND,
                                        list.size()
                                );

                        for (int i = 0; i < limit; i++) {
                            exercises.add(
                                    list.get(i)
                            );
                        }

                        if (exercises.isEmpty()) {

                            Toast.makeText(
                                    requireContext(),
                                    "Chưa có dữ liệu Viết Lại Câu.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        currentIndex = 0;
                        totalXp = 0;

                        btnCheck.setEnabled(true);

                        progressQuestions.setMax(
                                exercises.size()
                        );

                        showQuestion();
                    }

                    @Override
                    public void onFailure(Exception e) {

                        if (!isAdded()) {
                            return;
                        }

                        progressBar.setVisibility(View.GONE);

                        Toast.makeText(
                                requireContext(),
                                "Không tải được bài tập: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================
    // HIỂN THỊ CÂU HIỆN TẠI
    // =========================================================
    private void showQuestion() {

        if (currentIndex < 0
                || currentIndex >= exercises.size()) {
            return;
        }

        SentenceRewriteExercise item =
                exercises.get(currentIndex);

        answeredCorrectly = false;
        wrongAttempts = 0;

        tvProgress.setText(
                "Câu "
                        + (currentIndex + 1)
                        + "/"
                        + exercises.size()
        );

        tvXp.setText(
                totalXp + " XP"
        );

        progressQuestions.setProgress(
                currentIndex + 1
        );

        tvOriginalSentence.setText(
                safe(item.getOriginalSentence())
        );

        tvPrompt.setText(
                safe(item.getPrompt())
        );

        String topic =
                safe(item.getTopic());

        String level =
                safe(item.getLevel());

        tvTopic.setText(
                topic.isEmpty()
                        ? "Grammar"
                        : topic
        );

        tvLevel.setText(
                level.isEmpty()
                        ? "Easy"
                        : level
        );

        edtAnswer.setText("");
        edtAnswer.setEnabled(true);
        edtAnswer.clearFocus();

        layoutResult.setVisibility(
                View.GONE
        );

        btnCheck.setVisibility(
                View.VISIBLE
        );

        btnCheck.setText(
                "KIỂM TRA"
        );

        btnNext.setVisibility(
                View.GONE
        );
    }

    // =========================================================
    // KIỂM TRA CÂU TRẢ LỜI
    //
    // ĐÚNG:
    // - Cộng XP
    // - Cho qua câu tiếp theo
    //
    // SAI:
    // - KHÔNG hiện đáp án hoàn chỉnh
    // - Hiện gợi ý dạng che chữ
    // - Người dùng được sửa và kiểm tra lại
    // =========================================================
    private void checkAnswer() {

        if (answeredCorrectly
                || exercises.isEmpty()) {
            return;
        }

        String userAnswer =
                edtAnswer
                        .getText()
                        .toString()
                        .trim();

        if (userAnswer.isEmpty()) {

            edtAnswer.setError(
                    "Hãy nhập câu trả lời"
            );

            edtAnswer.requestFocus();

            return;
        }

        SentenceRewriteExercise item =
                exercises.get(currentIndex);

        boolean correct =
                normalize(userAnswer)
                        .equals(
                                normalize(
                                        item.getAnswer()
                                )
                        );

        layoutResult.setVisibility(
                View.VISIBLE
        );

        if (correct) {

            hideKeyboard();

            answeredCorrectly = true;
            edtAnswer.setEnabled(false);

            int earnedXp =
                    (int) (
                            item.getXp() > 0
                                    ? item.getXp()
                                    : 10
                    );

            totalXp += earnedXp;

            tvResultTitle.setText(
                    "✅ Chính xác! +"
                            + earnedXp
                            + " XP"
            );

            // Không cần hiện lại đáp án đầy đủ vì người dùng đã viết đúng
            tvHint.setText(
                    "🎉 Bạn đã viết đúng câu. Tiếp tục sang câu kế tiếp nhé!"
            );

            String explanation =
                    safe(
                            item.getExplanation()
                    );

            if (explanation.isEmpty()) {
                explanation =
                        "Bạn đã sử dụng đúng cấu trúc ngữ pháp.";
            }

            tvExplanation.setText(
                    "💡 Ghi nhớ:\n"
                            + explanation
            );

            tvXp.setText(
                    totalXp + " XP"
            );

            btnCheck.setVisibility(
                    View.GONE
            );

            btnNext.setVisibility(
                    View.VISIBLE
            );

            addXpToCurrentUser(
                    earnedXp
            );

        } else {

            wrongAttempts++;

            tvResultTitle.setText(
                    "💡 Chưa đúng, thử lại nhé!"
            );

            // Gợi ý câu đúng nhưng KHÔNG cho nguyên đáp án
            tvHint.setText(
                    "Gợi ý câu:\n"
                            + buildMaskedHint(
                            item.getAnswer(),
                            wrongAttempts
                    )
            );

            String explanation =
                    safe(
                            item.getExplanation()
                    );

            if (explanation.isEmpty()) {
                explanation =
                        "Hãy chú ý cấu trúc ngữ pháp và dạng của động từ.";
            }

            tvExplanation.setText(
                    "🔎 Gợi ý ngữ pháp:\n"
                            + explanation
            );

            // Giữ ô nhập mở để người dùng sửa câu
            edtAnswer.setEnabled(true);
            edtAnswer.requestFocus();

            btnCheck.setText(
                    "THỬ LẠI"
            );

            btnNext.setVisibility(
                    View.GONE
            );
        }
    }

    // =========================================================
    // TẠO GỢI Ý CÂU DẠNG CHE CHỮ
    //
    // Ví dụ:
    // "I haven't visited Hue for two years."
    // -> "I h_____ v______ H__ f__ t__ y____."
    //
    // Nếu sai nhiều lần, gợi ý mở thêm chữ cái.
    // =========================================================
    private String buildMaskedHint(
            String answer,
            int attempt
    ) {

        if (answer == null
                || answer.trim().isEmpty()) {
            return "Hãy xem lại cấu trúc ngữ pháp.";
        }

        String[] words =
                answer.trim()
                        .split("\\s+");

        StringBuilder hint =
                new StringBuilder();

        int visibleLetters;

        if (attempt <= 1) {
            visibleLetters = 1;
        } else if (attempt == 2) {
            visibleLetters = 2;
        } else {
            visibleLetters = 3;
        }

        for (int i = 0; i < words.length; i++) {

            String word =
                    words[i];

            // Giữ lại dấu câu cuối từ
            String punctuation = "";

            while (!word.isEmpty()
                    && !Character.isLetterOrDigit(
                    word.charAt(
                            word.length() - 1
                    )
            )) {

                punctuation =
                        word.charAt(
                                word.length() - 1
                        )
                                + punctuation;

                word =
                        word.substring(
                                0,
                                word.length() - 1
                        );
            }

            // Các từ cực ngắn như I, a, to... giữ nguyên để gợi ý tự nhiên
            if (word.length() <= 2) {

                hint.append(word);

            } else {

                int show =
                        Math.min(
                                visibleLetters,
                                Math.max(
                                        1,
                                        word.length() - 1
                                )
                        );

                hint.append(
                        word.substring(
                                0,
                                show
                        )
                );

                for (int j = show;
                     j < word.length();
                     j++) {

                    hint.append("_");
                }
            }

            hint.append(
                    punctuation
            );

            if (i
                    < words.length - 1) {

                hint.append(" ");
            }
        }

        return hint.toString();
    }

    // =========================================================
    // CÂU TIẾP THEO
    // =========================================================
    private void showNextQuestion() {

        currentIndex++;

        if (currentIndex
                >= exercises.size()) {

            Toast.makeText(
                    requireContext(),
                    "🎉 Hoàn thành 10 câu! Bạn nhận được "
                            + totalXp
                            + " XP.",
                    Toast.LENGTH_LONG
            ).show();

            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();

            return;
        }

        showQuestion();
    }

    // =========================================================
    // CỘNG XP VÀO USERS/{UID}/SCORE
    // =========================================================
    private void addXpToCurrentUser(
            int xp
    ) {

        if (FirebaseAuth
                .getInstance()
                .getCurrentUser()
                == null) {

            return;
        }

        String uid =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getUid();

        FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(uid)
                .update(
                        "score",
                        FieldValue.increment(xp)
                );
    }

    // =========================================================
    // CHUẨN HÓA ĐÁP ÁN
    // =========================================================
    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[.!?]+$", "")
                .replaceAll("\\s+", " ");
    }

    private void hideKeyboard() {

        if (!isAdded()) {
            return;
        }

        View current =
                requireActivity()
                        .getCurrentFocus();

        if (current == null) {
            return;
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager)
                        requireContext()
                                .getSystemService(
                                        Context.INPUT_METHOD_SERVICE
                                );

        if (inputMethodManager != null) {

            inputMethodManager
                    .hideSoftInputFromWindow(
                            current.getWindowToken(),
                            0
                    );
        }
    }

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}