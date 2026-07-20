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

// =========================================================================
// FLASHCARD FRAGMENT: Màn hình học từ vựng bằng thẻ lật 3D tương tác
// =========================================================================
public class FlashcardFragment extends Fragment {

    // Các thành phần giao diện (UI Components)
    private FrameLayout cardContainer;
    private TextView tvCardFront, tvCardBack;
    private Button btnNextCard;

    private boolean isFront = true;

    private List<Vocabulary> vocabularyList;
    private int currentIndex = 0;

    public FlashcardFragment() {}

    // =========================================================================
    // HÀM 1: TẠO GIAO DIỆN VÀ THIẾT LẬP CAMERA 3D
    // =========================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm bản thiết kế XML fragment_flashcard thành đối tượng View thực tế
        View view = inflater.inflate(R.layout.fragment_flashcard, container, false);

        // Xử lý nút Back
        View btnBackToStage = view.findViewById(R.id.btnBackToStage);
        if (btnBackToStage != null) {
            btnBackToStage.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Ánh xạ các thẻ UI
        cardContainer = view.findViewById(R.id.cardContainer);
        tvCardFront = view.findViewById(R.id.tvCardFront);
        tvCardBack = view.findViewById(R.id.tvCardBack);

        // Mặc định xoay mặt sau đi 180 độ
        // Để khi thẻ lật vòng qua, chữ ở mặt sau sẽ quay đúng chiều nhìn, không bị lật ngược gương
        tvCardBack.setRotationY(180f);

        btnNextCard = view.findViewById(R.id.btnNextCard);

        vocabularyList = DataRepository.getInstance().getVocabularyList();

        /*
         * THIẾT LẬP KHOẢNG CÁCH CAMERA 3D
         * Công thức: distance * density giúp nhân khoảng cách lên 8000 pixel
         */
        float distance = 8000;
        float scale = getResources().getDisplayMetrics().density;
        cardContainer.setCameraDistance(distance * scale);

        showCurrentWord();

        // Bấm vào khung thẻ để kích hoạt hiệu ứng lật qua lật lại
        cardContainer.setOnClickListener(v -> flipCard());

        // Bấm nút "Từ tiếp theo" để đổi sang từ mới
        btnNextCard.setOnClickListener(v -> nextWord());

        return view;
    }

    // =========================================================================
    // HÀM 2: HIỂN THỊ TỪ VỰNG & RESET TRẠNG THÁI THẺ
    // =========================================================================
    private void showCurrentWord() {
        if (vocabularyList != null && !vocabularyList.isEmpty()) {
            Vocabulary currentWord = vocabularyList.get(currentIndex);

            // CƠ CHẾ RESET THẺ AN TOÀN: Đảm bảo luôn hiển thị mặt trước khi chuyển sang từ mới
            if (!isFront) {
                cardContainer.setRotationY(0f);
                tvCardFront.setAlpha(1f);
                tvCardBack.setAlpha(0f);
                isFront = true;
            }

            // Đổ dữ liệu từ vựng lên 2 mặt của thẻ
            tvCardFront.setText(currentWord.getEnglishWord());
            tvCardBack.setText(currentWord.getVietnameseMeaning() + "\n\n" + currentWord.getPronunciation());
        }
    }

    // =========================================================================
    // HÀM 3: HIỆU ỨNG LẬT THẺ 3D
    // =========================================================================
    private void flipCard() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        if (isFront) {
            // 1. Xoay toàn bộ khung thẻ đi 180 độ trong 400 mili-giây
            cardContainer.animate().rotationY(180f).setDuration(400).start();

            // 2. Làm mờ mặt trước (alpha về 0) trong 200 mili-giây đầu tiên
            tvCardFront.animate().alpha(0f).setDuration(200).start();

            // 3. Dùng setStartDelay(200) để CHỜ đúng 200 mili-giây
            // rồi mới bắt đầu hiện mặt sau. Tránh lỗi 2 mặt thẻ bị lồng chữ vào nhau.
            tvCardBack.animate().alpha(1f).setDuration(200).setStartDelay(200).start();
        } else {
            // LẬT VỀ MẶT TRƯỚC (Tiếng Anh):
            cardContainer.animate().rotationY(0f).setDuration(400).start();
            tvCardBack.animate().alpha(0f).setDuration(200).start();
            tvCardFront.animate().alpha(1f).setDuration(200).setStartDelay(200).start();
        }

        isFront = !isFront;
    }

    // =========================================================================
    // HÀM 4: CHUYỂN TỪ & THUẬT TOÁN TUẦN HOÀN MẢNG
    // =========================================================================
    private void nextWord() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        /*
         * THUẬT TOÁN CHỈ SỐ TUẦN HOÀN:
         */
        currentIndex = (currentIndex + 1) % vocabularyList.size();

        // HIỆU ỨNG THU NHỎ PHẢN HỒI:
        cardContainer.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
            showCurrentWord();
            cardContainer.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
        }).start();
    }
}