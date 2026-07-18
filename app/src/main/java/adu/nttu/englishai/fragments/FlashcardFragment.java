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
    private FrameLayout cardContainer; // Khung chứa toàn bộ thẻ (đóng vai trò là trục lật 3D)
    private TextView tvCardFront, tvCardBack; // Mặt trước (Tiếng Anh) và Mặt sau (Nghĩa Tiếng Việt)
    private Button btnNextCard;

    // Cờ đánh dấu trạng thái hiện tại: true là đang hiện mặt trước, false là đang hiện mặt sau
    private boolean isFront = true;

    // Danh sách từ vựng và chỉ số của từ đang hiển thị trên màn hình
    private List<Vocabulary> vocabularyList;
    private int currentIndex = 0;

    public FlashcardFragment() {}

    // =========================================================================
    // HÀM QUAN TRỌNG 1: TẠO GIAO DIỆN VÀ THIẾT LẬP CAMERA 3D (ON CREATE VIEW)
    // =========================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm (inflate) bản thiết kế XML fragment_flashcard thành đối tượng View thực tế
        View view = inflater.inflate(R.layout.fragment_flashcard, container, false);

        // Xử lý nút Back: Bấm vào là trượt mượt mà về lại trang Danh Sách Nhiệm Vụ Ải
        View btnBackToStage = view.findViewById(R.id.btnBackToStage);
        if (btnBackToStage != null) {
            btnBackToStage.setOnClickListener(v -> {
                // popBackStack(): Đóng Fragment hiện tại và trở về màn hình trước đó trong ngăn xếp
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Ánh xạ các thẻ UI
        cardContainer = view.findViewById(R.id.cardContainer);
        tvCardFront = view.findViewById(R.id.tvCardFront);
        tvCardBack = view.findViewById(R.id.tvCardBack);

        // KỸ THUẬT QUAN TRỌNG: Mặc định xoay mặt sau đi 180 độ (quay lưng lại)
        // Để khi thẻ lật vòng qua, chữ ở mặt sau sẽ quay đúng chiều nhìn, không bị lật ngược gương (mirrored)
        tvCardBack.setRotationY(180f);

        btnNextCard = view.findViewById(R.id.btnNextCard);

        // Lấy danh sách từ vựng từ bộ nhớ tập trung (Singleton Pattern)
        vocabularyList = DataRepository.getInstance().getVocabularyList();

        /*
         * KỸ THUẬT QUAN TRỌNG: THIẾT LẬP KHOẢNG CÁCH CAMERA 3D (CAMERA DISTANCE)
         * Trong Android, khi xoay một View theo trục Y (rotationY), nếu không chỉnh cameraDistance,
         * góc nhìn 3D mặc định nằm quá gần màn hình sẽ khiến thẻ bị méo mó, phóng to biến dạng cực kỳ xấu.
         * Công thức: distance * density giúp nhân khoảng cách lên 8000 pixel,
         * tạo ra chiều sâu không gian lý tưởng để thẻ lật mượt mà hệt như trong không gian thực.
         */
        float distance = 8000;
        float scale = getResources().getDisplayMetrics().density;
        cardContainer.setCameraDistance(distance * scale);

        // Hiển thị từ vựng đầu tiên lên thẻ
        showCurrentWord();

        // Bấm vào khung thẻ để kích hoạt hiệu ứng lật qua lật lại
        cardContainer.setOnClickListener(v -> flipCard());

        // Bấm nút "Từ tiếp theo" để đổi sang từ mới
        btnNextCard.setOnClickListener(v -> nextWord());

        return view;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 2: HIỂN THỊ TỪ VỰNG & RESET TRẠNG THÁI THẺ
    // =========================================================================
    private void showCurrentWord() {
        if (vocabularyList != null && !vocabularyList.isEmpty()) {
            Vocabulary currentWord = vocabularyList.get(currentIndex);

            // CƠ CHẾ RESET THẺ AN TOÀN: Đảm bảo luôn hiển thị mặt trước khi chuyển sang từ mới
            // Nếu người dùng đang xem dở mặt sau (isFront = false) mà bấm qua từ tiếp theo,
            // ta lập tức trả góc xoay về 0 độ và bật độ sáng (alpha) của mặt trước lên 100%
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
    // HÀM QUAN TRỌNG 3: HIỆU ỨNG LẬT THẺ 3D (3D FLIP ANIMATION)
    // =========================================================================
    private void flipCard() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        if (isFront) {
            // LẬT SANG MẶT SAU (Nghĩa tiếng Việt):
            // 1. Xoay toàn bộ khung thẻ đi 180 độ trong 400 mili-giây
            cardContainer.animate().rotationY(180f).setDuration(400).start();

            // 2. Làm mờ mặt trước (alpha về 0) trong 200 mili-giây đầu tiên
            tvCardFront.animate().alpha(0f).setDuration(200).start();

            // 3. KỸ THUẬT ĐỒNG BỘ: Dùng setStartDelay(200) để CHỜ đúng 200 mili-giây
            // (thời điểm thẻ quay vuông góc 90 độ, mép thẻ hướng vào mắt người nhìn)
            // rồi mới bắt đầu hiện mặt sau (alpha lên 1). Tránh lỗi 2 mặt thẻ bị lồng chữ vào nhau.
            tvCardBack.animate().alpha(1f).setDuration(200).setStartDelay(200).start();
        } else {
            // LẬT VỀ MẶT TRƯỚC (Tiếng Anh):
            // Làm ngược lại quá trình trên: Xoay khung về 0 độ, giấu mặt sau và hiện mặt trước
            cardContainer.animate().rotationY(0f).setDuration(400).start();
            tvCardBack.animate().alpha(0f).setDuration(200).start();
            tvCardFront.animate().alpha(1f).setDuration(200).setStartDelay(200).start();
        }

        // Đảo ngược cờ trạng thái
        isFront = !isFront;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 4: CHUYỂN TỪ & THUẬT TOÁN TUẦN HOÀN MẢNG
    // =========================================================================
    private void nextWord() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        /*
         * THUẬT TOÁN CHỈ SỐ TUẦN HOÀN (MODULO ARITHMETIC):
         * Thay vì viết lệnh if (currentIndex >= vocabularyList.size()) currentIndex = 0;
         * Việc dùng phép chia lấy dư (%) giúp tăng chỉ số lên 1, và ngay khi chạm tới giới hạn
         * của danh sách, chỉ số sẽ tự động lặp về 0. Giúp học viên lướt thẻ vô tận không bao giờ bị lỗi tràn mảng.
         */
        currentIndex = (currentIndex + 1) % vocabularyList.size();

        // HIỆU ỨNG THU NHỎ PHẢN HỒI (Tactile Feedback Animation):
        // Bước 1: Thu nhỏ thẻ xuống còn 95% (0.95f) trong 100ms tạo cảm giác thẻ đang được rút ra
        cardContainer.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
            // Bước 2: Ngay khi thu nhỏ xong -> Đổi nội dung từ mới
            showCurrentWord();
            // Bước 3: Phóng to thẻ về lại kích thước gốc 100% (1f) trong 100ms
            cardContainer.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
        }).start();
    }
}