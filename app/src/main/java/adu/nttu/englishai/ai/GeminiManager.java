package adu.nttu.englishai.ai;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.ChatFutures;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import adu.nttu.englishai.models.AiMessage;

public class GeminiManager {

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public interface GeminiStreamCallback {
        void onChunk(String chunk);
        void onComplete(String fullResponse);
        void onError(String errorMessage);
    }

    private final Executor executor;
    private GenerativeModelFutures model;
    private ChatFutures chat;
    private String currentTopic = "GENERAL";

    public GeminiManager() {
        executor = Executors.newSingleThreadExecutor();
        createChatSession("GENERAL");
    }

    public void changeTopic(@NonNull String topic) {
        if (!topic.equals(currentTopic)) {
            createChatSession(topic);
        }
    }

    public void resetCurrentChat() {
        createChatSession(currentTopic);
    }

    public String getCurrentTopic() {
        return currentTopic;
    }

    private void createChatSession(@NonNull String topic) {
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

        List<Content> initialHistory = new ArrayList<>();

        Content systemContext =
                new Content.Builder()
                        .setRole("user")
                        .addText(buildInitialInstruction(topic))
                        .build();

        Content modelConfirmation =
                new Content.Builder()
                        .setRole("model")
                        .addText(
                                "Đã hiểu. Tôi sẽ hỗ trợ đúng theo chế độ này, "
                                        + "ghi nhớ các câu hỏi tiếp theo và trả lời rõ ràng."
                        )
                        .build();

        initialHistory.add(systemContext);
        initialHistory.add(modelConfirmation);

        chat = model.startChat(initialHistory);
    }

    public void sendMessage(
            @NonNull String userQuestion,
            @NonNull GeminiCallback callback
    ) {
        String cleanedQuestion = userQuestion.trim();

        if (cleanedQuestion.isEmpty()) {
            callback.onError("Vui lòng nhập câu hỏi.");
            return;
        }

        Content message =
                new Content.Builder()
                        .setRole("user")
                        .addText(buildMessagePrompt(cleanedQuestion))
                        .build();

        ListenableFuture<GenerateContentResponse> future =
                chat.sendMessage(message);

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
                            callback.onError("AI chưa tạo được câu trả lời.");
                            return;
                        }

