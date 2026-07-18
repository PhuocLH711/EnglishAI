package adu.nttu.englishai.repositories;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.models.Vocabulary;

public class VocabularyRepository {

    private final FirebaseFirestore db;

    private final CollectionReference vocabularyRef;

    public VocabularyRepository() {

        db = FirebaseFirestore.getInstance();

        vocabularyRef = db.collection("vocabularies");
    }

    public CollectionReference getVocabularyRef() {
        return vocabularyRef;
    }
    public void getAllVocabulary(VocabularyCallback callback){

        vocabularyRef
                .orderBy("englishWord")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Vocabulary> list = new ArrayList<>();

                    for(var document : queryDocumentSnapshots){

                        Vocabulary vocabulary =
                                document.toObject(Vocabulary.class);

                        vocabulary.setId(document.getId());

                        list.add(vocabulary);
                    }

                    callback.onSuccess(list);

                })
                .addOnFailureListener(callback::onFailure);

    }
}