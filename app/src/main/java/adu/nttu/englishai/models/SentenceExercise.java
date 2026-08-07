package adu.nttu.englishai.models;

public class SentenceExercise {

    private String id;
    private String englishSentence;
    private String vietnameseMeaning;
    private String grammarTopic;
    private String level;
    private String explanation;

    public SentenceExercise() {
        // Constructor rỗng bắt buộc cho Firestore
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnglishSentence() {
        return englishSentence;
    }

    public void setEnglishSentence(String englishSentence) {
        this.englishSentence = englishSentence;
    }

    public String getVietnameseMeaning() {
        return vietnameseMeaning;
    }

    public void setVietnameseMeaning(String vietnameseMeaning) {
        this.vietnameseMeaning = vietnameseMeaning;
    }

    public String getGrammarTopic() {
        return grammarTopic;
    }

    public void setGrammarTopic(String grammarTopic) {
        this.grammarTopic = grammarTopic;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}