                        callback.onSuccess(text.trim());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        callback.onError(getFriendlyErrorMessage(throwable));
                    }
                },
                executor
        );
    }

    public void sendMessageStream(
            @NonNull String userQuestion,
            @NonNull GeminiStreamCallback callback
    ) {
        String cleanedQuestion = userQuestion.trim();

        if (cleanedQuestion.isEmpty()) {
            callback.onError("Vui lòng nhập câu hỏi.");
            return;
        }

        Content content =
                new Content.Builder()
                        .setRole("user")
                        .addText(buildMessagePrompt(cleanedQuestion))
                        .build();

        streamContent(content, callback);
    }

    public void sendImageMessageStream(
            @NonNull Bitmap bitmap,
            String userQuestion,
            @NonNull GeminiStreamCallback callback
    ) {
        String cleanedQuestion =
                userQuestion == null
                        ? ""
                        : userQuestion.trim();

        if (cleanedQuestion.isEmpty()) {
            cleanedQuestion =
                    "Hãy nhận diện nội dung trong hình ảnh này "
                            + "và giải thích rõ ràng, dễ hiểu cho người học.";
        }

        Content content =
                new Content.Builder()
                        .setRole("user")
                        .addImage(bitmap)
                        .addText(buildMessagePrompt(cleanedQuestion))
                        .build();

        streamContent(content, callback);
    }

    private void streamContent(
            @NonNull Content content,
            @NonNull GeminiStreamCallback callback
    ) {
        final Publisher<GenerateContentResponse> publisher;

        try {
            publisher = chat.sendMessageStream(content);
        } catch (Exception exception) {
            callback.onError(getFriendlyErrorMessage(exception));
            return;
        }

        StringBuilder fullResponse = new StringBuilder();

        publisher.subscribe(
                new Subscriber<GenerateContentResponse>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(GenerateContentResponse result) {
                        if (result == null) {
                            return;
                        }

                        String chunk = result.getText();

                        if (chunk == null || chunk.isEmpty()) {
                            return;
                        }

                        fullResponse.append(chunk);
                        callback.onChunk(chunk);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        callback.onError(getFriendlyErrorMessage(throwable));
                    }

                    @Override
                    public void onComplete() {
                        String result =
                                fullResponse.toString().trim();

                        if (result.isEmpty()) {
                            callback.onError("AI chưa tạo được câu trả lời.");
                            return;
                        }

                        callback.onComplete(result);
                    }
                }
        );
    }

    private float getTemperature(String topic) {
        switch (topic) {
            case "CONVERSATION":
                return 0.7f;
            case "WRITING":
                return 0.5f;
            case "GRAMMAR":
            case "VOCABULARY":
            case "PRONUNCIATION":
            case "TRANSLATION":
                return 0.25f;
            default:
                return 0.45f;
        }
    }

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

        history.add(
                new Content.Builder()
                        .setRole("user")
                        .addText(buildInitialInstruction(topic))
                        .build()
        );

        history.add(
                new Content.Builder()
                        .setRole("model")
                        .addText("Đã hiểu. Tôi sẽ hỗ trợ theo đúng chủ đề này.")
                        .build()
        );

        int startIndex =
                Math.max(
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

            String role =
                    message.isUser()
                            ? "user"
                            : "model";

            history.add(
                    new Content.Builder()
                            .setRole(role)
                            .addText(message.getContent().trim())
                            .build()
            );
        }

        chat = model.startChat(history);
    }

    private String buildMessagePrompt(String question) {
        return "Tin nhắn mới của người học:\n"
                + question
                + "\n\nHãy dựa vào toàn bộ cuộc trò chuyện trước đó. "
                + "Nếu người học nói như 'cho thêm ví dụ', "
                + "'ra bài tập', 'giải thích lại', 'tiếp tục' "
                + "thì phải hiểu họ đang nói về nội dung vừa trao đổi. "
                + "Nếu tin nhắn có hình ảnh, hãy quan sát kỹ nội dung trong ảnh "
                + "và trả lời đúng câu hỏi của người học về ảnh đó.";
    }

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

        return "Bạn là EnglishAI Tutor, một trợ giảng thông minh trong ứng dụng học tiếng Anh.\n\n"
                + specializedInstruction
                + "\n\nQuy tắc chung:"
                + "\n- Hiểu ngữ cảnh của toàn bộ hội thoại."
                + "\n- Trả lời trực tiếp, không giới thiệu bản thân."
                + "\n- Câu đơn giản trả lời ngắn."
                + "\n- Câu phức tạp được giải thích có cấu trúc."
                + "\n- Khi có ảnh, hãy đọc/quan sát nội dung ảnh trước khi trả lời."
                + "\n- Nếu ảnh mờ hoặc không đủ thông tin, nói rõ thay vì đoán."
                + "\n- Không bịa thông tin."
                + "\n- Nếu câu hỏi chưa rõ, hãy hỏi lại một câu ngắn."
                + "\n- Dùng Markdown để in đậm từ khóa bằng **...**."
                + "\n- Không dùng quá nhiều biểu tượng."
                + "\n- Ưu tiên tiếng Việt, trừ chế độ giao tiếp hoặc khi người học yêu cầu tiếng Anh.";
    }

    private String getFriendlyErrorMessage(Throwable throwable) {
        if (throwable == null
                || throwable.getMessage() == null) {
            return "Không thể kết nối với AI.";
        }

        String message = throwable.getMessage();

        if (message.contains("MAX_TOKENS")) {
            return "Câu trả lời bị giới hạn vì quá dài. "
                    + "Hãy hỏi cụ thể hơn hoặc yêu cầu AI chia thành từng phần.";
        }

        return message;
    }
}