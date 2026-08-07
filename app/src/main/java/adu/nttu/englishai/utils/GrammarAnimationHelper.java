package adu.nttu.englishai.utils;

import android.animation.ObjectAnimator;
import android.view.View;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/**
 * Chứa toàn bộ animation dùng riêng cho Grammar Sprint.
 *
 * Không chứa logic Firestore, SharedPreferences hay kiểm tra đáp án.
 */
public final class GrammarAnimationHelper {

    private GrammarAnimationHelper() {
        // Utility class
    }

    /**
     * Fade + trượt nhẹ câu hỏi khi sang câu mới.
     */
    public static void animateQuestion(
            View view,
            float translationY
    ) {

        if (view == null) {
            return;
        }

        view.setAlpha(0f);
        view.setTranslationY(
                translationY
        );

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
    }

    /**
     * Hiện card feedback bằng fade + slide.
     */
    public static void animateFeedbackCard(
            MaterialCardView card,
            float translationY
    ) {

        if (card == null) {
            return;
        }

        card.setAlpha(0f);
        card.setTranslationY(
                translationY
        );

        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
    }

    /**
     * Hiệu ứng nhún nhẹ cho nút sau khi trả lời đúng.
     */
    public static void animateSuccessButton(
            View button
    ) {

        if (button == null) {
            return;
        }

        button.setScaleX(0.96f);
        button.setScaleY(0.96f);

        button.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .start();
    }

    /**
     * Rung vùng câu trả lời khi đáp án sai.
     */
    public static void shakeWrongAnswer(
            View target,
            float strongDistance,
            float lightDistance
    ) {

        if (target == null) {
            return;
        }

        ObjectAnimator shakeAnimator =
                ObjectAnimator.ofFloat(
                        target,
                        "translationX",
                        0f,
                        -strongDistance,
                        strongDistance,
                        -lightDistance,
                        lightDistance,
                        0f
                );

        shakeAnimator.setDuration(
                320
        );

        shakeAnimator.start();
    }

    /**
     * Cập nhật thanh tiến trình với animation.
     */
    public static void animateProgress(
            LinearProgressIndicator progressIndicator,
            int max,
            int progress
    ) {

        if (progressIndicator == null) {
            return;
        }

        progressIndicator.setMax(
                max
        );

        progressIndicator.setProgressCompat(
                progress,
                true
        );
    }
}