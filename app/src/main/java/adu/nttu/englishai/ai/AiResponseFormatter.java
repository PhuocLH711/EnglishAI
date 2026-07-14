package adu.nttu.englishai.ai;

import android.text.Spanned;

import androidx.core.text.HtmlCompat;

public final class AiResponseFormatter {

    private AiResponseFormatter() {
        // Không cho tạo object
    }

    public static Spanned formatFullAnswer(
            String question,
            String response,
            boolean isFromVoice
    ) {
        String questionLabel =
                isFromVoice
                        ? "🗣 Bạn nói:"
                        : "🙋 Bạn hỏi:";

        String fullText =
                "<b>" + questionLabel + "</b><br>"
                        + escapeHtml(question)
                        + "<br><br>"
                        + "<b>EnglishAI Tutor:</b><br>"
                        + formatAiResponse(response);

        return HtmlCompat.fromHtml(
                fullText,
                HtmlCompat.FROM_HTML_MODE_COMPACT
        );
    }

    private static String formatAiResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "AI chưa trả về nội dung.";
        }

        String formatted = response.trim();

        // Chuyển Markdown in đậm sang HTML
        formatted = formatted.replaceAll(
                "\\*\\*(.+?)\\*\\*",
                "<b>$1</b>"
        );

        // Xóa Markdown thừa
        formatted = formatted
                .replace("```html", "")
                .replace("```", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "");

        // Chuyển xuống dòng sang HTML
        formatted = formatted.replace("\n", "<br>");

        return formatted;
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}