package adu.nttu.englishai.models;

public class VocabularyProgress {

    private String vocabularyId;
    private boolean favorite;
    private String learningStatus;
    private int reviewCount;
    private int correctCount;
    private long lastStudiedAt;

    public VocabularyProgress() {
        favorite = false;
        learningStatus = Vocabulary.STATUS_NOT_STARTED;
        reviewCount = 0;
        correctCount = 0;
        lastStudiedAt = 0;
    }

    public VocabularyProgress(String vocabularyId) {
        this();
        this.vocabularyId = vocabularyId;
    }

    public String getVocabularyId() {
        return vocabularyId;
    }

    public void setVocabularyId(String vocabularyId) {
        this.vocabularyId = vocabularyId;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getLearningStatus() {
        return learningStatus;
    }

    public void setLearningStatus(String learningStatus) {
        this.learningStatus = learningStatus;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public long getLastStudiedAt() {
        return lastStudiedAt;
    }

    public void setLastStudiedAt(long lastStudiedAt) {
        this.lastStudiedAt = lastStudiedAt;
    }
}