package adu.nttu.englishai.ai;

import android.text.Spanned;

import androidx.core.text.HtmlCompat;

// =========================================================================
// AI RESPONSE FORMATTER: Lớp tiện ích định dạng văn bản AI và bảo mật chuỗi
// =========================================================================
// Sử dụng từ khóa "final" để ngăn các lớp khác kế thừa (OOP concept),
// đảm bảo tính đóng gói và an toàn cho các hàm tiện ích hệ thống.
public final class AiResponseFormatter {

    // =========================================================================
    // PRIVATE CONSTRUCTOR (MÔ HÌNH UTILITY CLASS)
    // =========================================================================
    // Để constructor là private để KHÔNG CHO PHÉP tạo đối tượng (new AiResponseFormatter())
    // Vì tất cả các hàm trong lớp này đều là static (gọi trực tiếp qua tên lớp),
    // việc ngăn tạo object giúp tiết kiệm bộ nhớ RAM tuyệt đối cho ứng dụng.
    private AiResponseFormatter() {
        // Không cho tạo object
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 1: ĐÓNG GÓI TOÀN BỘ CÂU HỎI VÀ CÂU TRẢ LỜI THÀNH HTML
    // =========================================================================
    // Hàm này kết hợp câu hỏi của người dùng và câu trả lời của AI thành một khối Spanned duy nhất
    public static Spanned formatFullAnswer(
            String question,
            String response,
            boolean isFromVoice
    ) {
        // Dùng toán tử ba ngôi kiểm tra xem câu hỏi được gửi bằng giọng nói (Voice) hay gõ phím
        // Để gắn nhãn icon tương ứng giúp giao diện trực quan, sinh động hơn
        String questionLabel =
                isFromVoice
                        ? "🗣 Bạn nói:"
                        : "🙋 Bạn hỏi:";

        // Ghép nối chuỗi tạo cấu trúc HTML:
        // 1. Nhãn câu hỏi in đậm <b>...</b>
        // 2. Nội dung câu hỏi đã được làm sạch qua hàm escapeHtml() để chống lỗi giao diện
        // 3. Thẻ <br><br> để xuống 2 dòng tạo khoảng trống thoáng mắt
        // 4. Nhãn của AI Tutor in đậm kèm câu trả lời đã được định dạng
        String fullText =
                "<b>" + questionLabel + "</b><br>"
                        + escapeHtml(question)
                        + "<br><br>"
                        + "<b>EnglishAI Tutor:</b><br>"
                        + formatAiResponse(response);

        // HtmlCompat.fromHtml: Biên dịch chuỗi chứa các thẻ HTML thành đối tượng Spanned
        // Giúp TextView trên Android có thể hiểu và hiển thị đúng các hiệu ứng in đậm, xuống dòng
        return HtmlCompat.fromHtml(
                fullText,
                HtmlCompat.FROM_HTML_MODE_COMPACT
        );
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 2: CHUYỂN ĐỔI MARKDOWN CỦA GEMINI SANG HTML
    // =========================================================================
    // AI Gemini thường trả về văn bản dạng Markdown (dùng dấu ** để in đậm, dấu # làm tiêu đề)
    // Hàm này làm nhiệm vụ dọn dẹp và dịch sang định dạng HTML mà Android đọc được
    private static String formatAiResponse(String response) {
        // Kiểm tra an toàn: Nếu AI bị lỗi trả về null hoặc chuỗi rỗng -> Hiển thị thông báo mặc định
        if (response == null || response.trim().isEmpty()) {
            return "AI chưa trả về nội dung.";
        }

        String formatted = response.trim();

        // 1. DÙNG BIỂU THỨC CHÍNH QUY (REGEX) ĐỂ XỬ LÝ IN ĐẬM
        // "\\*\\*(.+?)\\*\\*": Tìm tất cả các đoạn văn bản nằm giữa 2 dấu ** (ví dụ: **hello**)
        // "<b>$1</b>": Thay thế bằng thẻ HTML <b>hello</b> ($1 đại diện cho nội dung giữ lại bên trong)
        formatted = formatted.replaceAll(
                "\\*\\*(.+?)\\*\\*",
                "<b>$1</b>"
        );

        // 2. DỌN DẸP SẠCH LỖI NGỮ CẢNH CỦA LLM (PROMPT ARTIFACTS)
        // AI Gemini đôi khi tự động sinh ra các thẻ code block hoặc dấu tiêu đề Markdown thừa
        // Lệnh replace giúp cắt bỏ toàn bộ các ký tự rác này cho văn bản sạch sẽ
        formatted = formatted
                .replace("```html", "")
                .replace("```", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "");

        // 3. CHUYỂN DẤU XUỐNG DÒNG
        // Trong lập trình chuỗi, \n là xuống dòng, nhưng trong HTML phải dùng thẻ <br> mới có tác dụng
        formatted = formatted.replace("\n", "<br>");

        return formatted;
    }

    // =========================================================================
    // HÀM BẢO MẬT & LẬP TRÌNH PHÒNG VỆ: CHỐNG LỖI HIỆN THỊ HTML (HTML ESCAPING)
    // =========================================================================
    // Nếu người dùng vô tình (hoặc cố ý) nhập các ký tự đặc biệt như <, > vào ô câu hỏi
    // (Ví dụ nhập: "<script>hoặc <abc>"), TextView có thể hiểu nhầm đó là thẻ HTML gây vỡ bố cục giao diện.
    // Hàm này chuyển đổi các ký tự nhạy cảm đó thành mã an toàn (HTML Entities).
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        // & -> &amp; | < -> &lt; (less than) | > -> &gt; (greater than)
        // Giúp chữ "<hello>" hiển thị nguyên vẹn lên màn hình như một văn bản bình thường, không bị vỡ giao diện
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}