package adu.nttu.englishai.fragments;

import android.graphics.Color;
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

public class MemoryMatchFragment extends Fragment {

    private GridLayout gridCards;
    private TextView tvPairsFound, tvScore;
    private Button btnPlayAgain;

    private List<CardItem> cardList = new ArrayList<>();
    private CardItem firstSelectedCard = null;
    private CardItem secondSelectedCard = null;

    private boolean isProcessing = false;
    private int pairsFound = 0;
    private int totalScore = 0;
    private final int TOTAL_PAIRS = 6; // 6 cặp = 12 thẻ

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

        gridCards = view.findViewById(R.id.gridCards);
        tvPairsFound = view.findViewById(R.id.tvPairsFound);
        tvScore = view.findViewById(R.id.tvScore);
        btnPlayAgain = view.findViewById(R.id.btnPlayAgain);

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

        // 1. Lấy từ vựng từ hệ thống
        List<Vocabulary> allWords = new ArrayList<>();
        if (DataRepository.getInstance().getVocabularyList() != null) {
            allWords.addAll(DataRepository.getInstance().getVocabularyList());
        }

        // 2. KHẮC PHỤC LỖI TRỐNG THẺ: Nếu không đủ 6 từ, tự động tạo từ vựng mẫu!
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

        // 3. Tạo 12 thẻ (6 Anh + 6 Việt)
        for (Vocabulary vocab : selectedWords) {
            cardList.add(new CardItem(vocab.getId(), vocab.getEnglishWord()));
            cardList.add(new CardItem(vocab.getId(), vocab.getVietnameseMeaning()));
        }

        // 4. Xáo trộn vị trí thẻ
        Collections.shuffle(cardList);

        // 5. Gắn vào lưới 3x4 (Đã sửa lại chiều cao để thẻ hiển thị rõ nét)
        for (int i = 0; i < cardList.size(); i++) {
            CardItem card = cardList.get(i);
            Button btn = createCardButton(card);
            card.button = btn;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // Để chia đều cho 3 cột
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            params.setMargins(12, 12, 12, 12);
            btn.setLayoutParams(params);

            // Ép chiều cao tối thiểu cho thẻ nổi bật, không bao giờ bị tàng hình
            btn.setMinHeight(220);

            gridCards.addView(btn);
        }
    }

    private Button createCardButton(CardItem card) {
        Button btn = new Button(getContext());
        btn.setText("❓"); // Mặc định úp thẻ
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

            resetSelection();

            if (pairsFound == TOTAL_PAIRS) {
                Toast.makeText(getContext(), "🎉 Xuất sắc! Bạn đã tìm được toàn bộ từ vựng!", Toast.LENGTH_LONG).show();
                btnPlayAgain.setVisibility(View.VISIBLE);
            }
        } else {
            firstSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            secondSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            firstSelectedCard.button.setTextColor(Color.parseColor("#B71C1C"));
            secondSelectedCard.button.setTextColor(Color.parseColor("#B71C1C"));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (firstSelectedCard != null) flipCardBack(firstSelectedCard);
                if (secondSelectedCard != null) flipCardBack(secondSelectedCard);
                resetSelection();
            }, 1000);
        }
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
}