package adu.nttu.englishai.models;

/**
 * Thống kê của một lượt Grammar Sprint.
 *
 * Quy tắc điểm:
 * - Đúng ngay lần đầu: 10 điểm
 * - Sai 1 lần rồi đúng: 8 điểm
 * - Sai từ 2 lần trở lên rồi đúng: 5 điểm
 */
public class GrammarGameStats {

    private int score;
    private int combo;
    private int bestCombo;

    private int correctCount;
    private int wrongQuestionCount;
    private int currentQuestionMistakes;

    private long elapsedSeconds;

    public GrammarGameStats() {
        reset();
    }

    public void reset() {
        score = 0;
        combo = 0;
        bestCombo = 0;
        correctCount = 0;
        wrongQuestionCount = 0;
        currentQuestionMistakes = 0;
        elapsedSeconds = 0L;
    }

    public void registerWrongAttempt() {
        currentQuestionMistakes++;
        combo = 0;
    }

    /**
     * Ghi nhận câu vừa hoàn thành đúng và trả về số điểm vừa nhận.
     */
    public int registerCorrectAnswer() {

        int earnedPoints;

        if (currentQuestionMistakes == 0) {
            earnedPoints = 10;
        } else if (currentQuestionMistakes == 1) {
            earnedPoints = 8;
        } else {
            earnedPoints = 5;
        }

        score += earnedPoints;
        correctCount++;

        if (currentQuestionMistakes > 0) {
            wrongQuestionCount++;
        }

        combo++;

        if (combo > bestCombo) {
            bestCombo = combo;
        }

        currentQuestionMistakes = 0;

        return earnedPoints;
    }

    public void incrementElapsedSecond() {
        elapsedSeconds++;
    }

    public int getFirstTryCorrectCount() {
        return Math.max(
                0,
                correctCount - wrongQuestionCount
        );
    }

    public int getFirstTryAccuracyPercent(
            int totalQuestions
    ) {

        if (totalQuestions <= 0) {
            return 0;
        }

        return Math.round(
                getFirstTryCorrectCount()
                        * 100f
                        / totalQuestions
        );
    }

    public String getFormattedTime() {

        long minutes =
                elapsedSeconds / 60;

        long seconds =
                elapsedSeconds % 60;

        return String.format(
                java.util.Locale.ROOT,
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public int getCombo() {
        return combo;
    }

    public void setCombo(int combo) {
        this.combo = Math.max(0, combo);
    }

    public int getBestCombo() {
        return bestCombo;
    }

    public void setBestCombo(int bestCombo) {
        this.bestCombo = Math.max(0, bestCombo);
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = Math.max(0, correctCount);
    }

    public int getWrongQuestionCount() {
        return wrongQuestionCount;
    }

    public void setWrongQuestionCount(int wrongQuestionCount) {
        this.wrongQuestionCount = Math.max(0, wrongQuestionCount);
    }

    public int getCurrentQuestionMistakes() {
        return currentQuestionMistakes;
    }

    public void setCurrentQuestionMistakes(
            int currentQuestionMistakes
    ) {
        this.currentQuestionMistakes =
                Math.max(
                        0,
                        currentQuestionMistakes
                );
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(
            long elapsedSeconds
    ) {
        this.elapsedSeconds =
                Math.max(
                        0L,
                        elapsedSeconds
                );
    }
}