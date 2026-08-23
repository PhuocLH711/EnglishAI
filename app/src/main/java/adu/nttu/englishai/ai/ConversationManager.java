package adu.nttu.englishai.ai;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.UUID;

import adu.nttu.englishai.models.AiMessage;

// =========================================================================
// CONVERSATION MANAGER: Bộ quản lý trạng thái và vòng đời phiên hội thoại AI
// =========================================================================
// Lớp này đóng vai trò như "Người điều phối" (Mediator) nằm giữa Giao diện (AiTutorActivity)
// và Kho lưu trữ (ChatRepository), chịu trách nhiệm tạo phòng, nhớ ID và lưu tin nhắn.
public class ConversationManager {

    // =========================================================================
    // INTERFACE CALLBACK (GIAO TIẾP VỚI ACTIVITY)
    // =========================================================================
    // Báo lại cho Activity biết khi phòng chat đã được tạo thành công trên Cloud hoặc có lỗi xảy ra
    public interface ConversationListener {
        void onConversationCreated(
                String conversationId
        );

        void onSaveError(
                String errorMessage
        );
    }

    // Kho giao tiếp Firebase và Bộ lắng nghe sự kiện
    private final ChatRepository chatRepository;
    private final ConversationListener listener;

    // =========================================================================
    // HÀNG ĐỢI BỘ NHỚ TẠM (MESSAGE BUFFER / PENDING LIST)
    // =========================================================================
    // Khi phòng chat trên Firebase chưa kịp tạo xong (do mạng chậm), các tin nhắn gửi đi
    // sẽ được nhốt tạm vào danh sách này. Tạo phòng xong mới lấy ra lưu tiếp.
    private final ArrayList<AiMessage> pendingMessages =
            new ArrayList<>();

    // Các biến lưu thông tin của phiên hội thoại hiện tại
    private String currentConversationId;
    private String currentTopicCode;
    private String currentTopicName;

    // =========================================================================
    // CỜ KIỂM SOÁT TRẠNG THÁI (STATE MACHINE FLAGS)
    // =========================================================================
    private boolean conversationCreated;     // Đánh dấu phòng chat đã thực sự tồn tại trên Firebase chưa
    private boolean isCreatingConversation;  // Đánh dấu hệ thống có đang trong quá trình gọi API tạo phòng không

    // Constructor: Khởi tạo bộ quản lý và tự động tạo sẵn một phiên hội thoại trống
    public ConversationManager(
            @NonNull ChatRepository chatRepository,
            @NonNull ConversationListener listener
    ) {
        this.chatRepository = chatRepository;
        this.listener = listener;

        // Mặc định vào app là khởi tạo phiên chủ đề Hỏi tự do (GENERAL)
        startNewConversation(
                AiTopicManager.GENERAL,
                AiTopicManager.getTopicName(
                        AiTopicManager.GENERAL
                )
        );
    }


    public void startNewConversation(
            @NonNull String topicCode,
            @NonNull String topicName
    ) {

        currentConversationId =
                UUID.randomUUID().toString();

        currentTopicCode = topicCode;
        currentTopicName = topicName;


        conversationCreated = false;
        isCreatingConversation = false;

        pendingMessages.clear();
    }


    public void openExistingConversation(
            @NonNull String conversationId,
            @NonNull String topicCode,
            @NonNull String topicName
    ) {
        currentConversationId = conversationId;
        currentTopicCode = topicCode;
        currentTopicName = topicName;


        conversationCreated = true;
        isCreatingConversation = false;

        pendingMessages.clear();
    }
    public void saveMessage(
            @NonNull AiMessage message
    ) {

        if (!chatRepository.isUserLoggedIn()) {
            return;
        }


        if (conversationCreated) {
            saveMessageDirectly(message);
            return;
        }


        pendingMessages.add(message);


        if (isCreatingConversation) {
            return;
        }


        if (!message.isUser()) {
            return;
        }


        createConversation(message);
    }


    private void createConversation(
            @NonNull AiMessage firstUserMessage
    ) {

        isCreatingConversation = true;


        String title =
                ConversationTitleGenerator.generate(
                        firstUserMessage.getContent()
                );

        // Gọi Repository tạo Document phòng chat trên Firebase Firestore
        chatRepository.createConversation(
                currentConversationId,
                title,
                currentTopicCode,
                currentTopicName,
                new ChatRepository.OperationCallback() {

                    @Override
                    public void onSuccess() {
                        // KHI TẠO PHÒNG THÀNH CÔNG:
                        conversationCreated = true;      // Mở khóa cờ trạng thái
                        isCreatingConversation = false;

                        // KỸ THUẬT XẢ HÀNG ĐỢI (Flushing Buffer):
                        // Sao chép toàn bộ tin nhắn đang bị nhốt tạm trong pendingMessages ra một danh sách riêng
                        ArrayList<AiMessage> messagesToSave =
                                new ArrayList<>(
                                        pendingMessages
                                );

                        pendingMessages.clear(); // Xóa trống hàng đợi

                        // Quét qua danh sách và lưu tuần tự từng tin nhắn lên Firebase
                        for (AiMessage pendingMessage
                                : messagesToSave) {

                            saveMessageDirectly(
                                    pendingMessage
                            );
                        }

                        // Báo về cho Activity biết phòng đã tạo xong
                        listener.onConversationCreated(
                                currentConversationId
                        );
                    }

                    @Override
                    public void onError(
                            String errorMessage
                    ) {
                        // Nếu tạo phòng thất bại -> Tắt cờ khóa và báo lỗi về UI
                        isCreatingConversation = false;

                        listener.onSaveError(
                                errorMessage
                        );
                    }
                }
        );
    }

    // Hàm phụ: Gọi trực tiếp xuống Repository để lưu 1 tin nhắn vào phòng chat hiện tại
    private void saveMessageDirectly(
            @NonNull AiMessage message
    ) {
        chatRepository.saveMessage(
                currentConversationId,
                message,
                new ChatRepository.OperationCallback() {

                    @Override
                    public void onSuccess() {
                        // Đã lưu thành công lên Cloud Firestore
                    }

                    @Override
                    public void onError(
                            String errorMessage
                    ) {
                        listener.onSaveError(
                                errorMessage
                        );
                    }
                }
        );
    }

    // Các hàm Getter lấy thông tin ID và trạng thái phòng chat
    public String getCurrentConversationId() {
        return currentConversationId;
    }

    public boolean isConversationCreated() {
        return conversationCreated;
    }
}