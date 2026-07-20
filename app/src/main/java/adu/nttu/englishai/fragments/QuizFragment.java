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


    private TextView tvQuestion, tvConfetti;
    private Button btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4, btnNextQuestion;


    private List<Vocabulary> vocabularyList;
    private Vocabulary currentQuestionWord;


    private boolean isQuestionAnswered = false;

    public QuizFragment() {}

    // =========================================================================
    // HÀM TẠO GIAO DIỆN & GÁN SỰ KIỆN
    // =========================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm  bản thiết kế XML fragment_quiz thành đối tượng View
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        // Xử lý nút Back
        View btnBackToStage = view.findViewById(R.id.btnBackToStage);
        if (btnBackToStage != null) {
            btnBackToStage.setOnClickListener(v -> {
                // Đóng Fragment hiện tại và trở về màn hình bản đồ Ải trong ngăn xếp
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

        // Lấy danh sách từ vựng từ bộ nhớ trung tâm
        vocabularyList = DataRepository.getInstance().getVocabularyList();

        // Khởi tạo câu hỏi trắc nghiệm đầu tiên
        generateNewQuestion();

        // Bấm nút "Câu tiếp theo" -> Gọi hàm tạo câu hỏi mới
        btnNextQuestion.setOnClickListener(v -> generateNewQuestion());

        return view;
    }

    // =========================================================================
    // HÀM 1: THUẬT TOÁN TẠO CÂU HỎI & ĐÁP ÁN NHIỄU
    // =========================================================================
    private void generateNewQuestion() {
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

        // 2. Tạo danh sách chứa 4 đáp án
        List<String> options = new ArrayList<>();
        options.add(currentQuestionWord.getVietnameseMeaning());

        /*
         * 3. THUẬT TOÁN TẠO ĐÁP ÁN NHIỄU KHÔNG TRÙNG LẶP
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
    // HÀM 2: KIỂM TRA ĐÁP ÁN & CƠ CHẾ CHO PHÉP CHỌN LẠI
    // =========================================================================
    private void checkAnswer(Button selectedButton, String selectedAnswer) {
        if (isQuestionAnswered) return; // Nếu đã trả lời đúng rồi thì không cho bấm các nút khác nữa

        // TRƯỜNG HỢP 1: CHỌN ĐÚNG ĐÁP ÁN
        if (selectedAnswer.equals(currentQuestionWord.getVietnameseMeaning())) {
            isQuestionAnswered = true;

            // 1. Đổi nút sang tông màu xanh ngọc
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
            // TRƯỜNG HỢP 2: CHỌN SAI ĐÁP ÁN
            // Đổi nút vừa bấm sang tông màu đỏ
            selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            selectedButton.setTextColor(Color.parseColor("#B71C1C"));

            selectedButton.setEnabled(false);

            // Hiện Toast động viên người học chọn lại
            Toast.makeText(getContext(), "Chưa đúng rồi, bạn thử chọn lại xem! 😅", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // HÀM 3: HIỆU ỨNG HOẠT HÌNH PHÁO HOA
    // =========================================================================
    private void showConfettiAnimation() {
        tvConfetti.setVisibility(View.VISIBLE);

        ScaleAnimation scale = new ScaleAnimation(0.2f, 1.5f, 0.2f, 1.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        AlphaAnimation alpha = new AlphaAnimation(1f, 0f);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        set.setDuration(1200);

        tvConfetti.startAnimation(set);

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