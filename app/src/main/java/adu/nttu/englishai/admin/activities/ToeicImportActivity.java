package adu.nttu.englishai.admin.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.ToeicQuestion;
import adu.nttu.englishai.models.ToeicTest;

public class ToeicImportActivity extends AppCompatActivity {

    private static final String ASSET_FILE =
            "toeic_test_import.json";

    private static final int MAX_BATCH_WRITES =
            400;

    private TextView btnToeicImportBack;
    private MaterialButton btnImportToeic;
    private TextView tvImportStatus;

    private FirebaseFirestore firestore;

    private final Gson gson =
            new Gson();

    private final List<ImportPackage> packages =
            new ArrayList<>();

    private int totalImportedQuestions = 0;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_import_toeic
        );

        firestore =
                FirebaseFirestore.getInstance();

        btnToeicImportBack =
                findViewById(
                        R.id.btnToeicImportBack
                );

        btnImportToeic =
                findViewById(
                        R.id.btnImportToeic
                );

        tvImportStatus =
                findViewById(
                        R.id.tvImportStatus
                );

        btnToeicImportBack.setOnClickListener(
                view -> finish()
        );

        btnImportToeic.setOnClickListener(
                view -> importToeicFromAssets()
        );
    }

    private void importToeicFromAssets() {

        btnImportToeic.setEnabled(false);

        tvImportStatus.setText(
                "Đang đọc dữ liệu TOEIC..."
        );

        try {

            String json =
                    readAssetFile(
                            ASSET_FILE
                    );

            JsonObject root =
                    JsonParser.parseString(json)
                            .getAsJsonObject();

            packages.clear();
            totalImportedQuestions = 0;

            // Hỗ trợ file mới gồm nhiều bộ đề.
            if (root.has("tests")
                    && root.get("tests").isJsonArray()) {

                JsonArray array =
                        root.getAsJsonArray(
                                "tests"
                        );

                for (JsonElement element
                        : array) {

                    if (!element.isJsonObject()) {
                        continue;
                    }

                    packages.add(
                            parsePackage(
                                    element.getAsJsonObject()
                            )
                    );
                }

            } else {

                // Vẫn hỗ trợ format cũ:
                // { "test": {...}, "questions": [...] }
                packages.add(
                        parsePackage(root)
                );
            }

            if (packages.isEmpty()) {

                throw new IllegalArgumentException(
                        "Không tìm thấy bộ đề TOEIC hợp lệ."
                );
            }

            int totalQuestions = 0;

            for (ImportPackage value
                    : packages) {

                totalQuestions +=
                        value.questions.size();
            }

            tvImportStatus.setText(
                    "Đã đọc "
                            + packages.size()
                            + " bộ đề / "
                            + totalQuestions
                            + " câu. Đang tải lên Firestore..."
            );

            uploadPackage(0);

        } catch (Exception exception) {

            showError(
                    "Import thất bại:\n"
                            + exception.getMessage()
            );
        }
    }

    private ImportPackage parsePackage(
            JsonObject root
    ) {

        if (!root.has("test")
                || !root.has("questions")) {

            throw new IllegalArgumentException(
                    "Mỗi bộ đề phải có test và questions."
            );
        }

        JsonObject testObject =
                root.getAsJsonObject(
                        "test"
                );

        JsonArray questionArray =
                root.getAsJsonArray(
                        "questions"
                );

        String testId =
                readRequiredString(
                        testObject,
                        "id"
                );

        ToeicTest test =
                gson.fromJson(
                        testObject,
                        ToeicTest.class
                );

        if (test == null) {
            throw new IllegalArgumentException(
                    "Không đọc được test "
                            + testId
            );
        }

        List<ToeicQuestion> questions =
                new ArrayList<>();

        for (int i = 0;
             i < questionArray.size();
             i++) {

            JsonObject questionObject =
                    questionArray.get(i)
                            .getAsJsonObject();

            ToeicQuestion question =
                    gson.fromJson(
                            questionObject,
                            ToeicQuestion.class
                    );

            if (question == null) {
                continue;
            }

            String questionId;

            if (questionObject.has("id")
                    && !questionObject.get("id").isJsonNull()) {

                questionId =
                        questionObject.get("id")
                                .getAsString()
                                .trim();

            } else {

                questionId =
                        testId
                                + "_q"
                                + String.format(
                                Locale.ROOT,
                                "%03d",
                                question.getQuestionNumber()
                        );
            }

            if (questionId.isEmpty()
                    || question.getQuestionNumber() <= 0
                    || question.getPart() < 1
                    || question.getPart() > 7) {

                continue;
            }

            String correct =
                    question.getCorrectAnswer();

            if (correct == null) {
                continue;
            }

            correct =
                    correct.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );

            if (!correct.equals("A")
                    && !correct.equals("B")
                    && !correct.equals("C")
                    && !correct.equals("D")) {

                continue;
            }

            question.setId(
                    questionId
            );

            question.setTestId(
                    testId
            );

            question.setCorrectAnswer(
                    correct
            );

            questions.add(
                    question
            );
        }

        if (questions.isEmpty()) {

            throw new IllegalArgumentException(
                    testId
                            + ": không có câu hỏi hợp lệ."
            );
        }

        test.setId(
                testId
        );

        test.setTotalQuestions(
                questions.size()
        );

        return new ImportPackage(
                testId,
                test,
                questions
        );
    }

    private void uploadPackage(
            int packageIndex
    ) {

        if (packageIndex
                >= packages.size()) {

            btnImportToeic.setEnabled(
                    true
            );

            tvImportStatus.setText(
                    "✅ Import thành công "
                            + packages.size()
                            + " bộ đề / "
                            + totalImportedQuestions
                            + " câu TOEIC."
            );

            Toast.makeText(
                    this,
                    "Đã import "
                            + packages.size()
                            + " bộ đề TOEIC.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        ImportPackage value =
                packages.get(
                        packageIndex
                );

        tvImportStatus.setText(
                "Đang tạo "
                        + value.test.getTitle()
                        + "..."
        );

        firestore.collection("toeicTests")
                .document(
                        value.testId
                )
                .set(
                        value.test
                )
                .addOnSuccessListener(
                        unused ->
                                uploadQuestionBatch(
                                        packageIndex,
                                        value,
                                        0
                                )
                )
                .addOnFailureListener(
                        exception ->
                                showError(
                                        "Không ghi được "
                                                + value.testId
                                                + ":\n"
                                                + exception.getMessage()
                                )
                );
    }

    private void uploadQuestionBatch(
            int packageIndex,
            ImportPackage value,
            int startIndex
    ) {

        if (startIndex
                >= value.questions.size()) {

            totalImportedQuestions +=
                    value.questions.size();

            uploadPackage(
                    packageIndex + 1
            );

            return;
        }

        int endIndex =
                Math.min(
                        startIndex
                                + MAX_BATCH_WRITES,
                        value.questions.size()
                );

        WriteBatch batch =
                firestore.batch();

        for (int i = startIndex;
             i < endIndex;
             i++) {

            ToeicQuestion question =
                    value.questions.get(i);

            batch.set(
                    firestore
                            .collection(
                                    "toeicQuestions"
                            )
                            .document(
                                    question.getId()
                            ),
                    question
            );
        }

        tvImportStatus.setText(
                value.test.getTitle()
                        + "\nĐang tải câu "
                        + (startIndex + 1)
                        + " - "
                        + endIndex
                        + " / "
                        + value.questions.size()
        );

        final int nextIndex =
                endIndex;

        batch.commit()
                .addOnSuccessListener(
                        unused ->
                                uploadQuestionBatch(
                                        packageIndex,
                                        value,
                                        nextIndex
                                )
                )
                .addOnFailureListener(
                        exception ->
                                showError(
                                        "Lỗi upload "
                                                + value.testId
                                                + ":\n"
                                                + exception.getMessage()
                                )
                );
    }

    private String readRequiredString(
            JsonObject object,
            String key
    ) {

        if (!object.has(key)
                || object.get(key).isJsonNull()) {

            throw new IllegalArgumentException(
                    "Thiếu trường "
                            + key
            );
        }

        String value =
                object.get(key)
                        .getAsString()
                        .trim();

        if (value.isEmpty()) {

            throw new IllegalArgumentException(
                    key
                            + " không được để trống."
            );
        }

        return value;
    }

    private String readAssetFile(
            String fileName
    ) throws Exception {

        StringBuilder builder =
                new StringBuilder();

        try (
                InputStream inputStream =
                        getAssets().open(
                                fileName
                        );

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;

            while ((line = reader.readLine())
                    != null) {

                builder.append(line)
                        .append('\n');
            }
        }

        return builder.toString();
    }

    private void showError(
            String message
    ) {

        btnImportToeic.setEnabled(
                true
        );

        tvImportStatus.setText(
                message
        );

        Toast.makeText(
                this,
                "Không thể import TOEIC.",
                Toast.LENGTH_LONG
        ).show();
    }

    private static class ImportPackage {

        final String testId;
        final ToeicTest test;
        final List<ToeicQuestion> questions;

        ImportPackage(
                String testId,
                ToeicTest test,
                List<ToeicQuestion> questions
        ) {
            this.testId = testId;
            this.test = test;
            this.questions = questions;
        }
    }
}