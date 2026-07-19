package adu.nttu.englishai.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.DataRepository;
import adu.nttu.englishai.models.Vocabulary;

// =========================================================================
// SPEAKING FRAGMENT: Màn hình luyện phát âm & Kiểm tra giọng nói bằng AI/Google STT
// =========================================================================
public class SpeakingFragment extends Fragment {

    // Các thành phần giao diện (UI Components)
    private TextView tvWordToSpeak, tvPhoneticSpeaking, tvMeaningSpeaking, tvSpeakingResult;
    private ImageButton btnRecord;
    private Button btnNextWord;
    private MaterialCardView cardResult;

    // Danh sách từ vựng và chỉ số của từ đang luyện tập hiện tại
    private List<Vocabulary> vocabularyList;
    private int currentIndex = 0;

    /*
     * KỸ THUẬT QUAN TRỌNG: API LẮNG NGHE KẾT QUẢ HIỆN ĐẠI (ACTIVITY RESULT LAUNCHER):
     * Đây là bộ công cụ mới của Android Jetpack dùng để thay thế cho hàm onActivityResult() cũ đã bị Deprecated.
     * Nó giúp quản lý việc gửi request thu âm và nhận kết quả trả về một cách an toàn theo vòng đời Fragment.
     */
    private ActivityResultLauncher<Intent> speechLauncher;

    public SpeakingFragment() {}

