package adu.nttu.englishai.models;

import java.util.ArrayList;
import java.util.List;

public class DataRepository {

    private static DataRepository instance;
    private final List<Vocabulary> vocabularyList = new ArrayList<>();

    private DataRepository() {
        createSampleVocabulary();
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    public List<Vocabulary> getVocabularyList() {
        return vocabularyList;
    }

    private void createSampleVocabulary() {
        vocabularyList.clear();
        vocabularyList.add(new Vocabulary("1", "Apple", "Quả táo", "/ˈæp.əl/", "I eat an apple every day.", "Food", "Easy"));
        vocabularyList.add(new Vocabulary("2", "Banana", "Quả chuối", "/bəˈnɑː.nə/", "The banana is yellow.", "Food", "Easy"));
        vocabularyList.add(new Vocabulary("3", "Dog", "Con chó", "/dɒɡ/", "The dog is friendly.", "Animals", "Easy"));
        vocabularyList.add(new Vocabulary("4", "Cat", "Con mèo", "/kæt/", "The cat is sleeping.", "Animals", "Easy"));
        vocabularyList.add(new Vocabulary("5", "Teacher", "Giáo viên", "/ˈtiː.tʃər/", "My teacher is very kind.", "School", "Easy"));
        vocabularyList.add(new Vocabulary("6", "Student", "Học sinh", "/ˈstjuː.dənt/", "She is a good student.", "School", "Easy"));
        vocabularyList.add(new Vocabulary("7", "Airplane", "Máy bay", "/ˈeə.pleɪn/", "The airplane is flying.", "Travel", "Medium"));
        vocabularyList.add(new Vocabulary("8", "Beautiful", "Xinh đẹp", "/ˈbjuː.tɪ.fəl/", "The flower is beautiful.", "Adjectives", "Medium"));
        vocabularyList.add(new Vocabulary("9", "Hospital", "Bệnh viện", "/ˈhɒs.pɪ.təl/", "He works at a hospital.", "Places", "Medium"));
        vocabularyList.add(new Vocabulary("10", "Computer", "Máy tính", "/kəmˈpjuː.tər/", "I use a computer for studying.", "Technology", "Easy"));
    }
}