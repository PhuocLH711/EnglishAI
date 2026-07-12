package adu.nttu.englishai.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import adu.nttu.englishai.R;

public class AiTutorActivity extends AppCompatActivity {

    private TextView tvAiState;
    private TextView tvAiResponse;

    private Button btnSpeakToAi;
    private Button btnTypeQuestion;
    private Button btnChooseTeacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_tutor);

        MaterialToolbar toolbar =
                findViewById(R.id.toolbarAiTutor);

        toolbar.setNavigationOnClickListener(view -> finish());

        tvAiState = findViewById(R.id.tvAiState);
        tvAiResponse = findViewById(R.id.tvAiResponse);

        btnSpeakToAi = findViewById(R.id.btnSpeakToAi);
        btnTypeQuestion = findViewById(R.id.btnTypeQuestion);
        btnChooseTeacher = findViewById(R.id.btnChooseTeacher);



        btnSpeakToAi.setOnClickListener(view -> {
            tvAiState.setText("🎤 Đang nghe...");

            Toast.makeText(
                    AiTutorActivity.this,
                    "Phần nhận giọng nói sẽ làm ở bước tiếp theo",
                    Toast.LENGTH_SHORT
            ).show();
        });

        btnTypeQuestion.setOnClickListener(view -> {
            Toast.makeText(
                    AiTutorActivity.this,
                    "Phần nhập câu hỏi sẽ làm tiếp theo",
                    Toast.LENGTH_SHORT
            ).show();
        });

        btnChooseTeacher.setOnClickListener(view -> {
            Toast.makeText(
                    AiTutorActivity.this,
                    "Sắp mở màn hình chọn giáo viên",
                    Toast.LENGTH_SHORT
            ).show();
        });


    }

}