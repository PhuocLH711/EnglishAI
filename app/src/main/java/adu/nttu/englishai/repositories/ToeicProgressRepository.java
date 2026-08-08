package adu.nttu.englishai.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToeicProgressRepository {

    public static class TestProgress {

        private final Map<Integer, Integer> partPercent =
                new HashMap<>();

        private int overallPercent;

        public int getPartPercent(int part) {
            Integer value =
                    partPercent.get(part);

            return value == null
                    ? 0
                    : value;
        }

        public void setPartPercent(
                int part,
                int percent
        ) {
            partPercent.put(
                    part,
                    Math.max(
                            0,
                            Math.min(100, percent)
                    )
            );
        }

        public int getOverallPercent() {
            return overallPercent;
        }

        public void setOverallPercent(
                int overallPercent
        ) {
            this.overallPercent =
                    Math.max(
                            0,
                            Math.min(100, overallPercent)
                    );
        }

        public boolean isCompleted() {
            return overallPercent >= 100;
        }
    }

    public interface ProgressCallback {
        void onSuccess(TestProgress progress);
        void onFailure(Exception exception);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ToeicProgressRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void markAnswered(
            String testId,
            int part,
            String questionId,
            ActionCallback callback
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            if (callback != null) {
                callback.onFailure(
                        new IllegalStateException(
                                "Người dùng chưa đăng nhập."
                        )
                );
            }

            return;
        }

        if (testId == null
                || testId.trim().isEmpty()
                || questionId == null
                || questionId.trim().isEmpty()
                || part < 1
                || part > 7) {

            if (callback != null) {
                callback.onFailure(
                        new IllegalArgumentException(
                                "Dữ liệu tiến độ TOEIC không hợp lệ."
                        )
                );
            }

            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "testId",
                testId
        );

        updates.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        updates.put(
                "answeredPart" + part,
                FieldValue.arrayUnion(
                        questionId
                )
        );

        db.collection("users")
                .document(user.getUid())
                .collection("toeicProgress")
                .document(testId)
                .set(
                        updates,
                        com.google.firebase.firestore.SetOptions.merge()
                )
                .addOnSuccessListener(
                        unused -> {
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        }
                )
                .addOnFailureListener(
                        exception -> {
                            if (callback != null) {
                                callback.onFailure(exception);
                            }
                        }
                );
    }

    public void getProgress(
            String testId,
            ProgressCallback callback
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            callback.onFailure(
                    new IllegalStateException(
                            "Người dùng chưa đăng nhập."
                    )
            );

            return;
        }

        db.collection("toeicQuestions")
                .whereEqualTo(
                        "testId",
                        testId
                )
                .get()
                .addOnSuccessListener(
                        questionSnapshot -> {

                            int[] totals =
                                    new int[8];

                            for (DocumentSnapshot document
                                    : questionSnapshot.getDocuments()) {

                                Long partValue =
                                        document.getLong(
                                                "part"
                                        );

                                if (partValue == null) {
                                    continue;
                                }

                                int part =
                                        partValue.intValue();

                                if (part >= 1
                                        && part <= 7) {
                                    totals[part]++;
                                }
                            }

                            loadAnsweredData(
                                    user.getUid(),
                                    testId,
                                    totals,
                                    callback
                            );
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    private void loadAnsweredData(
            String uid,
            String testId,
            int[] totals,
            ProgressCallback callback
    ) {

        db.collection("users")
                .document(uid)
                .collection("toeicProgress")
                .document(testId)
                .get()
                .addOnSuccessListener(
                        document -> {

                            TestProgress progress =
                                    new TestProgress();

                            int totalQuestions = 0;
                            int totalAnswered = 0;

                            for (int part = 1;
                                 part <= 7;
                                 part++) {

                                int total =
                                        totals[part];

                                int answered =
                                        countAnswered(
                                                document,
                                                "answeredPart" + part
                                        );

                                if (answered > total
                                        && total > 0) {
                                    answered = total;
                                }

                                int percent =
                                        total <= 0
                                                ? 0
                                                : Math.round(
                                                answered
                                                        * 100f
                                                        / total
                                        );

                                progress.setPartPercent(
                                        part,
                                        percent
                                );

                                totalQuestions += total;
                                totalAnswered += answered;
                            }

                            int overall =
                                    totalQuestions <= 0
                                            ? 0
                                            : Math.round(
                                            totalAnswered
                                                    * 100f
                                                    / totalQuestions
                                    );

                            progress.setOverallPercent(
                                    overall
                            );

                            callback.onSuccess(
                                    progress
                            );
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    private int countAnswered(
            DocumentSnapshot document,
            String field
    ) {

        if (document == null
                || !document.exists()) {
            return 0;
        }

        Object value =
                document.get(field);

        if (!(value instanceof List<?>)) {
            return 0;
        }

        return ((List<?>) value).size();
    }
}
