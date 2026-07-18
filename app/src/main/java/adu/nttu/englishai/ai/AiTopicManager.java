package adu.nttu.englishai.ai;

// =========================================================================
// AI TOPIC MANAGER: Lớp quản lý trung tâm các chủ đề học tập cùng AI Gemini
// =========================================================================
// Sử dụng từ khóa "final" để ngăn việc kế thừa lớp này, đảm bảo cấu trúc dữ liệu không bị phá vỡ.
public final class AiTopicManager {

    // =========================================================================
    // HẰNG SỐ ĐỊNH DANH CÁC CHỦ ĐỀ (TOPIC CODES)
    // =========================================================================
    // Việc định nghĩa hằng số (constants) giúp tránh lỗi gõ nhầm chính tả (Magic Strings)
    // Khi gọi ở các file khác chỉ cần dùng AiTopicManager.VOCABULARY là đảm bảo chính xác 100%.
    public static final String GENERAL = "GENERAL";
    public static final String VOCABULARY = "VOCABULARY";
    public static final String GRAMMAR = "GRAMMAR";
    public static final String PRONUNCIATION = "PRONUNCIATION";
    public static final String CONVERSATION = "CONVERSATION";
    public static final String WRITING = "WRITING";
    public static final String TRANSLATION = "TRANSLATION";

    // Mảng lưu trữ TÊN HIỂN THỊ bằng tiếng Việt cho người dùng xem trên giao diện (Hộp thoại chọn chủ đề)
    private static final String[] TOPIC_NAMES = {
            "Hỏi tự do",
            "Từ vựng",
            "Ngữ pháp",
            "Phát âm",
            "Giao tiếp",
            "Luyện viết",
            "Dịch Anh ↔ Việt"
    };

    // Mảng lưu trữ MÃ HỆ THỐNG tương ứng 1:1 với vị trí của mảng tên bên trên
    private static final String[] TOPIC_CODES = {
            GENERAL,
            VOCABULARY,
            GRAMMAR,
            PRONUNCIATION,
            CONVERSATION,
            WRITING,
            TRANSLATION
    };

    // =========================================================================
    // PRIVATE CONSTRUCTOR (MÔ HÌNH UTILITY CLASS)
    // =========================================================================
    // Để constructor là private để KHÔNG CHO PHÉP khởi tạo đối tượng bằng từ khóa "new"
    // Vì mọi phương thức trong lớp đều là static, việc ngăn khởi tạo giúp tiết kiệm bộ nhớ RAM cho thiết bị.
    private AiTopicManager() {
        // Không cho tạo object
    }

    // =========================================================================
    // KỸ THUẬT SAO CHÉP PHÒNG VỆ (DEFENSIVE COPYING)
    // =========================================================================
    // Trong Java, mảng (array) là một dạng đối tượng có thể bị thay đổi giá trị phần tử từ bên ngoài.
    // Lệnh .clone() tạo ra một bản sao mới của mảng rồi mới trả về cho bên ngoài sử dụng,
    // đảm bảo mảng gốc (TOPIC_NAMES và TOPIC_CODES) được bảo vệ tuyệt đối không bị mã độc hoặc lỗi vô tình viết đè.
    public static String[] getTopicNames() {
        return TOPIC_NAMES.clone();
    }

    public static String[] getTopicCodes() {
        return TOPIC_CODES.clone();
    }

    // =========================================================================
    // HÀM QUAN TRỌNG: TÌM VỊ TRÍ (INDEX) CỦA CHỦ ĐỀ TRONG MẢNG
    // =========================================================================
    // Phục vụ cho việc chọn đúng mục mặc định (setSingleChoiceItems) khi mở hộp thoại chọn chủ đề
    public static int getTopicIndex(String topicCode) {
        // Quét tuần tự mảng mã chủ đề để đối chiếu
        for (int index = 0; index < TOPIC_CODES.length; index++) {
            if (TOPIC_CODES[index].equals(topicCode)) {
                return index; // Trả về vị trí nếu tìm thấy
            }
        }

        // CƠ CHẾ PHÒNG VỆ: Nếu mã không hợp lệ hoặc không tìm thấy -> Mặc định trả về 0 (Chủ đề "Hỏi tự do")
        return 0;
    }

    // Chuyển đổi từ Mã hệ thống (ví dụ: VOCABULARY) sang Tên tiếng Việt ("Từ vựng")
    public static String getTopicName(String topicCode) {
        int index = getTopicIndex(topicCode);
        return TOPIC_NAMES[index];
    }

    // =========================================================================
    // HÀM QUAN TRỌNG: TẠO LỜI CHÀO & HƯỚNG DẪN THEO TỪNG CHỦ ĐỀ (PROMPT GUIDance)
    // =========================================================================
    // Dựa vào chủ đề người dùng chọn, AI sẽ thay đổi lời dẫn đường để định hướng họ hỏi đúng trọng tâm
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

            // Trường hợp GENERAL hoặc mặc định
            default:
                return "Bạn có thể hỏi mình bất kỳ nội dung nào. "
                        + "AI sẽ trò chuyện linh hoạt và ghi nhớ nội dung trước đó.";
        }
    }
}