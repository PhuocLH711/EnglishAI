package adu.nttu.englishai.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.models.SentenceExercise;
import adu.nttu.englishai.models.GrammarGameStats;

/**
 * Quản lý tiến trình cục bộ của Grammar Sprint bằng SharedPreferences.
 *
 * Class này chỉ chịu trách nhiệm lưu / đọc / xóa dữ liệu tiến trình.
 * Không chứa UI, Firestore hay logic sắp xếp từ.
 */
public class GrammarProgressManager {

    private static final String PREFS_NAME =
            "grammar_sprint_progress";

    private static final String KEY_IN_PROGRESS =
            "in_progress";

    private static final String KEY_CURRENT_INDEX =
            "current_index";

    private static final String KEY_QUESTION_IDS =
            "question_ids";

    private static final String KEY_SELECTED_WORDS =
            "selected_words";

    private static final String KEY_AVAILABLE_WORDS =
            "available_words";

    private static final String KEY_SCORE =
            "score";

    private static final String KEY_COMBO =
            "combo";

    private static final String KEY_BEST_COMBO =
            "best_combo";

    private static final String KEY_CORRECT_COUNT =
            "correct_count";

    private static final String KEY_WRONG_QUESTION_COUNT =
            "wrong_question_count";

    private static final String KEY_CURRENT_MISTAKES =
            "current_mistakes";

    private static final String KEY_ELAPSED_SECONDS =
            "elapsed_seconds";

    private final SharedPreferences preferences;
    private final Gson gson;

    public GrammarProgressManager(Context context) {
        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        gson = new Gson();
    }

    public boolean hasSavedProgress() {
        return preferences.getBoolean(
                KEY_IN_PROGRESS,
                false
        );
    }

    public int getCurrentIndex() {
        return preferences.getInt(
                KEY_CURRENT_INDEX,
                0
        );
    }

    public void saveProgress(
            List<SentenceExercise> exerciseList,
            int currentQuestionIndex,
            List<String> selectedWords,
            List<String> availableWords,
            GrammarGameStats gameStats
    ) {

        if (exerciseList == null
                || exerciseList.isEmpty()
                || currentQuestionIndex < 0
                || currentQuestionIndex >= exerciseList.size()) {

            return;
        }

        List<String> questionIds =
                new ArrayList<>();

        for (SentenceExercise exercise : exerciseList) {

            if (exercise == null
                    || exercise.getId() == null
                    || exercise.getId().trim().isEmpty()) {

                continue;
            }

            questionIds.add(
                    exercise.getId()
            );
        }

        if (questionIds.isEmpty()) {
            return;
        }

        preferences.edit()
                .putBoolean(
                        KEY_IN_PROGRESS,
                        true
                )
                .putInt(
                        KEY_CURRENT_INDEX,
                        currentQuestionIndex
                )
                .putString(
                        KEY_QUESTION_IDS,
                        gson.toJson(questionIds)
                )
                .putString(
                        KEY_SELECTED_WORDS,
                        gson.toJson(
                                selectedWords == null
                                        ? new ArrayList<>()
                                        : selectedWords
                        )
                )
                .putString(
                        KEY_AVAILABLE_WORDS,
                        gson.toJson(
                                availableWords == null
                                        ? new ArrayList<>()
                                        : availableWords
                        )
                )
                .putInt(
                        KEY_SCORE,
                        gameStats == null
                                ? 0
                                : gameStats.getScore()
                )
                .putInt(
                        KEY_COMBO,
                        gameStats == null
                                ? 0
                                : gameStats.getCombo()
                )
                .putInt(
                        KEY_BEST_COMBO,
                        gameStats == null
                                ? 0
                                : gameStats.getBestCombo()
                )
                .putInt(
                        KEY_CORRECT_COUNT,
                        gameStats == null
                                ? 0
                                : gameStats.getCorrectCount()
                )
                .putInt(
                        KEY_WRONG_QUESTION_COUNT,
                        gameStats == null
                                ? 0
                                : gameStats.getWrongQuestionCount()
                )
                .putInt(
                        KEY_CURRENT_MISTAKES,
                        gameStats == null
                                ? 0
                                : gameStats.getCurrentQuestionMistakes()
                )
                .putLong(
                        KEY_ELAPSED_SECONDS,
                        gameStats == null
                                ? 0L
                                : gameStats.getElapsedSeconds()
                )
                .apply();
    }

    public SavedProgress getSavedProgress() {

        SavedProgress progress =
                new SavedProgress();

        progress.currentQuestionIndex =
                getCurrentIndex();

        progress.questionIds =
                readStringList(
                        KEY_QUESTION_IDS
                );

        progress.selectedWords =
                readStringList(
                        KEY_SELECTED_WORDS
                );

        progress.availableWords =
                readStringList(
                        KEY_AVAILABLE_WORDS
                );

        progress.score =
                preferences.getInt(
                        KEY_SCORE,
                        0
                );

        progress.combo =
                preferences.getInt(
                        KEY_COMBO,
                        0
                );

        progress.bestCombo =
                preferences.getInt(
                        KEY_BEST_COMBO,
                        0
                );

        progress.correctCount =
                preferences.getInt(
                        KEY_CORRECT_COUNT,
                        0
                );

        progress.wrongQuestionCount =
                preferences.getInt(
                        KEY_WRONG_QUESTION_COUNT,
                        0
                );

        progress.currentMistakes =
                preferences.getInt(
                        KEY_CURRENT_MISTAKES,
                        0
                );

        progress.elapsedSeconds =
                preferences.getLong(
                        KEY_ELAPSED_SECONDS,
                        0L
                );

        return progress;
    }

    public void clearProgress() {
        preferences.edit()
                .clear()
                .apply();
    }

    private List<String> readStringList(
            String key
    ) {

        String json =
                preferences.getString(
                        key,
                        ""
                );

        if (json == null
                || json.trim().isEmpty()) {

            return new ArrayList<>();
        }

        try {

            Type type =
                    new TypeToken<List<String>>() {
                    }.getType();

            List<String> result =
                    gson.fromJson(
                            json,
                            type
                    );

            if (result == null) {
                return new ArrayList<>();
            }

            return new ArrayList<>(
                    result
            );

        } catch (Exception exception) {

            return new ArrayList<>();
        }
    }

    public static class SavedProgress {

        private int currentQuestionIndex;

        private List<String> questionIds =
                new ArrayList<>();

        private List<String> selectedWords =
                new ArrayList<>();

        private List<String> availableWords =
                new ArrayList<>();

        private int score;
        private int combo;
        private int bestCombo;
        private int correctCount;
        private int wrongQuestionCount;
        private int currentMistakes;
        private long elapsedSeconds;

        public int getCurrentQuestionIndex() {
            return currentQuestionIndex;
        }

        public List<String> getQuestionIds() {
            return new ArrayList<>(
                    questionIds
            );
        }

        public List<String> getSelectedWords() {
            return new ArrayList<>(
                    selectedWords
            );
        }

        public List<String> getAvailableWords() {
            return new ArrayList<>(
                    availableWords
            );
        }


        public int getScore() {
            return score;
        }

        public int getCombo() {
            return combo;
        }

        public int getBestCombo() {
            return bestCombo;
        }

        public int getCorrectCount() {
            return correctCount;
        }

        public int getWrongQuestionCount() {
            return wrongQuestionCount;
        }

        public int getCurrentMistakes() {
            return currentMistakes;
        }

        public long getElapsedSeconds() {
            return elapsedSeconds;
        }
    }
}