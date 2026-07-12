package adu.nttu.englishai.models;

public class Vocabulary {
    private String learningStatus;
    private String id;
    private String englishWord;
    private String vietnameseMeaning;
    private String pronunciation;
    private String example;
    private String category;
    private String level;
    private boolean favorite;
    private boolean learned;

    // Constructor rỗng: cần cho Firebase Firestore
    public Vocabulary() {
    }

    public String getLearningStatus() {
        return learningStatus;
    }

    public void setLearningStatus(String learningStatus) {
        this.learningStatus = learningStatus;

    }
    public Vocabulary(
            String id,
            String englishWord,
            String vietnameseMeaning,
            String pronunciation,
            String example,
            String category,
            String level
    ) {
        this.id = id;
        this.englishWord = englishWord;
        this.vietnameseMeaning = vietnameseMeaning;
        this.pronunciation = pronunciation;
        this.example = example;
        this.category = category;
        this.level = level;

        this.favorite = false;
        this.learned = false;

        this.learningStatus = "NOT_STARTED";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnglishWord() {
        return englishWord;
    }

    public void setEnglishWord(String englishWord) {
        this.englishWord = englishWord;
    }

    public String getVietnameseMeaning() {
        return vietnameseMeaning;
    }

    public void setVietnameseMeaning(String vietnameseMeaning) {
        this.vietnameseMeaning = vietnameseMeaning;
    }

    public String getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isLearned() {
        return learned;
    }

    public void setLearned(boolean learned) {
        this.learned = learned;
    }
}