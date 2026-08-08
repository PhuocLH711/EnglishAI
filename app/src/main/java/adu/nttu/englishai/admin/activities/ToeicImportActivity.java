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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.ToeicQuestion;
import adu.nttu.englishai.models.ToeicTest;

/**
 * Công cụ import một bộ đề TOEIC từ assets/toeic_test_import.json lên Firestore.
 *
 * LƯU Ý:
 * - File JSON phải là dữ liệu mà bạn có quyền sử dụng.
 * - Activity này KHÔNG tự sinh câu hỏi TOEIC.
 * - Collection:
 *      toeicTests
 *      toeicQuestions
 */
public class ToeicImportActivity extends AppCompatActivity {

    private static final String ASSET_FILE =
            "toeic_test_import.json";

    // Firestore batch tối đa 500 writes.
    // Dùng 400 để chừa khoảng an toàn.
    private static final int MAX_BATCH_WRITES =
            400;

    private MaterialButton btnImportToeic;
    private TextView tvImportStatus;

    private FirebaseFirestore firestore;
    private final Gson gson =
            new Gson();

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

        btnImportToeic =
                findViewById(
                        R.id.btnImportToeic
                );

        tvImportStatus =
                findViewById(
                        R.id.tvImportStatus
                );

        btnImportToeic.setOnClickListener(
                view -> importToeicFromAssets()
        );
    }

    private void importToeicFromAssets() {

        btnImportToeic.setEnabled(false);

        tvImportStatus.setText(
                "Đang đọc dữ liệu..."
        );

        try {

            String json =
                    readAssetFile(
                            ASSET_FILE
                    );

            JsonObject root =
                    JsonParser.parseString(
                            json
                    ).getAsJsonObject();

            if (!root.has("test")
                    || !root.has("questions")) {

                throw new IllegalArgumentException(
                        "JSON phải có 2 trường: test và questions."
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

            ToeicTest test =
                    gson.fromJson(
                            testObject,
                            ToeicTest.class
                    );

            String testId =
                    readRequiredString(
                            testObject,
                            "id"
                    );

            validateTest(
                    testId,
                    test
            );

            List<ToeicQuestion> questions =
                    new ArrayList<>();

            for (int i = 0;
                 i < questionArray.size();
                 i++) {

                JsonObject questionObject =
                        questionArray
                                .get(i)
                                .getAsJsonObject();

                ToeicQuestion question =
                        gson.fromJson(
                                questionObject,
                                ToeicQuestion.class
                        );

                String questionId;

                if (questionObject.has("id")
                        && !questionObject
                        .get("id")
                        .isJsonNull()) {

                    questionId =
                            questionObject
                                    .get("id")
                                    .getAsString()
                                    .trim();

                } else {

                    questionId =
                            testId
                                    + "_q"
                                    + String.format(
                                            java.util.Locale.ROOT,
                                            "%03d",
                                            question.getQuestionNumber()
                                    );
                }

                if (questionId.isEmpty()) {

                    throw new IllegalArgumentException(
                            "Câu thứ "
                                    + (i + 1)
                                    + " không có id hợp lệ."
                    );
                }

                question.setId(
                        questionId
                );

                // Luôn ép question thuộc đúng test đang import.
                question.setTestId(
                        testId
                );

                validateQuestion(
                        question,
                        i + 1
                );

                questions.add(
                        question
                );
            }

            if (questions.isEmpty()) {

                throw new IllegalArgumentException(
                        "Không có câu hỏi nào trong file JSON."
                );
            }

            // Đồng bộ metadata theo dữ liệu thực tế.
            test.setId(
                    testId
            );

            test.setTotalQuestions(
                    questions.size()
            );

            tvImportStatus.setText(
                    "Đã kiểm tra "
                            + questions.size()
                            + " câu. Đang tải lên Firestore..."
            );

            uploadTestAndQuestions(
                    testId,
                    test,
                    questions
            );

        } catch (Exception exception) {

            btnImportToeic.setEnabled(true);

            tvImportStatus.setText(
                    "Import thất bại:\n"
                            + exception.getMessage()
            );

            Toast.makeText(
                    this,
                    "Không thể import TOEIC.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void uploadTestAndQuestions(
            String testId,
            ToeicTest test,
            List<ToeicQuestion> questions
    ) {

        // Ghi metadata test trước.
        firestore
                .collection("toeicTests")
                .document(testId)
                .set(test)
                .addOnSuccessListener(
                        unused -> uploadQuestionBatches(
                                questions,
                                0
                        )
                )
                .addOnFailureListener(
                        exception -> {

                            btnImportToeic.setEnabled(
                                    true
                            );

                            tvImportStatus.setText(
                                    "Không ghi được toeicTests:\n"
                                            + exception.getMessage()
                            );
                        }
                );
    }

    private void uploadQuestionBatches(
            List<ToeicQuestion> questions,
            int startIndex
    ) {

        if (startIndex >= questions.size()) {

            btnImportToeic.setEnabled(
                    true
            );

            tvImportStatus.setText(
                    "✅ Import thành công "
                            + questions.size()
                            + " câu TOEIC."
            );

            Toast.makeText(
                    this,
                    "Đã import TOEIC thành công.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        int endIndex =
                Math.min(
                        startIndex
                                + MAX_BATCH_WRITES,
                        questions.size()
                );

        WriteBatch batch =
                firestore.batch();

        for (int i = startIndex;
             i < endIndex;
             i++) {

            ToeicQuestion question =
                    questions.get(i);

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

        final int nextIndex =
                endIndex;

        tvImportStatus.setText(
                "Đang tải câu "
                        + (startIndex + 1)
                        + " - "
                        + endIndex
                        + " / "
                        + questions.size()
        );

        batch.commit()
                .addOnSuccessListener(
                        unused ->
                                uploadQuestionBatches(
                                        questions,
                                        nextIndex
                                )
                )
                .addOnFailureListener(
                        exception -> {

                            btnImportToeic.setEnabled(
                                    true
                            );

                            tvImportStatus.setText(
                                    "Lỗi khi upload câu "
                                            + (startIndex + 1)
                                            + " - "
                                            + endIndex
                                            + ":\n"
                                            + exception.getMessage()
                            );
                        }
                );
    }

    private void validateTest(
            String testId,
            ToeicTest test
    ) {

        if (testId == null
                || testId.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "test.id không được để trống."
            );
        }

        if (test == null) {

            throw new IllegalArgumentException(
                    "Không đọc được metadata của bộ đề."
            );
        }

        if (test.getTitle() == null
                || test.getTitle()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "test.title không được để trống."
            );
        }
    }

    private void validateQuestion(
            ToeicQuestion question,
            int index
    ) {

        if (question.getQuestionNumber() <= 0) {

            throw new IllegalArgumentException(
                    "Câu "
                            + index
                            + ": questionNumber không hợp lệ."
            );
        }

        if (question.getPart() < 1
                || question.getPart() > 7) {

            throw new IllegalArgumentException(
                    "Câu "
                            + question.getQuestionNumber()
                            + ": part phải từ 1 đến 7."
            );
        }

        String correct =
                question.getCorrectAnswer();

        if (correct == null
                || correct.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Câu "
                            + question.getQuestionNumber()
                            + ": thiếu correctAnswer."
            );
        }

        String normalizedCorrect =
                correct.trim()
                        .toUpperCase(
                                java.util.Locale.ROOT
                        );

        if (!normalizedCorrect.equals("A")
                && !normalizedCorrect.equals("B")
                && !normalizedCorrect.equals("C")
                && !normalizedCorrect.equals("D")) {

            throw new IllegalArgumentException(
                    "Câu "
                            + question.getQuestionNumber()
                            + ": correctAnswer phải là A/B/C/D."
            );
        }

        question.setCorrectAnswer(
                normalizedCorrect
        );

        // Reading Part 5 bắt buộc có câu và 4 lựa chọn.
        if (question.getPart() == 5) {

            requireNonEmpty(
                    question.getQuestionText(),
                    "questionText",
                    question.getQuestionNumber()
            );

            requireNonEmpty(
                    question.getOptionA(),
                    "optionA",
                    question.getQuestionNumber()
            );

            requireNonEmpty(
                    question.getOptionB(),
                    "optionB",
                    question.getQuestionNumber()
            );

            requireNonEmpty(
                    question.getOptionC(),
                    "optionC",
                    question.getQuestionNumber()
            );

            requireNonEmpty(
                    question.getOptionD(),
                    "optionD",
                    question.getQuestionNumber()
            );
        }

        // Listening phải có audio URL nếu đang import dữ liệu để thi.
        if (question.getPart() >= 1
                && question.getPart() <= 4) {

            requireNonEmpty(
                    question.getAudioUrl(),
                    "audioUrl",
                    question.getQuestionNumber()
            );
        }

        // Part 1 cần hình ảnh.
        if (question.getPart() == 1) {

            requireNonEmpty(
                    question.getImageUrl(),
                    "imageUrl",
                    question.getQuestionNumber()
            );
        }
    }

    private void requireNonEmpty(
            String value,
            String fieldName,
            int questionNumber
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Câu "
                            + questionNumber
                            + ": thiếu "
                            + fieldName
                            + "."
            );
        }
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
                            + "."
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
                        getAssets().open(fileName);

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

                builder.append(line);
                builder.append('\n');
            }
        }

        return builder.toString();
    }
}
