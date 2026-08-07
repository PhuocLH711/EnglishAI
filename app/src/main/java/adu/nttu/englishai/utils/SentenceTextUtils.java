package adu.nttu.englishai.utils;

import java.util.Locale;

/**
 * Tiện ích xử lý chuỗi cho chức năng Grammar Sprint.
 *
 * File này chỉ chịu trách nhiệm:
 * - Chuẩn hóa câu trước khi tách thành các từ.
 * - Chuẩn hóa câu trước khi so sánh đáp án.
 *
 * Không chứa UI, Firestore hay SharedPreferences.
 */
public final class SentenceTextUtils {

    private SentenceTextUtils() {
        // Không cho tạo object utility.
    }

    /**
     * Chuẩn hóa câu trước khi tách thành các chip.
     *
     * Ví dụ:
     * “I’m studying English now.” -> "I'm studying English now"
     */
    public static String cleanSentence(
            String sentence
    ) {

        if (sentence == null) {
            return "";
        }

        return sentence
                // Khoảng trắng đặc biệt do copy/paste.
                .replace('\u00A0', ' ')
                .trim()

                // Chuẩn hóa dấu nháy cong.
                .replace('’', '\'')
                .replace('‘', '\'')

                // Chuẩn hóa dấu ngoặc kép cong.
                .replace('“', '"')
                .replace('”', '"')

                // Bỏ dấu kết thúc câu.
                // Dấu phẩy, ;, : ở giữa câu vẫn được giữ.
                .replaceAll(
                        "[.!?…]+$",
                        ""
                )

                // Gom nhiều khoảng trắng thành một.
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    /**
     * Chuẩn hóa câu để kiểm tra đáp án.
     *
     * Không phân biệt chữ hoa/thường và không bắt buộc
     * dấu . ! ? … ở cuối câu.
     */
    public static String normalizeForCompare(
            String sentence
    ) {

        if (sentence == null) {
            return "";
        }

        return sentence
                .replace('\u00A0', ' ')
                .trim()

                .replace('’', '\'')
                .replace('‘', '\'')

                .replace('“', '"')
                .replace('”', '"')

                .replaceAll(
                        "[.!?…]+$",
                        ""
                )

                // "hello ," -> "hello,"
                .replaceAll(
                        "\\s+([,;:])",
                        "$1"
                )

                .replaceAll(
                        "\\s+",
                        " "
                )

                .toLowerCase(
                        Locale.ROOT
                );
    }
}