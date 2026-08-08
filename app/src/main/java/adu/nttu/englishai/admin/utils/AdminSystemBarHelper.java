package adu.nttu.englishai.admin.utils;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helper dùng chung cho các màn hình Admin:
 * - Đẩy header xuống dưới status bar / camera cutout.
 * - Giữ nguyên padding gốc trong XML.
 */
public final class AdminSystemBarHelper {

    private AdminSystemBarHelper() {
    }

    public static void applyTopInset(
            Activity activity,
            View root
    ) {

        if (activity == null || root == null) {
            return;
        }

        final int initialLeft = root.getPaddingLeft();
        final int initialTop = root.getPaddingTop();
        final int initialRight = root.getPaddingRight();
        final int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {

                    Insets systemBars =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.statusBars()
                                            | WindowInsetsCompat.Type.displayCutout()
                            );

                    view.setPadding(
                            initialLeft,
                            initialTop + systemBars.top,
                            initialRight,
                            initialBottom
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(root);
    }
}
