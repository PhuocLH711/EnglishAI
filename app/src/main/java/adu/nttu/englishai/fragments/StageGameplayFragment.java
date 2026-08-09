package adu.nttu.englishai.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

public class StageGameplayFragment extends Fragment {

    // UI Elements
    private ProgressBar progressBar;
    private TextView tvHearts, tvQuestionTitle, tvTargetWord, tvFeedback;
    private LinearLayout layoutMultipleChoice, layoutTyping, layoutSentenceArrange;
    private Button btnAns1, btnAns2, btnAns3, btnAns4, btnCheck;
    private EditText edtTypingAnswer;
    private ChipGroup layoutSelectedWords, layoutAvailableWords;
    private nl.dionsegijn.konfetti.xml.KonfettiView konfettiView;

    // Logic Variables
    private List<GameQuestion> playList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int hearts = 3, targetScore = 0, correctAnswers = 0, xpReward = 50, numQuestions = 10;

    private GameQuestion currentQuestion;
    private String selectedAnswer = "";
    private boolean isChecked = false;

    private MediaPlayer soundCorrect, soundWrong;
    private String difficultyLevel = "Easy";

    // Class đại diện cho Câu hỏi
    private static class GameQuestion {
        int type; // 0: Trắc nghiệm, 1: Gõ phím, 2: Xếp câu
        Vocabulary vocab;
        String engSentence;
        String vieSentence;

        GameQuestion(int type, Vocabulary vocab) {
            this.type = type; this.vocab = vocab;
        }
        GameQuestion(int type, String engSentence, String vieSentence) {
            this.type = type; this.engSentence = engSentence; this.vieSentence = vieSentence;
        }
    }

    public StageGameplayFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stage_gameplay, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ UI
        view.findViewById(R.id.btnClose).setOnClickListener(v -> {
            if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
        });

        progressBar = view.findViewById(R.id.progressBar);
        tvHearts = view.findViewById(R.id.tvHearts);
        tvQuestionTitle = view.findViewById(R.id.tvQuestionTitle);
        tvTargetWord = view.findViewById(R.id.tvTargetWord);
        tvFeedback = view.findViewById(R.id.tvFeedback);

        layoutMultipleChoice = view.findViewById(R.id.layoutMultipleChoice);
        layoutTyping = view.findViewById(R.id.layoutTyping);
        layoutSentenceArrange = view.findViewById(R.id.layoutSentenceArrange);

        edtTypingAnswer = view.findViewById(R.id.edtTypingAnswer);
        btnCheck = view.findViewById(R.id.btnCheck);
        konfettiView = view.findViewById(R.id.konfettiView);

        layoutSelectedWords = view.findViewById(R.id.layoutSelectedWords);
        layoutAvailableWords = view.findViewById(R.id.layoutAvailableWords);

        btnAns1 = view.findViewById(R.id.btnAns1);
        btnAns2 = view.findViewById(R.id.btnAns2);
        btnAns3 = view.findViewById(R.id.btnAns3);
        btnAns4 = view.findViewById(R.id.btnAns4);

        if (getContext() != null) {
            soundCorrect = MediaPlayer.create(getContext(), R.raw.correct);
            soundWrong = MediaPlayer.create(getContext(), R.raw.wrong);
        }

        if (getArguments() != null) {
            difficultyLevel = getArguments().getString("DIFFICULTY_LEVEL", "Easy");
        }

