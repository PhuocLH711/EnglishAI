package adu.nttu.englishai.repositories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.models.ToeicTest;

/**
 * Repository dành riêng cho màn quản trị TOEIC.
 *
 * Chức năng:
 * - Lấy danh sách bộ đề.
 * - Lấy thống kê số bộ đề / số câu.
 * - Xóa một bộ đề và toàn bộ câu hỏi thuộc bộ đề đó.
 */
public class ToeicAdminRepository {

    private static final int DELETE_BATCH_SIZE = 400;

    private final FirebaseFirestore db;

    public ToeicAdminRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface TestListCallback {
        void onSuccess(List<ToeicTest> tests);
        void onFailure(Exception exception);
    }

    public interface StatsCallback {
        void onSuccess(int totalTests, int totalQuestions);
        void onFailure(Exception exception);
    }

    public interface DeleteCallback {
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

                            List<ToeicTest> tests =
                                    new ArrayList<>();

                            for (DocumentSnapshot document
                                    : snapshots.getDocuments()) {

                                ToeicTest test =
                                        document.toObject(
                                                ToeicTest.class
                                        );

                                if (test == null) {
                                    continue;
                                }

                                test.setId(
                                        document.getId()
                                );

                                tests.add(test);
                            }

                            callback.onSuccess(
                                    tests
                            );
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void getStats(
            StatsCallback callback
    ) {

        db.collection("toeicTests")
                .get()
                .addOnSuccessListener(
                        testSnapshots ->

                                db.collection("toeicQuestions")
                                        .get()
                                        .addOnSuccessListener(
                                                questionSnapshots ->
                                                        callback.onSuccess(
                                                                testSnapshots.size(),
                                                                questionSnapshots.size()
                                                        )
                                        )
                                        .addOnFailureListener(
                                                callback::onFailure
                                        )
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void deleteTestWithQuestions(
            String testId,
            DeleteCallback callback
    ) {

        if (testId == null
                || testId.trim().isEmpty()) {

            callback.onFailure(
                    new IllegalArgumentException(
                            "testId không hợp lệ."
                    )
            );

            return;
        }

        deleteQuestionBatch(
                testId,
                callback
        );
    }

    private void deleteQuestionBatch(
            String testId,
            DeleteCallback callback
    ) {

        db.collection("toeicQuestions")
                .whereEqualTo(
                        "testId",
                        testId
                )
                .limit(
                        DELETE_BATCH_SIZE
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            if (snapshots.isEmpty()) {

                                deleteTestDocument(
                                        testId,
                                        callback
                                );

                                return;
                            }

                            WriteBatch batch =
                                    db.batch();

                            for (DocumentSnapshot document
                                    : snapshots.getDocuments()) {

                                batch.delete(
                                        document.getReference()
                                );
                            }

                            batch.commit()
                                    .addOnSuccessListener(
                                            unused ->
                                                    deleteQuestionBatch(
                                                            testId,
                                                            callback
                                                    )
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

    private void deleteTestDocument(
            String testId,
            DeleteCallback callback
    ) {

        db.collection("toeicTests")
                .document(testId)
                .delete()
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }
}
