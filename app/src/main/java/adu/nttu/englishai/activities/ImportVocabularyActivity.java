package adu.nttu.englishai.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.Vocabulary;

public class ImportVocabularyActivity extends AppCompatActivity {

    private Button btnImportVocabulary;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_vocabulary);

        btnImportVocabulary =
                findViewById(R.id.btnImportVocabulary);

        db = FirebaseFirestore.getInstance();

        btnImportVocabulary.setOnClickListener(
                view -> importVocabulary()
        );
    }

    private void importVocabulary() {
        btnImportVocabulary.setEnabled(false);
        btnImportVocabulary.setText("Đang nhập dữ liệu...");

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                getAssets().open("vocabularies.json")
                        )
                )
        ) {
            Type listType =
                    new TypeToken<List<Vocabulary>>() {
                    }.getType();

            List<Vocabulary> vocabularyList =
                    new Gson().fromJson(reader, listType);

            if (vocabularyList == null
                    || vocabularyList.isEmpty()) {

                showResult("File từ vựng đang trống.");
                return;
            }

            Toast.makeText(
                    this,
                    "Đã đọc được "
                            + vocabularyList.size()
                            + " từ trong JSON",
                    Toast.LENGTH_SHORT
            ).show();

            uploadVocabularyList(vocabularyList);

        } catch (Exception exception) {
            showResult(
                    "Không đọc được file JSON: "
                            + exception.getMessage()
            );
        }
    }

    private void uploadVocabularyList(
            List<Vocabulary> vocabularyList
    ) {
        final int total = vocabularyList.size();
        final int[] successCount = {0};
        final int[] failureCount = {0};

        for (Vocabulary vocabulary : vocabularyList) {
            String englishWord =
                    vocabulary.getEnglishWord();

            String documentId =
                    createDocumentId(englishWord);

            db.collection("vocabularies")
                    .document(documentId)
                    .set(vocabulary)
                    .addOnSuccessListener(unused -> {
                        successCount[0]++;

                        checkUploadFinished(
                                total,
                                successCount[0],
                                failureCount[0]
                        );
                    })
                    .addOnFailureListener(exception -> {
                        failureCount[0]++;

                        checkUploadFinished(
                                total,
                                successCount[0],
                                failureCount[0]
                        );
                    });
        }
    }

    private String createDocumentId(
            String englishWord
    ) {
        if (englishWord == null
                || englishWord.trim().isEmpty()) {

            return db.collection("vocabularies")
                    .document()
                    .getId();
        }

        return englishWord
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private void checkUploadFinished(
            int total,
            int successCount,
            int failureCount
    ) {
        if (successCount + failureCount != total) {
            return;
        }

        btnImportVocabulary.setEnabled(true);
        btnImportVocabulary.setText(
                "Nhập dữ liệu từ vựng"
        );

        Toast.makeText(
                this,
                "Thành công: "
                        + successCount
                        + "\nThất bại: "
                        + failureCount,
                Toast.LENGTH_LONG
        ).show();
    }

    private void showResult(
            String message
    ) {
        btnImportVocabulary.setEnabled(true);
        btnImportVocabulary.setText(
                "Nhập dữ liệu từ vựng"
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}