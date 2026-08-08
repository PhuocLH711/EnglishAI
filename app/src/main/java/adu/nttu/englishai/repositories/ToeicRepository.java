package adu.nttu.englishai.repositories;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import adu.nttu.englishai.models.ToeicQuestion;
import adu.nttu.englishai.models.ToeicTest;

/**
 * Repository dữ liệu TOEIC dành cho người học.
 *
 * Dùng cùng schema với ToeicImportActivity:
 * - toeicTests
 * - toeicQuestions
 *
 * Không cần composite index vì dữ liệu được sort ở client.
 */
public class ToeicRepository {

    private final CollectionReference testsRef;
    private final CollectionReference questionsRef;

    public ToeicRepository() {

        FirebaseFirestore db =
                FirebaseFirestore.getInstance();

        testsRef =
                db.collection("toeicTests");

        questionsRef =
                db.collection("toeicQuestions");
    }

    public interface TestsCallback {
        void onSuccess(List<ToeicTest> tests);
        void onFailure(Exception exception);
    }

    public interface QuestionsCallback {
        void onSuccess(List<ToeicQuestion> questions);
        void onFailure(Exception exception);
    }

    public void getAllTests(
            TestsCallback callback
    ) {

        testsRef.get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<ToeicTest> list =
                                    new ArrayList<>();

                            for (var document
                                    : snapshots) {

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

                                list.add(test);
                            }

                            list.sort(
                                    Comparator.comparing(
                                            test ->
                                                    safe(
                                                            test.getTitle()
                                                    ),
                                            String.CASE_INSENSITIVE_ORDER
                                    )
                            );

                            callback.onSuccess(list);
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void getQuestionsByTestAndPart(
            String testId,
            int part,
            QuestionsCallback callback
    ) {

        questionsRef
                .whereEqualTo(
                        "testId",
                        testId
                )
                .whereEqualTo(
                        "part",
                        part
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<ToeicQuestion> list =
                                    mapQuestions(
                                            snapshots
                                                    .getDocuments()
                                    );

                            callback.onSuccess(list);
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void getFullTestQuestions(
            String testId,
            QuestionsCallback callback
    ) {

        questionsRef
                .whereEqualTo(
                        "testId",
                        testId
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<ToeicQuestion> list =
                                    mapQuestions(
                                            snapshots
                                                    .getDocuments()
                                    );

                            callback.onSuccess(list);
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    private List<ToeicQuestion> mapQuestions(
            List<com.google.firebase.firestore.DocumentSnapshot> documents
    ) {

        List<ToeicQuestion> list =
                new ArrayList<>();

        for (var document : documents) {

            ToeicQuestion question =
                    document.toObject(
                            ToeicQuestion.class
                    );

            if (question == null) {
                continue;
            }

            question.setId(
                    document.getId()
            );

            list.add(question);
        }

        list.sort(
                Comparator.comparingInt(
                        ToeicQuestion::getQuestionNumber
                )
        );

        return list;
    }

    private String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }
}