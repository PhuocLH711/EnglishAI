package adu.nttu.englishai.ai;

import androidx.annotation.NonNull;

// Các thư viện xử lý bất đồng bộ (Asynchronous Futures) của Google Guava
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
// Các lớp tích hợp AI Gemini thông qua nền tảng Firebase AI
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.ChatFutures;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import java.util.List;

import adu.nttu.englishai.models.AiMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// =========================================================================
// GEMINI MANAGER: Bộ điều khiển trung tâm kết nối và xử lý AI Gemini
// =========================================================================
public class GeminiManager {

    // Giao tiếp Callback trả kết quả (Thành công/Thất bại) về cho Activity/Fragment
    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    // Bộ luồng thực thi (Executor): Quản lý luồng chạy ngầm để gọi API không làm đơ giao diện
    private final Executor executor;

    // Các đối tượng quản lý mô hình Gemini và phiên trò chuyện (Chat Session)
    private GenerativeModelFutures model;
    private ChatFutures chat;

    private String currentTopic = "GENERAL";

    // Constructor: Khởi tạo luồng đơn (SingleThreadExecutor) và tạo phòng chat chủ đề mặc định
    public GeminiManager() {
        executor = Executors.newSingleThreadExecutor();
        createChatSession("GENERAL");
    }

    /**
     * Gọi khi người dùng đổi chủ đề.
     * Chat cũ sẽ được reset để tránh lẫn ngữ cảnh.
     */
    public void changeTopic(@NonNull String topic) {
        if (!topic.equals(currentTopic)) {
            createChatSession(topic);
        }
    }

    /**
     * Xóa ngữ cảnh và bắt đầu lại chủ đề hiện tại.
     */
    public void resetCurrentChat() {
        createChatSession(currentTopic);
    }

