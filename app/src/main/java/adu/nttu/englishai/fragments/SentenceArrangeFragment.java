package adu.nttu.englishai.fragments;

import android.animation.LayoutTransition;
import android.content.ClipData;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
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
import adu.nttu.englishai.utils.GrammarFeedbackUiHelper;
import adu.nttu.englishai.utils.GrammarResultUiHelper;

public class SentenceArrangeFragment extends Fragment {

    private View rootSentenceArrange;
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

    // DATA
    private SentenceExerciseRepository sentenceExerciseRepository;
    private final List<SentenceExercise> allExercisePool = new ArrayList<>();
    private final GrammarSession session = new GrammarSession();
    private final GrammarGameStats gameStats = new GrammarGameStats();
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean timerRunning = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning) return;
            gameStats.incrementElapsedSecond();
            updateGameStatsUi();
            timerHandler.postDelayed(this, 1000);
        }
    };

    private static final int TOTAL_QUESTIONS = 10;
    private GrammarProgressManager progressManager;

    public SentenceArrangeFragment() {}

    // CREATE VIEW
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sentence_arrange, container, false);
    }

    // VIEW CREATED
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootSentenceArrange = view.findViewById(R.id.rootSentenceArrange);
        applyTopSystemInset();

        tvGrammarTopic = view.findViewById(R.id.tvGrammarTopic);
        tvGrammarLevel = view.findViewById(R.id.tvGrammarLevel);
        tvAnswerPlaceholder = view.findViewById(R.id.tvAnswerPlaceholder);
        tvAvailableWordsTitle = view.findViewById(R.id.tvAvailableWordsTitle);
        cardSentenceFeedback = view.findViewById(R.id.cardSentenceFeedback);
        btnBackSentence = view.findViewById(R.id.btnBackSentence);
        tvSentenceProgress = view.findViewById(R.id.tvSentenceProgress);
        tvVietnameseSentence = view.findViewById(R.id.tvVietnameseSentence);
        tvSentenceFeedback = view.findViewById(R.id.tvSentenceFeedback);
        tvGrammarScore = view.findViewById(R.id.tvGrammarScore);
        tvGrammarCombo = view.findViewById(R.id.tvGrammarCombo);
        tvGrammarTimer = view.findViewById(R.id.tvGrammarTimer);
        layoutGrammarResult = view.findViewById(R.id.layoutGrammarResult);
        cardGrammarResult = view.findViewById(R.id.cardGrammarResult);
        tvResultScore = view.findViewById(R.id.tvResultScore);
        tvResultAccuracy = view.findViewById(R.id.tvResultAccuracy);
        tvResultTime = view.findViewById(R.id.tvResultTime);
        tvResultCombo = view.findViewById(R.id.tvResultCombo);
        tvResultMessage = view.findViewById(R.id.tvResultMessage);
        btnPlayGrammarAgain = view.findViewById(R.id.btnPlayGrammarAgain);
        btnBackToGame = view.findViewById(R.id.btnBackToGame);
        progressSentence = view.findViewById(R.id.progressSentence);
        layoutSelectedWords = view.findViewById(R.id.layoutSelectedWords);
        layoutAvailableWords = view.findViewById(R.id.layoutAvailableWords);
        btnCheckSentence = view.findViewById(R.id.btnCheckSentence);

        progressManager = new GrammarProgressManager(requireContext());
        sentenceExerciseRepository = new SentenceExerciseRepository();

        setupDragAndDropZones(); //KHỞI ĐỘNG CƠ CHẾ KÉO THẢ LIVE
        setupBackButton();
        setupSystemBackButton();
        setupCheckButton();
        setupResultButtons();
        loadExercises();
    }

    private void applyTopSystemInset() {
        if (rootSentenceArrange == null) return;
        final int initialLeft = rootSentenceArrange.getPaddingLeft();
        final int initialTop = rootSentenceArrange.getPaddingTop();
        final int initialRight = rootSentenceArrange.getPaddingRight();
        final int initialBottom = rootSentenceArrange.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootSentenceArrange, (view, insets) -> {
            Insets topInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(initialLeft, initialTop + topInsets.top, initialRight, initialBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(rootSentenceArrange);
    }

    @Override
    public void onStart() {
        super.onStart();
        View bottomNavigation = requireActivity().findViewById(R.id.bottom_navigation);
        View aiTutor = requireActivity().findViewById(R.id.layoutAiTutor);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.GONE);
        if (aiTutor != null) aiTutor.setVisibility(View.GONE);
        if (!session.isEmpty() && !session.isCompleted()) startTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
        if (!isSessionCompleted()) saveProgress();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!isSessionCompleted()) saveProgress();
        View bottomNavigation = requireActivity().findViewById(R.id.bottom_navigation);
        View aiTutor = requireActivity().findViewById(R.id.layoutAiTutor);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.VISIBLE);
        if (aiTutor != null) aiTutor.setVisibility(View.VISIBLE);
    }

    private void setupBackButton() {
        btnBackSentence.setOnClickListener(view -> showExitDialog());
    }

    private void setupSystemBackButton() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isSessionCompleted()) {
                    progressManager.clearProgress();
                    requireActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
                showExitDialog();
            }
        });
    }

    private boolean isSessionCompleted() {
        return session.isCompleted();
    }

    private void showExitDialog() {
        if (session.isEmpty()) {
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        if (isSessionCompleted()) {
            progressManager.clearProgress();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        int completed = Math.min(session.getCurrentQuestionIndex(), session.getTotalQuestions());
        new AlertDialog.Builder(requireContext())
                .setTitle("Bạn chưa hoàn thành bài học")
                .setMessage("Đã hoàn thành: " + completed + "/" + session.getTotalQuestions() + " câu.\n\nBạn có thể lưu tiến trình và tiếp tục sau.")
                .setPositiveButton("Tiếp tục sau", (dialog, which) -> {
                    saveProgress();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNeutralButton("Làm lại", (dialog, which) -> {
                    progressManager.clearProgress();
                    startNewSession();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupCheckButton() {
        btnCheckSentence.setOnClickListener(view -> {
            String buttonText = btnCheckSentence.getText().toString();
            if ("TIẾP TỤC".equals(buttonText)) {
                moveToNextQuestion();
                return;
            }
            if ("VỀ TRÒ CHƠI".equals(buttonText) || "VỀ TỪ VỰNG".equals(buttonText)) {
                progressManager.clearProgress();
                requireActivity().getSupportFragmentManager().popBackStack();
                return;
            }
            checkAnswer();
        });
    }

    private void setupResultButtons() {
        if (btnPlayGrammarAgain != null) {
            btnPlayGrammarAgain.setOnClickListener(view -> {
                GrammarResultUiHelper.hideResult(layoutGrammarResult);
                startNewSession();
            });
        }
        if (btnBackToGame != null) {
            btnBackToGame.setOnClickListener(view -> {
                progressManager.clearProgress();
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }
    }

    private void loadExercises() {
        btnCheckSentence.setEnabled(false);
        tvVietnameseSentence.setText("Đang tải bài luyện...");

        sentenceExerciseRepository.getAllExercises(new SentenceExerciseRepository.SentenceExerciseCallback() {
            @Override
            public void onSuccess(List<SentenceExercise> exercises) {
                allExercisePool.clear();
                for (SentenceExercise exercise : exercises) {
                    if (!isValidExercise(exercise)) continue;
                    allExercisePool.add(exercise);
                }
                if (allExercisePool.isEmpty()) {
                    tvVietnameseSentence.setText("Chưa có bài xếp câu.");
                    Toast.makeText(requireContext(), "Không tìm thấy dữ liệu hợp lệ trong sentenceExercises.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (progressManager.hasSavedProgress()) {
                    showResumeDialog();
                } else {
                    startNewSession();
                }
            }

            @Override
            public void onFailure(Exception exception) {
                tvVietnameseSentence.setText("Không tải được bài luyện.");
                Toast.makeText(requireContext(), "Lỗi Firestore: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isValidExercise(SentenceExercise exercise) {
        if (exercise == null) return false;
        String englishSentence = exercise.getEnglishSentence();
        String vietnameseMeaning = exercise.getVietnameseMeaning();
        if (englishSentence == null || englishSentence.trim().isEmpty()) return false;
        if (vietnameseMeaning == null || vietnameseMeaning.trim().isEmpty()) return false;
        String cleanedSentence = SentenceTextUtils.cleanSentence(englishSentence);
        return cleanedSentence.split("\\s+").length >= 2;
    }

    private void startNewSession() {
        GrammarResultUiHelper.hideResult(layoutGrammarResult);
        session.startNewSession(allExercisePool, TOTAL_QUESTIONS);
        gameStats.reset();
        updateGameStatsUi();
        progressManager.clearProgress();
        showCurrentQuestion(true);
        saveProgress();
        startTimer();
    }

    private void showResumeDialog() {
        int savedIndex = progressManager.getCurrentIndex();
        new AlertDialog.Builder(requireContext())
                .setTitle("Tiếp tục Grammar Sprint?")
                .setMessage("Bạn có một bài đang làm dở.\n\nTiến trình: " + Math.min(savedIndex + 1, TOTAL_QUESTIONS) + "/" + TOTAL_QUESTIONS + " câu.")
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    if (!restoreProgress()) {
                        progressManager.clearProgress();
                        startNewSession();
                    }
                })
                .setNegativeButton("Làm lại", (dialog, which) -> {
                    progressManager.clearProgress();
                    startNewSession();
                })
                .setCancelable(false)
                .show();
    }

    private boolean restoreProgress() {
        GrammarProgressManager.SavedProgress savedProgress = progressManager.getSavedProgress();
        List<String> savedIds = savedProgress.getQuestionIds();
        if (savedIds.isEmpty()) return false;
        Map<String, SentenceExercise> exerciseMap = new HashMap<>();
        for (SentenceExercise exercise : allExercisePool) {
            if (exercise.getId() != null) exerciseMap.put(exercise.getId(), exercise);
        }
        List<SentenceExercise> restoredExercises = new ArrayList<>();
        for (String id : savedIds) {
            SentenceExercise exercise = exerciseMap.get(id);
            if (exercise != null) restoredExercises.add(exercise);
        }
        if (restoredExercises.isEmpty()) return false;

        session.restore(restoredExercises, savedProgress.getCurrentQuestionIndex(), savedProgress.getSelectedWords(), savedProgress.getAvailableWords());
        gameStats.setScore(savedProgress.getScore());
        gameStats.setCombo(savedProgress.getCombo());
        gameStats.setBestCombo(savedProgress.getBestCombo());
        gameStats.setCorrectCount(savedProgress.getCorrectCount());
        gameStats.setWrongQuestionCount(savedProgress.getWrongQuestionCount());
        gameStats.setCurrentQuestionMistakes(savedProgress.getCurrentMistakes());
        gameStats.setElapsedSeconds(savedProgress.getElapsedSeconds());

        updateGameStatsUi();
        startTimer();

        if (session.getSelectedWords().isEmpty() && session.getAvailableWords().isEmpty()) {
            showCurrentQuestion(true);
        } else {
            showCurrentQuestion(false);
        }
        return true;
    }

    private void showCurrentQuestion(boolean rebuildWords) {
        if (session.isCompleted()) {
            showResult(); return;
        }
        SentenceExercise exercise = session.getCurrentExercise();
        if (exercise == null) {
            showResult(); return;
        }

        session.clearSelectedSwapIndex();
        cardSentenceFeedback.setVisibility(View.GONE);
        tvAvailableWordsTitle.setVisibility(View.VISIBLE);
        layoutAvailableWords.setVisibility(View.VISIBLE);
        btnCheckSentence.setVisibility(View.VISIBLE);
        btnCheckSentence.setText("KIỂM TRA");
        btnCheckSentence.setEnabled(!session.getSelectedWords().isEmpty());
        tvVietnameseSentence.setText(exercise.getVietnameseMeaning());
        GrammarAnimationHelper.animateQuestion(tvVietnameseSentence, dpToPx(8));
        tvGrammarTopic.setText(safeDisplay(exercise.getGrammarTopic(), "Grammar"));
        tvGrammarLevel.setText(safeDisplay(exercise.getLevel(), "Easy"));

        if (rebuildWords) {
            String englishSentence = SentenceTextUtils.cleanSentence(exercise.getEnglishSentence());
            String[] words = englishSentence.split("\\s+");
            session.prepareWords(words);
        }

        updateProgress();
        renderWordLayouts();
        saveProgress();
    }

    private String safeDisplay(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    // ĐỘNG CƠ KÉO THẢ LIVE SWAP (TÀNG HÌNH HOÀN HẢO NATIVE)
    private void setupDragAndDropZones() {
        if (layoutSelectedWords == null || layoutAvailableWords == null) return;

        // BẬT HIỆU ỨNG TRƯỢT TỰ ĐỘNG CHO CHIPGROUP
        LayoutTransition transition = new LayoutTransition();
        transition.enableTransitionType(LayoutTransition.CHANGING);
        transition.setDuration(150); // Tốc độ trượt
        layoutSelectedWords.setLayoutTransition(transition);
        layoutAvailableWords.setLayoutTransition(transition);

        // ZONE TRÊN: Câu của bạn
        layoutSelectedWords.setOnDragListener((v, event) -> {
            if (btnCheckSentence.getText().toString().equals("TIẾP TỤC")) return false;
            View draggedView = (View) event.getLocalState();
            if (draggedView == null) return false;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (draggedView.getParent() != layoutSelectedWords) {
                        ViewGroup oldParent = (ViewGroup) draggedView.getParent();
                        if (oldParent != null) oldParent.removeView(draggedView);
                        layoutSelectedWords.addView(draggedView);
                        tvAnswerPlaceholder.setVisibility(View.GONE);
                    }
                    return true;
                case DragEvent.ACTION_DROP:
                case DragEvent.ACTION_DRAG_ENDED:
                    // Khôi phục hiển thị
                    draggedView.setVisibility(View.VISIBLE);
                    if (layoutSelectedWords.getChildCount() == 0) tvAnswerPlaceholder.setVisibility(View.VISIBLE);
                    syncSessionWithUI();
                    return true;
            }
            return true;
        });

        // ZONE DƯỚI: Các từ có sẵn
        layoutAvailableWords.setOnDragListener((v, event) -> {
            if (btnCheckSentence.getText().toString().equals("TIẾP TỤC")) return false;
            View draggedView = (View) event.getLocalState();
            if (draggedView == null) return false;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (draggedView.getParent() != layoutAvailableWords) {
                        ViewGroup oldParent = (ViewGroup) draggedView.getParent();
                        if (oldParent != null) oldParent.removeView(draggedView);
                        layoutAvailableWords.addView(draggedView);
                        if (layoutSelectedWords.getChildCount() == 0) tvAnswerPlaceholder.setVisibility(View.VISIBLE);
                    }
                    return true;
                case DragEvent.ACTION_DROP:
                case DragEvent.ACTION_DRAG_ENDED:
                    // Khôi phục hiển thị (Tắt tàng hình)
                    draggedView.setVisibility(View.VISIBLE);
                    syncSessionWithUI();
                    return true;
            }
            return true;
        });
    }

    private void renderWordLayouts() {
        layoutSelectedWords.removeAllViews();
        layoutAvailableWords.removeAllViews();

        for (String word : session.getSelectedWords()) {
            Chip chip = createChip(word);
            setupChipEvents(chip);
            layoutSelectedWords.addView(chip);
        }

        for (String word : session.getAvailableWords()) {
            Chip chip = createChip(word);
            setupChipEvents(chip);
            layoutAvailableWords.addView(chip);
        }

        tvAnswerPlaceholder.setVisibility(session.getSelectedWords().isEmpty() ? View.VISIBLE : View.GONE);
        btnCheckSentence.setEnabled(!session.getSelectedWords().isEmpty());
    }

    private void setupChipEvents(Chip chip) {

        // CHẠM NHANH
        chip.setOnClickListener(v -> {
            if (btnCheckSentence.getText().toString().equals("TIẾP TỤC")) return;

            ViewGroup currentParent = (ViewGroup) chip.getParent();
            if (currentParent == layoutAvailableWords) {
                layoutAvailableWords.removeView(chip);
                layoutSelectedWords.addView(chip);
                tvAnswerPlaceholder.setVisibility(View.GONE);
            } else if (currentParent == layoutSelectedWords) {
                layoutSelectedWords.removeView(chip);
                layoutAvailableWords.addView(chip);
                if (layoutSelectedWords.getChildCount() == 0) tvAnswerPlaceholder.setVisibility(View.VISIBLE);
            }
            syncSessionWithUI();
        });

        // BẮT ĐẦU KÉO
        chip.setOnLongClickListener(v -> {
            if (btnCheckSentence.getText().toString().equals("TIẾP TỤC")) return false;
            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            androidx.core.view.ViewCompat.startDragAndDrop(v, data, shadowBuilder, v, 0);

            // Tàng hình 100% để ép thẻ khác dạt ra nhường chỗ trống
            v.setVisibility(View.INVISIBLE);
            return true;
        });

        // BÓNG BAY NGANG QUA TỪ KHÁC
        chip.setOnDragListener((v, event) -> {
            if (btnCheckSentence.getText().toString().equals("TIẾP TỤC")) return false;
            View draggedView = (View) event.getLocalState();
            if (draggedView == null || draggedView == v) return false;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    ViewGroup targetParent = (ViewGroup) v.getParent();
                    if (targetParent == layoutSelectedWords) {
                        ViewGroup oldParent = (ViewGroup) draggedView.getParent();
                        if (oldParent != null) oldParent.removeView(draggedView);

                        int targetIndex = targetParent.indexOfChild(v);
                        targetParent.addView(draggedView, targetIndex);
                        tvAnswerPlaceholder.setVisibility(View.GONE);
                    }
                    return true;
                case DragEvent.ACTION_DROP:
                case DragEvent.ACTION_DRAG_ENDED:
                    // Khôi phục hiển thị
                    draggedView.setVisibility(View.VISIBLE);
                    syncSessionWithUI();
                    return true;
            }
            return true;
        });
    }

    private Chip createChip(String text) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(false);
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setTextColor(Color.parseColor("#1A73E8"));
        chip.setTextSize(16f);
        return chip;
    }

    // ĐỒNG BỘ GIAO DIỆN VỀ BỘ NHỚ SESSION
    private void syncSessionWithUI() {
        if (layoutSelectedWords == null || layoutAvailableWords == null) return;

        List<String> selectedWords = session.getSelectedWords();
        List<String> availableWords = session.getAvailableWords();

        selectedWords.clear();
        for (int i = 0; i < layoutSelectedWords.getChildCount(); i++) {
            selectedWords.add(((Chip) layoutSelectedWords.getChildAt(i)).getText().toString());
        }

        availableWords.clear();
        for (int i = 0; i < layoutAvailableWords.getChildCount(); i++) {
            availableWords.add(((Chip) layoutAvailableWords.getChildAt(i)).getText().toString());
        }

        btnCheckSentence.setEnabled(!selectedWords.isEmpty());
        saveProgress();
    }


    private void checkAnswer() {
        if (session.isCompleted()) return;

        if (session.getSelectedWords().isEmpty()) {
            Toast.makeText(requireContext(), "Hãy kéo ít nhất một từ vào câu của bạn.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!session.getAvailableWords().isEmpty()) {
            showIncompleteFeedback();
            return;
        }

        session.clearSelectedSwapIndex();
        SentenceExercise exercise = session.getCurrentExercise();
        if (exercise == null) return;

        String correctAnswer = SentenceTextUtils.normalizeForCompare(exercise.getEnglishSentence());
        StringBuilder builder = new StringBuilder();
        for (String word : session.getSelectedWords()) {
            if (builder.length() > 0) builder.append(" ");
            builder.append(word);
        }

        String userAnswer = SentenceTextUtils.normalizeForCompare(builder.toString());

        if (correctAnswer.equals(userAnswer)) {
            int earnedPoints = gameStats.registerCorrectAnswer();
            updateGameStatsUi();
            showCorrectFeedback(exercise, earnedPoints);
            btnCheckSentence.setText("TIẾP TỤC");
            btnCheckSentence.setEnabled(true);
            setWordChipsEnabled(false);
            saveProgress();
            return;
        }

        gameStats.registerWrongAttempt();
        updateGameStatsUi();
        showWrongFeedback();
        saveProgress();
    }

    private void showCorrectFeedback(SentenceExercise exercise, int earnedPoints) {
        GrammarFeedbackUiHelper.showCorrectFeedback(cardSentenceFeedback, tvSentenceFeedback, tvAvailableWordsTitle, layoutAvailableWords, exercise);
        tvSentenceFeedback.append("\n\n⭐ +" + earnedPoints + " điểm");
        if (gameStats.getCombo() >= 2) tvSentenceFeedback.append("\n🔥 Combo x" + gameStats.getCombo());
        GrammarAnimationHelper.animateFeedbackCard(cardSentenceFeedback, dpToPx(12));
    }

    private void showIncompleteFeedback() {
        int remainingWords = session.getAvailableWords().size();
        cardSentenceFeedback.setVisibility(View.VISIBLE);
        tvSentenceFeedback.setText("⚠️ Câu chưa hoàn chỉnh.\n\nBạn còn " + remainingWords + " từ chưa sử dụng.\nNhấn giữ và kéo các từ vào đúng vị trí rồi kiểm tra lại.");
        tvSentenceFeedback.setTextColor(android.graphics.Color.parseColor("#A46B08"));
        GrammarAnimationHelper.animateFeedbackCard(cardSentenceFeedback, dpToPx(12));
        btnCheckSentence.setEnabled(true);
    }

    private void showWrongFeedback() {
        GrammarFeedbackUiHelper.showWrongFeedback(cardSentenceFeedback, tvSentenceFeedback);
        GrammarAnimationHelper.animateFeedbackCard(cardSentenceFeedback, dpToPx(12));
    }

    private void setWordChipsEnabled(boolean enabled) {
        for (int i = 0; i < layoutSelectedWords.getChildCount(); i++) {
            layoutSelectedWords.getChildAt(i).setEnabled(enabled);
        }
        for (int i = 0; i < layoutAvailableWords.getChildCount(); i++) {
            layoutAvailableWords.getChildAt(i).setEnabled(enabled);
        }
    }

    private void moveToNextQuestion() {
        session.moveToNextQuestion();
        if (session.isCompleted()) {
            progressManager.clearProgress();
            showResult();
            return;
        }
        showCurrentQuestion(true);
    }

    private void showResult() {
        stopTimer();
        progressManager.clearProgress();
        layoutSelectedWords.removeAllViews();
        layoutAvailableWords.removeAllViews();
        tvAnswerPlaceholder.setVisibility(View.GONE);
        tvAvailableWordsTitle.setVisibility(View.GONE);
        layoutAvailableWords.setVisibility(View.GONE);
        cardSentenceFeedback.setVisibility(View.GONE);

        tvSentenceProgress.setText(session.getTotalQuestions() + "/" + session.getTotalQuestions());
        progressSentence.setMax(session.getTotalQuestions());
        progressSentence.setProgress(session.getTotalQuestions());
        tvGrammarTopic.setText("Hoàn thành");
        tvGrammarLevel.setText(session.getTotalQuestions() + " câu");
        tvVietnameseSentence.setText("🎉 Grammar Sprint");
        btnCheckSentence.setVisibility(View.GONE);
        updateGameStatsUi();

        GrammarResultUiHelper.showResult(layoutGrammarResult, cardGrammarResult, tvResultScore, tvResultAccuracy, tvResultTime, tvResultCombo, tvResultMessage, gameStats, session.getTotalQuestions());
    }

    private void updateProgress() {
        int current = session.getCurrentQuestionIndex() + 1;
        int total = session.getTotalQuestions();
        tvSentenceProgress.setText(current + "/" + total);
        GrammarAnimationHelper.animateProgress(progressSentence, total, current);
    }

    private void saveProgress() {
        if (progressManager == null || session.isEmpty() || session.isCompleted()) return;
        progressManager.saveProgress(session.getExerciseList(), session.getCurrentQuestionIndex(), session.getSelectedWords(), session.getAvailableWords(), gameStats);
    }

    private void startTimer() {
        if (timerRunning || session.isEmpty() || session.isCompleted()) return;
        timerRunning = true;
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateGameStatsUi() {
        if (tvGrammarScore != null) tvGrammarScore.setText("⭐ " + gameStats.getScore());
        if (tvGrammarCombo != null) {
            if (gameStats.getCombo() >= 2) tvGrammarCombo.setText("🔥 x" + gameStats.getCombo());
            else tvGrammarCombo.setText("🔥 x0");
        }
        if (tvGrammarTimer != null) tvGrammarTimer.setText("⏱ " + gameStats.getFormattedTime());
    }

    @Override
    public void onDestroyView() {
        stopTimer();
        timerHandler.removeCallbacksAndMessages(null);
        rootSentenceArrange = null;
        super.onDestroyView();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}