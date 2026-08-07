package adu.nttu.englishai.models;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model dùng chung cho câu hỏi TOEIC Part 1 - Part 7.
 *
 * Không tự sinh nội dung câu hỏi.
 * Dữ liệu phải được import từ nguồn hợp lệ vào Firestore.
 */
public class ToeicQuestion implements Serializable {

    private String id;

    private String testId;
    private int part;
    private int questionNumber;

    private String questionText;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String correctAnswer;
    private String explanation;

    private String grammarTopic;
    private List<String> vocabulary;
    private String difficulty;

    // Listening / visual content
    private String imageUrl;
    private String audioUrl;

    // Reading Part 6 / 7
    private String passageId;
    private String passageText;

    // Có thể dùng thêm cho email / notice / article metadata
    private Map<String, Object> extraData;

    // Runtime-only state
    private String selectedAnswer;
    private boolean bookmarked;

    public ToeicQuestion() {
        vocabulary = new ArrayList<>();
        selectedAnswer = "";
        bookmarked = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public int getPart() {
        return part;
    }

    public void setPart(int part) {
        this.part = part;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getGrammarTopic() {
        return grammarTopic;
    }

    public void setGrammarTopic(String grammarTopic) {
        this.grammarTopic = grammarTopic;
    }

    public List<String> getVocabulary() {
        if (vocabulary == null) {
            vocabulary = new ArrayList<>();
        }
        return vocabulary;
    }

    public void setVocabulary(List<String> vocabulary) {
        this.vocabulary =
                vocabulary == null
                        ? new ArrayList<>()
                        : vocabulary;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getPassageId() {
        return passageId;
    }

    public void setPassageId(String passageId) {
        this.passageId = passageId;
    }

    public String getPassageText() {
        return passageText;
    }

    public void setPassageText(String passageText) {
        this.passageText = passageText;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }

    @Exclude
    public String getSelectedAnswer() {
        return selectedAnswer == null ? "" : selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer =
                selectedAnswer == null
                        ? ""
                        : selectedAnswer;
    }

    @Exclude
    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    @Exclude
    public boolean isAnswered() {
        return !getSelectedAnswer().trim().isEmpty();
    }

    @Exclude
    public boolean isCorrect() {
        if (correctAnswer == null) {
            return false;
        }

        return correctAnswer.trim()
                .equalsIgnoreCase(
                        getSelectedAnswer().trim()
                );
    }
}