    public String getCurrentTopic() {
        return currentTopic;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 1: KHỞI TẠO PHIÊN CHAT & THIẾT LẬP CẤU HÌNH AI (AI CONFIG)
    // =========================================================================
    private void createChatSession(@NonNull String topic) {
        currentTopic = topic;

        // 1. Cấu hình thông số sinh văn bản của Gemini (GenerationConfig):
        // - setCandidateCount(1): Chỉ lấy 1 câu trả lời tốt nhất để tiết kiệm băng thông
        // - setMaxOutputTokens(700): Giới hạn độ dài câu trả lời tối đa 700 token (khoảng 500 từ), tránh AI nói dài dòng
        // - setTemperature: Điều chỉnh "độ sáng tạo/ngẫu nhiên" tùy theo chủ đề học
        GenerationConfig generationConfig =
                GenerationConfig.builder()
                        .setCandidateCount(1)
                        .setMaxOutputTokens(700)
                        .setTemperature(getTemperature(topic))
                        .build();

        // 2. Khởi tạo mô hình Gemini thông qua Firebase AI backend
        // Ở đây sử dụng model "gemini-3.1-flash-lite": Phiên bản siêu nhanh, nhẹ, tối ưu cho ứng dụng di động
        GenerativeModel generativeModel =
                FirebaseAI.getInstance(
                        GenerativeBackend.googleAI()
                ).generativeModel(
                        "gemini-3.1-flash-lite",
                        generationConfig
                );

        model = GenerativeModelFutures.from(generativeModel);

        // 3. KỸ THUẬT SYSTEM PROMPT INJECTION (Tiêm câu lệnh hệ thống qua lịch sử giả):
        // Để "nhập vai" cho AI (trở thành gia sư tiếng Anh), ta tạo sẵn 1 lượt hội thoại giả ban đầu:
        // - Lượt của User: Gửi bộ quy tắc và hướng dẫn (System Instruction)
        // - Lượt của Model: Xác nhận "Đã hiểu" để AI khóa chặt vai diễn trong suốt phiên chat
        List<Content> initialHistory = new ArrayList<>();

        Content systemContext = new Content.Builder()
                .setRole("user")
                .addText(buildInitialInstruction(topic))
                .build();

        Content modelConfirmation = new Content.Builder()
                .setRole("model")
                .addText(
                        "Đã hiểu. Tôi sẽ hỗ trợ đúng theo chế độ này, "
                                + "ghi nhớ các câu hỏi tiếp theo và trả lời rõ ràng."
                )
                .build();

        initialHistory.add(systemContext);
        initialHistory.add(modelConfirmation);

        // Khởi động phiên chat với bộ nhớ ban đầu đã được tiêm quy tắc
        chat = model.startChat(initialHistory);
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 2: GỬI TIN NHẮN CHO AI VÀ XỬ LÝ KẾT QUẢ TRẢ VỀ
    // =========================================================================
    public void sendMessage(
            @NonNull String userQuestion,
            @NonNull GeminiCallback callback
    ) {
        String cleanedQuestion = userQuestion.trim();

        if (cleanedQuestion.isEmpty()) {
            callback.onError("Vui lòng nhập câu hỏi.");
            return;
        }

        // Đóng gói câu hỏi của người dùng kèm lời nhắc ngữ cảnh (Prompt Wrapping)
        Content message = new Content.Builder()
                .setRole("user")
                .addText(buildMessagePrompt(cleanedQuestion))
                .build();

        // Gửi tin nhắn bất đồng bộ qua Futures (Không làm đơ màn hình điện thoại)
        ListenableFuture<GenerateContentResponse> future =
                chat.sendMessage(message);

        // Lắng nghe kết quả từ Google Cloud trả về
        Futures.addCallback(
                future,
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String text =
                                result != null
                                        ? result.getText()
                                        : null;

                        if (text == null || text.trim().isEmpty()) {
                            callback.onError(
                                    "AI chưa tạo được câu trả lời."
                            );
                            return;
                        }

                        // Trả văn bản AI trả lời về cho tầng Giao diện (UI)
                        callback.onSuccess(text.trim());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        String message = throwable.getMessage();

                        // XỬ LÝ LỖI THÔNG MINH: Nếu câu trả lời bị cắt ngang do vượt quá 700 token
                        // -> Báo lỗi thân thiện hướng dẫn người học cách hỏi ngắn lại
                        if (message != null
                                && message.contains("MAX_TOKENS")) {

                            callback.onError(
                                    "Câu trả lời bị giới hạn vì quá dài. "
                                            + "Hãy hỏi cụ thể hơn hoặc yêu cầu AI chia thành từng phần."
                            );
                            return;
                        }

                        callback.onError(
                                message != null
                                        ? message
                                        : "Không thể kết nối với AI."
                        );
                    }
                },
                executor // Chạy trên luồng phụ của Executor
        );
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 3: ĐIỀU CHỈNH TEMPERATURE (ĐỘ SÁNG TẠO / NGUYÊN TẮC CỦA AI)
    // =========================================================================
    // Temperature dao động từ 0.0 đến 1.0:
    // - Chỉ số THẤP (0.25): AI trả lời chính xác, rập khuôn, chuẩn chỉ theo nguyên tắc (Hợp Ngữ pháp, Từ vựng, Dịch).
    // - Chỉ số CAO (0.7): AI trả lời sáng tạo, linh hoạt, từ ngữ phong phú (Hợp Giao tiếp hội thoại).
    private float getTemperature(String topic) {
        switch (topic) {
            case "CONVERSATION":
                return 0.7f; // Giao tiếp cần tự nhiên, sáng tạo

            case "WRITING":
                return 0.5f; // Luyện viết cần cân bằng giữa sáng tạo và chuẩn ngữ pháp

            case "GRAMMAR":
            case "VOCABULARY":
            case "PRONUNCIATION":
            case "TRANSLATION":
                return 0.25f; // Ngữ pháp, từ vựng, dịch thuật cần chính xác tuyệt đối, không được bịa

            default:
                return 0.45f; // Mức cân bằng cho trò chuyện chung
        }
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 4: KHÔI PHỤC NGỮ CẢNH CHAT TỪ DATABASE (CONTEXT RESTORATION)
    // =========================================================================
    public void restoreConversation(
            @NonNull String topic,
            @NonNull List<AiMessage> savedMessages
    ) {
        currentTopic = topic;

        GenerationConfig generationConfig =
                GenerationConfig.builder()
                        .setCandidateCount(1)
                        .setMaxOutputTokens(700)
                        .setTemperature(getTemperature(topic))
                        .build();

        GenerativeModel generativeModel =
                FirebaseAI.getInstance(
                        GenerativeBackend.googleAI()
                ).generativeModel(
                        "gemini-3.1-flash-lite",
                        generationConfig
                );

        model = GenerativeModelFutures.from(generativeModel);

        List<Content> history = new ArrayList<>();

        // 1. Tiêm lại bộ quy tắc hướng dẫn của chủ đề tương ứng
        history.add(
                new Content.Builder()
                        .setRole("user")
                        .addText(buildInitialInstruction(topic))
                        .build()
        );

        history.add(
                new Content.Builder()
                        .setRole("model")
                        .addText(
                                "Đã hiểu. Tôi sẽ hỗ trợ theo đúng chủ đề này."
                        )
                        .build()
        );

        /*
         * 2. KỸ THUẬT CẮT TỈA CỬA SỔ BỘ NHỚ (CONTEXT WINDOW PRUNING):
         * Chỉ khôi phục tối đa 30 tin nhắn gần nhất để tránh lịch sử quá dài.
         * LÝ DO: Nếu tải hàng trăm tin nhắn cũ vào bộ nhớ Gemini, app sẽ bị:
         * - Tốn rất nhiều tiền phí tài nguyên (Token consumption).
         * - Xử lý rất chậm vì AI phải đọc lại toàn bộ lịch sử dài.
         * - AI bị "nhiễu thông tin" (Hallucination), quên mất chủ đề hiện tại.
         */
        int startIndex = Math.max(
                savedMessages.size() - 30,
                0
        );

        for (int index = startIndex;
             index < savedMessages.size();
             index++) {

            AiMessage message = savedMessages.get(index);

            if (message == null
                    || message.getContent() == null
                    || message.getContent().trim().isEmpty()) {
                continue;
            }

            // Chuyển đổi role từ AiMessage model sang role chuẩn của Gemini ("user" hoặc "model")
            String role = message.isUser()
                    ? "user"
                    : "model";

            history.add(
                    new Content.Builder()
                            .setRole(role)
                            .addText(message.getContent().trim())
                            .build()
            );
        }

        // Khởi tạo phòng chat với bộ nhớ 30 câu gần nhất
        chat = model.startChat(history);
    }

    // Hàm phụ: Bọc câu hỏi của người dùng thêm lời nhắc để AI hiểu ngữ cảnh các câu nói tắt (như "tiếp tục", "ví dụ đi")
    private String buildMessagePrompt(String question) {
        return "Tin nhắn mới của người học:\n"
                + question
                + "\n\nHãy dựa vào toàn bộ cuộc trò chuyện trước đó. "
                + "Nếu người học nói như 'cho thêm ví dụ', "
                + "'ra bài tập', 'giải thích lại', 'tiếp tục' "
                + "thì phải hiểu họ đang nói về nội dung vừa trao đổi.";
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 5: KỸ THUẬT PROMPT ENGINEERING (SYSTEM PROMPT BUILDER)
    // =========================================================================
    // Thiết lập vai trò (Persona), nhiệm vụ (Task) và giới hạn (Constraints) cho AI theo từng chế độ học
    private String buildInitialInstruction(String topic) {
        String specializedInstruction;

        switch (topic) {
            case "VOCABULARY":
                specializedInstruction =
                        "Chế độ chuyên sâu: TỪ VỰNG.\n"
                                + "Giải thích nghĩa, IPA, từ loại, cách dùng "
                                + "và ví dụ thực tế."
                                + "\nNếu người học hỏi thêm, hãy tiếp tục từ từ vựng đang nói tới.";

                break;

            case "GRAMMAR":
                specializedInstruction =
                        "Chế độ chuyên sâu: NGỮ PHÁP.\n"
                                + "Giải thích theo cấu trúc:"
                                + "\n**Tên ngữ pháp**"
                                + "\n**Công thức**"
                                + "\n**Cách dùng**"
                                + "\n**Dấu hiệu nhận biết**"
                                + "\n**Ví dụ rõ ràng kèm nghĩa tiếng Việt**."
                                + "\nCuối câu trả lời nên hỏi:"
                                + "\nBạn muốn xem thêm ví dụ hay làm bài tập ngắn?"
                                + "\nKhông tự tạo bài tập nếu người học chưa yêu cầu.";

                break;

            case "PRONUNCIATION":
                specializedInstruction =
                        "Chế độ chuyên sâu: PHÁT ÂM.\n"
                                + "Nêu IPA, trọng âm, khẩu hình, cách đặt lưỡi "
                                + "và lỗi người Việt thường mắc."
                                + "\nDùng cách giải thích dễ thực hành.";

                break;

            case "CONVERSATION":
                specializedInstruction =
                        "Chế độ chuyên sâu: GIAO TIẾP.\n"
                                + "Hội thoại tự nhiên bằng tiếng Anh."
                                + "\nNếu người học sai, sửa ngắn gọn bằng tiếng Việt "
                                + "rồi tiếp tục hội thoại."
                                + "\nLuôn kết thúc bằng một câu hỏi để người học trả lời.";

                break;

            case "WRITING":
                specializedInstruction =
                        "Chế độ chuyên sâu: LUYỆN VIẾT.\n"
                                + "Phân tích câu người học, chỉ lỗi, sửa câu "
                                + "và đưa phiên bản tự nhiên hơn."
                                + "\nIn đậm phần cần chú ý.";

                break;

            case "TRANSLATION":
                specializedInstruction =
                        "Chế độ chuyên sâu: DỊCH ANH-VIỆT VÀ VIỆT-ANH.\n"
                                + "Một từ đơn: trả nghĩa chính và từ loại thật ngắn."
                                + "\nMột câu: trả bản dịch tự nhiên."
                                + "\nChỉ giải thích khi người học yêu cầu.";

                break;

            default:
                specializedInstruction =
                        "Chế độ CHAT CHUNG.\n"
                                + "Trò chuyện linh hoạt như một trợ lý thông minh."
                                + "\nCó thể trả lời câu hỏi về học tập, đời sống "
                                + "và tiếng Anh."
                                + "\nKhi câu hỏi liên quan tiếng Anh, hãy ưu tiên "
                                + "giải thích dễ hiểu cho người học."
                                + "\nKhông ép mọi câu hỏi vào một khuôn cố định.";

                break;
        }

        // Ghép nối cấu trúc Prompt chuẩn: Persona -> Nhiệm vụ chuyên sâu -> Quy tắc ràng buộc (Guardrails)
        return "Bạn là EnglishAI Tutor, một trợ giảng thông minh trong ứng dụng học tiếng Anh.\n\n"
                + specializedInstruction
                + "\n\nQuy tắc chung:"
                + "\n- Hiểu ngữ cảnh của toàn bộ hội thoại."
                + "\n- Trả lời trực tiếp, không giới thiệu bản thân."
                + "\n- Câu đơn giản trả lời ngắn."
                + "\n- Câu phức tạp được giải thích có cấu trúc."
                + "\n- Không bịa thông tin."
                + "\n- Nếu câu hỏi chưa rõ, hãy hỏi lại một câu ngắn."
                + "\n- Dùng Markdown để in đậm từ khóa bằng **...**."
                + "\n- Không dùng quá nhiều biểu tượng."
                + "\n- Ưu tiên tiếng Việt, trừ chế độ giao tiếp hoặc khi người học yêu cầu tiếng Anh.";
    }
}