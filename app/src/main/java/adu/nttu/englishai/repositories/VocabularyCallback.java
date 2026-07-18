package adu.nttu.englishai.repositories;

import java.util.List;

import adu.nttu.englishai.models.Vocabulary;

public interface VocabularyCallback {

    void onSuccess(List<Vocabulary> vocabularies);

    void onFailure(Exception e);

}