package adu.nttu.englishai.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Locale;

import adu.nttu.englishai.R;

public class AiTutorActivity extends AppCompatActivity {

    private TextView tvAiState;
    private TextView tvAiResponse;
    private TextView tvTeacherName;

    private Button btnSpeakToAi;
    private Button btnTypeQuestion;
    private Button btnChooseTeacher;

    private ActivityResultLauncher<Intent> speechLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_tutor);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAiTutor);
        toolbar.setNavigationOnClickListener(view -> finish());

        tvAiState = findViewById(R.id.tvAiState);
        tvAiResponse = findViewById(R.id.tvAiResponse);
        tvTeacherName = findViewById(R.id.tvTeacherName);

        btnSpeakToAi = findViewById(R.id.btnSpeakToAi);
        btnTypeQuestion = findViewById(R.id.btnTypeQuestion);
        btnChooseTeacher = findViewById(R.id.btnChooseTeacher);

        // 1. Đăng ký bộ lắng nghe giọng nói Google Speech-to-Text
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            processUserQuestion(spokenText, true);
                        }
                    } else {
                        tvAiState.setText("💡 Bạn chưa nói gì cả!");
                    }
                }
        );

        // 2. Nút Nói -> Bật Mic Google nghe câu hỏi tiếng Anh
        btnSpeakToAi.setOnClickListener(view -> {
            tvAiState.setText("🎤 Đang nghe bạn nói...");
            startVoiceRecognition();
        });

        // 3. Nút Nhập câu hỏi -> Mở hộp thoại gõ chữ
        btnTypeQuestion.setOnClickListener(view -> showTextInputDialog());

        // 4. Nút Chọn giáo viên -> Đổi qua lại giữa Emma và Jack
        btnChooseTeacher.setOnClickListener(view -> {
            if (tvTeacherName.getText().toString().equals("Emma")) {
                tvTeacherName.setText("Jack");
                tvAiState.setText("Hello! I'm Jack. Let's practice English!");
                tvAiResponse.setText("Xin chào! Thầy Jack có thể giúp gì cho kiến thức ngữ pháp của bạn?");
            } else {
                tvTeacherName.setText("Emma");
                tvAiState.setText("Sẵn sàng giúp bạn học tiếng Anh!");
                tvAiResponse.setText("Xin chào! Hôm nay bạn muốn học gì với cô Emma?");
            }
        });
    }

    // Hàm gọi Google Voice
    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói từ vựng hoặc câu hỏi tiếng Anh...");

        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói!", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm xử lý câu hỏi (Dùng chung cho cả gõ phím và nói bằng Mic)
    private void processUserQuestion(String question, boolean isFromVoice) {
        tvAiState.setText("🤔 " + tvTeacherName.getText().toString() + " đang suy nghĩ...");

        tvAiResponse.postDelayed(() -> {
            String answer = generateAiResponse(question);
            tvAiState.setText("💡 Đã trả lời:");
            String icon = isFromVoice ? "🗣️ Bạn nói: " : "🙋 Bạn hỏi: ";
            tvAiResponse.setText(icon + "\"" + question + "\"\n\n💬 Trả lời:\n" + answer);
        }, 600);
    }

    // Hàm hiển thị hộp thoại gõ câu hỏi
    private void showTextInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đặt câu hỏi cho " + tvTeacherName.getText().toString());

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20);

        final EditText input = new EditText(this);
        input.setLayoutParams(params);
        input.setHint("Ví dụ: Apple / Present simple...");
        input.setBackgroundResource(android.R.drawable.edit_text);
        input.setPadding(30, 30, 30, 30);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setMinLines(2);

        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String question = input.getText().toString().trim();
            if (!question.isEmpty()) {
                processUserQuestion(question, false);
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Bộ não AI nhận diện từ khóa (Hiểu tốt tiếng Anh và từ không dấu)
    private String generateAiResponse(String input) {
        String query = input.toLowerCase(Locale.ROOT);

        if (query.contains("xin chào") || query.contains("hi") || query.contains("hello") || query.contains("chao")) {
            return "Chào bạn! Mình có thể giúp bạn giải thích từ vựng hoặc ngữ pháp. Hãy hỏi mình nhé!";
        } else if (query.contains("apple") || query.contains("táo") || query.contains("tao")) {
            return "🍎 'Apple' (Quả táo) phát âm là /ˈæp.əl/. Ví dụ: 'An apple a day keeps the doctor away'.";
        } else if (query.contains("banana") || query.contains("chuối") || query.contains("chuoi")) {
            return "🍌 'Banana' (Quả chuối) phát âm là /bəˈnɑː.nə/. Ví dụ: 'Monkeys love eating bananas.'";
        } else if (query.contains("hiện tại đơn") || query.contains("present simple") || query.contains("hien tai don") || query.contains("hn ti")) {
            return "📘 THÌ HIỆN TẠI ĐƠN (Present Simple):\n- Diễn tả sự thật hiển nhiên, thói quen.\n- Công thức: S + V(s/es) + O.\n- Ví dụ: She learns English every day.";
        } else if (query.contains("quá khứ") || query.contains("past simple") || query.contains("qua khu")) {
            return "📗 THÌ QUÁ KHỨ ĐƠN (Past Simple):\n- Diễn tả hành động đã kết thúc trong quá khứ.\n- Công thức: S + V2/ed + O.\n- Ví dụ: I visited Vietnam last year.";
        } else {
            return "Cảm ơn câu hỏi của bạn! Bạn có thể thử hỏi mình về nghĩa của từ (ví dụ: apple, banana...) hoặc ngữ pháp (ví dụ: present simple, hien tai don) nhé!";
        }
    }
}