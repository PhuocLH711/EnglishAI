package adu.nttu.englishai.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

import adu.nttu.englishai.R;

public class ImportVocabularyActivity extends AppCompatActivity {

    private EditText etBulkData;
    private MaterialButton btnStartImport;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_vocabulary);

        firestore = FirebaseFirestore.getInstance();
        etBulkData = findViewById(R.id.etBulkData);
        btnStartImport = findViewById(R.id.btnStartImport);

        btnStartImport.setOnClickListener(v -> processAndUploadData());
        // Khai báo sự kiện bấm nút Quay lại (Back)
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // Lệnh finish() giúp đóng màn hình này lại
    }

    private void processAndUploadData() {
        String rawData = etBulkData.getText().toString().trim();

        if (rawData.isEmpty()) {
            Toast.makeText(this, "Vui lòng dán dữ liệu vào ô trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Đổi trạng thái nút bấm tránh bấm 2 lần
        btnStartImport.setEnabled(false);
        btnStartImport.setText("ĐANG XỬ LÝ...");

        // Tách văn bản thành từng dòng
        String[] lines = rawData.split("\n");

        // Khởi tạo Firebase WriteBatch (cho phép nạp tối đa 500 document cùng lúc)
        WriteBatch batch = firestore.batch();
        int validCount = 0;

        for (String line : lines) {
            // Tách các cột bằng ký tự "|"
            String[] parts = line.split("\\|");

            // Đảm bảo có ít nhất Từ tiếng Anh và Nghĩa tiếng Việt
            if (parts.length >= 2) {
                String englishWord = parts[0].trim();
                String phonetic = parts.length > 2 ? parts[1].trim() : "";
                String vietnameseMeaning = parts[parts.length > 2 ? 2 : 1].trim();

                if (!englishWord.isEmpty() && !vietnameseMeaning.isEmpty()) {
                    // Tạo ID tự động và đóng gói dữ liệu
                    DocumentReference docRef = firestore.collection("vocabularies").document();
                    Map<String, Object> wordMap = new HashMap<>();
                    wordMap.put("englishWord", englishWord);
                    wordMap.put("phonetic", phonetic);
                    wordMap.put("vietnameseMeaning", vietnameseMeaning);
                    wordMap.put("createdAt", System.currentTimeMillis());

                    // Thêm vào hàng đợi Batch
                    batch.set(docRef, wordMap);
                    validCount++;
                }
            }
        }

        if (validCount == 0) {
            Toast.makeText(this, "Không tìm thấy dữ liệu hợp lệ. Vui lòng kiểm tra lại định dạng!", Toast.LENGTH_LONG).show();
            btnStartImport.setEnabled(true);
            btnStartImport.setText("BẮT ĐẦU NẠP DỮ LIỆU");
            return;
        }

        // Bắt đầu đẩy toàn bộ lô dữ liệu lên server
        final int finalValidCount = validCount;
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã nạp thành công " + finalValidCount + " từ vựng!", Toast.LENGTH_LONG).show();
                    etBulkData.setText(""); // Xóa trắng ô nhập
                    btnStartImport.setEnabled(true);
                    btnStartImport.setText("BẮT ĐẦU NẠP DỮ LIỆU");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi nạp dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnStartImport.setEnabled(true);
                    btnStartImport.setText("BẮT ĐẦU NẠP DỮ LIỆU");
                });
    }
}