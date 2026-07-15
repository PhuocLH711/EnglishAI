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

public class SpeakingFragment extends Fragment {

    private TextView tvWordToSpeak, tvPhoneticSpeaking, tvMeaningSpeaking, tvSpeakingResult;
    private ImageButton btnRecord;
    private Button btnNextWord;
    private MaterialCardView cardResult;

    private List<Vocabulary> vocabularyList;
    private int currentIndex = 0;
    private ActivityResultLauncher<Intent> speechLauncher;

    public SpeakingFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đăng ký bộ lắng nghe Google Speech-to-Text
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            checkPronunciation(matches.get(0));
                        }
                    } else {
                        tvSpeakingResult.setText("💡 Bạn chưa nói gì cả, thử lại nhé!");
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_speaking, container, false);

        tvWordToSpeak = view.findViewById(R.id.tvWordToSpeak);
        tvPhoneticSpeaking = view.findViewById(R.id.tvPhoneticSpeaking);
        tvMeaningSpeaking = view.findViewById(R.id.tvMeaningSpeaking);
        tvSpeakingResult = view.findViewById(R.id.tvSpeakingResult);
        btnRecord = view.findViewById(R.id.btnRecord);
        btnNextWord = view.findViewById(R.id.btnNextWord);
        cardResult = view.findViewById(R.id.cardResult);

        vocabularyList = DataRepository.getInstance().getVocabularyList();
        showCurrentWord();

        // Bấm Mic -> Mở khung thu âm của Google
        btnRecord.setOnClickListener(v -> startVoiceRecognition());

        // Bấm Từ tiếp theo -> Chuyển sang từ mới
        btnNextWord.setOnClickListener(v -> nextWord());

        return view;
    }

    private void showCurrentWord() {
        if (vocabularyList != null && !vocabularyList.isEmpty()) {
            Vocabulary currentWord = vocabularyList.get(currentIndex);
            tvWordToSpeak.setText(currentWord.getEnglishWord());
            tvPhoneticSpeaking.setText(currentWord.getPronunciation());
            tvMeaningSpeaking.setText("(" + currentWord.getVietnameseMeaning() + ")");

            // Reset thẻ kết quả về màu trắng ban đầu
            cardResult.setCardBackgroundColor(Color.WHITE);
            tvSpeakingResult.setTextColor(Color.parseColor("#555555"));
            tvSpeakingResult.setText("👆 Chạm vào Mic ở trên để bắt đầu kiểm tra");
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy phát âm từ: " + tvWordToSpeak.getText().toString());

        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Thiết bị không hỗ trợ thu âm!", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPronunciation(String spokenText) {
        String targetWord = tvWordToSpeak.getText().toString().trim();

        // So sánh từ bạn nói với từ mục tiêu (không phân biệt hoa thường)
        if (spokenText.equalsIgnoreCase(targetWord)) {
            // ĐÚNG: Nền xanh lá pastel (#E8F5E9), Chữ xanh đậm (#1B5E20)
            cardResult.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            tvSpeakingResult.setTextColor(Color.parseColor("#1B5E20"));
            tvSpeakingResult.setText("🎉 Xuất sắc! Bạn vừa phát âm chuẩn từ \"" + targetWord + "\"");
            Toast.makeText(getContext(), "Phát âm chuẩn 100%! 🏆", Toast.LENGTH_SHORT).show();
        } else {
            // SAI: Nền đỏ hồng pastel (#FFEBEE), Chữ đỏ đậm (#B71C1C)
            cardResult.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            tvSpeakingResult.setTextColor(Color.parseColor("#B71C1C"));
            tvSpeakingResult.setText("😅 Bạn vừa nói là: \"" + spokenText + "\"\nHãy nghe kỹ lại và bấm Mic thử lại nhé!");
            Toast.makeText(getContext(), "Chưa chuẩn lắm, thử lại nào!", Toast.LENGTH_SHORT).show();
        }
    }

    private void nextWord() {
        if (vocabularyList == null || vocabularyList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % vocabularyList.size();
        showCurrentWord();
    }
}