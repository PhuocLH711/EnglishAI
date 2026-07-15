package adu.nttu.englishai.ai;

import java.util.Locale;

public final class ConversationTitleGenerator {

    private static final int MAX_TITLE_LENGTH = 35;

    private ConversationTitleGenerator() {
        // Không cho tạo object
    }

    public static String generate(String firstQuestion) {
        if (firstQuestion == null
                || firstQuestion.trim().isEmpty()) {

            return "Cuộc trò chuyện mới";
        }

        String originalQuestion = firstQuestion.trim();

        String normalizedQuestion =
                originalQuestion.toLowerCase(Locale.ROOT);

        if (containsAny(
                normalizedQuestion,
                "present simple",
                "hiện tại đơn"
        )) {
            return "📘 Present Simple";
        }

        if (containsAny(
                normalizedQuestion,
                "present continuous",
                "hiện tại tiếp diễn"
        )) {
            return "📘 Present Continuous";
        }

        if (containsAny(
                normalizedQuestion,
                "present perfect",
                "hiện tại hoàn thành"
        )) {
            return "📘 Present Perfect";
        }

        if (containsAny(
                normalizedQuestion,
                "past simple",
                "quá khứ đơn"
        )) {
            return "📘 Past Simple";
        }

        if (containsAny(
                normalizedQuestion,
                "future simple",
                "tương lai đơn"
        )) {
            return "📘 Future Simple";
        }

        if (containsAny(
                normalizedQuestion,
                "dịch",
                "translate",
                "translation"
        )) {
            return "🌐 Luyện dịch";
        }

        if (containsAny(
                normalizedQuestion,
                "writing",
                "viết đoạn",
                "viết bài",
                "essay"
        )) {
            return "📝 Luyện viết";
        }

        if (containsAny(
                normalizedQuestion,
                "speaking",
                "giao tiếp",
                "hội thoại"
        )) {
            return "🗣 Luyện giao tiếp";
        }

        if (normalizedQuestion.contains("ielts")) {
            return "🎯 IELTS";
        }

        if (normalizedQuestion.contains("toeic")) {
            return "🎓 TOEIC";
        }

        if (originalQuestion.length() <= MAX_TITLE_LENGTH) {
            return originalQuestion;
        }

        return originalQuestion.substring(
                0,
                MAX_TITLE_LENGTH
        ) + "...";
    }

    private static boolean containsAny(
            String text,
            String... keywords
    ) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}