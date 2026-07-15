package adu.nttu.englishai.ai;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.UUID;

import adu.nttu.englishai.models.AiMessage;

public class ConversationManager {

    public interface ConversationListener {
        void onConversationCreated(
                String conversationId
        );

        void onSaveError(
                String errorMessage
        );
    }

    private final ChatRepository chatRepository;
    private final ConversationListener listener;

    private final ArrayList<AiMessage> pendingMessages =
            new ArrayList<>();

    private String currentConversationId;
    private String currentTopicCode;
    private String currentTopicName;

    private boolean conversationCreated;
    private boolean isCreatingConversation;

    public ConversationManager(
            @NonNull ChatRepository chatRepository,
            @NonNull ConversationListener listener
    ) {
        this.chatRepository = chatRepository;
        this.listener = listener;

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

        chatRepository.createConversation(
                currentConversationId,
                title,
                currentTopicCode,
                currentTopicName,
                new ChatRepository.OperationCallback() {

                    @Override
                    public void onSuccess() {
                        conversationCreated = true;
                        isCreatingConversation = false;

                        ArrayList<AiMessage> messagesToSave =
                                new ArrayList<>(
                                        pendingMessages
                                );

                        pendingMessages.clear();

                        for (AiMessage pendingMessage
                                : messagesToSave) {

                            saveMessageDirectly(
                                    pendingMessage
                            );
                        }

                        listener.onConversationCreated(
                                currentConversationId
                        );
                    }

                    @Override
                    public void onError(
                            String errorMessage
                    ) {
                        isCreatingConversation = false;

                        listener.onSaveError(
                                errorMessage
                        );
                    }
                }
        );
    }

    private void saveMessageDirectly(
            @NonNull AiMessage message
    ) {
        chatRepository.saveMessage(
                currentConversationId,
                message,
                new ChatRepository.OperationCallback() {

                    @Override
                    public void onSuccess() {
                        // Đã lưu thành công
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

    public String getCurrentConversationId() {
        return currentConversationId;
    }

    public boolean isConversationCreated() {
        return conversationCreated;
    }
}