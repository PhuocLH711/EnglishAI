package adu.nttu.englishai.fragments;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

// =========================================================================
// MEMORY MATCH FRAGMENT: Màn hình trò chơi Lật Thẻ Tìm Cặp Từ
// =========================================================================
public class MemoryMatchFragment extends Fragment {

    private GridLayout gridCards;
    private TextView tvPairsFound, tvScore;
    private Button btnPlayAgain;

    // View pháo hoa từ thư viện
    private nl.dionsegijn.konfetti.xml.KonfettiView konfettiView;

    // Biến lưu trữ âm thanh
    private MediaPlayer soundCorrect;
    private MediaPlayer soundWrong;

    private List<CardItem> cardList = new ArrayList<>();
    private CardItem firstSelectedCard = null;
    private CardItem secondSelectedCard = null;

    private boolean isProcessing = false;
    private int pairsFound = 0;
    private int totalScore = 0;
    private final int TOTAL_PAIRS = 6;

    private static class CardItem {
        String wordId;
        String displayText;
        Button button;
        boolean isFlipped = false;
        boolean isMatched = false;

        CardItem(String wordId, String displayText) {
            this.wordId = wordId;
            this.displayText = displayText;
        }
    }

    public MemoryMatchFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memory_match, container, false);

        View btnBackToStage = view.findViewById(R.id.btnBackToStage);
        if (btnBackToStage != null) {
            btnBackToStage.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        gridCards = view.findViewById(R.id.gridCards);
        tvPairsFound = view.findViewById(R.id.tvPairsFound);
        tvScore = view.findViewById(R.id.tvScore);
        btnPlayAgain = view.findViewById(R.id.btnPlayAgain);
        konfettiView = view.findViewById(R.id.konfettiView);

        // KHỞI TẠO ÂM THANH
        soundCorrect = MediaPlayer.create(requireContext(), R.raw.correct);
        soundWrong = MediaPlayer.create(requireContext(), R.raw.wrong);

        startNewGame();

        btnPlayAgain.setOnClickListener(v -> startNewGame());

        return view;
    }

    private void startNewGame() {
        pairsFound = 0;
        totalScore = 0;
        isProcessing = false;
        firstSelectedCard = null;
        secondSelectedCard = null;

        tvPairsFound.setText("Đã tìm thấy: 0/" + TOTAL_PAIRS + " cặp");
        tvScore.setText("⚡ 0 XP");
        btnPlayAgain.setVisibility(View.GONE);
        gridCards.removeAllViews();
        cardList.clear();

        List<Vocabulary> allWords = new ArrayList<>();
        if (DataRepository.getInstance().getVocabularyList() != null) {
            allWords.addAll(DataRepository.getInstance().getVocabularyList());
        }

        if (allWords.size() < TOTAL_PAIRS) {
            allWords.clear();
            allWords.add(new Vocabulary("1", "Apple", "Quả táo", "/ˈæp.əl/", "I eat an apple.", "Food", "Easy"));
            allWords.add(new Vocabulary("2", "Banana", "Quả chuối", "/bəˈnɑː.nə/", "Yellow banana.", "Food", "Easy"));
            allWords.add(new Vocabulary("3", "Cat", "Con mèo", "/kæt/", "Sleeping cat.", "Animals", "Easy"));
            allWords.add(new Vocabulary("4", "Dog", "Con chó", "/dɒɡ/", "Friendly dog.", "Animals", "Easy"));
            allWords.add(new Vocabulary("5", "Teacher", "Giáo viên", "/ˈtiː.tʃər/", "Kind teacher.", "School", "Easy"));
            allWords.add(new Vocabulary("6", "Student", "Học sinh", "/ˈstjuː.dənt/", "Good student.", "School", "Easy"));
            allWords.add(new Vocabulary("7", "Computer", "Máy tính", "/kəmˈpjuː.tər/", "My computer.", "Tech", "Easy"));
            allWords.add(new Vocabulary("8", "Hospital", "Bệnh viện", "/ˈhɒs.pɪ.təl/", "Big hospital.", "Places", "Medium"));
        }

        Collections.shuffle(allWords);
        List<Vocabulary> selectedWords = allWords.subList(0, TOTAL_PAIRS);

        for (Vocabulary vocab : selectedWords) {
            cardList.add(new CardItem(vocab.getId(), vocab.getEnglishWord()));
            cardList.add(new CardItem(vocab.getId(), vocab.getVietnameseMeaning()));
        }

        Collections.shuffle(cardList);

        for (int i = 0; i < cardList.size(); i++) {
            CardItem card = cardList.get(i);
            Button btn = createCardButton(card);
            card.button = btn;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            btn.setLayoutParams(params);
            btn.setMinHeight(220);

            gridCards.addView(btn);
        }
    }

    private Button createCardButton(CardItem card) {
        Button btn = new Button(getContext());
        btn.setText("❓");
        btn.setTextSize(24f);
        btn.setGravity(Gravity.CENTER);
        btn.setAllCaps(false);
        btn.setPadding(8, 8, 8, 8);

        btn.setBackgroundResource(R.drawable.bg_button_quiz);
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E1BEE7")));
        btn.setTextColor(Color.parseColor("#4A148C"));
        btn.setElevation(6f);

        btn.setOnClickListener(v -> onCardClicked(card));
        return btn;
    }

    private void onCardClicked(CardItem card) {
        if (card.isFlipped || card.isMatched || isProcessing) return;

        card.isFlipped = true;
        card.button.setText(card.displayText);
        card.button.setTextSize(14f);
        card.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        card.button.setTextColor(Color.parseColor("#1A1A1A"));

        if (firstSelectedCard == null) {
            firstSelectedCard = card;
        } else {
            secondSelectedCard = card;
            isProcessing = true;
            checkMatch();
        }
    }

    private void checkMatch() {
        if (firstSelectedCard.wordId.equals(secondSelectedCard.wordId)) {
            // 👉 LẬT ĐÚNG
            firstSelectedCard.isMatched = true;
            secondSelectedCard.isMatched = true;

            firstSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            secondSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            firstSelectedCard.button.setTextColor(Color.parseColor("#1B5E20"));
            secondSelectedCard.button.setTextColor(Color.parseColor("#1B5E20"));

            pairsFound++;
            totalScore += 10;
            tvPairsFound.setText("Đã tìm thấy: " + pairsFound + "/" + TOTAL_PAIRS + " cặp");
            tvScore.setText("⚡ " + totalScore + " XP");
            // Đẩy 10 XP lên Server mỗi khi lật đúng 1 cặp
            updateProgressAndStreak(10);

            // Phát âm thanh đúng
            if (soundCorrect != null) {
                soundCorrect.seekTo(0);
                soundCorrect.start();
            }

            resetSelection();

            if (pairsFound == TOTAL_PAIRS) {
                // Bắn pháo hoa khi lật đủ 6 cặp
                showConfettiAnimation();
                Toast.makeText(getContext(), "🎉 Xuất sắc! Bạn đã tìm được toàn bộ từ vựng!", Toast.LENGTH_LONG).show();
                btnPlayAgain.setVisibility(View.VISIBLE);
            }
        } else {
            // 👉 LẬT SAI
            firstSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            secondSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            firstSelectedCard.button.setTextColor(Color.parseColor("#B71C1C"));
            secondSelectedCard.button.setTextColor(Color.parseColor("#B71C1C"));

            // Phát âm thanh sai
            if (soundWrong != null) {
                soundWrong.seekTo(0);
                soundWrong.start();
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (firstSelectedCard != null) flipCardBack(firstSelectedCard);
                if (secondSelectedCard != null) flipCardBack(secondSelectedCard);
                resetSelection();
            }, 1000);
        }
    }

    // BẮN PHÁO HOA KONFETTI
    private void showConfettiAnimation() {
        nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig =
                new nl.dionsegijn.konfetti.core.emitter.Emitter(100L, java.util.concurrent.TimeUnit.MILLISECONDS).max(100);

        konfettiView.start(
                new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig)
                        .spread(360)
                        .colors(java.util.Arrays.asList(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED, Color.CYAN))
                        .setSpeedBetween(0f, 30f)
                        .position(new nl.dionsegijn.konfetti.core.Position.Relative(0.5, 0.3))
                        .build()
        );
    }

    private void flipCardBack(CardItem card) {
        card.isFlipped = false;
        card.button.setText("❓");
        card.button.setTextSize(24f);
        card.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E1BEE7")));
        card.button.setTextColor(Color.parseColor("#4A148C"));
    }

    private void resetSelection() {
        firstSelectedCard = null;
        secondSelectedCard = null;
        isProcessing = false;
    }

    // Dọn dẹp RAM khi thoát
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