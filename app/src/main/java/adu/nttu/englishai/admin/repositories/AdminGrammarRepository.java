package adu.nttu.englishai.admin.repositories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminGrammarRepository {

    private final FirebaseFirestore db;

    public AdminGrammarRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static class AdminGrammarExercise {

        private String id;
        private String vietnameseSentence;
        private String correctSentence;
        private String topic;
        private String level;
        private String explanation;

        private Map<String, Object> rawData =
                new HashMap<>();

        public String getId() {
            return id == null ? "" : id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVietnameseSentence() {
            return safe(vietnameseSentence);
        }

        public void setVietnameseSentence(String vietnameseSentence) {
            this.vietnameseSentence = vietnameseSentence;
        }

        public String getCorrectSentence() {
            return safe(correctSentence);
        }

        public void setCorrectSentence(String correctSentence) {
            this.correctSentence = correctSentence;
        }

        public String getTopic() {
            return safe(topic);
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getLevel() {
            return safe(level);
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getExplanation() {
            return safe(explanation);
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public Map<String, Object> getRawData() {
            return rawData;
        }

        public void setRawData(Map<String, Object> rawData) {
            this.rawData =
                    rawData == null
                            ? new HashMap<>()
                            : new HashMap<>(rawData);
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    public interface ListCallback {
        void onSuccess(List<AdminGrammarExercise> exercises);
        void onFailure(Exception exception);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }

    public void getAllExercises(
            ListCallback callback
    ) {

        // Không dùng orderBy("createdAt") vì dữ liệu hiện tại
        // sentence_001 ... sentence_100 không có field createdAt.
        // Firestore sẽ trả về danh sách rỗng chứ không báo lỗi.
        db.collection("sentenceExercises")
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<AdminGrammarExercise> list =
                                    new ArrayList<>();

                            for (DocumentSnapshot document
                                    : snapshots.getDocuments()) {

                                list.add(
                                        fromDocument(document)
                                );
                            }

                            // Sắp xếp theo document id:
                            // sentence_001 -> sentence_100
                            list.sort(
                                    (first, second) ->
                                            first.getId().compareTo(
                                                    second.getId()
                                            )
                            );

                            callback.onSuccess(list);
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void updateExercise(
            AdminGrammarExercise original,
            AdminGrammarExercise edited,
            ActionCallback callback
    ) {

        if (original == null
                || original.getId().trim().isEmpty()) {

            callback.onFailure(
                    new IllegalArgumentException(
                            "Không xác định được bài tập cần sửa."
                    )
            );
            return;
        }

        Map<String, Object> raw =
                original.getRawData();

        Map<String, Object> updates =
                new HashMap<>();

        // QUAN TRỌNG: hỗ trợ đúng field JSON hiện tại là vietnameseMeaning.
        updates.put(
                resolveExistingKey(
                        raw,
                        "vietnameseMeaning",
                        "vietnameseSentence",
                        "vietnamese",
                        "translation"
                ),
                edited.getVietnameseSentence().trim()
        );

        updates.put(
                resolveExistingKey(
                        raw,
                        "englishSentence",
                        "correctSentence",
                        "answer",
                        "sentence"
                ),
                edited.getCorrectSentence().trim()
        );

        updates.put(
                resolveExistingKey(
                        raw,
                        "grammarTopic",
                        "topic"
                ),
                edited.getTopic().trim()
        );

        updates.put(
                resolveExistingKey(
                        raw,
                        "level",
                        "grammarLevel",
                        "difficulty"
                ),
                edited.getLevel().trim()
        );

        updates.put(
                resolveExistingKey(
                        raw,
                        "explanation",
                        "note"
                ),
                edited.getExplanation().trim()
        );

        db.collection("sentenceExercises")
                .document(original.getId())
                .update(updates)
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void deleteExercise(
            String id,
            ActionCallback callback
    ) {

        if (id == null
                || id.trim().isEmpty()) {

            callback.onFailure(
                    new IllegalArgumentException(
                            "Document ID không hợp lệ."
                    )
            );
            return;
        }

        db.collection("sentenceExercises")
                .document(id)
                .delete()
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    private AdminGrammarExercise fromDocument(
            DocumentSnapshot document
    ) {

        Map<String, Object> raw =
                document.getData();

        AdminGrammarExercise item =
                new AdminGrammarExercise();

        item.setId(
                document.getId()
        );

        item.setRawData(
                raw
        );

        // JSON hiện tại dùng vietnameseMeaning.
        item.setVietnameseSentence(
                firstNonEmpty(
                        getString(raw, "vietnameseMeaning"),
                        getString(raw, "vietnameseSentence"),
                        getString(raw, "vietnamese"),
                        getString(raw, "translation")
                )
        );

        // JSON hiện tại dùng englishSentence.
        item.setCorrectSentence(
                firstNonEmpty(
                        getString(raw, "englishSentence"),
                        getString(raw, "correctSentence"),
                        getString(raw, "answer"),
                        getString(raw, "sentence")
                )
        );

        item.setTopic(
                firstNonEmpty(
                        getString(raw, "grammarTopic"),
                        getString(raw, "topic")
                )
        );

        item.setLevel(
                firstNonEmpty(
                        getString(raw, "level"),
                        getString(raw, "grammarLevel"),
                        getString(raw, "difficulty")
                )
        );

        item.setExplanation(
                firstNonEmpty(
                        getString(raw, "explanation"),
                        getString(raw, "note")
                )
        );

        return item;
    }

    private String getString(
            Map<String, Object> raw,
            String key
    ) {

        if (raw == null
                || !raw.containsKey(key)) {
            return "";
        }

        Object value =
                raw.get(key);

        return value == null
                ? ""
                : String.valueOf(value);
    }

    private String resolveExistingKey(
            Map<String, Object> raw,
            String... keys
    ) {

        if (raw != null) {

            for (String key : keys) {

                if (raw.containsKey(key)) {
                    return key;
                }
            }
        }

        return keys[0];
    }

    private String firstNonEmpty(
            String... values
    ) {

        if (values == null) {
            return "";
        }

        for (String value : values) {

            if (value != null
                    && !value.trim().isEmpty()) {

                return value.trim();
            }
        }

        return "";
    }
}