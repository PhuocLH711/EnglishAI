package adu.nttu.englishai.utils;

import android.animation.LayoutTransition;
import android.content.ClipData;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import adu.nttu.englishai.models.GrammarSession;

/**
 * Helper UI cho phần sắp xếp từ của Grammar Sprint.
 *
 * UX:
 * - Nhấn giữ một Chip để kéo.
 * - Kéo từ Word Bank -> Câu của bạn.
 * - Kéo từ Câu của bạn -> Word Bank.
 * - Kéo trong cùng khu vực để đổi thứ tự.
 * - Chạm bình thường KHÔNG đổi vị trí.
 */
public final class GrammarWordUiHelper {

    private static final String SOURCE_SELECTED =
            "selected";

    private static final String SOURCE_AVAILABLE =
            "available";

    private GrammarWordUiHelper() {
    }

    public interface WordDragListener {

        void onSelectedToSelected(
                int fromPosition,
                int toPosition
        );

        void onAvailableToAvailable(
                int fromPosition,
                int toPosition
        );

        void onSelectedToAvailable(
                int selectedPosition,
                int availablePosition
        );

        void onAvailableToSelected(
                int availablePosition,
                int selectedPosition
        );
    }

    private static class DragWord {

        final String source;
        final int position;
        final View sourceView;

        DragWord(
                String source,
                int position,
                View sourceView
        ) {
            this.source = source;
            this.position = position;
            this.sourceView = sourceView;
        }
    }

