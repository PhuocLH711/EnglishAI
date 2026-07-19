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

// =========================================================================
// MEMORY MATCH FRAGMENT: Màn hình trò chơi Lật Thẻ Tìm Cặp Từ (English <-> Việt)
// =========================================================================
public class MemoryMatchFragment extends Fragment {

    // Các thành phần giao diện (UI Components)
    private GridLayout gridCards; // Khung lưới chứa 12 thẻ (3 cột x 4 hàng)
    private TextView tvPairsFound, tvScore;
    private Button btnPlayAgain;

    // Danh sách lưu trữ 12 thẻ trên màn hình và biến theo dõi 2 thẻ đang được chọn
    private List<CardItem> cardList = new ArrayList<>();
    private CardItem firstSelectedCard = null;
    private CardItem secondSelectedCard = null;

    /*
     * CỜ BẢO VỆ LUỒNG (STATE LOCK FLAG - CRITICAL):
     * Khi người dùng lật sai 2 thẻ, hệ thống cần 1 giây (1000ms) để úp thẻ lại.
     * Biến isProcessing = true giúp khóa giao diện, KHÔNG cho người dùng bấm tiếp thẻ thứ 3, thứ 4
     * trong lúc chờ 1 giây đó. Tránh tuyệt đối lỗi xung đột logic (Race Condition / Click Spamming).
     */
    private boolean isProcessing = false;
    private int pairsFound = 0;
    private int totalScore = 0;
    private final int TOTAL_PAIRS = 6; // 6 cặp từ = 12 thẻ trên lưới

    // =========================================================================
    // LỚP MÔ HÌNH NỘI BỘ: THẺ TỪ VỰNG (INNER STATIC CLASS)
    // =========================================================================
    // Dùng static để tối ưu bộ nhớ. Mỗi CardItem đại diện cho 1 nút bấm trên màn hình.
    private static class CardItem {
        String wordId;      // Mã ID của từ vựng (Dùng để đối chiếu thẻ Tiếng Anh và thẻ Tiếng Việt có cùng ID không)
        String displayText; // Chữ hiển thị trên thẻ (Nghĩa Anh hoặc nghĩa Việt)
        Button button;      // Tham chiếu trực tiếp đến nút bấm UI
        boolean isFlipped = false; // Thẻ đang ngửa (true) hay úp (false)
        boolean isMatched = false; // Thẻ đã tìm đúng cặp và bị khóa (true) hay chưa

        CardItem(String wordId, String displayText) {
            this.wordId = wordId;
            this.displayText = displayText;
        }
    }

    public MemoryMatchFragment() {}

    // =========================================================================
    // HÀM TẠO GIAO DIỆN & GÁN SỰ KIỆN (ON CREATE VIEW)
    // =========================================================================
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm (inflate) bản thiết kế XML fragment_memory_match thành đối tượng View
        View view = inflater.inflate(R.layout.fragment_memory_match, container, false);

