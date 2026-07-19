package adu.nttu.englishai.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

// =========================================================================
// QUIZ FRAGMENT: Màn hình Trò chơi Trắc nghiệm Từ vựng 4 đáp án (A, B, C, D)
// =========================================================================
public class QuizFragment extends Fragment {

    // Các thành phần giao diện (UI Components)
    private TextView tvQuestion, tvConfetti; // Nhãn câu hỏi và nhãn pháo hoa chúc mừng
    private Button btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4, btnNextQuestion;

    // Danh sách toàn bộ từ vựng và biến lưu từ vựng đang được hỏi hiện tại
    private List<Vocabulary> vocabularyList;
    private Vocabulary currentQuestionWord;

    // Cờ đánh dấu câu hỏi đã được trả lời đúng hay chưa (Tránh spam bấm liên tục)
    private boolean isQuestionAnswered = false;

    public QuizFragment() {}

    // =========================================================================
    // HÀM TẠO GIAO DIỆN & GÁN SỰ KIỆN (ON CREATE VIEW)
    // =========================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm (inflate) bản thiết kế XML fragment_quiz thành đối tượng View
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        // Xử lý nút Back: Bấm vào là trượt mượt mà về lại trang Danh Sách Nhiệm Vụ Ải
        View btnBackToStage = view.findViewById(R.id.btnBackToStage);
        if (btnBackToStage != null) {
            btnBackToStage.setOnClickListener(v -> {
                // Đóng Fragment hiện tại và trở về màn hình bản đồ Ải trong ngăn xếp (Backstack)
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Ánh xạ các thành phần UI
        tvQuestion = view.findViewById(R.id.tvQuestion);
        tvConfetti = view.findViewById(R.id.tvConfetti);
        btnAnswer1 = view.findViewById(R.id.btnAnswer1);
        btnAnswer2 = view.findViewById(R.id.btnAnswer2);
        btnAnswer3 = view.findViewById(R.id.btnAnswer3);
        btnAnswer4 = view.findViewById(R.id.btnAnswer4);
        btnNextQuestion = view.findViewById(R.id.btnNextQuestion);

        // Lấy danh sách từ vựng từ bộ nhớ trung tâm (Singleton Pattern)
        vocabularyList = DataRepository.getInstance().getVocabularyList();

        // Khởi tạo câu hỏi trắc nghiệm đầu tiên
        generateNewQuestion();

        // Bấm nút "Câu tiếp theo" -> Gọi hàm tạo câu hỏi mới
        btnNextQuestion.setOnClickListener(v -> generateNewQuestion());

        return view;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 1: THUẬT TOÁN TẠO CÂU HỎI & ĐÁP ÁN NHIỄU (QUESTION GENERATION)
    // =========================================================================
    private void generateNewQuestion() {
        /*
         * LẬP TRÌNH PHÒNG VỆ (Defensive Guard Check):
         * Trắc nghiệm cần 4 đáp án (1 đúng + 3 sai). Nếu danh sách từ vựng trống hoặc có ít hơn 4 từ,
         * hệ thống sẽ không đủ dữ liệu tạo đáp án nhiễu -> Dừng hàm ngay lập tức để tránh lỗi Crash!
         */
        if (vocabularyList == null || vocabularyList.size() < 4) return;

        // Reset trạng thái cờ, mở khóa 4 nút và giấu nút Tiếp theo & pháo hoa đi
        isQuestionAnswered = false;
        resetButtons();
        btnNextQuestion.setVisibility(View.GONE);
        tvConfetti.setVisibility(View.GONE);

        // 1. Chọn ngẫu nhiên 1 từ vựng trong kho làm câu hỏi chính
        Random random = new Random();
        currentQuestionWord = vocabularyList.get(random.nextInt(vocabularyList.size()));
        tvQuestion.setText("Nghĩa của từ '" + currentQuestionWord.getEnglishWord() + "' là gì?");

        // 2. Tạo danh sách chứa 4 đáp án (Bắt đầu bằng việc thêm ngay đáp án đúng vào trước)
        List<String> options = new ArrayList<>();
        options.add(currentQuestionWord.getVietnameseMeaning());

        /*
         * 3. THUẬT TOÁN TẠO ĐÁP ÁN NHIỄU KHÔNG TRÙNG LẶP (Unique Random Distractor Generation):
         * Dùng vòng lặp while để liên tục bốc ngẫu nhiên các từ khác trong kho.
         * Chỉ khi nào từ bốc được CHƯA TỒN TẠI trong danh sách options (!options.contains) thì mới thêm vào.
         * Vòng lặp dừng ngay khi đủ 4 đáp án (1 đúng + 3 nhiễu sai).
         */
        while (options.size() < 4) {
            Vocabulary randomWord = vocabularyList.get(random.nextInt(vocabularyList.size()));
            if (!options.contains(randomWord.getVietnameseMeaning())) {
                options.add(randomWord.getVietnameseMeaning());
            }
        }

        // 4. Xáo trộn vị trí 4 đáp án để đáp án đúng không phải lúc nào cũng nằm ở nút A
        Collections.shuffle(options);

        // 5. Gán nội dung lên 4 nút bấm A, B, C, D
        btnAnswer1.setText("A. " + options.get(0));
        btnAnswer2.setText("B. " + options.get(1));
        btnAnswer3.setText("C. " + options.get(2));
        btnAnswer4.setText("D. " + options.get(3));

        // Gán sự kiện kiểm tra kết quả khi bấm vào từng nút
        btnAnswer1.setOnClickListener(v -> checkAnswer(btnAnswer1, options.get(0)));
        btnAnswer2.setOnClickListener(v -> checkAnswer(btnAnswer2, options.get(1)));
        btnAnswer3.setOnClickListener(v -> checkAnswer(btnAnswer3, options.get(2)));
        btnAnswer4.setOnClickListener(v -> checkAnswer(btnAnswer4, options.get(3)));
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 2: KIỂM TRA ĐÁP ÁN & CƠ CHẾ CHO PHÉP CHỌN LẠI (ANSWER CHECKING)
    // =========================================================================
    private void checkAnswer(Button selectedButton, String selectedAnswer) {
        if (isQuestionAnswered) return; // Nếu đã trả lời đúng rồi thì không cho bấm các nút khác nữa

        // TRƯỜNG HỢP 1: CHỌN ĐÚNG ĐÁP ÁN
        if (selectedAnswer.equals(currentQuestionWord.getVietnameseMeaning())) {
            isQuestionAnswered = true;

            // 1. Đổi nút sang tông màu xanh ngọc dịu mắt (#E8F5E9) + Chữ xanh đậm (#1B5E20)
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            selectedButton.setTextColor(Color.parseColor("#1B5E20"));

            // 2. Khóa tất cả 4 nút lại vì đã qua câu hỏi này
            lockAllButtons();

            // 3. Hiện nút "Tiếp theo" góc phải dưới để sang câu mới
            btnNextQuestion.setVisibility(View.VISIBLE);

            // 4. Bắn hiệu ứng pháo hoa chúc mừng và hiện thông báo
            showConfettiAnimation();
            Toast.makeText(getContext(), "Chính xác! Bạn giỏi quá 🎉", Toast.LENGTH_SHORT).show();

        } else {
            // TRƯỜNG HỢP 2: CHỌN SAI ĐÁP ÁN (CƠ CHẾ RETRY THÂN THIỆN)
            // Đổi nút vừa bấm sang tông màu đỏ hồng dịu mắt (#FFEBEE) + Chữ đỏ đậm (#B71C1C)
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            selectedButton.setTextColor(Color.parseColor("#B71C1C"));

            /*
             * BÍ KÍP ĐỒ ÁN (UX GAMIFICATION ENHANCEMENT):
             * Thay vì khóa cả 4 nút làm người dùng bị mất lượt ngay lập tức,
             * ta CHỈ KHÓA duy nhất nút vừa chọn sai (selectedButton.setEnabled(false)).
             * Các nút còn lại VẪN MỞ để học viên được tiếp tục suy nghĩ và bấm thử lại cho đến khi tìm ra đáp án đúng!
             * Cách thiết kế này giúp giảm áp lực tâm lý, khuyến khích sự tự học qua thử-và-sai (Trial and Error).
             */
            selectedButton.setEnabled(false);

            // Hiện Toast động viên người học chọn lại
            Toast.makeText(getContext(), "Chưa đúng rồi, bạn thử chọn lại xem! 😅", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 3: HIỆU ỨNG HOẠT HÌNH PHÁO HOA (CONFETTI ANIMATION SET)
    // =========================================================================
    private void showConfettiAnimation() {
        tvConfetti.setVisibility(View.VISIBLE);

        /*
         * KỸ THUẬT KẾT HỢP HOẠT HÌNH KÉP (ANIMATION SET):
         * - ScaleAnimation: Phóng to pháo hoa từ 20% (0.2f) lên 150% (1.5f) lấy tâm nằm chính giữa thẻ (0.5f).
         * - AlphaAnimation: Làm mờ dần pháo hoa từ rõ ràng 100% (1f) xuống tàng hình 0% (0f).
         */
        ScaleAnimation scale = new ScaleAnimation(0.2f, 1.5f, 0.2f, 1.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        AlphaAnimation alpha = new AlphaAnimation(1f, 0f);

        // AnimationSet(true): Nhóm cả 2 hiệu ứng Phóng to và Làm mờ để chạy ĐỒNG THỜI cùng lúc!
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        set.setDuration(1200); // Tổng thời gian bay pháo hoa là 1.2 giây

        tvConfetti.startAnimation(set);

        // KỸ THUẬT DỌN DẸP GIAO DIỆN: Sau đúng 1.2 giây bay pháo hoa xong -> Lập tức ẩn View đi (GONE)
        // Ngăn thẻ TextView trống che khuất màn hình cản trở các thao tác bấm nút tiếp theo
        tvConfetti.postDelayed(() -> tvConfetti.setVisibility(View.GONE), 1200);
    }

    // Hàm phụ: Khóa toàn bộ 4 nút bấm A, B, C, D
    private void lockAllButtons() {
        btnAnswer1.setEnabled(false);
        btnAnswer2.setEnabled(false);
        btnAnswer3.setEnabled(false);
        btnAnswer4.setEnabled(false);
    }

    // Hàm phụ: Đặt lại màu sắc và mở khóa 4 nút chuẩn bị cho câu hỏi tiếp theo
    private void resetButtons() {
        Button[] buttons = {btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4};
        for (Button btn : buttons) {
            btn.setEnabled(true);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            btn.setTextColor(Color.parseColor("#333333"));
        }
    }
}