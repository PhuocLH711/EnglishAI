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

    // =========================================================================
    // HÀM 1: CHUẨN BỊ MỘT PHÒNG CHAT MỚI (LAZY INITIALIZATION)
    // =========================================================================
    // LƯU Ý QUAN TRỌNG: Hàm này CHỈ TẠO ID trong RAM điện thoại chứ CHƯA GỌI FIREBASE!
    // Tránh việc người dùng bấm tạo phòng mới rồi thoát ra ngay làm database bị rác phòng trống.
    public void startNewConversation(
            @NonNull String topicCode,
            @NonNull String topicName
    ) {
        // Tạo một mã UUID ngẫu nhiên duy nhất cho phòng chat
        currentConversationId =
                UUID.randomUUID().toString();

        currentTopicCode = topicCode;
        currentTopicName = topicName;

        // Đặt lại cờ: Phòng này mới chỉ ở trên RAM, chưa có trên Cloud Firestore
        conversationCreated = false;
        isCreatingConversation = false;

        pendingMessages.clear(); // Xóa sạch hàng đợi tin nhắn cũ
    }

    // =========================================================================
    // HÀM 2: MỞ LẠI MỘT PHÒNG CHAT CŨ TỪ LỊCH SỬ
    // =========================================================================
    public void openExistingConversation(
            @NonNull String conversationId,
            @NonNull String topicCode,
            @NonNull String topicName
    ) {
        currentConversationId = conversationId;
        currentTopicCode = topicCode;
        currentTopicName = topicName;

        // Đặt cờ = true vì phòng cũ này chắc chắn đã tồn tại trên Firebase rồi
        conversationCreated = true;
        isCreatingConversation = false;

        pendingMessages.clear();
    }

    // =========================================================================
    // HÀM QUAN TRỌNG NHẤT: ĐIỀU PHỐI LƯU TIN NHẮN (MESSAGE ROUTER & BUFFERING)
    // =========================================================================
    public void saveMessage(
            @NonNull AiMessage message
    ) {
        // Kiểm tra bảo mật: Nếu chưa đăng nhập thì không lưu vào Cloud
        if (!chatRepository.isUserLoggedIn()) {
            return;
        }

        // TRƯỜNG HỢP 1: Nếu phòng chat đã được tạo thành công trên Firebase từ trước
        // -> Gửi thẳng tin nhắn này lên Cloud lưu luôn
        if (conversationCreated) {
            saveMessageDirectly(message);
            return;
        }

        // TRƯỜNG HỢP 2: Phòng chat CHƯA CÓ trên Cloud (Đây là tin nhắn đầu tiên của phòng mới)
        // -> Nhốt tạm tin nhắn này vào hàng đợi bộ nhớ tạm (pendingMessages)
        pendingMessages.add(message);

        // CƠ CHẾ KHÓA CHẶN (Race Condition Protection):
        // Nếu hệ thống đang trong quá trình gọi mạng tạo phòng rồi thì không gọi tạo nữa, chỉ chờ thôi
        if (isCreatingConversation) {
            return;
        }

        // BÍ KÍP ĐỒ ÁN: Chỉ khi tin nhắn này là do NGƯỜI DÙNG gửi (isUser = true) thì mới kích hoạt tạo phòng!
        // Nếu là lời chào tự động của AI thì bỏ qua, không tạo phòng trên Firebase.
        if (!message.isUser()) {
            return;
        }

        // Bắt đầu gọi truy vấn lên Firebase để tạo phòng chat thực tế
        createConversation(message);
    }

    // =========================================================================
    // HÀM QUAN TRỌNG: TẠO PHÒNG CHAT TRÊN CLOUD & XẢ HÀNG ĐỢI (BUFFER FLUSHING)
    // =========================================================================
    private void createConversation(
            @NonNull AiMessage firstUserMessage
    ) {
        // Bật cờ khóa: Đang gọi mạng tạo phòng, các tin nhắn đến sau hãy chờ trong hàng đợi
        isCreatingConversation = true;

        // Tự động sinh tiêu đề phòng chat dựa vào câu hỏi đầu tiên của người dùng
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