    public static void renderWords(
            Context context,
            GrammarSession session,
            ChipGroup layoutSelectedWords,
            ChipGroup layoutAvailableWords,
            TextView tvAnswerPlaceholder,
            MaterialButton btnCheckSentence,
            WordDragListener listener
    ) {

        if (context == null
                || session == null
                || layoutSelectedWords == null
                || layoutAvailableWords == null
                || tvAnswerPlaceholder == null
                || btnCheckSentence == null
                || listener == null) {

            return;
        }

        List<String> selectedWords =
                session.getSelectedWords();

        List<String> availableWords =
                session.getAvailableWords();

        enableSmoothLayoutAnimation(
                layoutSelectedWords
        );

        enableSmoothLayoutAnimation(
                layoutAvailableWords
        );

        layoutSelectedWords.removeAllViews();
        layoutAvailableWords.removeAllViews();

        // =====================================================
        // CÂU CỦA BẠN
        // =====================================================

        for (int i = 0;
             i < selectedWords.size();
             i++) {

            final int position = i;

            Chip chip =
                    createWordChip(
                            context,
                            selectedWords.get(i),
                            true
                    );

            setupDragSource(
                    chip,
                    SOURCE_SELECTED,
                    position,
                    null
            );

            setupDropTarget(
                    chip,
                    SOURCE_SELECTED,
                    position,
                    listener
            );

            layoutSelectedWords.addView(
                    chip
            );
        }

        // =====================================================
        // CHỌN CÁC TỪ
        // =====================================================

        for (int i = 0;
             i < availableWords.size();
             i++) {

            final int position = i;

            Chip chip =
                    createWordChip(
                            context,
                            availableWords.get(i),
                            false
                    );

            final int selectedInsertPosition =
                    selectedWords.size();

            setupDragSource(
                    chip,
                    SOURCE_AVAILABLE,
                    position,
                    () -> listener.onAvailableToSelected(
                            position,
                            selectedInsertPosition
                    )
            );

            setupDropTarget(
                    chip,
                    SOURCE_AVAILABLE,
                    position,
                    listener
            );

            layoutAvailableWords.addView(
                    chip
            );
        }

        // Cho phép thả vào vùng trống / cuối danh sách.
        setupGroupDropTarget(
                layoutSelectedWords,
                SOURCE_SELECTED,
                selectedWords.size(),
                listener
        );

        setupGroupDropTarget(
                layoutAvailableWords,
                SOURCE_AVAILABLE,
                availableWords.size(),
                listener
        );

        /*
         * Không cần dùng hết từ mới được kiểm tra.
         * Chỉ cần người học đã kéo ít nhất 1 từ vào câu.
         */
        btnCheckSentence.setEnabled(
                !selectedWords.isEmpty()
        );

        tvAnswerPlaceholder.setVisibility(
                selectedWords.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private static void setupDragSource(
            Chip chip,
            String source,
            int position,
            Runnable tapAction
    ) {

        final Handler handler =
                new Handler(
                        Looper.getMainLooper()
                );

        final int touchSlop =
                ViewConfiguration
                        .get(chip.getContext())
                        .getScaledTouchSlop();

        final float[] downX = {0f};
        final float[] downY = {0f};

        final boolean[] dragStarted = {false};

        final Runnable startDragRunnable =
                () -> {

                    if (!chip.isPressed()) {
                        return;
                    }

                    dragStarted[0] = true;

                    chip.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS
                    );

                    DragWord dragWord =
                            new DragWord(
                                    source,
                                    position,
                                    chip
                            );

                    ClipData clipData =
                            ClipData.newPlainText(
                                    "grammar_word",
                                    chip.getText()
                            );

                    View.DragShadowBuilder shadowBuilder =
                            new View.DragShadowBuilder(
                                    chip
                            );

                    boolean started;

                    if (Build.VERSION.SDK_INT
                            >= Build.VERSION_CODES.N) {

                        started =
                                chip.startDragAndDrop(
                                        clipData,
                                        shadowBuilder,
                                        dragWord,
                                        0
                                );

                    } else {

                        //noinspection deprecation
                        started =
                                chip.startDrag(
                                        clipData,
                                        shadowBuilder,
                                        dragWord,
                                        0
                                );
                    }

                    if (started) {

                        chip.setSelected(
                                true
                        );

                        chip.animate()
                                .alpha(0.35f)
                                .scaleX(0.95f)
                                .scaleY(0.95f)
                                .translationZ(
                                        dpToPx(
                                                chip.getContext(),
                                                8
                                        )
                                )
                                .setDuration(100)
                                .start();
                    }
                };

        chip.setClickable(
                true
        );

        chip.setLongClickable(
                false
        );

        chip.setOnTouchListener(
                (view, event) -> {

                    switch (event.getActionMasked()) {

                        case MotionEvent.ACTION_DOWN:

                            downX[0] =
                                    event.getX();

                            downY[0] =
                                    event.getY();

                            dragStarted[0] =
                                    false;

                            view.setPressed(
                                    true
                            );

                            handler.postDelayed(
                                    startDragRunnable,
                                    180
                            );

                            return true;

                        case MotionEvent.ACTION_MOVE:

                            float dx =
                                    Math.abs(
                                            event.getX()
                                                    - downX[0]
                                    );

                            float dy =
                                    Math.abs(
                                            event.getY()
                                                    - downY[0]
                                    );

                            if (!dragStarted[0]
                                    && (dx > touchSlop
                                    || dy > touchSlop)) {

                                /*
                                 * Người dùng bắt đầu di chuyển sớm:
                                 * khởi động drag luôn, không phải chờ 0.5 giây.
                                 */
                                handler.removeCallbacks(
                                        startDragRunnable
                                );

                                view.setPressed(
                                        true
                                );

                                startDragRunnable.run();
                            }

                            return true;

                        case MotionEvent.ACTION_UP:

                            handler.removeCallbacks(
                                    startDragRunnable
                            );

                            view.setPressed(
                                    false
                            );

                            if (!dragStarted[0]
                                    && tapAction != null) {

                                view.performClick();

                                tapAction.run();
                            }

                            return true;

                        case MotionEvent.ACTION_CANCEL:

                            handler.removeCallbacks(
                                    startDragRunnable
                            );

                            view.setPressed(
                                    false
                            );

                            return true;

                        default:
                            return true;
                    }
                }
        );
    }

    private static void setupDropTarget(
            View targetView,
            String targetSource,
            int targetPosition,
            WordDragListener listener
    ) {

        targetView.setOnDragListener(
                (view, event) -> {

                    switch (event.getAction()) {

                        case DragEvent.ACTION_DRAG_STARTED:

                            return event.getLocalState()
                                    instanceof DragWord;

                        case DragEvent.ACTION_DRAG_ENTERED:

                            view.animate()
                                    .scaleX(1.06f)
                                    .scaleY(1.06f)
                                    .setDuration(90)
                                    .start();

                            return true;

                        case DragEvent.ACTION_DRAG_EXITED:

                            view.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(90)
                                    .start();

                            return true;

                        case DragEvent.ACTION_DROP:

                            view.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(90)
                                    .start();

                            Object state =
                                    event.getLocalState();

                            if (!(state instanceof DragWord)) {
                                return false;
                            }

                            dispatchMove(
                                    (DragWord) state,
                                    targetSource,
                                    targetPosition,
                                    listener
                            );

                            return true;

                        case DragEvent.ACTION_DRAG_ENDED:

                            restoreDraggedView(
                                    event
                            );

                            return true;

                        default:
                            return true;
                    }
                }
        );
    }

    private static void setupGroupDropTarget(
            ChipGroup group,
            String targetSource,
            int targetPosition,
            WordDragListener listener
    ) {

        group.setClickable(
                true
        );

        group.setOnDragListener(
                (view, event) -> {

                    switch (event.getAction()) {

                        case DragEvent.ACTION_DRAG_STARTED:

                            return event.getLocalState()
                                    instanceof DragWord;

                        case DragEvent.ACTION_DRAG_ENTERED:

                            group.setAlpha(
                                    0.92f
                            );

                            return true;

                        case DragEvent.ACTION_DRAG_EXITED:

                            group.setAlpha(
                                    1f
                            );

                            return true;

                        case DragEvent.ACTION_DROP:

                            group.setAlpha(
                                    1f
                            );

                            Object state =
                                    event.getLocalState();

                            if (!(state instanceof DragWord)) {
                                return false;
                            }

                            int dropPosition =
                                    findDropPosition(
                                            group,
                                            event.getX(),
                                            event.getY(),
                                            targetPosition
                                    );

                            dispatchMove(
                                    (DragWord) state,
                                    targetSource,
                                    dropPosition,
                                    listener
                            );

                            return true;

                        case DragEvent.ACTION_DRAG_ENDED:

                            group.setAlpha(
                                    1f
                            );

                            restoreDraggedView(
                                    event
                            );

                            return true;

                        default:
                            return true;
                    }
                }
        );
    }

    private static int findDropPosition(
            ChipGroup group,
            float x,
            float y,
            int fallback
    ) {

        int count =
                group.getChildCount();

        if (count == 0) {
            return 0;
        }

        int bestIndex =
                fallback;

        double bestDistance =
                Double.MAX_VALUE;

        for (int i = 0;
             i < count;
             i++) {

            View child =
                    group.getChildAt(i);

            float centerX =
                    child.getX()
                            + child.getWidth()
                            / 2f;

            float centerY =
                    child.getY()
                            + child.getHeight()
                            / 2f;

            double dx =
                    x - centerX;

            double dy =
                    y - centerY;

            double distance =
                    dx * dx
                            + dy * dy;

            if (distance
                    < bestDistance) {

                bestDistance =
                        distance;

                bestIndex =
                        x < centerX
                                ? i
                                : i + 1;
            }
        }

        return Math.max(
                0,
                Math.min(
                        bestIndex,
                        count
                )
        );
    }

    private static void dispatchMove(
            DragWord dragWord,
            String targetSource,
            int targetPosition,
            WordDragListener listener
    ) {

        if (SOURCE_SELECTED.equals(
                dragWord.source
        )) {

            if (SOURCE_SELECTED.equals(
                    targetSource
            )) {

                int adjustedTarget =
                        targetPosition;

                /*
                 * Nếu kéo từ vị trí nhỏ sang vị trí lớn,
                 * sau khi remove index sẽ dịch trái 1.
                 */
                if (dragWord.position
                        < adjustedTarget) {

                    adjustedTarget--;
                }

                if (adjustedTarget
                        != dragWord.position) {

                    listener.onSelectedToSelected(
                            dragWord.position,
                            Math.max(
                                    0,
                                    adjustedTarget
                            )
                    );
                }

            } else {

                listener.onSelectedToAvailable(
                        dragWord.position,
                        targetPosition
                );
            }

            return;
        }

        if (SOURCE_AVAILABLE.equals(
                dragWord.source
        )) {

            if (SOURCE_AVAILABLE.equals(
                    targetSource
            )) {

                int adjustedTarget =
                        targetPosition;

                if (dragWord.position
                        < adjustedTarget) {

                    adjustedTarget--;
                }

                if (adjustedTarget
                        != dragWord.position) {

                    listener.onAvailableToAvailable(
                            dragWord.position,
                            Math.max(
                                    0,
                                    adjustedTarget
                            )
                    );
                }

            } else {

                listener.onAvailableToSelected(
                        dragWord.position,
                        targetPosition
                );
            }
        }
    }

    private static void restoreDraggedView(
            DragEvent event
    ) {

        Object state =
                event.getLocalState();

        if (!(state instanceof DragWord)) {
            return;
        }

        DragWord dragWord =
                (DragWord) state;

        if (dragWord.sourceView != null) {

            dragWord.sourceView.setSelected(
                    false
            );

            dragWord.sourceView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(120)
                    .start();
        }
    }

    private static void enableSmoothLayoutAnimation(
            ViewGroup viewGroup
    ) {

        LayoutTransition transition =
                viewGroup.getLayoutTransition();

        if (transition == null) {

            transition =
                    new LayoutTransition();

            viewGroup.setLayoutTransition(
                    transition
            );
        }

        transition.enableTransitionType(
                LayoutTransition.CHANGING
        );

        transition.setDuration(
                150
        );
    }

    public static void setWordChipsEnabled(
            ChipGroup layoutSelectedWords,
            ChipGroup layoutAvailableWords,
            boolean enabled
    ) {

        setGroupEnabled(
                layoutSelectedWords,
                enabled
        );

        setGroupEnabled(
                layoutAvailableWords,
                enabled
        );
    }

    private static void setGroupEnabled(
            ChipGroup group,
            boolean enabled
    ) {

        if (group == null) {
            return;
        }

        group.setEnabled(
                enabled
        );

        for (int i = 0;
             i < group.getChildCount();
             i++) {

            View child =
                    group.getChildAt(i);

            child.setEnabled(
                    enabled
            );

            child.setLongClickable(
                    enabled
            );

            child.setAlpha(
                    enabled
                            ? 1f
                            : 0.72f
            );
        }
    }

    private static Chip createWordChip(
            Context context,
            String word,
            boolean selected
    ) {

        Chip chip =
                new Chip(context);

        chip.setText(
                word
        );

        chip.setTextSize(
                15
        );

        chip.setTextColor(
                Color.parseColor(
                        "#35334A"
                )
        );

        chip.setCheckable(
                false
        );

        /*
         * Không dùng click để thay đổi vị trí nữa.
         * Chip chỉ nhận nhấn giữ để kéo.
         */
        chip.setClickable(
                true
        );

        chip.setLongClickable(
                true
        );



        chip.setEnsureMinTouchTargetSize(
                false
        );

        chip.setMinHeight(
                dpToPx(
                        context,
                        44
                )
        );

        chip.setChipCornerRadius(
                dpToPx(
                        context,
                        14
                )
        );

        chip.setChipStrokeWidth(
                dpToPx(
                        context,
                        1
                )
        );

        chip.setChipStartPadding(
                dpToPx(
                        context,
                        8
                )
        );

        chip.setChipEndPadding(
                dpToPx(
                        context,
                        8
                )
        );

        if (selected) {

            chip.setChipBackgroundColor(
                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#EEEAFE"
                            )
                    )
            );

            chip.setChipStrokeColor(
                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#9F91F2"
                            )
                    )
            );

        } else {

            chip.setChipBackgroundColor(
                    ColorStateList.valueOf(
                            Color.WHITE
                    )
            );

            chip.setChipStrokeColor(
                    ColorStateList.valueOf(
                            Color.parseColor(
                                    "#DADCE8"
                            )
                    )
            );
        }

        return chip;
    }

    private static int dpToPx(
            Context context,
            int dp
    ) {

        float density =
                context
                        .getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dp * density
        );
    }
}