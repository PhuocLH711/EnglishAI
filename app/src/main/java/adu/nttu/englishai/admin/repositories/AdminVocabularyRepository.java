package adu.nttu.englishai.admin.repositories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository quản trị collection vocabularies.
 *
 * Dữ liệu hiện tại hỗ trợ các field:
 * - englishWord
 * - vietnameseMeaning
 * - phonetic
 * - pronunciation (fallback nếu dữ liệu cũ dùng field này)
 * - topic
 * - level
 * - createdAt
 */
public class AdminVocabularyRepository {

    private final FirebaseFirestore db;

    public AdminVocabularyRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static class AdminVocabulary {

        private String id;
        private String englishWord;
        private String vietnameseMeaning;
        private String phonetic;
        private String topic;
        private String level;

        public AdminVocabulary() {
        }

        public String getId() {
            return id == null ? "" : id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getEnglishWord() {
            return englishWord == null ? "" : englishWord;
        }

        public void setEnglishWord(String englishWord) {
            this.englishWord = englishWord;
        }

        public String getVietnameseMeaning() {
            return vietnameseMeaning == null ? "" : vietnameseMeaning;
        }

        public void setVietnameseMeaning(String vietnameseMeaning) {
            this.vietnameseMeaning = vietnameseMeaning;
        }

        public String getPhonetic() {
            return phonetic == null ? "" : phonetic;
        }

        public void setPhonetic(String phonetic) {
            this.phonetic = phonetic;
        }

        public String getTopic() {
            return topic == null ? "" : topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getLevel() {
            return level == null ? "" : level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    public interface VocabularyListCallback {
        void onSuccess(List<AdminVocabulary> vocabularies);
        void onFailure(Exception exception);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }

    public void getAllVocabulary(
            VocabularyListCallback callback
    ) {

        db.collection("vocabularies")
                .orderBy(
                        "englishWord",
                        Query.Direction.ASCENDING
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<AdminVocabulary> list =
                                    new ArrayList<>();

                            for (DocumentSnapshot document
                                    : snapshots.getDocuments()) {

                                AdminVocabulary vocabulary =
                                        new AdminVocabulary();

                                vocabulary.setId(
                                        document.getId()
                                );

                                vocabulary.setEnglishWord(
                                        firstNonEmpty(
                                                document.getString("englishWord"),
                                                document.getString("word"),
                                                document.getId()
                                        )
                                );

                                vocabulary.setVietnameseMeaning(
                                        firstNonEmpty(
                                                document.getString("vietnameseMeaning"),
                                                document.getString("meaning")
                                        )
                                );

                                vocabulary.setPhonetic(
                                        firstNonEmpty(
                                                document.getString("phonetic"),
                                                document.getString("pronunciation")
                                        )
                                );

                                vocabulary.setTopic(
                                        firstNonEmpty(
                                                document.getString("topic"),
                                                document.getString("category")
                                        )
                                );

                                vocabulary.setLevel(
                                        firstNonEmpty(
                                                document.getString("level"),
                                                document.getString("difficulty")
                                        )
                                );

                                list.add(vocabulary);
                            }

                            callback.onSuccess(list);
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void addVocabulary(
            AdminVocabulary vocabulary,
            ActionCallback callback
    ) {

        String word =
                vocabulary.getEnglishWord().trim();

        if (word.isEmpty()) {
            callback.onFailure(
                    new IllegalArgumentException(
                            "Từ tiếng Anh không được để trống."
                    )
            );
            return;
        }

        String documentId =
                normalizeDocumentId(word);

        Map<String, Object> data =
                buildVocabularyMap(vocabulary);

        db.collection("vocabularies")
                .document(documentId)
                .get()
                .addOnSuccessListener(
                        existing -> {

                            if (existing.exists()) {
                                callback.onFailure(
                                        new IllegalStateException(
                                                "Từ này đã tồn tại trong Firestore."
                                        )
                                );
                                return;
                            }

                            db.collection("vocabularies")
                                    .document(documentId)
                                    .set(data)
                                    .addOnSuccessListener(
                                            unused ->
                                                    callback.onSuccess()
                                    )
                                    .addOnFailureListener(
                                            callback::onFailure
                                    );
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void updateVocabulary(
            String originalId,
            AdminVocabulary vocabulary,
            ActionCallback callback
    ) {

        if (originalId == null
                || originalId.trim().isEmpty()) {

            callback.onFailure(
                    new IllegalArgumentException(
                            "Không xác định được document cần sửa."
                    )
            );
            return;
        }

        Map<String, Object> data =
                buildVocabularyMap(vocabulary);

        // Giữ nguyên document ID cũ để không làm đứt progress/favorites của user.
        db.collection("vocabularies")
                .document(originalId)
                .update(data)
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void deleteVocabulary(
            String documentId,
            ActionCallback callback
    ) {

        if (documentId == null
                || documentId.trim().isEmpty()) {

            callback.onFailure(
                    new IllegalArgumentException(
                            "documentId không hợp lệ."
                    )
            );
            return;
        }

        db.collection("vocabularies")
                .document(documentId)
                .delete()
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    private Map<String, Object> buildVocabularyMap(
            AdminVocabulary vocabulary
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "englishWord",
                vocabulary.getEnglishWord().trim()
        );

        data.put(
                "vietnameseMeaning",
                vocabulary.getVietnameseMeaning().trim()
        );

        data.put(
                "phonetic",
                vocabulary.getPhonetic().trim()
        );

        data.put(
                "topic",
                vocabulary.getTopic().trim()
        );

        data.put(
                "level",
                vocabulary.getLevel().trim()
        );

        return data;
    }

    private String normalizeDocumentId(
            String word
    ) {

        String normalized =
                word.trim()
                        .toLowerCase(java.util.Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isEmpty()) {
            normalized =
                    "word_"
                            + System.currentTimeMillis();
        }

        return normalized;
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
