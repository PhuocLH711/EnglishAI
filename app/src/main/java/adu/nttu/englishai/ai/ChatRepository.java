package adu.nttu.englishai.ai;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

import adu.nttu.englishai.models.AiMessage;

// =========================================================================
// CHAT REPOSITORY: Kho chứa dữ liệu, chuyên trách giao tiếp với Firebase Firestore
// =========================================================================
// Áp dụng Repository Pattern giúp tách biệt hoàn toàn logic cơ sở dữ liệu ra khỏi giao diện (Activity/Fragment)
public class ChatRepository {

    // =========================================================================
    // CÁC INTERFACE CALLBACK (GIAO TIẾP BẤT ĐỒNG BỘ - ASYNCHRONOUS CALLBACKS)
    // =========================================================================
    // Vì kết nối mạng tới Firebase tốn thời gian, code không thể chờ (chạy đồng bộ) mà phải chạy ngầm.
    // Các Interface này làm nhiệm vụ "người đưa thư": khi mạng tải xong hoặc bị lỗi,
    // Repository sẽ gọi callback để báo tin ngược lại cho Activity biết đường cập nhật giao diện.
    public interface OperationCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    public interface ConversationsCallback {
        void onSuccess(
                com.google.firebase.firestore.QuerySnapshot snapshots
        );

        void onError(String errorMessage);
    }

    public interface MessagesCallback {
        void onSuccess(
                com.google.firebase.firestore.QuerySnapshot snapshots
        );

        void onError(String errorMessage);
    }

    public interface DeleteCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    // Đối tượng xác thực tài khoản và thao tác cơ sở dữ liệu đám mây
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    // Constructor: Khởi tạo kết nối Firebase
    public ChatRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    // Hàm phụ trợ lấy người dùng hiện tại từ bộ nhớ đệm
    private FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    // Kiểm tra xem người dùng đã đăng nhập hay chưa (bảo vệ các tác vụ CRUD)
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 1: XÓA CUỘC TRÒ CHUYỆN (XỬ LÝ HẠN CHẾ CỦA NOSQL FIRESTORE)
    // =========================================================================
    // TRONG FIRESTORE: Khi bạn xóa 1 Document cha (aiConversations), các Document con (messages) bên trong
    // sẽ KHÔNG TỰ ĐỘNG BỊ XÓA mà trở thành "tài liệu mồ côi" gây rác bộ nhớ đám mây.
    // Do đó, quy trình xóa chuẩn ở đây được chia làm 3 bước nối tiếp (Chaining Tasks):
    // Bước 1: Lấy toàn bộ danh sách tin nhắn con -> Bước 2: Xóa hết tin nhắn con -> Bước 3: Xóa Document phòng chat cha.
    public void deleteConversation(
            @NonNull String conversationId,
            @NonNull DeleteCallback callback
    ) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        String userId = user.getUid();

