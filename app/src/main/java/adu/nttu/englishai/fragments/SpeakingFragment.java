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

    private TextView tvWordToSpeak, tvPhoneticSpeaking, tvMeaningSpeaking, tvSpeakingResult;
    private ImageButton btnRecord;
    private Button btnNextWord;
    private MaterialCardView cardResult;

    private List<Vocabulary> vocabularyList;
    private int currentIndex = 0;

    private ActivityResultLauncher<Intent> speechLauncher;

    public SpeakingFragment() {}

    // =========================================================================
    // HÀM 1: ĐĂNG KÝ BỘ LẮNG NGHE GIỌNG NÓI
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
                    // Kiểm tra nếu thu âm thành công và có dữ liệu trả về
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
    // HÀM TẠO GIAO DIỆN & GÁN SỰ KIỆN
    // =========================================================================
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm bản thiết kế XML fragment_speaking thành đối tượng View
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

            // Trả thẻ kết quả về màu trắng trung tính, chuẩn bị cho lần đọc mới
            cardResult.setCardBackgroundColor(Color.WHITE);
            tvSpeakingResult.setTextColor(Color.parseColor("#555555"));
            tvSpeakingResult.setText("👆 Chạm vào Mic ở trên để bắt đầu kiểm tra");
        }
    }

    // =========================================================================
    // HÀM 2: KÍCH HOẠT GOOGLE SPEECH-TO-TEXT
    // =========================================================================
    private void startVoiceRecognition() {
        // Tạo chuyến xe Intent gọi dịch vụ Nhận dạng Giọng nói của hệ điều hành
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // CẤU HÌNH THÔNG SỐ THU ÂM:
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy phát âm từ: " + tvWordToSpeak.getText().toString());

        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Thiết bị không hỗ trợ thu âm!", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // HÀM 3: ĐỐI CHIẾU PHÁT ÂM & PHẢN HỒI GIAO DIỆN
    // =========================================================================
    private void checkPronunciation(String spokenText) {
        String targetWord = tvWordToSpeak.getText().toString().trim();

        /*
         * SO SÁNH KHÔNG PHÂN BIỆT HOA/THƯỜNG
         */
        if (spokenText.equalsIgnoreCase(targetWord)) {
            // TRƯỜNG HỢP 1: PHÁT ÂM CHUẨN
            // Đổi nền thẻ sang màu xanh lá
            cardResult.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            tvSpeakingResult.setTextColor(Color.parseColor("#1B5E20"));
            tvSpeakingResult.setText("🎉 Xuất sắc! Bạn vừa phát âm chuẩn từ \"" + targetWord + "\"");
            Toast.makeText(getContext(), "Phát âm chuẩn 100%! 🏆", Toast.LENGTH_SHORT).show();
        } else {
            // TRƯỜNG HỢP 2: PHÁT ÂM CHƯA CHUẨN
            // Đổi nền thẻ sang màu đỏ hồng
            cardResult.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            tvSpeakingResult.setTextColor(Color.parseColor("#B71C1C"));
            // Hiển thị chính xác từ học viên vừa nói bị sai thành gì để họ tự điều chỉnh khẩu hình
            tvSpeakingResult.setText("😅 Bạn vừa nói là: \"" + spokenText + "\"\nHãy nghe kỹ lại và bấm Mic thử lại nhé!");
            Toast.makeText(getContext(), "Chưa chuẩn lắm, thử lại nào!", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // HÀM CHUYỂN TỪ TIẾP THEO
    // =========================================================================
    private void nextWord() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;

        // Thuật toán chỉ số tuần hoàn: Lặp lại từ đầu khi hết danh sách
        currentIndex = (currentIndex + 1) % vocabularyList.size();
        showCurrentWord();
    }
}