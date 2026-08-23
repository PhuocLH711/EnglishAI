package adu.nttu.englishai.admin.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.SentenceRewriteExercise;
import adu.nttu.englishai.repositories.SentenceRewriteRepository;

public class SentenceRewriteImportActivity
        extends AppCompatActivity {

    // =========================================================
    // FILE JSON TRONG ASSETS
    // =========================================================
    private static final String ASSET_FILE =
            "sentence_rewrite_import_sample.json";

    private final SentenceRewriteRepository repository =
            new SentenceRewriteRepository();

    private TextView tvImportStatus;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_sentence_rewrite_import
        );

        tvImportStatus =
                findViewById(
                        R.id.tvImportStatus
                );

        // QUAY LẠI
        findViewById(
                R.id.btnBack
        ).setOnClickListener(
                v -> finish()
        );

        // IMPORT
        findViewById(
                R.id.btnImportSentenceRewrite
        ).setOnClickListener(
                v -> importFromAssets()
        );
    }

    // =========================================================
    // ĐỌC FILE JSON TRONG ASSETS
    // =========================================================
    private void importFromAssets() {

        tvImportStatus.setText(
                "Đang đọc dữ liệu..."
        );

        try {

            String json =
                    readAssetFile(
                            ASSET_FILE
                    );

            Type type =
                    new TypeToken<
                            List<SentenceRewriteExercise>
                            >() {
                    }.getType();

            List<SentenceRewriteExercise> items =
                    new Gson().fromJson(
                            json,
                            type
                    );

            if (items == null
                    || items.isEmpty()) {

                tvImportStatus.setText(
                        "File JSON không có dữ liệu."
                );

                return;
            }

            // =================================================
            // CHUẨN HÓA DỮ LIỆU
            // =================================================
            for (SentenceRewriteExercise item
                    : items) {

                // Firestore tự tạo document ID
                item.setId(null);

                // Nếu JSON không ghi XP
                if (item.getXp() <= 0) {
                    item.setXp(10);
                }
            }

            tvImportStatus.setText(
                    "Đang import "
                            + items.size()
                            + " bài..."
            );

            // =================================================
            // GHI FIRESTORE
            // =================================================
            repository.importMany(
                    items,

                    new SentenceRewriteRepository
                            .ActionCallback() {

                        @Override
                        public void onSuccess() {

                            tvImportStatus.setText(
                                    "Import thành công "
                                            + items.size()
                                            + " bài Viết Lại Câu."
                            );

                            Toast.makeText(
                                    SentenceRewriteImportActivity.this,
                                    "Import thành công!",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        @Override
                        public void onFailure(
                                Exception e
                        ) {

                            tvImportStatus.setText(
                                    "Import thất bại: "
                                            + e.getMessage()
                            );

                            Toast.makeText(
                                    SentenceRewriteImportActivity.this,
                                    "Import lỗi: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

        } catch (Exception e) {

            tvImportStatus.setText(
                    "Không đọc được "
                            + ASSET_FILE
                            + ": "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // ĐỌC ASSET THÀNH STRING
    // =========================================================
    private String readAssetFile(
            String fileName
    ) throws Exception {

        StringBuilder builder =
                new StringBuilder();

        try (
                InputStream inputStream =
                        getAssets()
                                .open(fileName);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                builder
                        .append(line)
                        .append("\n");
            }
        }

        return builder.toString();
    }
}