package adu.nttu.englishai.activities;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import adu.nttu.englishai.R;

/**
 * Import từ vựng từ assets/vocabularies.json.
 *
 * Cách hoạt động giống TOEIC Import:
 * - Đọc JSON trong assets.
 * - Kiểm tra dữ liệu.
 * - Dùng document ID cố định từ field "id".
 * - Ghi theo nhiều WriteBatch nhỏ.
 * - Nếu ID đã tồn tại thì cập nhật/ghi đè, không tạo bản sao.
 */
public class ImportVocabularyActivity extends AppCompatActivity {

    private static final String ASSET_FILE =
            "vocabularies.json";

    /*
     * Firestore hỗ trợ tối đa 500 write / batch.
     * Dùng 400 để có khoảng an toàn.
     */
    private static final int MAX_BATCH_WRITES =
            400;

    private TextView btnBack;
    private TextView tvImportVocabularyStatus;
    private MaterialButton btnStartImport;

    private FirebaseFirestore firestore;

    private final Gson gson =
            new Gson();

    private final List<VocabularyImportItem> items =
            new ArrayList<>();

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_import_vocabulary
        );

        firestore =
                FirebaseFirestore.getInstance();

        btnBack =
                findViewById(
                        R.id.btnBack
                );

        tvImportVocabularyStatus =
                findViewById(
                        R.id.tvImportVocabularyStatus
                );

        btnStartImport =
                findViewById(
                        R.id.btnStartImport
                );

        btnBack.setOnClickListener(
                view -> finish()
        );

        btnStartImport.setOnClickListener(
                view -> importVocabularyFromAssets()
        );
    }

    private void importVocabularyFromAssets() {

        setImporting(
                true,
                "Đang đọc assets/"
                        + ASSET_FILE
                        + "..."
        );

        try {

            String json =
                    readAssetFile(
                            ASSET_FILE
                    );

            JsonElement root =
                    JsonParser.parseString(
                            json
                    );

            if (!root.isJsonArray()) {

                throw new IllegalArgumentException(
                        "vocabularies.json phải là một mảng JSON."
                );
            }

            JsonArray array =
                    root.getAsJsonArray();

            items.clear();

            for (JsonElement element
                    : array) {

                if (!element.isJsonObject()) {
                    continue;
                }

                VocabularyImportItem item =
                        parseVocabulary(
                                element.getAsJsonObject()
                        );

                if (item != null) {
                    items.add(item);
                }
            }

            if (items.isEmpty()) {

                throw new IllegalArgumentException(
                        "Không tìm thấy từ vựng hợp lệ trong JSON."
                );
            }

            tvImportVocabularyStatus.setText(
                    "Đã đọc "
                            + items.size()
                            + " từ hợp lệ.\n"
                            + "Đang tải lên Firestore..."
            );

            uploadBatch(
                    0
            );

        } catch (Exception exception) {

            showError(
                    "Import thất bại:\n"
                            + exception.getMessage()
            );
        }
    }

    @Nullable
    private VocabularyImportItem parseVocabulary(
            JsonObject object
    ) {

        String englishWord =
                readString(
                        object,
                        "englishWord"
                );

        String vietnameseMeaning =
                readString(
                        object,
                        "vietnameseMeaning"
                );

        if (englishWord.isEmpty()
                || vietnameseMeaning.isEmpty()) {

            return null;
        }

        String id =
                readString(
                        object,
                        "id"
                );

        if (id.isEmpty()) {

            id =
                    makeDocumentId(
                            englishWord
                    );
        }

        String pronunciation =
                firstNonEmpty(
                        readString(
                                object,
                                "pronunciation"
                        ),
                        readString(
                                object,
                                "phonetic"
                        )
                );

        String category =
                firstNonEmpty(
                        readString(
                                object,
                                "category"
                        ),
                        readString(
                                object,
                                "topic"
                        )
                );

        String difficulty =
                normalizeDifficulty(
                        firstNonEmpty(
                                readString(
                                        object,
                                        "difficulty"
                                ),
                                readString(
                                        object,
                                        "level"
                                )
                        )
                );

        String example =
                readString(
                        object,
                        "example"
                );

        Map<String, Object> values =
                new HashMap<>();

        /*
         * Field chuẩn đang dùng ở màn học từ vựng.
         */
        values.put(
                "id",
                id
        );

        values.put(
                "englishWord",
                englishWord
        );

        values.put(
                "vietnameseMeaning",
                vietnameseMeaning
        );

        values.put(
                "pronunciation",
                pronunciation
        );

        values.put(
                "example",
                example
        );

        values.put(
                "category",
                category
        );

        values.put(
                "difficulty",
                difficulty
        );

        /*
         * Alias để AdminVocabularyRepository hiện tại
         * vẫn đọc được dữ liệu cũ/ mới.
         */
        values.put(
                "phonetic",
                pronunciation
        );

        values.put(
                "topic",
                category
        );

        values.put(
                "level",
                difficulty
        );

        values.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        values.put(
                "importSource",
                ASSET_FILE
        );

        return new VocabularyImportItem(
                id,
                values
        );
    }

    private void uploadBatch(
            int startIndex
    ) {

        if (startIndex
                >= items.size()) {

            setImporting(
                    false,
                    "✅ Import thành công "
                            + items.size()
                            + " từ vựng.\n\n"
                            + "Các document có cùng ID đã được cập nhật, "
                            + "không tạo bản sao."
            );

            Toast.makeText(
                    this,
                    "Đã import "
                            + items.size()
                            + " từ vựng.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        int endIndex =
                Math.min(
                        startIndex
                                + MAX_BATCH_WRITES,
                        items.size()
                );

        WriteBatch batch =
                firestore.batch();

        for (int i = startIndex;
             i < endIndex;
             i++) {

            VocabularyImportItem item =
                    items.get(i);

            batch.set(
                    firestore
                            .collection(
                                    "vocabularies"
                            )
                            .document(
                                    item.id
                            ),
                    item.values
            );
        }

        tvImportVocabularyStatus.setText(
                "Đang tải từ "
                        + (startIndex + 1)
                        + " - "
                        + endIndex
                        + " / "
                        + items.size()
                        + "..."
        );

        final int nextIndex =
                endIndex;

        batch.commit()
                .addOnSuccessListener(
                        unused ->
                                uploadBatch(
                                        nextIndex
                                )
                )
                .addOnFailureListener(
                        exception ->
                                showError(
                                        "Lỗi upload từ "
                                                + (startIndex + 1)
                                                + " - "
                                                + endIndex
                                                + ":\n"
                                                + exception.getMessage()
                                )
                );
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

    private String readString(
            JsonObject object,
            String key
    ) {

        if (object == null
                || key == null
                || !object.has(key)
                || object.get(key).isJsonNull()) {

            return "";
        }

        try {

            return object
                    .get(key)
                    .getAsString()
                    .trim();

        } catch (Exception exception) {

            return "";
        }
    }

    private String firstNonEmpty(
            String first,
            String second
    ) {

        if (first != null
                && !first.trim().isEmpty()) {

            return first.trim();
        }

        return second == null
                ? ""
                : second.trim();
    }

    private String normalizeDifficulty(
            String value
    ) {

        if (value == null) {
            return "Easy";
        }

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (normalized.equals("medium")
                || normalized.equals("trung bình")
                || normalized.equals("trung binh")) {

            return "Medium";
        }

        if (normalized.equals("hard")
                || normalized.equals("khó")
                || normalized.equals("kho")) {

            return "Hard";
        }

        return "Easy";
    }

    private String makeDocumentId(
            String word
    ) {

        String result =
                word == null
                        ? ""
                        : word.trim()
                          .toLowerCase(
                                  Locale.ROOT
                          )
                          .replaceAll(
                                  "[^a-z0-9]+",
                                  "_"
                          )
                          .replaceAll(
                                  "^_+|_+$",
                                  ""
                          );

        if (result.isEmpty()) {

            result =
                    "word_"
                            + System.currentTimeMillis();
        }

        return result;
    }

    private void setImporting(
            boolean importing,
            String message
    ) {

        btnStartImport.setEnabled(
                !importing
        );

        btnBack.setEnabled(
                !importing
        );

        btnStartImport.setText(
                importing
                        ? "ĐANG IMPORT..."
                        : "IMPORT TỪ VỰNG"
        );

        tvImportVocabularyStatus.setText(
                message
        );
    }

    private void showError(
            String message
    ) {

        setImporting(
                false,
                message
        );

        Toast.makeText(
                this,
                "Không thể import từ vựng.",
                Toast.LENGTH_LONG
        ).show();
    }

    private static class VocabularyImportItem {

        final String id;
        final Map<String, Object> values;

        VocabularyImportItem(
                String id,
                Map<String, Object> values
        ) {
            this.id = id;
            this.values = values;
        }
    }
}