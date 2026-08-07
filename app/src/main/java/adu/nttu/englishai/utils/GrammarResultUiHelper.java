package adu.nttu.englishai.utils;

import android.view.View;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import adu.nttu.englishai.models.GrammarGameStats;

/**
 * UI helper riêng cho màn tổng kết Grammar Sprint.
 *
 * Không chứa logic điểm, timer, Firestore hay session.
 */
public final class GrammarResultUiHelper {

    private GrammarResultUiHelper() {
        // Utility class
    }

    public static void showResult(
            View layoutGrammarResult,
            MaterialCardView cardResult,
            TextView tvResultScore,
            TextView tvResultAccuracy,
            TextView tvResultTime,
            TextView tvResultCombo,
            TextView tvResultMessage,
            GrammarGameStats stats,
            int totalQuestions
    ) {

        if (layoutGrammarResult == null
                || cardResult == null
                || stats == null) {
            return;
        }

        layoutGrammarResult.setVisibility(
                View.VISIBLE
        );

        cardResult.setAlpha(0f);
        cardResult.setTranslationY(24f);

        cardResult.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260)
                .start();

        int maxScore =
                totalQuestions * 10;

        int accuracy =
                stats.getFirstTryAccuracyPercent(
                        totalQuestions
                );

        if (tvResultScore != null) {
            tvResultScore.setText(
                    stats.getScore()
                            + " / "
                            + maxScore
            );
        }

        if (tvResultAccuracy != null) {
            tvResultAccuracy.setText(
                    accuracy + "%"
            );
        }

        if (tvResultTime != null) {
            tvResultTime.setText(
                    stats.getFormattedTime()
            );
        }

        if (tvResultCombo != null) {
            tvResultCombo.setText(
                    "x" + stats.getBestCombo()
            );
        }

        if (tvResultMessage != null) {

            String message;

            if (accuracy == 100) {
                message =
                        "Hoàn hảo! Bạn đã hoàn thành tất cả câu ngay lần đầu.";
            } else if (accuracy >= 80) {
                message =
                        "Rất tốt! Bạn đang nắm khá chắc cấu trúc ngữ pháp.";
            } else if (accuracy >= 60) {
                message =
                        "Khá tốt. Luyện thêm một lượt để tăng độ chính xác nhé.";
            } else {
                message =
                        "Tiếp tục luyện tập để ghi nhớ thứ tự từ và cấu trúc câu tốt hơn.";
            }

            tvResultMessage.setText(
                    message
            );
        }
    }

    public static void hideResult(
            View layoutGrammarResult
    ) {

        if (layoutGrammarResult != null) {
            layoutGrammarResult.setVisibility(
                    View.GONE
            );
        }
    }
}