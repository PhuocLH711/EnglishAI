package adu.nttu.englishai.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.SentenceExercise;

public class ImportSentenceExerciseActivity extends AppCompatActivity {

    private Button btnImportSentenceExercises;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_import_sentence_exercise
        );

        db = FirebaseFirestore.getInstance();

        btnImportSentenceExercises =
                findViewById(
                        R.id.btnImportSentenceExercises
                );

        btnImportSentenceExercises.setOnClickListener(
                view -> importSentenceExercises()
        );
    }

    private void importSentenceExercises() {

        btnImportSentenceExercises.setEnabled(false);
        btnImportSentenceExercises.setText("Đang nhập dữ liệu...");

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    getAssets().open(
                                            "sentence_exercises_100.json"
                                    )
                            )
                    );

            Type listType =
                    new TypeToken<List<SentenceExercise>>() {
                    }.getType();

            List<SentenceExercise> exerciseList =
                    new Gson().fromJson(
                            reader,
                            listType
                    );

            reader.close();

            if (exerciseList == null
                    || exerciseList.isEmpty()) {

                showError(
                        "File JSON không có dữ liệu."
                );
                return;
            }

            uploadExercises(exerciseList);

        } catch (Exception exception) {

            showError(
                    "Không đọc được file JSON: "
                            + exception.getMessage()
            );
        }
    }

    private void uploadExercises(
            List<SentenceExercise> exerciseList
    ) {

        WriteBatch batch =
                db.batch();

        int validCount = 0;

        for (SentenceExercise exercise
                : exerciseList) {

            if (exercise == null) {
                continue;
            }

            String id =
                    exercise.getId();

            String englishSentence =
                    exercise.getEnglishSentence();

            if (id == null
                    || id.trim().isEmpty()) {
                continue;
            }

            if (englishSentence == null
                    || englishSentence.trim().isEmpty()) {
                continue;
            }

            DocumentReference documentReference =
                    db.collection("sentenceExercises")
                            .document(id.trim());

            // Dùng set để có thể import lại mà không tạo document trùng.
            batch.set(
                    documentReference,
                    exercise
            );

            validCount++;
        }

        if (validCount == 0) {
            showError(
                    "Không tìm thấy câu hợp lệ để import."
            );
            return;
        }

        final int total = validCount;

        batch.commit()
                .addOnSuccessListener(
                        unused -> {

                            btnImportSentenceExercises.setEnabled(true);
                            btnImportSentenceExercises.setText(
                                    "Nhập 100 câu lên Firestore"
                            );

                            Toast.makeText(
                                    this,
                                    "Import thành công "
                                            + total
                                            + " câu.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        exception ->
                                showError(
                                        "Import thất bại: "
                                                + exception.getMessage()
                                )
                );
    }

    private void showError(
            String message
    ) {

        btnImportSentenceExercises.setEnabled(true);
        btnImportSentenceExercises.setText(
                "Nhập 100 câu lên Firestore"
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}