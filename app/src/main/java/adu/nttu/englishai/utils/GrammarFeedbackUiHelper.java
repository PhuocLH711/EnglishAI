package adu.nttu.englishai.utils;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import adu.nttu.englishai.models.SentenceExercise;
import adu.nttu.englishai.models.GrammarGameStats;

/**
 * Helper UI cho feedback đúng/sai và trạng thái hoàn thành Grammar Sprint.
 *
 * Không chứa logic kiểm tra đáp án, Firestore, SharedPreferences hay session state.
 */
public final class GrammarFeedbackUiHelper {

    private GrammarFeedbackUiHelper() {
        // Utility class
    }

    public static void showCorrectFeedback(
            MaterialCardView cardSentenceFeedback,
            TextView tvSentenceFeedback,
            TextView tvAvailableWordsTitle,
            View layoutAvailableWords,
            SentenceExercise exercise
    ) {

        if (cardSentenceFeedback == null
                || tvSentenceFeedback == null
                || exercise == null) {

            return;
        }

        cardSentenceFeedback.setVisibility(
                View.VISIBLE
        );

        StringBuilder feedback =
                new StringBuilder();

        feedback.append("✓ Chính xác!");
        feedback.append("\n\n");
        feedback.append(
                exercise.getEnglishSentence()
        );

        String grammarTopic =
                exercise.getGrammarTopic();

        if (grammarTopic != null
                && !grammarTopic.trim().isEmpty()) {

            feedback.append("\n\n📘 ");
            feedback.append(
                    grammarTopic
            );
        }

        String explanation =
                exercise.getExplanation();

        if (explanation != null
                && !explanation.trim().isEmpty()) {

            feedback.append("\n");
            feedback.append(
                    explanation
            );
        }

        tvSentenceFeedback.setText(
                feedback.toString()
        );

        tvSentenceFeedback.setTextColor(
                Color.parseColor(
                        "#19733A"
                )
        );

        cardSentenceFeedback.setCardBackgroundColor(
                Color.parseColor(
                        "#E9F8EF"
                )
        );

        if (tvAvailableWordsTitle != null) {
            tvAvailableWordsTitle.setVisibility(
                    View.GONE
            );
        }

        if (layoutAvailableWords != null) {
            layoutAvailableWords.setVisibility(
                    View.GONE
            );
        }
    }

    public static void showWrongFeedback(
            MaterialCardView cardSentenceFeedback,
            TextView tvSentenceFeedback
    ) {

        if (cardSentenceFeedback == null
                || tvSentenceFeedback == null) {

            return;
        }

        cardSentenceFeedback.setVisibility(
                View.VISIBLE
        );

        tvSentenceFeedback.setText(
                "↻ Chưa chính xác.\n\n"
                        + "Chạm vào một từ trong câu, sau đó chạm từ khác "
                        + "để đổi chỗ trực tiếp."
        );

        tvSentenceFeedback.setTextColor(
                Color.parseColor(
                        "#B64A3A"
                )
        );

        cardSentenceFeedback.setCardBackgroundColor(
                Color.parseColor(
                        "#FFF0EE"
                )
        );
    }

    public static void hideFeedback(
            MaterialCardView cardSentenceFeedback
    ) {

        if (cardSentenceFeedback != null) {
            cardSentenceFeedback.setVisibility(
                    View.GONE
            );
        }
    }

    public static void showCompletionFeedback(
            MaterialCardView cardSentenceFeedback,
            TextView tvSentenceFeedback,
            int totalQuestions,
            GrammarGameStats stats
    ) {

        if (cardSentenceFeedback == null
                || tvSentenceFeedback == null
                || stats == null) {

            return;
        }

        cardSentenceFeedback.setVisibility(
                View.VISIBLE
        );

        String summary =
                "🎉 Grammar Sprint Complete"
                        + "\n\n⭐ Điểm: "
                        + stats.getScore()
                        + "/"
                        + (totalQuestions * 10)
                        + "\n✅ Đúng ngay lần đầu: "
                        + stats.getFirstTryCorrectCount()
                        + "/"
                        + totalQuestions
                        + "\n🎯 Độ chính xác: "
                        + stats.getFirstTryAccuracyPercent(
                        totalQuestions
                )
                        + "%"
                        + "\n🔥 Combo tốt nhất: x"
                        + stats.getBestCombo()
                        + "\n⏱ Thời gian: "
                        + stats.getFormattedTime();

        tvSentenceFeedback.setText(
                summary
        );

        tvSentenceFeedback.setTextColor(
                Color.parseColor(
                        "#4B408E"
                )
        );

        cardSentenceFeedback.setCardBackgroundColor(
                Color.parseColor(
                        "#EEEAFE"
                )
        );
    }

}