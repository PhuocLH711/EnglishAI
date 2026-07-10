package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import adu.nttu.englishai.R;

public class FlashcardFragment extends Fragment {

    private FrameLayout cardContainer;
    private TextView tvCardFront, tvCardBack;
    private boolean isFront = true; // Biến kiểm tra xem thẻ đang ở mặt trước hay sau

    public FlashcardFragment() {
        // Constructor rỗng bắt buộc
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcard, container, false);

        cardContainer = view.findViewById(R.id.cardContainer);
        tvCardFront = view.findViewById(R.id.tvCardFront);
        tvCardBack = view.findViewById(R.id.tvCardBack);

        // Chỉnh lại tiêu cự camera ảo để lúc lật thẻ 3D không bị tràn viền màn hình
        float distance = 8000;
        float scale = getResources().getDisplayMetrics().density;
        cardContainer.setCameraDistance(distance * scale);

        // Lắng nghe thao tác chạm vào thẻ
        cardContainer.setOnClickListener(v -> flipCard());

        return view;
    }

    private void flipCard() {
        if (isFront) {
            // Hiệu ứng lật ra mặt sau
            cardContainer.animate().rotationY(180f).setDuration(500).start(); // Xoay khung thẻ 180 độ
            tvCardFront.animate().alpha(0f).setDuration(250).start(); // Làm mờ chữ tiếng Anh
            tvCardBack.animate().alpha(1f).setDuration(250).setStartDelay(250).start(); // Hiện chữ tiếng Việt
        } else {
            // Hiệu ứng lật về mặt trước
            cardContainer.animate().rotationY(0f).setDuration(500).start(); // Xoay khung thẻ về 0 độ
            tvCardBack.animate().alpha(0f).setDuration(250).start();
            tvCardFront.animate().alpha(1f).setDuration(250).setStartDelay(250).start();
        }
        isFront = !isFront; // Đảo ngược trạng thái thẻ
    }
}