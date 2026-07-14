package adu.nttu.englishai.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import java.util.Locale;

public class SpeechRecognitionManager {

    private final Context context;
    private final ActivityResultLauncher<Intent> speechLauncher;

    public SpeechRecognitionManager(
            Context context,
            ActivityResultLauncher<Intent> speechLauncher
    ) {
        this.context = context;
        this.speechLauncher = speechLauncher;
    }

    public void startEnglishRecognition() {
        Intent intent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.US.toLanguageTag()
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Hãy nói câu hỏi tiếng Anh của bạn..."
        );

        try {
            speechLauncher.launch(intent);

        } catch (Exception exception) {
            Toast.makeText(
                    context,
                    "Thiết bị chưa hỗ trợ nhận diện giọng nói",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    public static boolean isSuccessfulResult(
            int resultCode,
            Intent resultData
    ) {
        return resultCode == Activity.RESULT_OK
                && resultData != null;
    }
}