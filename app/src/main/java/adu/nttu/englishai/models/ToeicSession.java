package adu.nttu.englishai.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * State của một lượt luyện / thi TOEIC.
 *
 * Không chứa UI hoặc Firestore.
 */
public class ToeicSession {

    private final List<ToeicQuestion> questions =
            new ArrayList<>();

    private final Map<String, String> answers =
            new HashMap<>();

    private final Set<String> bookmarkedQuestionIds =
            new HashSet<>();

    private int currentIndex = 0;

    public void start(
            List<ToeicQuestion> source
    ) {

        questions.clear();
        answers.clear();
        bookmarkedQuestionIds.clear();

        currentIndex = 0;

        if (source != null) {
            questions.addAll(source);
        }
    }

    public ToeicQuestion getCurrentQuestion() {

        if (currentIndex < 0
                || currentIndex >= questions.size()) {

            return null;
        }

        return questions.get(
                currentIndex
        );
    }

    public void selectAnswer(
            String answer
    ) {

        ToeicQuestion question =
                getCurrentQuestion();

        if (question == null
                || question.getId() == null) {

            return;
        }

        String safeAnswer =
                answer == null
                        ? ""
                        : answer.trim();

        answers.put(
                question.getId(),
                safeAnswer
        );

        question.setSelectedAnswer(
                safeAnswer
        );
    }

    public String getAnswer(
            ToeicQuestion question
    ) {

        if (question == null
                || question.getId() == null) {

            return "";
        }

        String answer =
                answers.get(
                        question.getId()
                );

        return answer == null
                ? ""
                : answer;
    }

    public void toggleBookmark() {

        ToeicQuestion question =
                getCurrentQuestion();

        if (question == null
                || question.getId() == null) {

            return;
        }

        String id =
                question.getId();

        if (bookmarkedQuestionIds.contains(id)) {

            bookmarkedQuestionIds.remove(id);
            question.setBookmarked(false);

        } else {

            bookmarkedQuestionIds.add(id);
            question.setBookmarked(true);
        }
    }

    public boolean moveNext() {

        if (currentIndex
                >= questions.size() - 1) {

            return false;
        }

        currentIndex++;
        return true;
    }

    public boolean movePrevious() {

        if (currentIndex <= 0) {
            return false;
        }

        currentIndex--;
        return true;
    }

    public boolean goToQuestion(
            int index
    ) {

        if (index < 0
                || index >= questions.size()) {

            return false;
        }

        currentIndex = index;
        return true;
    }

    public int getAnsweredCount() {

        int count = 0;

        for (String value
                : answers.values()) {

            if (value != null
                    && !value.trim().isEmpty()) {

                count++;
            }
        }

        return count;
    }

    public int getCorrectCount() {

        int count = 0;

        for (ToeicQuestion question
                : questions) {

            String answer =
                    getAnswer(question);

            question.setSelectedAnswer(
                    answer
            );

            if (question.isCorrect()) {
                count++;
            }
        }

        return count;
    }

    public int getWrongCount() {

        int wrong = 0;

        for (ToeicQuestion question
                : questions) {

            String answer =
                    getAnswer(question);

            if (answer.isEmpty()) {
                continue;
            }

            question.setSelectedAnswer(
                    answer
            );

            if (!question.isCorrect()) {
                wrong++;
            }
        }

        return wrong;
    }

    public int getUnansweredCount() {

        return Math.max(
                0,
                questions.size()
                        - getAnsweredCount()
        );
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public List<ToeicQuestion> getQuestions() {
        return questions;
    }

    public Set<String> getBookmarkedQuestionIds() {
        return bookmarkedQuestionIds;
    }

    public boolean isEmpty() {
        return questions.isEmpty();
    }
}