        // ĐĂNG KÝ VÙNG NHẬN SỰ KIỆN "THẢ" CHO TOÀN BỘ KHUNG SELECTED WORDS
        if (layoutSelectedWords != null) {
            layoutSelectedWords.setOnDragListener((v, event) -> {
                if (isChecked) return false;
                View draggedView = (View) event.getLocalState();
                if (draggedView == null) return false;

                if (event.getAction() == DragEvent.ACTION_DROP) {
                    ViewGroup parent = (ViewGroup) draggedView.getParent();
                    if (parent == layoutSelectedWords) {
                        // Nếu thẻ bị thả vào khoảng trống, quăng nó xuống cuối hàng
                        parent.removeView(draggedView);
                        parent.addView(draggedView);
                    }
                } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    draggedView.setAlpha(1.0f);
                }
                return true;
            });
        }

        prepareData();
        setupClickListeners();
    }

    private void prepareData() {
        if (btnCheck != null) btnCheck.setEnabled(false);
        if (tvTargetWord != null) tvTargetWord.setText("Đang tải dữ liệu Ải...");

        switch (difficultyLevel) {
            case "Easy": hearts = 5; xpReward = 30; numQuestions = 10; break;
            case "Medium": hearts = 3; xpReward = 50; numQuestions = 10; break;
            case "Hard": hearts = 2; xpReward = 80; numQuestions = 12; break;
            case "Boss": hearts = 1; xpReward = 200; numQuestions = 15; break;
        }
        if (tvHearts != null) tvHearts.setText("❤️ " + hearts);

        List<Vocabulary> vocabList = new ArrayList<>();
        for (Vocabulary v : DataRepository.getInstance().getVocabularyList()) {
            if (v.getDifficulty() != null && v.getDifficulty().equalsIgnoreCase(difficultyLevel)) vocabList.add(v);
        }
        if (vocabList.size() < 10) vocabList.addAll(DataRepository.getInstance().getVocabularyList());

        if (vocabList.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi: Kho từ vựng đang trống!", Toast.LENGTH_LONG).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
            return;
        }
        Collections.shuffle(vocabList);

        FirebaseFirestore.getInstance().collection("sentenceExercises").get().addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) return;
            List<GameQuestion> sentenceList = new ArrayList<>();
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String eng = doc.getString("englishSentence");
                    String vie = doc.getString("vietnameseMeaning");
                    if (eng != null && vie != null && !eng.trim().isEmpty()) {
                        sentenceList.add(new GameQuestion(2, eng, vie));
                    }
                }
            }
            buildStagePlaylist(vocabList, sentenceList);
        });
    }

    private void buildStagePlaylist(List<Vocabulary> vocabList, List<GameQuestion> sentenceList) {
        playList.clear();
        Collections.shuffle(sentenceList);

        if (difficultyLevel.equals("Easy")) {
            for(int i=0; i<numQuestions; i++) playList.add(new GameQuestion(0, vocabList.get(i % vocabList.size())));
        }
        else if (difficultyLevel.equals("Medium")) {
            int half = numQuestions / 2;
            for(int i=0; i<half; i++) playList.add(new GameQuestion(0, vocabList.get(i % vocabList.size())));
            for(int i=0; i<half; i++) {
                if(sentenceList.size() > i) playList.add(sentenceList.get(i));
                else playList.add(new GameQuestion(0, vocabList.get(i % vocabList.size())));
            }
        }
        else if (difficultyLevel.equals("Hard")) {
            int half = numQuestions / 2;
            for(int i=0; i<half; i++) {
                if(sentenceList.size() > i) playList.add(sentenceList.get(i));
                else playList.add(new GameQuestion(1, vocabList.get(i % vocabList.size())));
            }
            for(int i=0; i<half; i++) playList.add(new GameQuestion(1, vocabList.get((i+half) % vocabList.size())));
        }
        else {
            for(int i=0; i<numQuestions; i++) playList.add(new GameQuestion(1, vocabList.get(i % vocabList.size())));
        }

        Collections.shuffle(playList);
        targetScore = playList.size();

        if (progressBar != null) {
            progressBar.setMax(targetScore);
            progressBar.setProgress(0);
        }

        if (btnCheck != null) btnCheck.setEnabled(true);
        loadQuestion();
    }

    private void loadQuestion() {
        if (!isAdded()) return;

        if (correctAnswers >= targetScore || currentQuestionIndex >= playList.size()) {
            showWinDialog(); return;
        }

        isChecked = false;
        btnCheck.setText("KIỂM TRA");
        btnCheck.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1976D2")));
        tvFeedback.setVisibility(View.GONE);
        selectedAnswer = "";
        edtTypingAnswer.setText("");
        resetButtonColors();

        if (layoutSelectedWords != null) layoutSelectedWords.removeAllViews();
        if (layoutAvailableWords != null) layoutAvailableWords.removeAllViews();

        currentQuestion = playList.get(currentQuestionIndex);

        if (layoutMultipleChoice != null) layoutMultipleChoice.setVisibility(View.GONE);
        if (layoutTyping != null) layoutTyping.setVisibility(View.GONE);
        if (layoutSentenceArrange != null) layoutSentenceArrange.setVisibility(View.GONE);

        if (currentQuestion.type == 0) {
            if (layoutMultipleChoice != null) layoutMultipleChoice.setVisibility(View.VISIBLE);
            tvQuestionTitle.setText("Chọn nghĩa đúng của từ này:");
            tvTargetWord.setText(currentQuestion.vocab.getEnglishWord() != null ? currentQuestion.vocab.getEnglishWord() : "");

            List<String> options = new ArrayList<>();
            String correctMeaning = currentQuestion.vocab.getVietnameseMeaning();
            options.add(correctMeaning != null ? correctMeaning : "Lỗi: Chưa có nghĩa");

            List<Vocabulary> dummyList = new ArrayList<>(DataRepository.getInstance().getVocabularyList());
            Collections.shuffle(dummyList);
            for (Vocabulary v : dummyList) {
                if (options.size() >= 4) break;
                String dummyMeaning = v.getVietnameseMeaning();
                if (dummyMeaning != null && !options.contains(dummyMeaning)) {
                    options.add(dummyMeaning);
                }
            }

            int dummyCount = 1;
            while (options.size() < 4) {
                options.add("Đáp án ngẫu nhiên " + dummyCount);
                dummyCount++;
            }

            Collections.shuffle(options);
            if (btnAns1 != null) btnAns1.setText(options.get(0));
            if (btnAns2 != null) btnAns2.setText(options.get(1));
            if (btnAns3 != null) btnAns3.setText(options.get(2));
            if (btnAns4 != null) btnAns4.setText(options.get(3));
        }
        else if (currentQuestion.type == 1) {
            if (layoutTyping != null) layoutTyping.setVisibility(View.VISIBLE);
            tvQuestionTitle.setText("Viết từ tiếng Anh của từ sau:");
            tvTargetWord.setText(currentQuestion.vocab.getVietnameseMeaning());
        }
        else if (currentQuestion.type == 2) {
            if (layoutSentenceArrange != null) layoutSentenceArrange.setVisibility(View.VISIBLE);
            tvQuestionTitle.setText("Sắp xếp các từ thành câu đúng:");
            tvTargetWord.setText(currentQuestion.vieSentence);

            if (currentQuestion.engSentence != null) {
                String[] words = currentQuestion.engSentence.replaceAll("[.,!?]", "").trim().split("\\s+");
                List<String> shuffledWords = new ArrayList<>();
                for(String w : words) if (!w.isEmpty()) shuffledWords.add(w);
                Collections.shuffle(shuffledWords);

                for (String word : shuffledWords) {
                    Chip chip = createChip(word);
                    setupChipEvents(chip); // 👉 SỬ DỤNG HÀM DRAG & DROP MỚI
                    if (layoutAvailableWords != null) layoutAvailableWords.addView(chip);
                }
            }
        }
    }

    // =======================================================================
    // 🛡️ BỘ ĐỘNG CƠ KÉO THẢ (DRAG & DROP) SIÊU MƯỢT VÀ AN TOÀN
    // =======================================================================
    private void setupChipEvents(Chip chip) {

        // 1. CHẠM NHANH: Đẩy từ lên hoặc ném từ xuống (Như cũ)
        chip.setOnClickListener(v -> {
            if (isChecked) return;
            ViewGroup currentParent = (ViewGroup) chip.getParent();
            if (currentParent == layoutAvailableWords) {
                layoutAvailableWords.removeView(chip);
                layoutSelectedWords.addView(chip);
            } else if (currentParent == layoutSelectedWords) {
                layoutSelectedWords.removeView(chip);
                layoutAvailableWords.addView(chip);
            }
        });

        // 2. NHẤN GIỮ: Bắt đầu quá trình kéo thả (Chỉ cho phép khi từ đang ở khung trên)
        chip.setOnLongClickListener(v -> {
            if (isChecked || chip.getParent() != layoutSelectedWords) return false;

            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);

            // Tương thích mọi phiên bản Android
            androidx.core.view.ViewCompat.startDragAndDrop(v, data, shadowBuilder, v, 0);
            v.setAlpha(0.3f); // Làm mờ chiếc thẻ đang bị nhấc lên
            return true;
        });

        // 3. THẢ: Khi bạn thả chiếc thẻ đang cầm đè lên 1 chiếc thẻ khác
        chip.setOnDragListener((v, event) -> {
            if (isChecked) return false;
            View draggedView = (View) event.getLocalState();
            if (draggedView == null || draggedView == v) return false;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (v.getParent() == layoutSelectedWords) v.setAlpha(0.5f); // Hiệu ứng hover nhường chỗ
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    if (v.getParent() == layoutSelectedWords) v.setAlpha(1.0f);
                    return true;
                case DragEvent.ACTION_DROP:
                    if (v.getParent() == layoutSelectedWords) v.setAlpha(1.0f);

                    ViewGroup parent = (ViewGroup) v.getParent();
                    if (parent == layoutSelectedWords && draggedView.getParent() == layoutSelectedWords) {
                        // Tính toán và tráo đổi vị trí
                        int targetIndex = parent.indexOfChild(v);
                        parent.removeView(draggedView);
                        parent.addView(draggedView, targetIndex);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    draggedView.setAlpha(1.0f);
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

    private void setupClickListeners() {
        View.OnClickListener ansClickListener = v -> {
            if (isChecked) return;
            resetButtonColors();
            v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BBDEFB")));
            selectedAnswer = ((Button) v).getText().toString();
        };

        if (btnAns1 != null) btnAns1.setOnClickListener(ansClickListener);
        if (btnAns2 != null) btnAns2.setOnClickListener(ansClickListener);
        if (btnAns3 != null) btnAns3.setOnClickListener(ansClickListener);
        if (btnAns4 != null) btnAns4.setOnClickListener(ansClickListener);

        if (btnCheck != null) {
            btnCheck.setOnClickListener(v -> {
                if (!isChecked) checkAnswer();
                else {
                    currentQuestionIndex++;
                    loadQuestion();
                }
            });
        }
    }

    private void checkAnswer() {
        boolean isCorrect = false;
        String rightAnswerStr = "";

        if (currentQuestion.type == 0) {
            if (selectedAnswer.isEmpty()) return;
            isCorrect = selectedAnswer.equals(currentQuestion.vocab.getVietnameseMeaning());
            rightAnswerStr = currentQuestion.vocab.getVietnameseMeaning();
        }
        else if (currentQuestion.type == 1) {
            if (edtTypingAnswer != null) selectedAnswer = edtTypingAnswer.getText().toString().trim();
            if (selectedAnswer.isEmpty()) return;
            isCorrect = selectedAnswer.equalsIgnoreCase(currentQuestion.vocab.getEnglishWord());
            rightAnswerStr = currentQuestion.vocab.getEnglishWord();
        }
        else if (currentQuestion.type == 2) {
            if (layoutSelectedWords != null && layoutSelectedWords.getChildCount() == 0) {
                Toast.makeText(getContext(), "Vui lòng chọn ít nhất 1 từ!", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            if (layoutSelectedWords != null) {
                for (int i = 0; i < layoutSelectedWords.getChildCount(); i++) {
                    Chip c = (Chip) layoutSelectedWords.getChildAt(i);
                    sb.append(c.getText().toString()).append(" ");
                }
            }
            selectedAnswer = sb.toString().trim();
            rightAnswerStr = currentQuestion.engSentence != null ? currentQuestion.engSentence.replaceAll("[.,!?]", "").trim() : "";
            isCorrect = selectedAnswer.equalsIgnoreCase(rightAnswerStr);
        }

        isChecked = true;
        tvFeedback.setVisibility(View.VISIBLE);

        if (isCorrect) {
            correctAnswers++;
            progressBar.setProgress(correctAnswers);
            tvFeedback.setText("Tuyệt vời! Chính xác! 🎉");
            tvFeedback.setTextColor(Color.parseColor("#2E7D32"));
            btnCheck.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            btnCheck.setText("TIẾP TỤC");
            if (soundCorrect != null) { soundCorrect.seekTo(0); soundCorrect.start(); }
        } else {
            playList.add(currentQuestion);
            tvFeedback.setText("Sai rồi. Đáp án đúng:\n" + rightAnswerStr);
            tvFeedback.setTextColor(Color.parseColor("#D32F2F"));
            btnCheck.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            btnCheck.setText("TIẾP TỤC");

            hearts--;
            tvHearts.setText("❤️ " + hearts);
            if (soundWrong != null) { soundWrong.seekTo(0); soundWrong.start(); }

            if (hearts <= 0) showLoseDialog();
        }
    }

    private void resetButtonColors() {
        if (btnAns1 != null) btnAns1.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F7FA")));
        if (btnAns2 != null) btnAns2.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F7FA")));
        if (btnAns3 != null) btnAns3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F7FA")));
        if (btnAns4 != null) btnAns4.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F7FA")));
    }

    private void showWinDialog() {
        if (!isAdded() || getContext() == null) return;
        showConfettiAnimation();
        updateProgressAndStreak(xpReward);

        int currentUnlocked = getContext().getSharedPreferences("EnglishAI_Prefs", android.content.Context.MODE_PRIVATE)
                .getInt("UNLOCKED_STAGE", 1);

        int nextStageNumber = 1;
        if (difficultyLevel.equals("Easy")) nextStageNumber = 2;
        else if (difficultyLevel.equals("Medium")) nextStageNumber = 3;
        else if (difficultyLevel.equals("Hard")) nextStageNumber = 4;
        else if (difficultyLevel.equals("Boss")) nextStageNumber = 5;

        if (nextStageNumber > currentUnlocked) {
            getContext().getSharedPreferences("EnglishAI_Prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putInt("UNLOCKED_STAGE", nextStageNumber).apply();
        }

        String nextDifficulty = getNextDifficulty(difficultyLevel);

        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_result);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        TextView tvIcon = dialog.findViewById(R.id.tvDialogIcon);
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        Button btnPositive = dialog.findViewById(R.id.btnDialogPositive);
        Button btnNegative = dialog.findViewById(R.id.btnDialogNegative);

        tvIcon.setText("🏆");
        tvIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
        tvTitle.setText("Vượt Ải Thành Công!");
        tvTitle.setTextColor(Color.parseColor("#E65100"));

        if (nextDifficulty != null) {
            tvMessage.setText("Tuyệt vời! Nhận được " + xpReward + " XP.\nBạn có muốn khiêu chiến Ải tiếp theo luôn không? 🔥");
            btnPositive.setText("CHIẾN TIẾP ⚔️");
            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                if (isAdded()) {
                    StageGameplayFragment nextStage = new StageGameplayFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("DIFFICULTY_LEVEL", nextDifficulty);
                    nextStage.setArguments(bundle);
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, nextStage).commit();
                }
            });

            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setOnClickListener(v -> {
                dialog.dismiss();
                if (isAdded()) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        } else {
            tvMessage.setText("CHÚC MỪNG NHÀ VÔ ĐỊCH! 👑\n\nBạn đã phá đảo toàn bộ thử thách và nhận " + xpReward + " XP!");
            btnPositive.setText("VỀ BẢN ĐỒ");
            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                if (isAdded()) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
            btnNegative.setVisibility(View.GONE);
        }
        dialog.show();
    }

    private void showLoseDialog() {
        if (!isAdded() || getContext() == null) return;
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_result);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        TextView tvIcon = dialog.findViewById(R.id.tvDialogIcon);
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        Button btnPositive = dialog.findViewById(R.id.btnDialogPositive);
        Button btnNegative = dialog.findViewById(R.id.btnDialogNegative);

        tvIcon.setText("💔");
        tvIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
        tvTitle.setText("Game Over!");
        tvTitle.setTextColor(Color.parseColor("#D32F2F"));

        String msg = (difficultyLevel.equals("Boss")) ? "Trùm cuối quá mạnh! Sai 1 ly đi 1 dặm. Hãy thử lại!" : "Bạn đã hết sạch Trái Tim. Hãy nghỉ ngơi và thử lại sau nhé!";
        tvMessage.setText(msg);

        btnPositive.setText("VỀ BẢN ĐỒ");
        btnPositive.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            if (isAdded()) {
                requireActivity().getSupportFragmentManager().popBackStack();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
        btnNegative.setVisibility(View.GONE);
        dialog.show();
    }

    private void showConfettiAnimation() {
        if (konfettiView != null) {
            nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig =
                    new nl.dionsegijn.konfetti.core.emitter.Emitter(100L, java.util.concurrent.TimeUnit.MILLISECONDS).max(100);
            konfettiView.start(
                    new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig).spread(360)
                            .colors(java.util.Arrays.asList(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED))
                            .setSpeedBetween(0f, 30f).position(new nl.dionsegijn.konfetti.core.Position.Relative(0.5, 0.3)).build()
            );
        }
    }

    private void updateProgressAndStreak(int xpAmount) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        com.google.firebase.firestore.DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
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
                if (today.equals(lastActiveDate)) newStreak = currentStreak;
                else if (yesterday.equals(lastActiveDate)) newStreak = currentStreak + 1;
                else newStreak = 1;
                userRef.update("score", com.google.firebase.firestore.FieldValue.increment(xpAmount), "streak", newStreak, "lastActiveDate", today);
            }
        });
    }

    private String getNextDifficulty(String currentDifficulty) {
        switch (currentDifficulty) {
            case "Easy": return "Medium";
            case "Medium": return "Hard";
            case "Hard": return "Boss";
            default: return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (soundCorrect != null) { soundCorrect.release(); soundCorrect = null; }
        if (soundWrong != null) { soundWrong.release(); soundWrong = null; }
    }
}