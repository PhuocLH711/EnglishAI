package adu.nttu.englishai.ai;

import java.util.Locale;

// =========================================================================
// CONVERSATION TITLE GENERATOR: Trình tự động đặt tiêu đề phòng chat thông minh
// =========================================================================
// Sử dụng từ khóa "final" và constructor private theo mô hình Utility Class,
// không cho phép khởi tạo đối tượng để tiết kiệm tối đa bộ nhớ RAM.
public final class ConversationTitleGenerator {

    // Giới hạn chiều dài tối đa của tiêu đề là 35 ký tự
    // Tránh trường hợp tiêu đề quá dài làm vỡ bố cục giao diện thẻ Lịch sử trò chuyện
    private static final int MAX_TITLE_LENGTH = 35;

    private ConversationTitleGenerator() {
        // Không cho tạo object
    }

    // =========================================================================
    // HÀM QUAN TRỌNG NHẤT: PHÂN TÍCH TỪ KHÓA VÀ SINH TIÊU ĐỀ (HEURISTIC MATCHING)
    // =========================================================================
    public static String generate(String firstQuestion) {
        // 1. Kiểm tra an toàn: Nếu câu hỏi trống hoặc null -> Đặt tên mặc định
        if (firstQuestion == null
                || firstQuestion.trim().isEmpty()) {

            return "Cuộc trò chuyện mới";
        }

        String originalQuestion = firstQuestion.trim();

        // 2. CHUẨN HÓA CHUỖI (String Normalization):
        // Chuyển toàn bộ câu hỏi về chữ thường để so sánh không phân biệt hoa/thường.
        // Dùng Locale.ROOT để đảm bảo việc biến đổi chữ thường luôn chuẩn quốc tế,
        // không bị ảnh hưởng bởi cài đặt ngôn ngữ riêng của hệ điều hành điện thoại (tránh lỗi Turkish 'i' bug).
        String normalizedQuestion =
                originalQuestion.toLowerCase(Locale.ROOT);

        // =========================================================================
        // 3. QUÉT TỪ KHÓA GẮN NHÃN BIỂU TƯỢNG (RULE-BASED NLP)
        // =========================================================================
        // Nếu câu hỏi chứa từ khóa về Ngữ pháp -> Gán tiêu đề chuẩn kèm icon cuốn sách 📘
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

        // Nếu câu hỏi có ý định dịch thuật -> Gán icon quả địa cầu 🌐
        if (containsAny(
                normalizedQuestion,
                "dịch",
                "translate",
                "translation"
        )) {
            return "🌐 Luyện dịch";
        }

        // Nếu câu hỏi có ý định luyện viết/essay -> Gán icon ghi chép 📝
        if (containsAny(
                normalizedQuestion,
                "writing",
                "viết đoạn",
                "viết bài",
                "essay"
        )) {
            return "📝 Luyện viết";
        }

        // Nếu câu hỏi thiên về hội thoại/giao tiếp -> Gán icon phát âm 🗣
        if (containsAny(
                normalizedQuestion,
                "speaking",
                "giao tiếp",
                "hội thoại"
        )) {
            return "🗣 Luyện giao tiếp";
        }

        // Nếu hỏi ôn thi chứng chỉ -> Gán icon mục tiêu 🎯 / học vị 🎓
        if (normalizedQuestion.contains("ielts")) {
            return "🎯 IELTS";
        }

        if (normalizedQuestion.contains("toeic")) {
            return "🎓 TOEIC";
        }

        // =========================================================================
        // 4. XỬ LÝ TRƯỜNG HỢP KHÔNG TRÚNG TỪ KHÓA (FALLBACK TRUNCATION)
        // =========================================================================
        // Nếu câu hỏi ngắn <= 35 ký tự -> Lấy nguyên câu hỏi làm tiêu đề phòng chat
        if (originalQuestion.length() <= MAX_TITLE_LENGTH) {
            return originalQuestion;
        }

        // Nếu câu hỏi quá dài -> Cắt lấy đúng 35 ký tự đầu tiên và thêm dấu "..." ở cuối
        // Giúp tiêu đề hiển thị gọn gàng, thẩm mỹ trên giao diện danh sách
        return originalQuestion.substring(
                0,
                MAX_TITLE_LENGTH
        ) + "...";
    }

    // =========================================================================
    // HÀM HỖ TRỢ: KIỂM TRA CHUỖI CÓ CHỨA BẤT KỲ TỪ KHÓA NÀO KHÔNG (VARARGS helper)
    // =========================================================================
    // String... keywords (Varargs): Cho phép truyền vào số lượng từ khóa tùy ý separated by comma
    // (ví dụ: containsAny(text, "a", "b", "c") mà không cần phải tạo mảng mới gây cồng kềnh code)
    private static boolean containsAny(
            String text,
            String... keywords
    ) {
        // Quét qua từng từ khóa, chỉ cần trúng 1 từ (OR logic) là lập tức trả về true
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}