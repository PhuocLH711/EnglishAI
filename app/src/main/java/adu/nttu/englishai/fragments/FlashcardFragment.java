package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

public class FlashcardFragment extends Fragment {

    private FrameLayout cardContainer;
    private TextView tvCardFront, tvCardBack;
    private boolean isFront = true;

    private List<Vocabulary> vocabularyList;
    private int currentIndex = 0;

    public FlashcardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcard, container, false);

        cardContainer = view.findViewById(R.id.cardContainer);
        tvCardFront = view.findViewById(R.id.tvCardFront);
        tvCardBack = view.findViewById(R.id.tvCardBack);

        // Kéo danh sách từ vựng từ Kho chung
        vocabularyList = DataRepository.getInstance().getVocabularyList();

        float distance = 8000;
        float scale = getResources().getDisplayMetrics().density;
        cardContainer.setCameraDistance(distance * scale);

        // Hiển thị từ đầu tiên
        showCurrentWord();

        cardContainer.setOnClickListener(v -> flipCard());

        return view;
    }

    private void showCurrentWord() {
        if (vocabularyList != null && !vocabularyList.isEmpty()) {
            Vocabulary currentWord = vocabularyList.get(currentIndex);
            tvCardFront.setText(currentWord.getEnglishWord());
            tvCardBack.setText(currentWord.getVietnameseMeaning() + "\n\n" + currentWord.getPronunciation());
        }
    }

    private void flipCard() {
        if (isFront) {
            cardContainer.animate().rotationY(180f).setDuration(500).start();
            tvCardFront.animate().alpha(0f).setDuration(250).start();
            tvCardBack.animate().alpha(1f).setDuration(250).setStartDelay(250).start();
        } else {
            cardContainer.animate().rotationY(0f).setDuration(500).start();
            tvCardBack.animate().alpha(0f).setDuration(250).start();
            tvCardFront.animate().alpha(1f).setDuration(250).setStartDelay(250).start();

            // Lật về mặt trước xong thì tự động chuyển sang từ tiếp theo!
            currentIndex = (currentIndex + 1) % vocabularyList.size();
            cardContainer.postDelayed(this::showCurrentWord, 300);
            Toast.makeText(getContext(), "Chuyển từ tiếp theo ➡️", Toast.LENGTH_SHORT).show();
        }
        isFront = !isFront;
    }
}