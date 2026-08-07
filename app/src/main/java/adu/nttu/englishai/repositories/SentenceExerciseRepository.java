package adu.nttu.englishai.repositories;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.models.SentenceExercise;

public class SentenceExerciseRepository {

    private final FirebaseFirestore db;

    public SentenceExerciseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface SentenceExerciseCallback {
        void onSuccess(List<SentenceExercise> exercises);
        void onFailure(Exception exception);
    }

    public void getAllExercises(
            SentenceExerciseCallback callback
    ) {

        db.collection("sentenceExercises")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<SentenceExercise> list =
                            new ArrayList<>();

                    snapshot.getDocuments().forEach(document -> {

                        SentenceExercise exercise =
                                document.toObject(
                                        SentenceExercise.class
                                );

                        if (exercise != null) {

                            exercise.setId(
                                    document.getId()
                            );

                            list.add(exercise);
                        }
                    });

                    callback.onSuccess(list);
                })
                .addOnFailureListener(
                        callback::onFailure
                );
    }
}