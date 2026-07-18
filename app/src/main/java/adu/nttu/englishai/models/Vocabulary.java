package adu.nttu.englishai.models;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class Vocabulary implements Serializable {

    private String id;
    private String englishWord;
    private String vietnameseMeaning;
    private String pronunciation;
    private String wordType;
    private String example;
    private String exampleMeaning;
    private String category;
    private String level;
    private String imageUrl;

    // Dữ liệu riêng của từng người học
    private boolean favorite;
    private String learningStatus;

    public static final String STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String STATUS_LEARNING = "LEARNING";
    public static final String STATUS_LEARNED = "LEARNED";

    /**
     * Constructor rỗng bắt buộc để Firestore
     * chuyển dữ liệu thành đối tượng Vocabulary.
     */
    public Vocabulary() {
        this.favorite = false;
        this.learningStatus = STATUS_NOT_STARTED;
    }

    public Vocabulary(
            String id,
            String englishWord,
            String vietnameseMeaning,
            String pronunciation,
            String wordType,
            String example,
            String exampleMeaning,
            String category,
            String level,
            String imageUrl
    ) {
        this.id = id;
        this.englishWord = englishWord;
        this.vietnameseMeaning = vietnameseMeaning;
        this.pronunciation = pronunciation;
        this.wordType = wordType;
        this.example = example;
        this.exampleMeaning = exampleMeaning;
        this.category = category;
        this.level = level;
        this.imageUrl = imageUrl;

        this.favorite = false;
        this.learningStatus = STATUS_NOT_STARTED;
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

    public String getWordType() {
        return wordType;
    }

    public void setWordType(String wordType) {
        this.wordType = wordType;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getExampleMeaning() {
        return exampleMeaning;
    }

    public void setExampleMeaning(String exampleMeaning) {
        this.exampleMeaning = exampleMeaning;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Favorite không được lưu chung trong collection vocabularies.
     * Trường này sẽ được ghép từ tiến độ riêng của người dùng.
     */
    @Exclude
    public boolean isFavorite() {
        return favorite;
    }

    @Exclude
    public boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    /**
     * learningStatus không được lưu chung trong collection vocabularies.
     * Trường này thuộc tiến độ riêng của từng tài khoản.
     */
    @Exclude
    public String getLearningStatus() {
        if (learningStatus == null || learningStatus.trim().isEmpty()) {
            return STATUS_NOT_STARTED;
        }

        return learningStatus;
    }

    public void setLearningStatus(String learningStatus) {
        if (learningStatus == null || learningStatus.trim().isEmpty()) {
            this.learningStatus = STATUS_NOT_STARTED;
            return;
        }

        if (!STATUS_NOT_STARTED.equals(learningStatus)
                && !STATUS_LEARNING.equals(learningStatus)
                && !STATUS_LEARNED.equals(learningStatus)) {

            this.learningStatus = STATUS_NOT_STARTED;
            return;
        }

        this.learningStatus = learningStatus;
    }

    @Exclude
    public boolean isNotStarted() {
        return STATUS_NOT_STARTED.equals(getLearningStatus());
    }

    @Exclude
    public boolean isLearning() {
        return STATUS_LEARNING.equals(getLearningStatus());
    }

    @Exclude
    public boolean isLearned() {
        return STATUS_LEARNED.equals(getLearningStatus());
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

        this.wordType = "";
        this.exampleMeaning = "";
        this.imageUrl = "";
        this.favorite = false;
        this.learningStatus = STATUS_NOT_STARTED;
    }
}