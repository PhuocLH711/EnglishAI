package adu.nttu.englishai.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VocabularySeeder {

    private static final String TAG = "VOCAB_SEEDER";

    public static void uploadVocabularyToFirestore(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {
            InputStream inputStream =
                    context.getAssets().open("vocabularies.json");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            inputStream,
                            StandardCharsets.UTF_8
                    )
            );

            StringBuilder jsonBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            reader.close();
            inputStream.close();

            JSONArray jsonArray =
                    new JSONArray(jsonBuilder.toString());

            int total = jsonArray.length();
            int[] uploadedCount = {0};
            int[] failedCount = {0};

            for (int i = 0; i < total; i++) {
                JSONObject object = jsonArray.getJSONObject(i);

                String id = object.optString("id").trim();
                String englishWord =
                        object.optString("englishWord").trim();

                if (id.isEmpty()) {
                    id = englishWord
                            .toLowerCase()
                            .replace(" ", "_")
                            .replace("/", "_");
                }

                if (id.isEmpty()) {
                    failedCount[0]++;
                    continue;
                }

                Map<String, Object> vocabulary = new HashMap<>();

                vocabulary.put(
                        "englishWord",
                        englishWord
                );

                vocabulary.put(
                        "vietnameseMeaning",
                        object.optString(
                                "vietnameseMeaning"
                        )
                );

                vocabulary.put(
                        "pronunciation",
                        object.optString(
                                "pronunciation"
                        )
                );

                vocabulary.put(
                        "example",
                        object.optString(
                                "example"
                        )
                );

                vocabulary.put(
                        "category",
                        object.optString(
                                "category"
                        )
                );

                vocabulary.put(
                        "difficulty",
                        object.optString(
                                "difficulty"
                        )
                );

                String documentId = id;

                db.collection("vocabularies")
                        .document(documentId)
                        .set(vocabulary)
                        .addOnSuccessListener(unused -> {
                            uploadedCount[0]++;

                            Log.d(
                                    TAG,
                                    "Đã upload: " + documentId
                            );

                            if (uploadedCount[0] + failedCount[0]
                                    == total) {

                                Toast.makeText(
                                        context,
                                        "Đã upload "
                                                + uploadedCount[0]
                                                + "/"
                                                + total
                                                + " từ vựng",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        })
                        .addOnFailureListener(error -> {
                            failedCount[0]++;

                            Log.e(
                                    TAG,
                                    "Lỗi upload: "
                                            + documentId,
                                    error
                            );

                            if (uploadedCount[0] + failedCount[0]
                                    == total) {

                                Toast.makeText(
                                        context,
                                        "Thành công: "
                                                + uploadedCount[0]
                                                + ", lỗi: "
                                                + failedCount[0],
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
            }

        } catch (Exception error) {
            Log.e(
                    TAG,
                    "Không đọc được vocabularies.json",
                    error
            );

            Toast.makeText(
                    context,
                    "Lỗi đọc JSON: " + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}