        // Xử lý nút Back: Bấm vào là trượt mượt mà về lại trang Danh Sách Nhiệm Vụ Ải
        View btnBackToStage = view.findViewById(R.id.btnBackToStage);
        if (btnBackToStage != null) {
            btnBackToStage.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Ánh xạ các thành phần UI
        gridCards = view.findViewById(R.id.gridCards);
        tvPairsFound = view.findViewById(R.id.tvPairsFound);
        tvScore = view.findViewById(R.id.tvScore);
        btnPlayAgain = view.findViewById(R.id.btnPlayAgain);

        // Khởi tạo ván chơi mới ngay khi mở màn hình
        startNewGame();

        // Bấm nút Chơi lại -> Gọi startNewGame() để reset bàn chơi
        btnPlayAgain.setOnClickListener(v -> startNewGame());

        return view;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 1: KHỞI TẠO BÀN CHƠI & XÁO TRỘN THẺ (GAME INITIALIZATION)
    // =========================================================================
    private void startNewGame() {
        // 1. Reset toàn bộ điểm số, trạng thái và xóa sạch thẻ cũ trên màn hình
        pairsFound = 0;
        totalScore = 0;
        isProcessing = false;
        firstSelectedCard = null;
        secondSelectedCard = null;

        tvPairsFound.setText("Đã tìm thấy: 0/" + TOTAL_PAIRS + " cặp");
        tvScore.setText("⚡ 0 XP");
        btnPlayAgain.setVisibility(View.GONE);
        gridCards.removeAllViews(); // Xóa sạch 12 nút bấm cũ của ván trước
        cardList.clear();

        // 2. Lấy danh sách từ vựng từ bộ nhớ trung tâm
        List<Vocabulary> allWords = new ArrayList<>();
        if (DataRepository.getInstance().getVocabularyList() != null) {
            allWords.addAll(DataRepository.getInstance().getVocabularyList());
        }

        /*
         * 3. KỸ THUẬT PHÒNG VỆ DỮ LIỆU (DEFENSIVE FALLBACK DATA):
         * Trò chơi cần đúng 6 cặp từ (TOTAL_PAIRS = 6). Nếu người dùng mới tải app,
         * chưa học hoặc chưa có đủ 6 từ trong hệ thống, ván chơi sẽ bị lỗi hoặc trống trơn.
         * Code tự động phát hiện và chèn ngay 8 từ vựng cơ bản mẫu vào để đảm bảo game luôn chơi được 100%!
         */
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

        // Xáo trộn ngẫu nhiên kho từ vựng và cắt lấy đúng 6 từ đầu tiên để chơi
        Collections.shuffle(allWords);
        List<Vocabulary> selectedWords = allWords.subList(0, TOTAL_PAIRS);

        // 4. Tạo ra 12 thẻ từ 6 từ vựng được chọn (Mỗi từ tạo 1 thẻ Tiếng Anh + 1 thẻ Tiếng Việt, chung wordId)
        for (Vocabulary vocab : selectedWords) {
            cardList.add(new CardItem(vocab.getId(), vocab.getEnglishWord()));
            cardList.add(new CardItem(vocab.getId(), vocab.getVietnameseMeaning()));
        }

        // 5. Xáo trộn ngẫu nhiên vị trí của 12 thẻ này trên bàn chơi
        Collections.shuffle(cardList);

        /*
         * 6. KỸ THUẬT TẠO GIAO DIỆN ĐỘNG BẰNG JAVA CODE (PROGRAMMATIC UI GENERATION):
         * Thay vì viết 12 nút trong XML, ta chạy vòng lặp tạo 12 Button và đưa vào GridLayout.
         * - params.width = 0 & columnSpec = spec(i % 3, 1f): BÍ KÍP CHIA ĐỀU 3 CỘT!
         *   Thiết lập chiều rộng 0 kèm trọng số weight = 1f ép Android phải tự động tính toán
         *   và chia đều chiều ngang màn hình cho đúng 3 cột bằng nhau, không bao giờ bị lệch.
         */
        for (int i = 0; i < cardList.size(); i++) {
            CardItem card = cardList.get(i);
            Button btn = createCardButton(card);
            card.button = btn;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // Để chia đều cho 3 cột theo trọng số 1f bên dưới
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 3, 1f); // i % 3 giúp tuần hoàn chỉ số cột: 0, 1, 2, 0, 1, 2...
            params.setMargins(12, 12, 12, 12);
            btn.setLayoutParams(params);

            // Ép chiều cao tối thiểu cho thẻ nổi bật, không bao giờ bị tàng hình hoặc quá bẹp
            btn.setMinHeight(220);

            gridCards.addView(btn);
        }
    }

