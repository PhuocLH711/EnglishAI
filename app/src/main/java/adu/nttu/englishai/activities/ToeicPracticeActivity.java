package adu.nttu.englishai.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.ToeicQuestion;
import adu.nttu.englishai.repositories.ToeicRepository;

public class ToeicPracticeActivity
        extends AppCompatActivity {

    private TextView btnBack;
    private TextView tvTitle;
    private TextView tvPart;
    private TextView tvProgress;
    private TextView tvTimer;
    private TextView tvPassage;
    private TextView tvQuestion;
    private TextView tvFeedback;

    private RadioGroup groupAnswers;
    private RadioButton radioA;
    private RadioButton radioB;
    private RadioButton radioC;
    private RadioButton radioD;

    private MaterialButton btnPrevious;
    private MaterialButton btnBookmark;
    private MaterialButton btnCheckOrSubmit;
    private MaterialButton btnNext;

    private final List<ToeicQuestion> questions =
            new ArrayList<>();

    private final Map<String, String> answers =
            new HashMap<>();

    private final Map<String, Boolean> bookmarks =
            new HashMap<>();

    private ToeicRepository repository;

    private String testId;
    private String testTitle;
    private String mode;

    private int durationMinutes;
    private int currentIndex = 0;

    private boolean showingCheckedAnswer = false;

    private CountDownTimer countDownTimer;
    private long remainingMillis;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_toeic_practice
        );

        readIntent();
        bindViews();

        repository =
                new ToeicRepository();

        setupButtons();
        setupSystemBack();
        loadQuestions();
    }

    private void readIntent() {

        testId =
                getIntent().getStringExtra(
                        ToeicHomeActivity.EXTRA_TEST_ID
                );

        testTitle =
                getIntent().getStringExtra(
                        ToeicHomeActivity.EXTRA_TEST_TITLE
                );

        durationMinutes =
                getIntent().getIntExtra(
                        ToeicHomeActivity.EXTRA_DURATION,
                        120
                );

        mode =
                getIntent().getStringExtra(
                        ToeicHomeActivity.EXTRA_MODE
                );

        if (mode == null) {
            mode =
                    ToeicHomeActivity.MODE_PRACTICE;
        }

        if (durationMinutes <= 0) {
            durationMinutes = 120;
        }

        remainingMillis =
                durationMinutes
                        * 60L
                        * 1000L;
    }

    private void bindViews() {

        btnBack =
                findViewById(
                        R.id.btnToeicPracticeBack
                );

        tvTitle =
                findViewById(
                        R.id.tvToeicPracticeTitle
                );

        tvPart =
                findViewById(
                        R.id.tvToeicPart
                );

        tvProgress =
                findViewById(
                        R.id.tvToeicProgress
                );

        tvTimer =
                findViewById(
                        R.id.tvToeicTimer
                );

        tvPassage =
                findViewById(
                        R.id.tvToeicPassage
                );

        tvQuestion =
                findViewById(
                        R.id.tvToeicQuestion
                );

        tvFeedback =
                findViewById(
                        R.id.tvToeicFeedback
                );

        groupAnswers =
                findViewById(
                        R.id.groupToeicAnswers
                );

        radioA =
                findViewById(
                        R.id.radioToeicA
                );

        radioB =
                findViewById(
                        R.id.radioToeicB
                );

        radioC =
                findViewById(
                        R.id.radioToeicC
                );

        radioD =
                findViewById(
                        R.id.radioToeicD
                );

        btnPrevious =
                findViewById(
                        R.id.btnToeicPrevious
                );

        btnBookmark =
                findViewById(
                        R.id.btnToeicBookmark
                );

        btnCheckOrSubmit =
                findViewById(
                        R.id.btnToeicCheckOrSubmit
                );

        btnNext =
                findViewById(
                        R.id.btnToeicNext
                );

        tvTitle.setText(
                safe(
                        testTitle,
                        "TOEIC Practice"
                )
        );

        tvTimer.setVisibility(
                isMockMode()
                        ? View.VISIBLE
                        : View.GONE
        );

        btnCheckOrSubmit.setText(
                isMockMode()
                        ? "NỘP BÀI"
                        : "KIỂM TRA"
        );
    }

    private void setupButtons() {

        btnBack.setOnClickListener(
                view -> handleExit()
        );

        btnPrevious.setOnClickListener(
                view -> {

                    saveCurrentAnswer();

                    if (currentIndex > 0) {
                        currentIndex--;
                        showQuestion();
                    }
                }
        );

        btnNext.setOnClickListener(
                view -> {

                    saveCurrentAnswer();

                    if (currentIndex
                            < questions.size() - 1) {

                        currentIndex++;
                        showQuestion();

                    } else if (isMockMode()) {
                        confirmSubmit();
                    }
                }
        );

        btnBookmark.setOnClickListener(
                view -> toggleBookmark()
        );

        btnCheckOrSubmit.setOnClickListener(
                view -> {

                    saveCurrentAnswer();

                    if (isMockMode()) {
                        confirmSubmit();
                    } else {
                        checkCurrentQuestion();
                    }
                }
        );
    }

    private void setupSystemBack() {

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {
                                handleExit();
                            }
                        }
                );
    }

    private void loadQuestions() {

        tvQuestion.setText(
                "Đang tải câu hỏi..."
        );

        repository.getFullTestQuestions(
                testId,
                new ToeicRepository.QuestionsCallback() {

                    @Override
                    public void onSuccess(
                            List<ToeicQuestion> loaded
                    ) {

                        questions.clear();

                        if (loaded != null) {
                            questions.addAll(loaded);
                        }

                        if (questions.isEmpty()) {

                            tvQuestion.setText(
                                    "Bộ đề chưa có câu hỏi."
                            );

                            btnCheckOrSubmit.setEnabled(false);
                            btnNext.setEnabled(false);
                            return;
                        }

                        currentIndex = 0;
                        showQuestion();

                        if (isMockMode()) {
                            startTimer();
                        }
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        tvQuestion.setText(
                                "Không tải được câu hỏi."
                        );

                        Toast.makeText(
                                ToeicPracticeActivity.this,
                                exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showQuestion() {

        if (questions.isEmpty()) {
            return;
        }

        ToeicQuestion question =
                questions.get(currentIndex);

        showingCheckedAnswer = false;

        tvFeedback.setVisibility(
                View.GONE
        );

        tvPart.setText(
                "PART "
                        + question.getPart()
        );

        tvProgress.setText(
                (currentIndex + 1)
                        + "/"
                        + questions.size()
        );

        String passage =
                safe(
                        question.getPassageText(),
                        ""
                );

        if (passage.isEmpty()) {

            tvPassage.setVisibility(
                    View.GONE
            );

        } else {

            tvPassage.setVisibility(
                    View.VISIBLE
            );

            tvPassage.setText(
                    passage
            );
        }

        tvQuestion.setText(
                question.getQuestionNumber()
                        + ". "
                        + safe(
                        question.getQuestionText(),
                        "(Câu hỏi nghe / hình ảnh)"
                )
        );

        radioA.setText(
                "A. "
                        + safe(
                        question.getOptionA(),
                        "—"
                )
        );

        radioB.setText(
                "B. "
                        + safe(
                        question.getOptionB(),
                        "—"
                )
        );

        radioC.setText(
                "C. "
                        + safe(
                        question.getOptionC(),
                        "—"
                )
        );

        radioD.setText(
                "D. "
                        + safe(
                        question.getOptionD(),
                        "—"
                )
        );

        groupAnswers.clearCheck();

        String savedAnswer =
                answers.get(
                        question.getId()
                );

        if ("A".equals(savedAnswer)) {
            radioA.setChecked(true);
        } else if ("B".equals(savedAnswer)) {
            radioB.setChecked(true);
        } else if ("C".equals(savedAnswer)) {
            radioC.setChecked(true);
        } else if ("D".equals(savedAnswer)) {
            radioD.setChecked(true);
        }

        boolean bookmarked =
                Boolean.TRUE.equals(
                        bookmarks.get(
                                question.getId()
                        )
                );

        btnBookmark.setText(
                bookmarked
                        ? "★ Đã đánh dấu"
                        : "☆ Đánh dấu"
        );

        btnPrevious.setEnabled(
                currentIndex > 0
        );

        btnNext.setText(
                currentIndex
                        == questions.size() - 1
                        ? (
                        isMockMode()
                                ? "NỘP BÀI"
                                : "XONG"
                )
                        : "TIẾP"
        );

        setAnswersEnabled(true);

        btnCheckOrSubmit.setVisibility(
                View.VISIBLE
        );

        btnCheckOrSubmit.setText(
                isMockMode()
                        ? "NỘP BÀI"
                        : "KIỂM TRA"
        );
    }

    private void saveCurrentAnswer() {

        if (questions.isEmpty()) {
            return;
        }

        String answer =
                selectedAnswer();

        if (answer.isEmpty()) {
            return;
        }

        ToeicQuestion question =
                questions.get(currentIndex);

        answers.put(
                question.getId(),
                answer
        );

        question.setSelectedAnswer(
                answer
        );
    }

    private String selectedAnswer() {

        int checkedId =
                groupAnswers
                        .getCheckedRadioButtonId();

        if (checkedId == R.id.radioToeicA) {
            return "A";
        }

        if (checkedId == R.id.radioToeicB) {
            return "B";
        }

        if (checkedId == R.id.radioToeicC) {
            return "C";
        }

        if (checkedId == R.id.radioToeicD) {
            return "D";
        }

        return "";
    }

    private void checkCurrentQuestion() {

        String selected =
                selectedAnswer();

        if (selected.isEmpty()) {

            Toast.makeText(
                    this,
                    "Hãy chọn một đáp án.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ToeicQuestion question =
                questions.get(currentIndex);

        String correct =
                safe(
                        question.getCorrectAnswer(),
                        ""
                ).toUpperCase(Locale.ROOT);

        boolean isCorrect =
                selected.equals(correct);

        StringBuilder feedback =
                new StringBuilder();

        if (isCorrect) {

            feedback.append(
                    "✅ Chính xác."
            );

        } else {

            feedback.append(
                    "❌ Chưa đúng. Đáp án đúng: "
            );

            feedback.append(
                    correct
            );
        }

        String explanation =
                safe(
                        question.getExplanation(),
                        ""
                );

        if (!explanation.isEmpty()) {

            feedback.append(
                    "\n\n"
            );

            feedback.append(
                    explanation
            );
        }

        tvFeedback.setText(
                feedback.toString()
        );

        tvFeedback.setVisibility(
                View.VISIBLE
        );

        showingCheckedAnswer = true;

        setAnswersEnabled(false);
    }

    private void setAnswersEnabled(
            boolean enabled
    ) {

        radioA.setEnabled(enabled);
        radioB.setEnabled(enabled);
        radioC.setEnabled(enabled);
        radioD.setEnabled(enabled);
    }

    private void toggleBookmark() {

        if (questions.isEmpty()) {
            return;
        }

        ToeicQuestion question =
                questions.get(currentIndex);

        String id =
                question.getId();

        boolean newValue =
                !Boolean.TRUE.equals(
                        bookmarks.get(id)
                );

        bookmarks.put(
                id,
                newValue
        );

        question.setBookmarked(
                newValue
        );

        btnBookmark.setText(
                newValue
                        ? "★ Đã đánh dấu"
                        : "☆ Đánh dấu"
        );
    }

    private void startTimer() {

        stopTimer();

        countDownTimer =
                new CountDownTimer(
                        remainingMillis,
                        1000L
                ) {

                    @Override
                    public void onTick(
                            long millisUntilFinished
                    ) {

                        remainingMillis =
                                millisUntilFinished;

                        long totalSeconds =
                                millisUntilFinished
                                        / 1000L;

                        long minutes =
                                totalSeconds / 60L;

                        long seconds =
                                totalSeconds % 60L;

                        tvTimer.setText(
                                String.format(
                                        Locale.ROOT,
                                        "⏱ %02d:%02d",
                                        minutes,
                                        seconds
                                )
                        );
                    }

                    @Override
                    public void onFinish() {

                        remainingMillis = 0L;

                        tvTimer.setText(
                                "⏱ 00:00"
                        );

                        submitTest();
                    }
                };

        countDownTimer.start();
    }

    private void stopTimer() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void confirmSubmit() {

        saveCurrentAnswer();

        int unanswered =
                questions.size()
                        - answers.size();

        String message =
                unanswered > 0
                        ? "Bạn còn "
                        + unanswered
                        + " câu chưa trả lời. Vẫn nộp bài?"
                        : "Bạn muốn nộp bài ngay?";

        new AlertDialog.Builder(this)
                .setTitle("Nộp bài TOEIC")
                .setMessage(message)
                .setPositiveButton(
                        "Nộp bài",
                        (dialog, which) ->
                                submitTest()
                )
                .setNegativeButton(
                        "Tiếp tục làm",
                        null
                )
                .show();
    }

    private void submitTest() {

        stopTimer();
        saveCurrentAnswer();

        int correct = 0;

        Map<Integer, int[]> byPart =
                new HashMap<>();

        for (ToeicQuestion question
                : questions) {

            String selected =
                    answers.get(
                            question.getId()
                    );

            if (selected == null) {
                selected = "";
            }

            String correctAnswer =
                    safe(
                            question.getCorrectAnswer(),
                            ""
                    ).toUpperCase(Locale.ROOT);

            int[] stats =
                    byPart.get(
                            question.getPart()
                    );

            if (stats == null) {
                stats = new int[]{0, 0};
                byPart.put(
                        question.getPart(),
                        stats
                );
            }

            stats[1]++;

            if (selected.equals(
                    correctAnswer
            )) {

                correct++;
                stats[0]++;
            }
        }

        int total =
                questions.size();

        int percent =
                total == 0
                        ? 0
                        : Math.round(
                        correct
                                * 100f
                                / total
                );

        StringBuilder message =
                new StringBuilder();

        message.append(
                "Đúng: "
        );

        message.append(correct);
        message.append("/");
        message.append(total);

        message.append(
                "\nĐộ chính xác: "
        );

        message.append(percent);
        message.append("%");

        for (int part = 1;
                part <= 7;
                part++) {

            int[] stats =
                    byPart.get(part);

            if (stats == null) {
                continue;
            }

            message.append(
                    "\nPart "
            );

            message.append(part);
            message.append(": ");
            message.append(stats[0]);
            message.append("/");
            message.append(stats[1]);
        }

        message.append(
                "\n\nLưu ý: Đây là điểm theo số câu đúng của bộ demo, không phải quy đổi thang điểm TOEIC 10–990."
        );

        new AlertDialog.Builder(this)
                .setTitle("Kết quả TOEIC")
                .setMessage(
                        message.toString()
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Làm lại",
                        (dialog, which) ->
                                restartTest()
                )
                .setNegativeButton(
                        "Thoát",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private void restartTest() {

        answers.clear();
        bookmarks.clear();

        for (ToeicQuestion question
                : questions) {

            question.setSelectedAnswer("");
            question.setBookmarked(false);
        }

        currentIndex = 0;

        remainingMillis =
                durationMinutes
                        * 60L
                        * 1000L;

        showQuestion();

        if (isMockMode()) {
            startTimer();
        }
    }

    private boolean isMockMode() {

        return ToeicHomeActivity.MODE_MOCK
                .equals(mode);
    }

    private void handleExit() {

        if (answers.isEmpty()) {
            stopTimer();
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Thoát bài TOEIC?")
                .setMessage(
                        "Tiến trình hiện tại chưa được lưu. Bạn có chắc muốn thoát?"
                )
                .setPositiveButton(
                        "Thoát",
                        (dialog, which) -> {
                            stopTimer();
                            finish();
                        }
                )
                .setNegativeButton(
                        "Ở lại",
                        null
                )
                .show();
    }

    private String safe(
            String value,
            String fallback
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return fallback;
        }

        return value.trim();
    }

    @Override
    protected void onDestroy() {
        stopTimer();
        super.onDestroy();
    }
}
