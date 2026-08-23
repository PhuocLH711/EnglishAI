package adu.nttu.englishai.repositories;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import adu.nttu.englishai.models.SentenceRewriteExercise;

public class SentenceRewriteRepository {

    private final FirebaseFirestore db =
            FirebaseFirestore.getInstance();

    private final CollectionReference ref =
            db.collection("sentenceRewriteExercises");

    public interface ListCallback {
        void onSuccess(List<SentenceRewriteExercise> list);
        void onFailure(Exception e);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void getAll(ListCallback callback) {
        ref.get()
                .addOnSuccessListener(snapshot -> {
                    List<SentenceRewriteExercise> list =
                            new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        SentenceRewriteExercise item =
                                doc.toObject(SentenceRewriteExercise.class);

                        item.setId(doc.getId());
                        list.add(item);
                    }

                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getForGame(ListCallback callback) {
        getAll(new ListCallback() {
            @Override
            public void onSuccess(List<SentenceRewriteExercise> list) {
                Collections.shuffle(list);
                callback.onSuccess(list);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void add(
            SentenceRewriteExercise item,
            ActionCallback callback
    ) {
        ref.add(item)
                .addOnSuccessListener(documentReference ->
                        callback.onSuccess()
                )
                .addOnFailureListener(callback::onFailure);
    }

    public void update(
            String id,
            SentenceRewriteExercise item,
            ActionCallback callback
    ) {
        ref.document(id)
                .set(item)
                .addOnSuccessListener(unused ->
                        callback.onSuccess()
                )
                .addOnFailureListener(callback::onFailure);
    }

    public void delete(
            String id,
            ActionCallback callback
    ) {
        ref.document(id)
                .delete()
                .addOnSuccessListener(unused ->
                        callback.onSuccess()
                )
                .addOnFailureListener(callback::onFailure);
    }

    // =========================================================
    // IMPORT NHIỀU BÀI BẰNG 1 BATCH
    // =========================================================
    public void importMany(
            List<SentenceRewriteExercise> items,
            ActionCallback callback
    ) {
        if (items == null || items.isEmpty()) {
            callback.onSuccess();
            return;
        }

        WriteBatch batch = db.batch();

        for (SentenceRewriteExercise item : items) {
            batch.set(
                    ref.document(),
                    item
            );
        }

        batch.commit()
                .addOnSuccessListener(unused ->
                        callback.onSuccess()
                )
                .addOnFailureListener(callback::onFailure);
    }
}
