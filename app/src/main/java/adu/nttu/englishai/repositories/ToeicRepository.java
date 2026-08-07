package adu.nttu.englishai.repositories;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.models.ToeicQuestion;
import adu.nttu.englishai.models.ToeicTest;

/**
 * Repository duy nhất đọc dữ liệu TOEIC từ Firestore.
 *
 * Collections:
 * - toeicTests
 * - toeicQuestions
 */
public class ToeicRepository {

    private final FirebaseFirestore db;
    private final CollectionReference testsRef;
    private final CollectionReference questionsRef;

    public ToeicRepository() {

        db = FirebaseFirestore.getInstance();

        testsRef =
                db.collection("toeicTests");

        questionsRef =
                db.collection("toeicQuestions");
    }

    public interface TestsCallback {

        void onSuccess(
                List<ToeicTest> tests
        );

        void onFailure(
                Exception exception
        );
    }

    public interface QuestionsCallback {

        void onSuccess(
                List<ToeicQuestion> questions
        );

        void onFailure(
                Exception exception
        );
    }

    public void getAllTests(
            TestsCallback callback
    ) {

        testsRef
                .get()
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

                                test.setId(
                                        document.getId()
                                );

                                list.add(test);
                            }

                            callback.onSuccess(
                                    list
                            );
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
                .orderBy(
                        "questionNumber",
                        Query.Direction.ASCENDING
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<ToeicQuestion> list =
                                    new ArrayList<>();

                            for (var document
                                    : snapshots) {

                                ToeicQuestion question =
                                        document.toObject(
                                                ToeicQuestion.class
                                        );

                                question.setId(
                                        document.getId()
                                );

                                list.add(question);
                            }

                            callback.onSuccess(
                                    list
                            );
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void getPart5Questions(
            String testId,
            QuestionsCallback callback
    ) {

        getQuestionsByTestAndPart(
                testId,
                5,
                callback
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
                .orderBy(
                        "questionNumber",
                        Query.Direction.ASCENDING
                )
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<ToeicQuestion> list =
                                    new ArrayList<>();

                            for (var document
                                    : snapshots) {

                                ToeicQuestion question =
                                        document.toObject(
                                                ToeicQuestion.class
                                        );

                                question.setId(
                                        document.getId()
                                );

                                list.add(question);
                            }

                            callback.onSuccess(
                                    list
                            );
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }
}
