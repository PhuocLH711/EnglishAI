package adu.nttu.englishai.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import adu.nttu.englishai.models.GrammarSession;

/**
 * Helper UI cho phần word bank của Grammar Sprint.
 *
 * Chịu trách nhiệm:
 * - Tạo Chip.
 * - Render từ đã chọn.
 * - Render từ chưa chọn.
 * - Highlight từ đang chờ swap.
 * - Cập nhật placeholder và trạng thái nút Kiểm tra.
 * - Enable / disable toàn bộ chip.
 *
 * Không chứa logic đúng/sai, Firestore hay SharedPreferences.
 */
public final class GrammarWordUiHelper {

    private GrammarWordUiHelper() {
        // Utility class
    }

    public interface WordClickListener {

        void onSelectedWordClick(
                int position
        );

        void onAvailableWordClick(
                int position
        );
    }

    public static void renderWords(
            Context context,
            GrammarSession session,
            ChipGroup layoutSelectedWords,
            ChipGroup layoutAvailableWords,
            TextView tvAnswerPlaceholder,
            MaterialButton btnCheckSentence,
            WordClickListener listener
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

        layoutSelectedWords.removeAllViews();
        layoutAvailableWords.removeAllViews();

        // =====================================================
        // CÂU CỦA BẠN
        // =====================================================

        for (int i = 0;
             i < selectedWords.size();
             i++) {

            final int position = i;

            String word =
                    selectedWords.get(i);

            Chip chip =
                    createWordChip(
                            context,
                            word,
                            true
                    );

            if (position
                    == session.getSelectedSwapIndex()) {

                chip.setChipBackgroundColor(
                        ColorStateList.valueOf(
                                Color.parseColor(
                                        "#DED8FF"
                                )
                        )
                );

                chip.setChipStrokeColor(
                        ColorStateList.valueOf(
                                Color.parseColor(
                                        "#6C5CE7"
                                )
                        )
                );

                chip.setChipStrokeWidth(
                        dpToPx(
                                context,
                                2
                        )
                );
            }

            chip.setOnClickListener(
                    view -> listener
                            .onSelectedWordClick(
                                    position
                            )
            );

            layoutSelectedWords.addView(
                    chip
            );
        }

        // =====================================================
        // WORD BANK
        // =====================================================

        for (int i = 0;
             i < availableWords.size();
             i++) {

            final int position = i;

            String word =
                    availableWords.get(i);

            Chip chip =
                    createWordChip(
                            context,
                            word,
                            false
                    );

            chip.setOnClickListener(
                    view -> listener
                            .onAvailableWordClick(
                                    position
                            )
            );

            layoutAvailableWords.addView(
                    chip
            );
        }

        btnCheckSentence.setEnabled(
                availableWords.isEmpty()
                        && !selectedWords.isEmpty()
        );

        tvAnswerPlaceholder.setVisibility(
                selectedWords.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    public static void setWordChipsEnabled(
            ChipGroup layoutSelectedWords,
            ChipGroup layoutAvailableWords,
            boolean enabled
    ) {

        if (layoutSelectedWords != null) {

            for (int i = 0;
                 i < layoutSelectedWords
                         .getChildCount();
                 i++) {

                View child =
                        layoutSelectedWords
                                .getChildAt(i);

                child.setEnabled(
                        enabled
                );
            }
        }

        if (layoutAvailableWords != null) {

            for (int i = 0;
                 i < layoutAvailableWords
                         .getChildCount();
                 i++) {

                View child =
                        layoutAvailableWords
                                .getChildAt(i);

                child.setEnabled(
                        enabled
                );
            }
        }
    }

    private static Chip createWordChip(
            Context context,
            String word,
            boolean selected
    ) {

        Chip chip =
                new Chip(context);

        chip.setText(word);
        chip.setTextSize(15);

        chip.setTextColor(
                Color.parseColor(
                        "#35334A"
                )
        );

        chip.setCheckable(false);
        chip.setClickable(true);
        chip.setEnsureMinTouchTargetSize(false);

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

        chip.setElevation(
                selected
                        ? 0f
                        : dpToPx(
                        context,
                        1
                )
        );

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