package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.SentenceExercise;
import adu.nttu.englishai.models.GrammarSession;
import adu.nttu.englishai.models.GrammarGameStats;
import adu.nttu.englishai.repositories.SentenceExerciseRepository;
import adu.nttu.englishai.utils.SentenceTextUtils;
import adu.nttu.englishai.utils.GrammarProgressManager;
import adu.nttu.englishai.utils.GrammarAnimationHelper;
import adu.nttu.englishai.utils.GrammarWordUiHelper;
import adu.nttu.englishai.utils.GrammarFeedbackUiHelper;
import adu.nttu.englishai.utils.GrammarResultUiHelper;

public class SentenceArrangeFragment extends Fragment {

    // =========================================================
    // VIEW
    // =========================================================

    private TextView tvGrammarTopic;
    private TextView tvGrammarLevel;
    private TextView tvAnswerPlaceholder;
    private TextView tvAvailableWordsTitle;

    private MaterialCardView cardSentenceFeedback;

    private TextView btnBackSentence;
    private TextView tvSentenceProgress;
    private TextView tvVietnameseSentence;
    private TextView tvSentenceFeedback;

    private TextView tvGrammarScore;
    private TextView tvGrammarCombo;
    private TextView tvGrammarTimer;

    private View layoutGrammarResult;
    private MaterialCardView cardGrammarResult;

    private TextView tvResultScore;
    private TextView tvResultAccuracy;
    private TextView tvResultTime;
    private TextView tvResultCombo;
    private TextView tvResultMessage;

    private MaterialButton btnPlayGrammarAgain;
    private MaterialButton btnBackToGame;

    private LinearProgressIndicator progressSentence;

    private ChipGroup layoutSelectedWords;
    private ChipGroup layoutAvailableWords;

    private MaterialButton btnCheckSentence;

    // =========================================================
    // DATA
    // =========================================================

    private SentenceExerciseRepository sentenceExerciseRepository;

    // Toàn bộ bài hợp lệ lấy từ Firestore
    private final List<SentenceExercise> allExercisePool =
            new ArrayList<>();

    // Trạng thái của lượt Grammar Sprint hiện tại
    private final GrammarSession session =
            new GrammarSession();

    private final GrammarGameStats gameStats =
            new GrammarGameStats();

    private final Handler timerHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private boolean timerRunning = false;

    private final Runnable timerRunnable =
            new Runnable() {
                @Override
                public void run() {

                    if (!timerRunning) {
                        return;
                    }

                    gameStats.incrementElapsedSecond();
                    updateGameStatsUi();

                    timerHandler.postDelayed(
                            this,
                            1000
                    );
                }
            };

    private static final int TOTAL_QUESTIONS = 10;

    // =========================================================
    // LOCAL PROGRESS
    // =========================================================

    private GrammarProgressManager progressManager;