    // =========================================================================
    // HÀM QUAN TRỌNG 1: ĐĂNG KÝ BỘ LẮNG NGHE GIỌNG NÓI (ON CREATE)
    // =========================================================================
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
         * ĐĂNG KÝ BỘ LẮNG NGHE GOOGLE SPEECH-TO-TEXT (STT):
         * Bắt buộc phải đăng ký trong onCreate() trước khi giao diện được tạo ra.
         * Khi người dùng thu âm xong, Google Speech sẽ đóng hộp thoại thu âm lại và gửi kết quả về khối lệnh này.
         */
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Kiểm tra nếu thu âm thành công (RESULT_OK) và có dữ liệu trả về
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Lấy danh sách các chuỗi văn bản mà Google nhận diện được từ giọng nói của học viên
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                        if (matches != null && !matches.isEmpty()) {
                            // Lấy kết quả có độ chính xác cao nhất (nằm ở vị trí 0) để đem đi đối chiếu
                            checkPronunciation(matches.get(0));
                        }
                    } else {
                        // Trường hợp người dùng bật mic lên nhưng không nói gì rồi bấm tắt
                        tvSpeakingResult.setText("💡 Bạn chưa nói gì cả, thử lại nhé!");
                    }
                }
        );
    }

    // =========================================================================
    // HÀM TẠO GIAO DIỆN & GÁN SỰ KIỆN (ON CREATE VIEW)
    // =========================================================================
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm (inflate) bản thiết kế XML fragment_speaking thành đối tượng View
        View view = inflater.inflate(R.layout.fragment_speaking, container, false);

        // Ánh xạ các thẻ giao diện
        tvWordToSpeak = view.findViewById(R.id.tvWordToSpeak);
        tvPhoneticSpeaking = view.findViewById(R.id.tvPhoneticSpeaking);
        tvMeaningSpeaking = view.findViewById(R.id.tvMeaningSpeaking);
        tvSpeakingResult = view.findViewById(R.id.tvSpeakingResult);
        btnRecord = view.findViewById(R.id.btnRecord);
        btnNextWord = view.findViewById(R.id.btnNextWord);
        cardResult = view.findViewById(R.id.cardResult);

        // Lấy danh sách từ vựng từ kho lưu trữ trung tâm Singleton
        vocabularyList = DataRepository.getInstance().getVocabularyList();

        // Hiển thị từ vựng đầu tiên lên màn hình
        showCurrentWord();

        // Bấm nút Mic -> Mở hộp thoại nhận dạng giọng nói của Google
        btnRecord.setOnClickListener(v -> startVoiceRecognition());

        // Bấm nút Từ tiếp theo -> Chuyển sang từ mới
        btnNextWord.setOnClickListener(v -> nextWord());

        return view;
    }

    // =========================================================================
    // HÀM HIỂN THỊ TỪ VỰNG & RESET GIAO DIỆN THẺ KẾT QUẢ
    // =========================================================================
    private void showCurrentWord() {
        if (vocabularyList != null && !vocabularyList.isEmpty()) {
            Vocabulary currentWord = vocabularyList.get(currentIndex);
            tvWordToSpeak.setText(currentWord.getEnglishWord());
            tvPhoneticSpeaking.setText(currentWord.getPronunciation());
            tvMeaningSpeaking.setText("(" + currentWord.getVietnameseMeaning() + ")");

            // UI STYLING RESET: Trả thẻ kết quả về màu trắng trung tính, chuẩn bị cho lần đọc mới
            cardResult.setCardBackgroundColor(Color.WHITE);
            tvSpeakingResult.setTextColor(Color.parseColor("#555555"));
            tvSpeakingResult.setText("👆 Chạm vào Mic ở trên để bắt đầu kiểm tra");
        }
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 2: KÍCH HOẠT GOOGLE SPEECH-TO-TEXT (VOICE RECOGNITION)
    // =========================================================================
    private void startVoiceRecognition() {
        // Tạo chuyến xe Intent gọi dịch vụ Nhận dạng Giọng nói (Speech Recognizer) của hệ điều hành
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // CẤU HÌNH THÔNG SỐ THU ÂM:
        // 1. LANGUAGE_MODEL_FREE_FORM: Nhận dạng tự do, cho phép học viên nói từ đơn hoặc câu ngắn mượt mà
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // 2. EXTRA_LANGUAGE - KỸ THUẬT QUAN TRỌNG: Ép bộ nhận diện nghe theo chuẩn Tiếng Anh Mỹ (Locale.US)
        // Tránh lỗi hệ thống tự lấy ngôn ngữ tiếng Việt của điện thoại làm sai lệch hoàn toàn kết quả thu âm
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());

        // 3. EXTRA_PROMPT: Hiển thị dòng chữ hướng dẫn trên hộp thoại thu âm của Google
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy phát âm từ: " + tvWordToSpeak.getText().toString());

        /*
         * LẬP TRÌNH PHÒNG VỆ (Defensive Programming - CRITICAL):
         * Một số dòng máy điện thoại (như máy nội địa Trung Quốc, Android TV, hoặc máy không cài Google Mobile Services)
         * sẽ KHÔNG CÓ sẵn app hỗ trợ nhận diện giọng nói. Nếu gọi trực tiếp speechLauncher.launch(intent),
         * ứng dụng sẽ bị văng lỗi crash app (ActivityNotFoundException). Khối try-catch giúp bắt lỗi này và báo cáo thân thiện!
         */
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Thiết bị không hỗ trợ thu âm!", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 3: ĐỐI CHIẾU PHÁT ÂM & PHẢN HỒI GIAO DIỆN (PRONUNCIATION CHECKING)
    // =========================================================================
    private void checkPronunciation(String spokenText) {
        String targetWord = tvWordToSpeak.getText().toString().trim();

        /*
         * SO SÁNH KHÔNG PHÂN BIỆT HOA/THƯỜNG (.equalsIgnoreCase):
         * Google STT có thể trả về "apple" hoặc "Apple" hoặc "APPLE" tùy cấu hình câu.
         * Việc dùng equalsIgnoreCase giúp đảm bảo học viên chỉ cần nói đúng từ là được tính điểm 100%!
         */
        if (spokenText.equalsIgnoreCase(targetWord)) {
            // TRƯỜNG HỢP 1: PHÁT ÂM CHUẨN
            // Đổi nền thẻ sang màu xanh lá pastel (#E8F5E9) + Chữ xanh đậm (#1B5E20) chúc mừng
            cardResult.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            tvSpeakingResult.setTextColor(Color.parseColor("#1B5E20"));
            tvSpeakingResult.setText("🎉 Xuất sắc! Bạn vừa phát âm chuẩn từ \"" + targetWord + "\"");
            Toast.makeText(getContext(), "Phát âm chuẩn 100%! 🏆", Toast.LENGTH_SHORT).show();
        } else {
            // TRƯỜNG HỢP 2: PHÁT ÂM CHƯA CHUẨN
            // Đổi nền thẻ sang màu đỏ hồng pastel (#FFEBEE) + Chữ đỏ đậm (#B71C1C) cảnh báo
            cardResult.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            tvSpeakingResult.setTextColor(Color.parseColor("#B71C1C"));
            // Hiển thị chính xác từ học viên vừa nói bị sai thành gì để họ tự điều chỉnh khẩu hình
            tvSpeakingResult.setText("😅 Bạn vừa nói là: \"" + spokenText + "\"\nHãy nghe kỹ lại và bấm Mic thử lại nhé!");
            Toast.makeText(getContext(), "Chưa chuẩn lắm, thử lại nào!", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // HÀM CHUYỂN TỪ TIẾP THEO (TUẦN HOÀN MẢNG)
    // =========================================================================
    private void nextWord() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        // Thuật toán chỉ số tuần hoàn (Modulo Arithmetic %): Lặp lại từ đầu khi hết danh sách
        currentIndex = (currentIndex + 1) % vocabularyList.size();
        showCurrentWord();
    }
}