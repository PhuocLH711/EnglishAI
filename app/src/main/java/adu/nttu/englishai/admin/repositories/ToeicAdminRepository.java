package adu.nttu.englishai.admin.repositories;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToeicAdminRepository {

    private final FirebaseFirestore db;

    public ToeicAdminRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static class ToeicAdminTest {
        private String id;
        private String title;
        private String source;
        private String difficulty;
        private long year;
        private long durationMinutes;
        private long totalQuestions;
        private boolean published;

        public String getId() {
            return id == null ? "" : id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title == null ? "" : title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSource() {
            return source == null ? "" : source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDifficulty() {
            return difficulty == null ? "" : difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public long getYear() {
            return year;
        }

        public void setYear(long year) {
            this.year = year;
        }

        public long getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(long durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public long getTotalQuestions() {
            return totalQuestions;
        }

        public void setTotalQuestions(long totalQuestions) {
            this.totalQuestions = totalQuestions;
        }

        public boolean isPublished() {
            return published;
        }

        public void setPublished(boolean published) {
            this.published = published;
        }
    }

    public interface TestListCallback {
        void onSuccess(List<ToeicAdminTest> tests);
        void onFailure(Exception exception);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }

    public void getAllTests(
            TestListCallback callback
    ) {

        db.collection("toeicTests")
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<ToeicAdminTest> tests =
                                    new ArrayList<>();

                            for (DocumentSnapshot document
                                    : snapshots.getDocuments()) {

                                ToeicAdminTest test =
                                        fromDocument(document);

                                tests.add(test);
                            }

                            tests.sort(
                                    (a, b) ->
                                            a.getTitle()
                                                    .compareToIgnoreCase(
                                                            b.getTitle()
                                                    )
                            );

                            callback.onSuccess(tests);
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void createTest(
            ToeicAdminTest test,
            ActionCallback callback
    ) {

        String title =
                test.getTitle().trim();

        if (title.isEmpty()) {
            callback.onFailure(
                    new IllegalArgumentException(
                            "Tên đề không được để trống."
                    )
            );
            return;
        }

        Map<String, Object> data =
                toMap(test);

        data.put(
                "createdAt",
                Timestamp.now()
        );

        db.collection("toeicTests")
                .add(data)
                .addOnSuccessListener(
                        documentReference ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void updateTest(
            String id,
            ToeicAdminTest test,
            ActionCallback callback
    ) {

        if (id == null
                || id.trim().isEmpty()) {

            callback.onFailure(
                    new IllegalArgumentException(
                            "Không xác định được đề TOEIC."
                    )
            );
            return;
        }

        db.collection("toeicTests")
                .document(id)
                .update(
                        toMap(test)
                )
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void setPublished(
            String id,
            boolean published,
            ActionCallback callback
    ) {

        db.collection("toeicTests")
                .document(id)
                .update(
                        "published",
                        published
                )
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void deleteTest(
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

        db.collection("toeicTests")
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

    public void countQuestions(
            String testId,
            QuestionCountCallback callback
    ) {

        db.collection("toeicQuestions")
                .whereEqualTo(
                        "testId",
                        testId
                )
                .get()
                .addOnSuccessListener(
                        snapshots ->
                                callback.onSuccess(
                                        snapshots.size()
                                )
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public interface QuestionCountCallback {
        void onSuccess(int count);
        void onFailure(Exception exception);
    }

    private ToeicAdminTest fromDocument(
            DocumentSnapshot document
    ) {

        ToeicAdminTest test =
                new ToeicAdminTest();

        test.setId(
                document.getId()
        );

        test.setTitle(
                firstNonEmpty(
                        document.getString("title"),
                        document.getString("name"),
                        "TOEIC Test"
                )
        );

        test.setSource(
                firstNonEmpty(
                        document.getString("source")
                )
        );

        test.setDifficulty(
                firstNonEmpty(
                        document.getString("difficulty"),
                        "Mixed"
                )
        );

        Long year =
                document.getLong("year");

        Long duration =
                document.getLong("durationMinutes");

        Long totalQuestions =
                document.getLong("totalQuestions");

        Boolean published =
                document.getBoolean("published");

        test.setYear(
                year == null ? 0L : year
        );

        test.setDurationMinutes(
                duration == null ? 120L : duration
        );

        test.setTotalQuestions(
                totalQuestions == null ? 0L : totalQuestions
        );

        test.setPublished(
                published != null && published
        );

        return test;
    }

    private Map<String, Object> toMap(
            ToeicAdminTest test
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "title",
                test.getTitle().trim()
        );

        data.put(
                "source",
                test.getSource().trim()
        );

        data.put(
                "difficulty",
                test.getDifficulty().trim()
        );

        data.put(
                "year",
                test.getYear()
        );

        data.put(
                "durationMinutes",
                test.getDurationMinutes()
        );

        data.put(
                "totalQuestions",
                test.getTotalQuestions()
        );

        data.put(
                "published",
                test.isPublished()
        );

        return data;
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