    public SentenceArrangeFragment() {
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_sentence_arrange,
                container,
                false
        );
    }

    // =========================================================
    // VIEW CREATED
    // =========================================================

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        tvGrammarTopic =
                view.findViewById(R.id.tvGrammarTopic);

        tvGrammarLevel =
                view.findViewById(R.id.tvGrammarLevel);

        tvAnswerPlaceholder =
                view.findViewById(R.id.tvAnswerPlaceholder);

        tvAvailableWordsTitle =
                view.findViewById(R.id.tvAvailableWordsTitle);

        cardSentenceFeedback =
                view.findViewById(R.id.cardSentenceFeedback);

        btnBackSentence =
                view.findViewById(R.id.btnBackSentence);

        tvSentenceProgress =
                view.findViewById(R.id.tvSentenceProgress);

        tvVietnameseSentence =
                view.findViewById(R.id.tvVietnameseSentence);

        tvSentenceFeedback =
                view.findViewById(R.id.tvSentenceFeedback);

        tvGrammarScore =
                view.findViewById(R.id.tvGrammarScore);

        tvGrammarCombo =
                view.findViewById(R.id.tvGrammarCombo);

        tvGrammarTimer =
                view.findViewById(R.id.tvGrammarTimer);

        layoutGrammarResult =
                view.findViewById(R.id.layoutGrammarResult);

        cardGrammarResult =
                view.findViewById(R.id.cardGrammarResult);

        tvResultScore =
                view.findViewById(R.id.tvResultScore);

        tvResultAccuracy =
                view.findViewById(R.id.tvResultAccuracy);

        tvResultTime =
                view.findViewById(R.id.tvResultTime);

        tvResultCombo =
                view.findViewById(R.id.tvResultCombo);

        tvResultMessage =
                view.findViewById(R.id.tvResultMessage);

        btnPlayGrammarAgain =
                view.findViewById(R.id.btnPlayGrammarAgain);

        btnBackToGame =
                view.findViewById(R.id.btnBackToGame);

        progressSentence =
                view.findViewById(R.id.progressSentence);

        layoutSelectedWords =
                view.findViewById(R.id.layoutSelectedWords);

        layoutAvailableWords =
                view.findViewById(R.id.layoutAvailableWords);

        btnCheckSentence =
                view.findViewById(R.id.btnCheckSentence);

        progressManager =
                new GrammarProgressManager(
                        requireContext()
                );

        sentenceExerciseRepository =
                new SentenceExerciseRepository();

        setupBackButton();
        setupSystemBackButton();
        setupCheckButton();
        setupResultButtons();
        loadExercises();
    }

    // =========================================================
    // IMMERSIVE LEARNING SCREEN
    // =========================================================

    @Override
    public void onStart() {
        super.onStart();

        View bottomNavigation =
                requireActivity()
                        .findViewById(R.id.bottom_navigation);

        View aiTutor =
                requireActivity()
                        .findViewById(R.id.layoutAiTutor);

        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.GONE);
        }

        if (aiTutor != null) {
            aiTutor.setVisibility(View.GONE);
        }

        if (!session.isEmpty()
                && !session.isCompleted()) {

            startTimer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopTimer();

        if (!isSessionCompleted()) {
            saveProgress();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (!isSessionCompleted()) {
            saveProgress();
        }

        View bottomNavigation =
                requireActivity()
                        .findViewById(R.id.bottom_navigation);

        View aiTutor =
                requireActivity()
                        .findViewById(R.id.layoutAiTutor);

        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.VISIBLE);
        }

        if (aiTutor != null) {
            aiTutor.setVisibility(View.VISIBLE);
        }
    }

    // =========================================================
    // BACK
    // =========================================================

    private void setupBackButton() {
        btnBackSentence.setOnClickListener(
                view -> showExitDialog()
        );
    }

    private void setupSystemBackButton() {

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {

                                if (isSessionCompleted()) {
                                    progressManager.clearProgress();

                                    requireActivity()
                                            .getSupportFragmentManager()
                                            .popBackStack();

                                    return;
                                }

                                showExitDialog();
                            }
                        }
                );
    }

    private boolean isSessionCompleted() {
        return session.isCompleted();
    }

    private void showExitDialog() {

        if (session.isEmpty()) {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
            return;
        }

        if (isSessionCompleted()) {
            progressManager.clearProgress();

            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();

            return;
        }

        int completed =
                Math.min(
                        session.getCurrentQuestionIndex(),
                        session.getTotalQuestions()
                );

        new AlertDialog.Builder(requireContext())
                .setTitle("Bạn chưa hoàn thành bài học")
                .setMessage(
                        "Đã hoàn thành: "
                                + completed
                                + "/"
                                + session.getTotalQuestions()
                                + " câu.\n\n"
                                + "Bạn có thể lưu tiến trình và tiếp tục sau."
                )
                .setPositiveButton(
                        "Tiếp tục sau",
                        (dialog, which) -> {
                            saveProgress();

                            requireActivity()
                                    .getSupportFragmentManager()
                                    .popBackStack();
                        }
                )
                .setNeutralButton(
                        "Làm lại",
                        (dialog, which) -> {
                            progressManager.clearProgress();
                            startNewSession();
                        }
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    // =========================================================
    // CHECK / CONTINUE
    // =========================================================

    private void setupCheckButton() {

        btnCheckSentence.setOnClickListener(
                view -> {

                    String buttonText =
                            btnCheckSentence
                                    .getText()
                                    .toString();

                    if ("TIẾP TỤC".equals(buttonText)) {
                        moveToNextQuestion();
                        return;
                    }

                    if ("VỀ TRÒ CHƠI".equals(buttonText)
                            || "VỀ TỪ VỰNG".equals(buttonText)) {

                        progressManager.clearProgress();

                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();
                        return;
                    }

                    checkAnswer();
                }
        );
    }

    private void setupResultButtons() {

        if (btnPlayGrammarAgain != null) {

            btnPlayGrammarAgain.setOnClickListener(
                    view -> {

                        GrammarResultUiHelper.hideResult(
                                layoutGrammarResult
                        );

                        startNewSession();
                    }
            );
        }

        if (btnBackToGame != null) {

            btnBackToGame.setOnClickListener(
                    view -> {

                        progressManager.clearProgress();

                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();
                    }
            );
        }
    }

    // =========================================================
    // LOAD FIRESTORE
    // =========================================================

    private void loadExercises() {

        btnCheckSentence.setEnabled(false);

        tvVietnameseSentence.setText(
                "Đang tải bài luyện..."
        );

        sentenceExerciseRepository.getAllExercises(
                new SentenceExerciseRepository
                        .SentenceExerciseCallback() {

                    @Override
                    public void onSuccess(
                            List<SentenceExercise> exercises
                    ) {

                        allExercisePool.clear();

                        for (SentenceExercise exercise
                                : exercises) {

                            if (!isValidExercise(exercise)) {
                                continue;
                            }

                            allExercisePool.add(exercise);
                        }

                        if (allExercisePool.isEmpty()) {

                            tvVietnameseSentence.setText(
                                    "Chưa có bài xếp câu."
                            );

                            Toast.makeText(
                                    requireContext(),
                                    "Không tìm thấy dữ liệu hợp lệ trong sentenceExercises.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        if (progressManager.hasSavedProgress()) {
                            showResumeDialog();
                        } else {
                            startNewSession();
                        }
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        tvVietnameseSentence.setText(
                                "Không tải được bài luyện."
                        );

                        Toast.makeText(
                                requireContext(),
                                "Lỗi Firestore: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private boolean isValidExercise(
            SentenceExercise exercise
    ) {

        if (exercise == null) {
            return false;
        }

        String englishSentence =
                exercise.getEnglishSentence();

        String vietnameseMeaning =
                exercise.getVietnameseMeaning();

        if (englishSentence == null
                || englishSentence.trim().isEmpty()) {

            return false;
        }

        if (vietnameseMeaning == null
                || vietnameseMeaning.trim().isEmpty()) {

            return false;
        }

        String cleanedSentence =
                SentenceTextUtils.cleanSentence(englishSentence);

        return cleanedSentence
                .split("\\s+")
                .length >= 2;
    }

    // =========================================================
    // NEW / RESUME SESSION
    // =========================================================

    private void startNewSession() {

        GrammarResultUiHelper.hideResult(
                layoutGrammarResult
        );

        session.startNewSession(
                allExercisePool,
                TOTAL_QUESTIONS
        );

        gameStats.reset();
        updateGameStatsUi();

        progressManager.clearProgress();

        showCurrentQuestion(true);

        // Bắt đầu lưu ngay bộ 10 câu đã random
        saveProgress();
        startTimer();
    }

    private void showResumeDialog() {

        int savedIndex =
                progressManager.getCurrentIndex();

        new AlertDialog.Builder(requireContext())
                .setTitle("Tiếp tục Grammar Sprint?")
                .setMessage(
                        "Bạn có một bài đang làm dở.\n\n"
                                + "Tiến trình: "
                                + savedIndex
                                + "/"
                                + TOTAL_QUESTIONS
                                + " câu."
                )
                .setPositiveButton(
                        "Tiếp tục",
                        (dialog, which) -> {
                            if (!restoreProgress()) {
                                progressManager.clearProgress();
                                startNewSession();
                            }
                        }
                )
                .setNegativeButton(
                        "Làm lại",
                        (dialog, which) -> {
                            progressManager.clearProgress();
                            startNewSession();
                        }
                )
                .setCancelable(false)
                .show();
    }

    private boolean restoreProgress() {

        GrammarProgressManager.SavedProgress savedProgress =
                progressManager.getSavedProgress();

        List<String> savedIds =
                savedProgress.getQuestionIds();

        if (savedIds.isEmpty()) {
            return false;
        }

        Map<String, SentenceExercise> exerciseMap =
                new HashMap<>();

        for (SentenceExercise exercise
                : allExercisePool) {

            if (exercise.getId() != null) {

                exerciseMap.put(
                        exercise.getId(),
                        exercise
                );
            }
        }

        List<SentenceExercise> restoredExercises =
                new ArrayList<>();

        for (String id : savedIds) {

            SentenceExercise exercise =
                    exerciseMap.get(id);

            if (exercise != null) {
                restoredExercises.add(exercise);
            }
        }

        if (restoredExercises.isEmpty()) {
            return false;
        }

        session.restore(
                restoredExercises,
                savedProgress.getCurrentQuestionIndex(),
                savedProgress.getSelectedWords(),
                savedProgress.getAvailableWords()
        );

        gameStats.setScore(
                savedProgress.getScore()
        );
        gameStats.setCombo(
                savedProgress.getCombo()
        );
        gameStats.setBestCombo(
                savedProgress.getBestCombo()
        );
        gameStats.setCorrectCount(
                savedProgress.getCorrectCount()
        );
        gameStats.setWrongQuestionCount(
                savedProgress.getWrongQuestionCount()
        );
        gameStats.setCurrentQuestionMistakes(
                savedProgress.getCurrentMistakes()
        );
        gameStats.setElapsedSeconds(
                savedProgress.getElapsedSeconds()
        );

        updateGameStatsUi();
        startTimer();

        // Nếu dữ liệu câu đang dở không còn hợp lệ thì dựng lại câu đó
        if (session.getSelectedWords().isEmpty()
                && session.getAvailableWords().isEmpty()) {

            showCurrentQuestion(true);

        } else {

            showCurrentQuestion(false);
        }

        return true;
    }

    // =========================================================
    // SHOW CURRENT QUESTION
    // =========================================================

    private void showCurrentQuestion(
            boolean rebuildWords
    ) {

        if (session.isCompleted()) {

            showResult();
            return;
        }

        SentenceExercise exercise =
                session.getCurrentExercise();

        if (exercise == null) {
            showResult();
            return;
        }

        session.clearSelectedSwapIndex();

        cardSentenceFeedback.setVisibility(
                View.GONE
        );

        tvAvailableWordsTitle.setVisibility(
                View.VISIBLE
        );

        layoutAvailableWords.setVisibility(
                View.VISIBLE
        );

        btnCheckSentence.setVisibility(
                View.VISIBLE
        );

        btnCheckSentence.setText(
                "KIỂM TRA"
        );

        btnCheckSentence.setEnabled(
                false
        );

        tvVietnameseSentence.setText(
                exercise.getVietnameseMeaning()
        );

        GrammarAnimationHelper.animateQuestion(
                tvVietnameseSentence,
                dpToPx(8)
        );

        tvGrammarTopic.setText(
                safeDisplay(
                        exercise.getGrammarTopic(),
                        "Grammar"
                )
        );

        tvGrammarLevel.setText(
                safeDisplay(
                        exercise.getLevel(),
                        "Easy"
                )
        );

        if (rebuildWords) {

            String englishSentence =
                    SentenceTextUtils.cleanSentence(
                            exercise.getEnglishSentence()
                    );

            String[] words =
                    englishSentence
                            .split("\\s+");

            session.prepareWords(
                    words
            );
        }

        updateProgress();
        renderWordLayouts();
        saveProgress();
    }

    private String safeDisplay(
            String value,
            String fallback
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return fallback;
        }

        return value.trim();
    }

    // =========================================================
    // RENDER WORDS
    //
    // UX MỚI:
    // - Chạm 1 từ trong câu -> chọn vị trí.
    // - Chạm từ thứ 2 trong câu -> đổi chỗ trực tiếp.
    // - Nếu còn từ phía dưới: chọn 1 vị trí phía trên rồi
    //   chạm một từ phía dưới -> thay đúng vị trí đó.
    // - Không xóa từ giữa câu nên các từ phía sau KHÔNG bị dồn.
    // =========================================================

    private void renderWordLayouts() {

        GrammarWordUiHelper.renderWords(
                requireContext(),
                session,
                layoutSelectedWords,
                layoutAvailableWords,
                tvAnswerPlaceholder,
                btnCheckSentence,
                new GrammarWordUiHelper.WordClickListener() {

                    @Override
                    public void onSelectedWordClick(
                            int position
                    ) {
                        handleSelectedWordClick(
                                position
                        );
                    }

                    @Override
                    public void onAvailableWordClick(
                            int position
                    ) {
                        handleAvailableWordClick(
                                position
                        );
                    }
                }
        );

        saveProgress();
    }

    // =========================================================
    // TAP WORD IN SELECTED SENTENCE
    // =========================================================

    private void handleSelectedWordClick(
            int position
    ) {

        List<String> selectedWords =
                session.getSelectedWords();

        if (position < 0
                || position
                >= selectedWords.size()) {

            return;
        }

        hideFeedback();

        int selectedSwapIndex =
                session.getSelectedSwapIndex();

        // Chưa chọn từ nào -> chọn từ này
        if (selectedSwapIndex == -1) {

            session.setSelectedSwapIndex(
                    position
            );

            renderWordLayouts();

            return;
        }

        // Bấm lại đúng từ đang chọn -> bỏ chọn
        if (selectedSwapIndex == position) {

            session.clearSelectedSwapIndex();

            renderWordLayouts();

            return;
        }

        // Đã chọn một từ khác -> SWAP trực tiếp hai vị trí
        session.swapSelectedWords(
                selectedSwapIndex,
                position
        );

        session.clearSelectedSwapIndex();

        renderWordLayouts();
    }

    // =========================================================
    // TAP WORD IN WORD BANK
    // =========================================================

    private void handleAvailableWordClick(
            int position
    ) {

        List<String> availableWords =
                session.getAvailableWords();

        if (position < 0
                || position
                >= availableWords.size()) {

            return;
        }

        hideFeedback();

        int selectedSwapIndex =
                session.getSelectedSwapIndex();

        // Có một vị trí trên câu đang được chọn:
        // thay trực tiếp tại vị trí đó.
        if (selectedSwapIndex >= 0
                && selectedSwapIndex
                < session.getSelectedWords().size()) {

            session.replaceSelectedWithAvailable(
                    selectedSwapIndex,
                    position
            );

            session.clearSelectedSwapIndex();

            renderWordLayouts();

            return;
        }

        // Bình thường: thêm từ vào cuối câu
        session.moveAvailableWordToSelected(
                position
        );

        renderWordLayouts();
    }

    // =========================================================
    // CREATE CHIP
    // =========================================================

    // =========================================================
    // CHECK ANSWER
    // =========================================================

    private void checkAnswer() {

        if (session.isCompleted()) {
            return;
        }

        if (!session.getAvailableWords().isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "Hãy sử dụng tất cả các từ trước khi kiểm tra.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        session.clearSelectedSwapIndex();

        SentenceExercise exercise =
                session.getCurrentExercise();

        if (exercise == null) {
            return;
        }

        String correctAnswer =
                SentenceTextUtils.normalizeForCompare(
                        exercise.getEnglishSentence()
                );

        StringBuilder builder =
                new StringBuilder();

        for (String word
                : session.getSelectedWords()) {

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append(word);
        }

        String userAnswer =
                SentenceTextUtils.normalizeForCompare(
                        builder.toString()
                );

        if (correctAnswer.equals(userAnswer)) {

            int earnedPoints =
                    gameStats.registerCorrectAnswer();

            updateGameStatsUi();

            showCorrectFeedback(
                    exercise,
                    earnedPoints
            );

            btnCheckSentence.setText(
                    "TIẾP TỤC"
            );

            btnCheckSentence.setEnabled(
                    true
            );

            setWordChipsEnabled(false);

            saveProgress();

            return;
        }

        gameStats.registerWrongAttempt();
        updateGameStatsUi();

        showWrongFeedback();

        saveProgress();

        // Sau khi sai vẫn render để có thể swap trực tiếp.
        renderWordLayouts();
    }

    // =========================================================
    // CORRECT
    // =========================================================

    private void showCorrectFeedback(
            SentenceExercise exercise,
            int earnedPoints
    ) {

        GrammarFeedbackUiHelper.showCorrectFeedback(
                cardSentenceFeedback,
                tvSentenceFeedback,
                tvAvailableWordsTitle,
                layoutAvailableWords,
                exercise
        );

        tvSentenceFeedback.append(
                "\n\n⭐ +"
                        + earnedPoints
                        + " điểm"
        );

        if (gameStats.getCombo() >= 2) {
            tvSentenceFeedback.append(
                    "\n🔥 Combo x"
                            + gameStats.getCombo()
            );
        }

        GrammarAnimationHelper.animateFeedbackCard(
                cardSentenceFeedback,
                dpToPx(12)
        );

        GrammarAnimationHelper.animateSuccessButton(
                btnCheckSentence
        );
    }

    // =========================================================
    // WRONG
    // =========================================================

    private void showWrongFeedback() {

        GrammarFeedbackUiHelper.showWrongFeedback(
                cardSentenceFeedback,
                tvSentenceFeedback
        );

        GrammarAnimationHelper.animateFeedbackCard(
                cardSentenceFeedback,
                dpToPx(12)
        );

        GrammarAnimationHelper.shakeWrongAnswer(
                layoutSelectedWords,
                dpToPx(10),
                dpToPx(7)
        );
    }

    private void hideFeedback() {

        GrammarFeedbackUiHelper.hideFeedback(
                cardSentenceFeedback
        );
    }

    // =========================================================
    // ENABLE / DISABLE CHIP
    // =========================================================

    private void setWordChipsEnabled(
            boolean enabled
    ) {

        GrammarWordUiHelper.setWordChipsEnabled(
                layoutSelectedWords,
                layoutAvailableWords,
                enabled
        );
    }

    // =========================================================
    // NEXT
    // =========================================================

    private void moveToNextQuestion() {

        session.moveToNextQuestion();

        if (session.isCompleted()) {

            progressManager.clearProgress();
            showResult();
            return;
        }

        showCurrentQuestion(true);
    }

    // =========================================================
    // RESULT
    // =========================================================

    private void showResult() {

        stopTimer();

        progressManager.clearProgress();

        layoutSelectedWords.removeAllViews();
        layoutAvailableWords.removeAllViews();

        tvAnswerPlaceholder.setVisibility(
                View.GONE
        );

        tvAvailableWordsTitle.setVisibility(
                View.GONE
        );

        layoutAvailableWords.setVisibility(
                View.GONE
        );

        cardSentenceFeedback.setVisibility(
                View.GONE
        );

        tvSentenceProgress.setText(
                session.getTotalQuestions()
                        + "/"
                        + session.getTotalQuestions()
        );

        progressSentence.setMax(
                session.getTotalQuestions()
        );

        progressSentence.setProgress(
                session.getTotalQuestions()
        );

        tvGrammarTopic.setText(
                "Hoàn thành"
        );

        tvGrammarLevel.setText(
                session.getTotalQuestions()
                        + " câu"
        );

        tvVietnameseSentence.setText(
                "🎉 Grammar Sprint"
        );

        btnCheckSentence.setVisibility(
                View.GONE
        );

        GrammarResultUiHelper.showResult(
                layoutGrammarResult,
                cardGrammarResult,
                tvResultScore,
                tvResultAccuracy,
                tvResultTime,
                tvResultCombo,
                tvResultMessage,
                gameStats,
                session.getTotalQuestions()
        );
    }

    // =========================================================
    // PROGRESS BAR
    // =========================================================

    private void updateProgress() {

        int current =
                session.getCurrentQuestionIndex() + 1;

        int total =
                session.getTotalQuestions();

        tvSentenceProgress.setText(
                current
                        + "/"
                        + total
        );

        GrammarAnimationHelper.animateProgress(
                progressSentence,
                total,
                current
        );
    }

    // =========================================================
    // SAVE LOCAL PROGRESS
    // =========================================================

    private void saveProgress() {

        if (progressManager == null
                || session.isEmpty()
                || session.isCompleted()) {

            return;
        }

        progressManager.saveProgress(
                session.getExerciseList(),
                session.getCurrentQuestionIndex(),
                session.getSelectedWords(),
                session.getAvailableWords(),
                gameStats
        );
    }

    // =========================================================
    // STRING UTILS
    // =========================================================

    private void startTimer() {

        if (timerRunning
                || session.isEmpty()
                || session.isCompleted()) {

            return;
        }

        timerRunning = true;

        timerHandler.removeCallbacks(
                timerRunnable
        );

        timerHandler.postDelayed(
                timerRunnable,
                1000
        );
    }

    private void stopTimer() {

        timerRunning = false;

        timerHandler.removeCallbacks(
                timerRunnable
        );
    }

    private void updateGameStatsUi() {

        if (tvGrammarScore != null) {
            tvGrammarScore.setText(
                    "⭐ "
                            + gameStats.getScore()
            );
        }

        if (tvGrammarCombo != null) {

            if (gameStats.getCombo() >= 2) {
                tvGrammarCombo.setText(
                        "🔥 x"
                                + gameStats.getCombo()
                );
            } else {
                tvGrammarCombo.setText(
                        "🔥 x0"
                );
            }
        }

        if (tvGrammarTimer != null) {
            tvGrammarTimer.setText(
                    "⏱ "
                            + gameStats.getFormattedTime()
            );
        }
    }

    private int dpToPx(
            int dp
    ) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dp * density
        );
    }
}