package adu.nttu.englishai.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quản lý trạng thái của một lượt Grammar Sprint.
 *
 * Class này chỉ giữ state và xử lý thao tác trên state:
 * - Danh sách câu của lượt chơi
 * - Câu hiện tại
 * - Từ đã chọn / chưa chọn
 * - Vị trí đang chọn để swap
 *
 * Không chứa UI, Firestore, SharedPreferences hay animation.
 */
public class GrammarSession {

    private final List<SentenceExercise> exerciseList =
            new ArrayList<>();

    private final List<String> selectedWords =
            new ArrayList<>();

    private final List<String> availableWords =
            new ArrayList<>();

    private int currentQuestionIndex = 0;
    private int selectedSwapIndex = -1;

    /**
     * Tạo một lượt chơi mới từ kho câu.
     */
    public void startNewSession(
            List<SentenceExercise> source,
            int totalQuestions
    ) {

        exerciseList.clear();
        selectedWords.clear();
        availableWords.clear();

        currentQuestionIndex = 0;
        selectedSwapIndex = -1;

        if (source == null || source.isEmpty()) {
            return;
        }

        List<SentenceExercise> shuffled =
                new ArrayList<>(source);

        Collections.shuffle(shuffled);

        int limit =
                Math.min(
                        totalQuestions,
                        shuffled.size()
                );

        exerciseList.addAll(
                shuffled.subList(
                        0,
                        limit
                )
        );
    }

    /**
     * Khôi phục state đã lưu.
     */
    public void restore(
            List<SentenceExercise> savedExercises,
            int savedIndex,
            List<String> savedSelectedWords,
            List<String> savedAvailableWords
    ) {

        exerciseList.clear();

        if (savedExercises != null) {
            exerciseList.addAll(
                    savedExercises
            );
        }

        if (exerciseList.isEmpty()) {
            currentQuestionIndex = 0;
        } else {
            currentQuestionIndex =
                    Math.max(
                            0,
                            Math.min(
                                    savedIndex,
                                    exerciseList.size() - 1
                            )
                    );
        }

        selectedWords.clear();

        if (savedSelectedWords != null) {
            selectedWords.addAll(
                    savedSelectedWords
            );
        }

        availableWords.clear();

        if (savedAvailableWords != null) {
            availableWords.addAll(
                    savedAvailableWords
            );
        }

        selectedSwapIndex = -1;
    }

    /**
     * Chuẩn bị word bank cho câu mới.
     */
    public void prepareWords(
            String[] originalWords
    ) {

        selectedWords.clear();
        availableWords.clear();
        selectedSwapIndex = -1;

        if (originalWords == null
                || originalWords.length == 0) {

            return;
        }

        Collections.addAll(
                availableWords,
                originalWords
        );

        shuffleAvoidingOriginalOrder(
                originalWords
        );
    }

    private void shuffleAvoidingOriginalOrder(
            String[] originalWords
    ) {

        if (availableWords.size() <= 1) {
            return;
        }

        int attempts = 0;

        do {
            Collections.shuffle(
                    availableWords
            );

            attempts++;

        } while (
                isSameOrder(
                        availableWords,
                        originalWords
                )
                        && attempts < 10
        );
    }

    private boolean isSameOrder(
            List<String> shuffled,
            String[] original
    ) {

        if (shuffled.size()
                != original.length) {

            return false;
        }

        for (int i = 0;
             i < original.length;
             i++) {

            if (!shuffled.get(i)
                    .equals(original[i])) {

                return false;
            }
        }

        return true;
    }

    /**
     * Chọn một từ ở word bank và đưa vào cuối câu.
     */
    public void moveAvailableWordToSelected(
            int position
    ) {

        if (!isValidAvailablePosition(position)) {
            return;
        }

        String word =
                availableWords.remove(
                        position
                );

        selectedWords.add(
                word
        );
    }

    /**
     * Swap trực tiếp hai từ đang nằm trong câu.
     */
    public void swapSelectedWords(
            int firstPosition,
            int secondPosition
    ) {

        if (!isValidSelectedPosition(firstPosition)
                || !isValidSelectedPosition(secondPosition)) {

            return;
        }

        Collections.swap(
                selectedWords,
                firstPosition,
                secondPosition
        );
    }

    /**
     * Thay từ trong câu bằng một từ ở word bank.
     * Từ cũ quay lại đúng vị trí trong word bank.
     */
    public void replaceSelectedWithAvailable(
            int selectedPosition,
            int availablePosition
    ) {

        if (!isValidSelectedPosition(selectedPosition)
                || !isValidAvailablePosition(availablePosition)) {

            return;
        }

        String newWord =
                availableWords.get(
                        availablePosition
                );

        String oldWord =
                selectedWords.get(
                        selectedPosition
                );

        selectedWords.set(
                selectedPosition,
                newWord
        );

        availableWords.set(
                availablePosition,
                oldWord
        );
    }

    public void moveToNextQuestion() {

        currentQuestionIndex++;

        selectedWords.clear();
        availableWords.clear();
        selectedSwapIndex = -1;
    }

    public SentenceExercise getCurrentExercise() {

        if (currentQuestionIndex < 0
                || currentQuestionIndex
                >= exerciseList.size()) {

            return null;
        }

        return exerciseList.get(
                currentQuestionIndex
        );
    }

    public boolean isCompleted() {

        return !exerciseList.isEmpty()
                && currentQuestionIndex
                >= exerciseList.size();
    }

    public boolean isEmpty() {
        return exerciseList.isEmpty();
    }

    public int getTotalQuestions() {
        return exerciseList.size();
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public List<SentenceExercise> getExerciseList() {
        return exerciseList;
    }

    public List<String> getSelectedWords() {
        return selectedWords;
    }

    public List<String> getAvailableWords() {
        return availableWords;
    }

    public int getSelectedSwapIndex() {
        return selectedSwapIndex;
    }

    public void setSelectedSwapIndex(
            int selectedSwapIndex
    ) {
        this.selectedSwapIndex =
                selectedSwapIndex;
    }

    public void clearSelectedSwapIndex() {
        selectedSwapIndex = -1;
    }

    private boolean isValidSelectedPosition(
            int position
    ) {

        return position >= 0
                && position
                < selectedWords.size();
    }

    private boolean isValidAvailablePosition(
            int position
    ) {

        return position >= 0
                && position
                < availableWords.size();
    }
}