    // =========================================================================
    // HÀM TẠO NÚT BẤM VÀ TRANG ĐIỂM THẺ (UI STYLING)
    // =========================================================================
    private Button createCardButton(CardItem card) {
        Button btn = new Button(getContext());
        btn.setText("❓"); // Mặc định khi mới tạo là úp thẻ (hiện dấu hỏi chấm)
        btn.setTextSize(24f);
        btn.setGravity(Gravity.CENTER);
        btn.setAllCaps(false); // Không ép chữ viết hoa để giữ nguyên định dạng từ vựng
        btn.setPadding(8, 8, 8, 8);

        // Thiết lập giao diện thẻ úp: Nền tím nhạt, chữ tím đậm, có bóng đổ 3D (elevation 6f)
        btn.setBackgroundResource(R.drawable.bg_button_quiz);
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E1BEE7")));
        btn.setTextColor(Color.parseColor("#4A148C"));
        btn.setElevation(6f);

        // Gán sự kiện khi bấm vào thẻ -> Gọi hàm xử lý lật thẻ
        btn.setOnClickListener(v -> onCardClicked(card));
        return btn;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 2: XỬ LÝ LẬT THẺ & KIỂM TRA ĐIỀU KIỆN (CARD CLICK LOGIC)
    // =========================================================================
    private void onCardClicked(CardItem card) {
        // LẬP TRÌNH PHÒNG VỆ: Nếu thẻ đã ngửa, đã tìm đúng cặp, hoặc hệ thống đang chờ úp thẻ sai -> Bỏ qua lệnh click
        if (card.isFlipped || card.isMatched || isProcessing) return;

        // 1. Lật ngửa thẻ: Đổi trạng thái, hiện chữ từ vựng, đổi sang nền trắng cho dễ đọc
        card.isFlipped = true;
        card.button.setText(card.displayText);
        card.button.setTextSize(14f);
        card.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        card.button.setTextColor(Color.parseColor("#1A1A1A"));

        // 2. Phân luồng thẻ thứ 1 và thẻ thứ 2
        if (firstSelectedCard == null) {
            // Đây là thẻ đầu tiên người dùng lật -> Lưu lại vào biến firstSelectedCard và chờ
            firstSelectedCard = card;
        } else {
            // Đây là thẻ thứ 2 người dùng lật -> Khóa giao diện (isProcessing = true) và kiểm tra đúng/sai
            secondSelectedCard = card;
            isProcessing = true;
            checkMatch();
        }
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 3: KIỂM TRA ĐÚNG/SAI & ĐỒNG BỘ BẤT ĐỒNG BỘ (MATCH CHECKING)
    // =========================================================================
    private void checkMatch() {
        /*
         * SO SÁNH ID TỪ VỰNG:
         * Không so sánh chữ (displayText) vì 1 bên là Tiếng Anh ("Apple"), 1 bên là Tiếng Việt ("Quả táo").
         * So sánh wordId bảo đảm chỉ cần 2 thẻ thuộc cùng 1 từ vựng là nhận diện trúng khớp 100%!
         */
        if (firstSelectedCard.wordId.equals(secondSelectedCard.wordId)) {
            // TRƯỜNG HỢP 1: LẬT ĐÚNG CẶP TỪ
            firstSelectedCard.isMatched = true;
            secondSelectedCard.isMatched = true;

            // Đổi giao diện 2 thẻ sang màu xanh lá chúc mừng
            firstSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            secondSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            firstSelectedCard.button.setTextColor(Color.parseColor("#1B5E20"));
            secondSelectedCard.button.setTextColor(Color.parseColor("#1B5E20"));

            // Cộng điểm và số cặp tìm được
            pairsFound++;
            totalScore += 10;
            tvPairsFound.setText("Đã tìm thấy: " + pairsFound + "/" + TOTAL_PAIRS + " cặp");
            tvScore.setText("⚡ " + totalScore + " XP");

            resetSelection(); // Mở khóa cho người dùng lật cặp tiếp theo

            // Kiểm tra điều kiện THẮNG GAME: Đã tìm đủ 6 cặp
            if (pairsFound == TOTAL_PAIRS) {
                Toast.makeText(getContext(), "🎉 Xuất sắc! Bạn đã tìm được toàn bộ từ vựng!", Toast.LENGTH_LONG).show();
                btnPlayAgain.setVisibility(View.VISIBLE); // Hiện nút Chơi lại
            }
        } else {
            // TRƯỜNG HỢP 2: LẬT SAI CẶP TỪ
            // Đổi giao diện 2 thẻ sang màu đỏ cảnh báo sai
            firstSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            secondSelectedCard.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            firstSelectedCard.button.setTextColor(Color.parseColor("#B71C1C"));
            secondSelectedCard.button.setTextColor(Color.parseColor("#B71C1C"));

            /*
             * KỸ THUẬT HẸN GIỜ BẤT ĐỒNG BỘ AN TOÀN (ASYNCHRONOUS DELAY HANDLING):
             * Không được dùng Thread.sleep(1000) vì sẽ làm đơ cứng toàn bộ điện thoại!
             * Dùng Handler(Looper.getMainLooper()).postDelayed() giúp giao diện vẫn mượt mà,
             * cho phép người dùng nhìn thấy 2 thẻ đỏ trong đúng 1 giây (1000ms) để nhớ vị trí từ vựng,
             * sau 1 giây hệ thống mới tự động úp thẻ lại và mở khóa giao diện.
             */
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (firstSelectedCard != null) flipCardBack(firstSelectedCard);
                if (secondSelectedCard != null) flipCardBack(secondSelectedCard);
                resetSelection(); // Mở khóa isProcessing = false sau 1 giây
            }, 1000);
        }
    }

    // Hàm phụ: Úp thẻ về lại trạng thái ban đầu (Hiện dấu hỏi chấm và nền tím)
    private void flipCardBack(CardItem card) {
        card.isFlipped = false;
        card.button.setText("❓");
        card.button.setTextSize(24f);
        card.button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E1BEE7")));
        card.button.setTextColor(Color.parseColor("#4A148C"));
    }

    // Hàm phụ: Xóa bộ nhớ tạm 2 thẻ đã chọn và mở khóa luồng cho ván lật tiếp theo
    private void resetSelection() {
        firstSelectedCard = null;
        secondSelectedCard = null;
        isProcessing = false;
    }
}