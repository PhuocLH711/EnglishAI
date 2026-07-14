package adu.nttu.englishai.ai;

public final class AiTopicManager {

    public static final String GENERAL = "GENERAL";
    public static final String VOCABULARY = "VOCABULARY";
    public static final String GRAMMAR = "GRAMMAR";
    public static final String PRONUNCIATION = "PRONUNCIATION";
    public static final String CONVERSATION = "CONVERSATION";
    public static final String WRITING = "WRITING";
    public static final String TRANSLATION = "TRANSLATION";

    private static final String[] TOPIC_NAMES = {
            "Hỏi tự do",
            "Từ vựng",
            "Ngữ pháp",
            "Phát âm",
            "Giao tiếp",
            "Luyện viết",
            "Dịch Anh ↔ Việt"
    };

    private static final String[] TOPIC_CODES = {
            GENERAL,
            VOCABULARY,
            GRAMMAR,
            PRONUNCIATION,
            CONVERSATION,
            WRITING,
            TRANSLATION
    };

    private AiTopicManager() {
        // Không cho tạo object
    }

    public static String[] getTopicNames() {
        return TOPIC_NAMES.clone();
    }

    public static String[] getTopicCodes() {
        return TOPIC_CODES.clone();
    }

    public static int getTopicIndex(String topicCode) {
        for (int index = 0; index < TOPIC_CODES.length; index++) {
            if (TOPIC_CODES[index].equals(topicCode)) {
                return index;
            }
        }

        return 0;
    }

    public static String getTopicName(String topicCode) {
        int index = getTopicIndex(topicCode);
        return TOPIC_NAMES[index];
    }

    public static String getWelcomeMessage(String topicCode) {
        switch (topicCode) {
            case VOCABULARY:
                return "Hãy nhập một từ tiếng Anh. "
                        + "Mình sẽ giải thích nghĩa, phiên âm và ví dụ.";

            case GRAMMAR:
                return "Hãy hỏi mình về một điểm ngữ pháp, "
                        + "ví dụ: thì hiện tại đơn.";

            case PRONUNCIATION:
                return "Hãy nhập hoặc nói một từ tiếng Anh. "
                        + "Mình sẽ hướng dẫn cách phát âm.";

            case CONVERSATION:
                return "Hãy bắt đầu cuộc hội thoại bằng tiếng Anh. "
                        + "Mình sẽ trò chuyện và sửa lỗi cho bạn.";

            case WRITING:
                return "Hãy nhập câu hoặc đoạn văn tiếng Anh. "
                        + "Mình sẽ sửa lỗi và đề xuất cách viết tự nhiên hơn.";

            case TRANSLATION:
                return "Hãy nhập câu tiếng Anh hoặc tiếng Việt cần dịch.";

            default:
                return "Bạn có thể hỏi mình bất kỳ nội dung nào. "
                        + "AI sẽ trò chuyện linh hoạt và ghi nhớ nội dung trước đó.";
        }
    }
}