        // Bước 1: Truy vấn lấy tất cả tài liệu trong subcollection "messages" của phòng chat này
        firestore.collection("users")
                .document(userId)
                .collection("aiConversations")
                .document(conversationId)
                .collection("messages")
                .get()
                .addOnSuccessListener(messageSnapshots ->
                        // Khi lấy được danh sách -> Chuyển sang Bước 2: Xóa tin nhắn con
                        deleteMessageDocuments(
                                userId,
                                conversationId,
                                messageSnapshots,
                                callback
                        )
                )
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    // Bước 2: Sử dụng kỹ thuật BATCH WRITE (Ghi theo lô) để xóa hàng loạt tin nhắn con
    private void deleteMessageDocuments(
            @NonNull String userId,
            @NonNull String conversationId,
            @NonNull com.google.firebase.firestore.QuerySnapshot snapshots,
            @NonNull DeleteCallback callback
    ) {
        // WriteBatch: Cho phép gom nhiều thao tác (lên tới 500 lệnh) lại và thực thi cùng 1 lúc
        // Giúp tiết kiệm số lần gọi mạng và đảm bảo tính toàn vẹn dữ liệu (hoặc cùng thành công, hoặc cùng thất bại)
        com.google.firebase.firestore.WriteBatch batch =
                firestore.batch();

        // Quét qua từng tin nhắn và đưa lệnh xóa vào lô (batch)
        for (com.google.firebase.firestore.DocumentSnapshot document
                : snapshots.getDocuments()) {

            batch.delete(document.getReference());
        }

        // Thực thi lệnh xóa hàng loạt (commit)
        batch.commit()
                .addOnSuccessListener(unused ->
                        // Bước 3: Xóa hết con xong thì mới gọi hàm xóa phòng chat cha
                        deleteConversationDocument(
                                userId,
                                conversationId,
                                callback
                        )
                )
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    // Bước 3: Xóa tài liệu phòng chat cha (aiConversations -> {conversationId})
    private void deleteConversationDocument(
            @NonNull String userId,
            @NonNull String conversationId,
            @NonNull DeleteCallback callback
    ) {
        firestore.collection("users")
                .document(userId)
                .collection("aiConversations")
                .document(conversationId)
                .delete()
                .addOnSuccessListener(unused ->
                        callback.onSuccess() // Hoàn tất 100% quy trình xóa -> Báo thành công về cho Activity
                )
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 2: TẠO MỘT PHÒNG CHAT MỚI TRÊN CLOUD
    // =========================================================================
    public void createConversation(
            @NonNull String conversationId,
            @NonNull String title,
            @NonNull String topicCode,
            @NonNull String topicName,
            @NonNull OperationCallback callback
    ) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Đóng gói siêu dữ liệu (metadata) của phòng chat vào Map
        Map<String, Object> conversationData =
                new HashMap<>();

        conversationData.put("id", conversationId);
        conversationData.put("title", title);
        conversationData.put("topicCode", topicCode);
        conversationData.put("topicName", topicName);
        conversationData.put("createdAt", currentTime);
        conversationData.put("updatedAt", currentTime); // Thời gian cập nhật ban đầu bằng thời gian tạo

        // Ghi lên đường dẫn: users -> {userId} -> aiConversations -> {conversationId}
        firestore.collection("users")
                .document(user.getUid())
                .collection("aiConversations")
                .document(conversationId)
                .set(conversationData)
                .addOnSuccessListener(unused ->
                        callback.onSuccess()
                )
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 3: LƯU TIN NHẮN & ĐỒNG BỘ THỜI GIAN PHÒNG CHAT
    // =========================================================================
    public void saveMessage(
            @NonNull String conversationId,
            @NonNull AiMessage message,
            @NonNull OperationCallback callback
    ) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        Map<String, Object> messageData = new HashMap<>();

        messageData.put("id", message.getId());
        messageData.put("role", message.getRole());       // AiMessage.ROLE_USER hoặc ROLE_AI
        messageData.put("content", message.getContent());
        messageData.put("createdAt", message.getCreatedAt());

        // Ghi tin nhắn vào subcollection: aiConversations -> {conversationId} -> messages -> {messageId}
        firestore.collection("users")
                .document(user.getUid())
                .collection("aiConversations")
                .document(conversationId)
                .collection("messages")
                .document(message.getId())
                .set(messageData)
                .addOnSuccessListener(unused ->
                        // KỸ THUẬT QUAN TRỌNG: Mỗi khi có tin nhắn mới, phải cập nhật lại biến updatedAt của phòng chat cha
                        // Để phòng chat này được đẩy lên đứng đầu danh sách trong Lịch sử gần đây
                        updateConversationTime(
                                user.getUid(),
                                conversationId,
                                callback
                        )
                )
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    // Hàm phụ: Cập nhật thời gian hoạt động mới nhất cho phòng chat
    private void updateConversationTime(
            String userId,
            String conversationId,
            OperationCallback callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        firestore.collection("users")
                .document(userId)
                .collection("aiConversations")
                .document(conversationId)
                .update(updates) // Chỉ cập nhật trường updatedAt, giữ nguyên các trường khác như title, topicCode
                .addOnSuccessListener(unused ->
                        callback.onSuccess()
                )
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 4: TẢI DANH SÁCH LỊCH SỬ (CÓ SẮP XẾP VÀ GIỚI HẠN)
    // =========================================================================
    public void loadRecentConversations(
            int limit,
            @NonNull ConversationsCallback callback
    ) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        // Truy vấn danh sách phòng chat của user hiện tại
        firestore.collection("users")
                .document(user.getUid())
                .collection("aiConversations")
                .orderBy(
                        "updatedAt",
                        Query.Direction.DESCENDING // Sắp xếp giảm dần theo updatedAt (phòng chat mới nhắn tin sẽ đứng đầu)
                )
                .limit(limit) // Giới hạn số lượng tải (ví dụ 50) để tối ưu băng thông và tốc độ app
                .get()
                .addOnSuccessListener(callback::onSuccess) // Truyền thẳng kết quả QuerySnapshot về cho Activity
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    // Tải toàn bộ chi tiết đoạn chat của 1 phòng hội thoại cụ thể
    public void loadMessages(
            @NonNull String conversationId,
            @NonNull MessagesCallback callback
    ) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("aiConversations")
                .document(conversationId)
                .collection("messages")
                .orderBy(
                        "createdAt",
                        Query.Direction.ASCENDING // Sắp xếp tăng dần theo thời gian tạo (tin cũ ở trên, tin mới ở dưới cùng)
                )
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    // Hàm phụ gia công câu báo lỗi cho thân thiện với người dùng
    private String getErrorMessage(Exception exception) {
        if (exception.getMessage() == null) {
            return "Không thể xử lý dữ liệu cuộc trò chuyện.";
        }

        return exception.getMessage();
    }
}