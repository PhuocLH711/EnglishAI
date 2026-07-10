package adu.nttu.englishai.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Locale;

import adu.nttu.englishai.R;

public class SpeakingFragment extends Fragment {

    private Button btnMic;
    private TextView tvTargetWord, tvSpokenText, tvAccuracy;
    private ActivityResultLauncher<Intent> speechLauncher;

    public SpeakingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Đăng ký cổng lắng nghe kết quả trả về từ bộ thu âm của Google
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            tvSpokenText.setText(spokenText);
                            checkPronunciation(spokenText);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_speaking, container, false);

        btnMic = view.findViewById(R.id.btnMic);
        tvTargetWord = view.findViewById(R.id.tvTargetWord);
        tvSpokenText = view.findViewById(R.id.tvSpokenText);
        tvAccuracy = view.findViewById(R.id.tvAccuracy);

        btnMic.setOnClickListener(v -> startSpeechToText());

        return view;
    }

    // Hàm kích hoạt hộp thoại thu âm tiếng Anh của Google
    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString()); // Ép nhận diện giọng tiếng Anh chuẩn US
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy đọc to từ trên màn hình...");

        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Thiết bị không hỗ trợ nhận diện giọng nói!", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm so sánh chuỗi chữ phát âm
    private void checkPronunciation(String spokenText) {
        String target = tvTargetWord.getText().toString().trim();

        // So sánh không phân biệt chữ hoa hay chữ thường
        if (spokenText.equalsIgnoreCase(target)) {
            tvAccuracy.setText("Chính xác! 100% 🌟");
            tvAccuracy.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvAccuracy.setText("Chưa chuẩn rồi, thử lại nhé! ❌");
            tvAccuracy.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
}