package adu.nttu.englishai.fragments;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

public class StageGameplayFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView tvHearts, tvQuestionTitle, tvTargetWord, tvFeedback;
    private LinearLayout layoutMultipleChoice, layoutTyping;
    private Button btnAns1, btnAns2, btnAns3, btnAns4, btnCheck;
    private EditText edtTypingAnswer;
    private nl.dionsegijn.konfetti.xml.KonfettiView konfettiView;

    private List<Vocabulary> playList = new ArrayList<>();
    private int currentQuestionIndex = 0;

    // 👉 CÁC BIẾN LOGIC SẼ THAY ĐỔI THEO ẢI
    private int hearts = 3;
    private int targetScore = 0;
    private int correctAnswers = 0;
    private int xpReward = 50;
    private int numQuestions = 10;

    private int currentQuestionType = 0; // 0 = Trắc nghiệm, 1 = Gõ phím
    private Vocabulary currentWord;
    private String selectedAnswer = "";
    private boolean isChecked = false;

    private MediaPlayer soundCorrect, soundWrong;
    private String difficultyLevel = "Easy";

    public StageGameplayFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stage_gameplay, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnClose).setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        progressBar = view.findViewById(R.id.progressBar);
        tvHearts = view.findViewById(R.id.tvHearts);
        tvQuestionTitle = view.findViewById(R.id.tvQuestionTitle);
        tvTargetWord = view.findViewById(R.id.tvTargetWord);
        tvFeedback = view.findViewById(R.id.tvFeedback);
        layoutMultipleChoice = view.findViewById(R.id.layoutMultipleChoice);
        layoutTyping = view.findViewById(R.id.layoutTyping);
        edtTypingAnswer = view.findViewById(R.id.edtTypingAnswer);
        btnCheck = view.findViewById(R.id.btnCheck);
        konfettiView = view.findViewById(R.id.konfettiView);

        btnAns1 = view.findViewById(R.id.btnAns1);
        btnAns2 = view.findViewById(R.id.btnAns2);
        btnAns3 = view.findViewById(R.id.btnAns3);
        btnAns4 = view.findViewById(R.id.btnAns4);

        soundCorrect = MediaPlayer.create(getContext(), R.raw.correct);
        soundWrong = MediaPlayer.create(getContext(), R.raw.wrong);

        if (getArguments() != null) {
            difficultyLevel = getArguments().getString("DIFFICULTY_LEVEL", "Easy");
        }

        prepareData();
        setupClickListeners();
        loadQuestion();
    }

    private void prepareData() {
        // =========================================================
        // 👉 THIẾT LẬP LUẬT CHƠI DỰA TRÊN ĐỘ KHÓ
        // =========================================================
        switch (difficultyLevel) {
            case "Easy": // Ải 1
                hearts = 5;
                xpReward = 30;
                numQuestions = 10;
                break;
            case "Medium": // Ải 2
                hearts = 3;
                xpReward = 50;
                numQuestions = 10;
                break;
            case "Hard": // Ải 3
                hearts = 2;
                xpReward = 80;
                numQuestions = 12;
                break;
            case "Boss": // Ải 4 (Trùm Cuối)
                hearts = 1; // Sinh tử 1 hit
                xpReward = 200;
                numQuestions = 15;
                break;
            default:
                hearts = 3;
                xpReward = 50;
                numQuestions = 10;
                break;
        }

        tvHearts.setText("❤️ " + hearts); // Cập nhật sinh mệnh lên màn hình

        List<Vocabulary> allWords = DataRepository.getInstance().getVocabularyList();
        List<Vocabulary> filtered = new ArrayList<>();

        for (Vocabulary v : allWords) {
            if (v.getDifficulty() != null && v.getDifficulty().equalsIgnoreCase(difficultyLevel)) {
                filtered.add(v);
            }
        }

        // Nếu kho từ vựng ít quá không đủ chơi, tự động lấy thêm các từ khác đắp vào
        if (filtered.size() < numQuestions) {
            filtered.addAll(allWords);
        }

        Collections.shuffle(filtered);

        // Lấy đúng số lượng câu hỏi của Ải đó
        playList = new ArrayList<>(filtered.subList(0, Math.min(numQuestions, filtered.size())));

        targetScore = playList.size();
        progressBar.setMax(targetScore);
        progressBar.setProgress(0);
    }

    private void loadQuestion() {
        if (correctAnswers >= targetScore) {
            showWinDialog();
            return;
        }

        if (currentQuestionIndex >= playList.size()) {
            showWinDialog();
            return;
        }

        isChecked = false;
        btnCheck.setText("KIỂM TRA");
        btnCheck.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1976D2")));
        tvFeedback.setVisibility(View.GONE);
        selectedAnswer = "";
        edtTypingAnswer.setText("");
        resetButtonColors();

        currentWord = playList.get(currentQuestionIndex);

        // =========================================================
        // 👉 PHÂN HÓA DẠNG CÂU HỎI THEO ĐỘ KHÓ
        // =========================================================
        if (difficultyLevel.equals("Easy")) {
            currentQuestionType = 0; // 100% Trắc nghiệm (Dễ)
        } else if (difficultyLevel.equals("Medium")) {
            currentQuestionType = Math.random() > 0.5 ? 0 : 1; // 50% Trắc nghiệm, 50% Gõ (Vừa)
        } else if (difficultyLevel.equals("Hard")) {
            currentQuestionType = Math.random() > 0.2 ? 1 : 0; // 80% Bắt buộc Gõ phím (Khó)
        } else if (difficultyLevel.equals("Boss")) {
            currentQuestionType = 1; // 100% Bắt buộc Gõ phím (Cực Nhọc)
        }

        if (currentQuestionType == 0) {
            layoutMultipleChoice.setVisibility(View.VISIBLE);
            layoutTyping.setVisibility(View.GONE);
            tvQuestionTitle.setText("Chọn nghĩa đúng của từ này:");
            tvTargetWord.setText(currentWord.getEnglishWord());

            List<String> options = new ArrayList<>();
            options.add(currentWord.getVietnameseMeaning());
            List<Vocabulary> dummyList = new ArrayList<>(DataRepository.getInstance().getVocabularyList());
            Collections.shuffle(dummyList);
            for (Vocabulary v : dummyList) {
                if (options.size() >= 4) break;
                if (!options.contains(v.getVietnameseMeaning())) options.add(v.getVietnameseMeaning());
            }
            Collections.shuffle(options);
            btnAns1.setText(options.get(0));
            btnAns2.setText(options.get(1));
            btnAns3.setText(options.get(2));
            btnAns4.setText(options.get(3));

        } else {
            layoutMultipleChoice.setVisibility(View.GONE);
            layoutTyping.setVisibility(View.VISIBLE);
            tvQuestionTitle.setText("Viết từ tiếng Anh của từ sau:");
            tvTargetWord.setText(currentWord.getVietnameseMeaning());
        }
    }

    private void setupClickListeners() {
        View.OnClickListener ansClickListener = v -> {
            if (isChecked) return;
            resetButtonColors();
            v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BBDEFB")));
            selectedAnswer = ((Button) v).getText().toString();
        };

        btnAns1.setOnClickListener(ansClickListener);
        btnAns2.setOnClickListener(ansClickListener);
        btnAns3.setOnClickListener(ansClickListener);
        btnAns4.setOnClickListener(ansClickListener);

        btnCheck.setOnClickListener(v -> {
            if (!isChecked) {
                checkAnswer();
            } else {
                currentQuestionIndex++;
                loadQuestion();
            }
        });
    }

    private void checkAnswer() {
        if (currentQuestionType == 1) {
            selectedAnswer = edtTypingAnswer.getText().toString().trim();
        }

        if (selectedAnswer.isEmpty()) return;

        isChecked = true;
        tvFeedback.setVisibility(View.VISIBLE);

        boolean isCorrect = false;
        if (currentQuestionType == 0) {
            isCorrect = selectedAnswer.equals(currentWord.getVietnameseMeaning());
        } else {
            isCorrect = selectedAnswer.equalsIgnoreCase(currentWord.getEnglishWord());
        }

        if (isCorrect) {
            correctAnswers++;
            progressBar.setProgress(correctAnswers);

            tvFeedback.setText("Tuyệt vời! Chính xác! 🎉");
            tvFeedback.setTextColor(Color.parseColor("#2E7D32"));
            btnCheck.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            btnCheck.setText("TIẾP TỤC");
            if (soundCorrect != null) { soundCorrect.seekTo(0); soundCorrect.start(); }
        } else {
            playList.add(currentWord);

            tvFeedback.setText("Sai rồi. Đáp án đúng là: " + (currentQuestionType == 0 ? currentWord.getVietnameseMeaning() : currentWord.getEnglishWord()));
            tvFeedback.setTextColor(Color.parseColor("#D32F2F"));
            btnCheck.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            btnCheck.setText("TIẾP TỤC");

            hearts--;
            tvHearts.setText("❤️ " + hearts);
            if (soundWrong != null) { soundWrong.seekTo(0); soundWrong.start(); }

            if (hearts <= 0) {
                showLoseDialog();
            }
        }
    }

    private void resetButtonColors() {
        btnAns1.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F7FA")));
        btnAns2.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F7FA")));
        btnAns3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F7FA")));
        btnAns4.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F7FA")));
    }

    private void showWinDialog() {
        showConfettiAnimation();
        updateProgressAndStreak(xpReward);

        // =========================================================
        // 👉 LƯU TIẾN ĐỘ MỞ KHÓA ẢI VÀO BỘ NHỚ MÁY
        // =========================================================
        int currentUnlocked = requireActivity().getSharedPreferences("EnglishAI_Prefs", android.content.Context.MODE_PRIVATE)
                .getInt("UNLOCKED_STAGE", 1);

        int nextStageNumber = 1;
        if (difficultyLevel.equals("Easy")) nextStageNumber = 2; // Thắng Ải 1 -> Mở Ải 2
        else if (difficultyLevel.equals("Medium")) nextStageNumber = 3; // Thắng Ải 2 -> Mở Ải 3
        else if (difficultyLevel.equals("Hard")) nextStageNumber = 4; // Thắng Ải 3 -> Mở Ải 4
        else if (difficultyLevel.equals("Boss")) nextStageNumber = 5; // Thắng Ải 4 -> Mở Rương

        // Chỉ cập nhật nếu ải mới lớn hơn ải đã mở hiện tại
        if (nextStageNumber > currentUnlocked) {
            requireActivity().getSharedPreferences("EnglishAI_Prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putInt("UNLOCKED_STAGE", nextStageNumber).apply();
        }
        // =========================================================

        String nextDifficulty = getNextDifficulty(difficultyLevel);

        // Khởi tạo Custom Dialog
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_result);

        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        // Ánh xạ UI trong Dialog
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
            // Còn Ải tiếp theo -> Gạ chơi tiếp
            tvMessage.setText("Tuyệt vời! Nhận được " + xpReward + " XP.\nBạn có muốn thừa thắng xông lên, khiêu chiến Ải tiếp theo luôn không? 🔥");
            btnPositive.setText("CHIẾN TIẾP ⚔️");
            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                StageGameplayFragment nextStage = new StageGameplayFragment();
                Bundle bundle = new Bundle();
                bundle.putString("DIFFICULTY_LEVEL", nextDifficulty);
                nextStage.setArguments(bundle);

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, nextStage)
                        .commit();
            });

            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setOnClickListener(v -> {
                dialog.dismiss();
                // 👉 Lùi 2 bước để thoát hẳn ra Bản Đồ (nhảy cóc qua Sảnh chờ)
                requireActivity().getSupportFragmentManager().popBackStack();
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        } else {
            // Đã vượt Trùm Cuối
            tvMessage.setText("CHÚC MỪNG NHÀ VÔ ĐỊCH! 👑\n\nBạn đã đánh bại Trùm Cuối và nhận được " + xpReward + " XP! Phá đảo toàn bộ thử thách!");
            btnPositive.setText("VỀ BẢN ĐỒ");
            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                // 👉 Lùi 2 bước để thoát hẳn ra Bản Đồ
                requireActivity().getSupportFragmentManager().popBackStack();
                requireActivity().getSupportFragmentManager().popBackStack();
            });
            btnNegative.setVisibility(View.GONE); // Ẩn nút phụ đi cho cân đối
        }

        dialog.show();
    }

    private void showLoseDialog() {
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

        // Chuyển sang giao diện Thất Bại (Màu Đỏ)
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
            // 👉 FIX: Lùi 2 bước để thoát hẳn ra Bản Đồ
            requireActivity().getSupportFragmentManager().popBackStack();
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnNegative.setVisibility(View.GONE); // Ẩn nút phụ

        dialog.show();
    }

    // 👉 HÀM PHỤ TRỢ XÁC ĐỊNH ẢI TIẾP THEO
    private String getNextDifficulty(String currentDifficulty) {
        switch (currentDifficulty) {
            case "Easy": return "Medium"; // Từ Ải 1 -> Ải 2
            case "Medium": return "Hard"; // Từ Ải 2 -> Ải 3
            case "Hard": return "Boss";   // Từ Ải 3 -> Ải 4
            default: return null;         // Ải 4 là hết, trả về null
        }
    }

    private void showConfettiAnimation() {
        nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig =
                new nl.dionsegijn.konfetti.core.emitter.Emitter(100L, java.util.concurrent.TimeUnit.MILLISECONDS).max(100);
        konfettiView.start(
                new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig).spread(360)
                        .colors(java.util.Arrays.asList(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED))
                        .setSpeedBetween(0f, 30f).position(new nl.dionsegijn.konfetti.core.Position.Relative(0.5, 0.3)).build()
        );
    }

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