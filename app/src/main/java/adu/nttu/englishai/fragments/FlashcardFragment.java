package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private Button btnNextCard;
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
        tvCardBack.setRotationY(180f);
        btnNextCard = view.findViewById(R.id.btnNextCard);

        vocabularyList = DataRepository.getInstance().getVocabularyList();

        // Thiết lập khoảng cách camera để hiệu ứng lật 3D không bị méo hình
        float distance = 8000;
        float scale = getResources().getDisplayMetrics().density;
        cardContainer.setCameraDistance(distance * scale);

        showCurrentWord();

        // Bấm vào thẻ để lật qua lật lại
        cardContainer.setOnClickListener(v -> flipCard());

        // Bấm nút "Từ tiếp theo" để đổi từ mới
        btnNextCard.setOnClickListener(v -> nextWord());

        return view;
    }

    private void showCurrentWord() {
        if (vocabularyList != null && !vocabularyList.isEmpty()) {
            Vocabulary currentWord = vocabularyList.get(currentIndex);

            // Đảm bảo luôn hiển thị mặt trước khi chuyển từ mới
            if (!isFront) {
                cardContainer.setRotationY(0f);
                tvCardFront.setAlpha(1f);
                tvCardBack.setAlpha(0f);
                isFront = true;
            }

            tvCardFront.setText(currentWord.getEnglishWord());
            tvCardBack.setText(currentWord.getVietnameseMeaning() + "\n\n" + currentWord.getPronunciation());
        }
    }

    private void flipCard() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        if (isFront) {
            // Lật sang mặt sau (Nghĩa tiếng Việt)
            cardContainer.animate().rotationY(180f).setDuration(400).start();
            tvCardFront.animate().alpha(0f).setDuration(200).start();
            tvCardBack.animate().alpha(1f).setDuration(200).setStartDelay(200).start();
        } else {
            // Lật về mặt trước (Tiếng Anh)
            cardContainer.animate().rotationY(0f).setDuration(400).start();
            tvCardBack.animate().alpha(0f).setDuration(200).start();
            tvCardFront.animate().alpha(1f).setDuration(200).setStartDelay(200).start();
        }
        isFront = !isFront;
    }

    private void nextWord() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        // Tăng chỉ số từ lên 1, nếu hết danh sách thì quay lại từ đầu (0)
        currentIndex = (currentIndex + 1) % vocabularyList.size();

        // Hiệu ứng thu nhỏ nhẹ rồi phóng to khi đổi từ
        cardContainer.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
            showCurrentWord();
            cardContainer.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
        }).start();
    }
}