package adu.nttu.englishai.ai;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

import adu.nttu.englishai.models.AiMessage;

public class ChatRepository {

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


    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public ChatRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

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

        firestore.collection("users")
                .document(userId)
                .collection("aiConversations")
                .document(conversationId)
                .collection("messages")
                .get()
                .addOnSuccessListener(messageSnapshots ->
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
    private void deleteMessageDocuments(
            @NonNull String userId,
            @NonNull String conversationId,
            @NonNull com.google.firebase.firestore.QuerySnapshot snapshots,
            @NonNull DeleteCallback callback
    ) {
        com.google.firebase.firestore.WriteBatch batch =
                firestore.batch();

        for (com.google.firebase.firestore.DocumentSnapshot document
                : snapshots.getDocuments()) {

            batch.delete(document.getReference());
        }

        batch.commit()
                .addOnSuccessListener(unused ->
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
                        callback.onSuccess()
                )
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }
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

        Map<String, Object> conversationData =
                new HashMap<>();

        conversationData.put("id", conversationId);
        conversationData.put("title", title);
        conversationData.put("topicCode", topicCode);
        conversationData.put("topicName", topicName);
        conversationData.put("createdAt", currentTime);
        conversationData.put("updatedAt", currentTime);

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
        messageData.put("role", message.getRole());
        messageData.put("content", message.getContent());
        messageData.put("createdAt", message.getCreatedAt());

        firestore.collection("users")
                .document(user.getUid())
                .collection("aiConversations")
                .document(conversationId)
                .collection("messages")
                .document(message.getId())
                .set(messageData)
                .addOnSuccessListener(unused ->
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
                .update(updates)
                .addOnSuccessListener(unused ->
                        callback.onSuccess()
                )
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    public void loadRecentConversations(
            int limit,
            @NonNull ConversationsCallback callback
    ) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("Bạn chưa đăng nhập.");
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("aiConversations")
                .orderBy(
                        "updatedAt",
                        Query.Direction.DESCENDING
                )
                .limit(limit)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

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
                        Query.Direction.ASCENDING
                )
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(exception ->
                        callback.onError(
                                getErrorMessage(exception)
                        )
                );
    }

    private String getErrorMessage(Exception exception) {
        if (exception.getMessage() == null) {
            return "Không thể xử lý dữ liệu cuộc trò chuyện.";
        }

        return exception.getMessage();
    }
}