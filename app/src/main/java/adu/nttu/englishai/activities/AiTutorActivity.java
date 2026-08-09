package adu.nttu.englishai.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.ChatMessageAdapter;
import adu.nttu.englishai.ai.AiTopicManager;
import adu.nttu.englishai.ai.ChatRepository;
import adu.nttu.englishai.ai.ConversationManager;
import adu.nttu.englishai.ai.GeminiManager;
import adu.nttu.englishai.models.AiMessage;
import adu.nttu.englishai.utils.SpeechRecognitionManager;

public class AiTutorActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> historyLauncher;
    private ActivityResultLauncher<Intent> speechLauncher;
    private ActivityResultLauncher<String> imagePickerLauncher;

    private TextView tvAiGuideMessage;
    private TextView tvSelectedTopic;
    private TextView btnRemoveSelectedImage;
    private EditText edtAiQuestion;

    private ImageButton btnBackAi;
    private ImageButton btnAiMenu;
    private ImageButton btnSpeakToAi;
    private ImageButton btnSendAi;
    private ImageButton btnPickAiImage;

    private ImageView imgSelectedImage;
    private View layoutSelectedImage;
    private RecyclerView recyclerAiMessages;

    private final ArrayList<AiMessage> messageList =
            new ArrayList<>();

    private ChatMessageAdapter messageAdapter;
    private GeminiManager geminiManager;
    private ChatRepository chatRepository;
    private ConversationManager conversationManager;
    private SpeechRecognitionManager speechManager;

    private String selectedTopic =
            AiTopicManager.GENERAL;

    private String selectedTopicName =
            AiTopicManager.getTopicName(
                    AiTopicManager.GENERAL
            );

    private boolean isSendingMessage = false;

    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_tutor);

        initViews();
        setupRecyclerView();
        setupGemini();
        setupChatRepository();
        setupSpeechRecognition();
        setupImagePicker();
        setupHistoryLauncher();
        setupEvents();
        updateTopicUI();
        startEmptyConversation();
    }

    private void initViews() {
        tvAiGuideMessage =
                findViewById(R.id.tvAiGuideMessage);

        tvSelectedTopic =
                findViewById(R.id.tvSelectedTopic);

        btnRemoveSelectedImage =
                findViewById(R.id.btnRemoveSelectedImage);

        edtAiQuestion =
                findViewById(R.id.edtAiQuestion);

        btnBackAi =
                findViewById(R.id.btnBackAi);

        btnAiMenu =
                findViewById(R.id.btnAiMenu);

        btnSpeakToAi =
                findViewById(R.id.btnSpeakToAi);

        btnSendAi =
                findViewById(R.id.btnSendAi);

        btnPickAiImage =
                findViewById(R.id.btnPickAiImage);

        imgSelectedImage =
                findViewById(R.id.imgSelectedImage);

        layoutSelectedImage =
                findViewById(R.id.layoutSelectedImage);

        recyclerAiMessages =
                findViewById(R.id.recyclerAiMessages);
    }

    private void setupRecyclerView() {
        messageAdapter =
                new ChatMessageAdapter(messageList);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this);

        layoutManager.setStackFromEnd(true);

        recyclerAiMessages.setLayoutManager(layoutManager);
        recyclerAiMessages.setAdapter(messageAdapter);
    }

    private void setupGemini() {
        geminiManager =
                new GeminiManager();
    }

    private void setupChatRepository() {
        chatRepository =
                new ChatRepository();

        conversationManager =
                new ConversationManager(
                        chatRepository,
                        new ConversationManager.ConversationListener() {
                            @Override
                            public void onConversationCreated(
                                    String conversationId
                            ) {
                                // Đã tạo phòng chat.
                            }

                            @Override
                            public void onSaveError(
                                    String errorMessage
                            ) {
                                runOnUiThread(
                                        () -> Toast.makeText(
                                                AiTutorActivity.this,
                                                "Không thể lưu lịch sử: "
                                                        + errorMessage,
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );
                            }
                        }
                );
    }

    private void setupSpeechRecognition() {
        speechLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> handleSpeechResult(
                                result.getResultCode(),
                                result.getData()
                        )
                );

        speechManager =
                new SpeechRecognitionManager(
                        this,
                        speechLauncher
                );
    }

    private void setupImagePicker() {
        imagePickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri == null) {
                                return;
                            }

                            try {
                                Bitmap bitmap;

                                try (
                                        InputStream inputStream =
                                                getContentResolver()
                                                        .openInputStream(uri)
                                ) {
                                    bitmap =
                                            BitmapFactory.decodeStream(
                                                    inputStream
                                            );
                                }

                                if (bitmap == null) {
                                    throw new IllegalStateException(
                                            "Không đọc được hình ảnh."
                                    );
                                }

                                selectedImageUri =
                                        uri;

                                selectedImageBitmap =
                                        resizeBitmapIfNeeded(
                                                bitmap,
                                                1600
                                        );

                                imgSelectedImage.setImageURI(
                                        uri
                                );

                                layoutSelectedImage.setVisibility(
                                        View.VISIBLE
                                );

                                updateAiGuideMessage(
                                        "Đã chọn ảnh. "
                                                + "Bạn có thể nhập câu hỏi về hình ảnh này."
                                );

                            } catch (Exception exception) {
                                clearSelectedImage();

                                Toast.makeText(
                                        AiTutorActivity.this,
                                        "Không thể đọc hình ảnh.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    private Bitmap resizeBitmapIfNeeded(
            Bitmap bitmap,
            int maxSize
    ) {
        if (bitmap == null) {
            return null;
        }

        int width =
                bitmap.getWidth();

        int height =
                bitmap.getHeight();

        if (width <= maxSize
                && height <= maxSize) {
            return bitmap;
        }

        float ratio =
                Math.min(
                        (float) maxSize / width,
                        (float) maxSize / height
                );

        int newWidth =
                Math.max(
                        1,
                        Math.round(width * ratio)
                );

        int newHeight =
                Math.max(
                        1,
                        Math.round(height * ratio)
                );

        return Bitmap.createScaledBitmap(
                bitmap,
                newWidth,
                newHeight,
                true
        );
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        selectedImageBitmap = null;

        if (imgSelectedImage != null) {
            imgSelectedImage.setImageDrawable(null);
        }

        if (layoutSelectedImage != null) {
            layoutSelectedImage.setVisibility(
                    View.GONE
            );
        }
    }

    private void setupHistoryLauncher() {
        historyLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() != RESULT_OK
                                    || result.getData() == null) {
                                return;
                            }

                            Intent data =
                                    result.getData();

                            String conversationId =
                                    data.getStringExtra(
                                            ChatHistoryActivity.EXTRA_CONVERSATION_ID
                                    );

                            String topicCode =
                                    data.getStringExtra(
                                            ChatHistoryActivity.EXTRA_TOPIC_CODE
                                    );

                            String topicName =
                                    data.getStringExtra(
                                            ChatHistoryActivity.EXTRA_TOPIC_NAME
                                    );

                            if (conversationId != null
                                    && !conversationId.isEmpty()) {
                                openConversation(
                                        conversationId,
                                        topicCode,
                                        topicName
                                );
                            }
                        }
                );
    }

    private void setupEvents() {
        btnBackAi.setOnClickListener(
                view -> finish()
        );

        btnAiMenu.setOnClickListener(
                this::showAiMenu
        );

        btnPickAiImage.setOnClickListener(
                view -> {
                    if (isSendingMessage) {
                        return;
                    }

                    imagePickerLauncher.launch(
                            "image/*"
                    );
                }
        );

        btnRemoveSelectedImage.setOnClickListener(
                view -> clearSelectedImage()
        );

        btnSendAi.setOnClickListener(
                view -> {
                    String question =
                            edtAiQuestion.getText()
                                    .toString()
                                    .trim();

                    sendQuestionToGemini(
                            question,
                            false
                    );
                }
        );

        btnSpeakToAi.setOnClickListener(
                view -> {
                    updateAiGuideMessage(
                            "Mình đang nghe bạn nói..."
                    );

                    speechManager.startEnglishRecognition();
                }
        );
    }

    private void handleSpeechResult(
            int resultCode,
            Intent resultData
    ) {
        if (!SpeechRecognitionManager.isSuccessfulResult(
                resultCode,
                resultData
        )) {
            updateAiGuideMessage(
                    "Mình chưa nghe thấy nội dung nào."
            );
            return;
        }

        ArrayList<String> matches =
                resultData.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                );

        if (matches == null
                || matches.isEmpty()) {
            updateAiGuideMessage(
                    "Mình chưa nhận diện được câu nói."
            );
            return;
        }

        String spokenText =
                matches.get(0);

        edtAiQuestion.setText(spokenText);
        edtAiQuestion.setSelection(spokenText.length());

        sendQuestionToGemini(
                spokenText,
                true
        );
    }

    private void sendQuestionToGemini(
            String question,
            boolean isFromVoice
    ) {
        boolean hasText =
                question != null
                        && !question.trim().isEmpty();

        boolean hasImage =
                selectedImageBitmap != null;

        if (!hasText
                && !hasImage) {
            edtAiQuestion.setError(
                    "Hãy nhập tin nhắn hoặc chọn hình ảnh"
            );
            edtAiQuestion.requestFocus();
            return;
        }

        if (isSendingMessage) {
            Toast.makeText(
                    this,
                    "AI đang trả lời, vui lòng đợi",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String cleanedQuestion =
                hasText
                        ? question.trim()
                        : "Hãy phân tích hình ảnh này.";

        hideKeyboard();
        setSendingState(true);

        final Bitmap imageForRequest =
                selectedImageBitmap;

        final String imageUriForMessage =
                selectedImageUri != null
                        ? selectedImageUri.toString()
                        : null;

        AiMessage userMessage =
                new AiMessage(
                        UUID.randomUUID().toString(),
                        AiMessage.ROLE_USER,
                        hasText
                                ? cleanedQuestion
                                : "",
                        System.currentTimeMillis()
                );

        if (imageUriForMessage != null) {
            userMessage.setImageUri(
                    imageUriForMessage
            );
        }

        addMessageObject(
                userMessage,
                true
        );

        edtAiQuestion.setText("");

        updateAiGuideMessage(
                hasImage
                        ? "Mình đang xem hình ảnh của bạn..."
                        : "Mình đang trả lời..."
        );

        AiMessage streamingAiMessage =
                new AiMessage(
                        UUID.randomUUID().toString(),
                        AiMessage.ROLE_AI,
                        "",
                        System.currentTimeMillis()
                );

        messageList.add(streamingAiMessage);

        final int aiPosition =
                messageList.size() - 1;

        messageAdapter.notifyItemInserted(
                aiPosition
        );

        recyclerAiMessages.scrollToPosition(
                aiPosition
        );

        GeminiManager.GeminiStreamCallback callback =
                new GeminiManager.GeminiStreamCallback() {

                    private final StringBuilder responseBuilder =
                            new StringBuilder();

                    @Override
                    public void onChunk(String chunk) {
                        runOnUiThread(
                                () -> {
                                    responseBuilder.append(
                                            chunk
                                    );

                                    streamingAiMessage.setContent(
                                            responseBuilder.toString()
                                    );

                                    messageAdapter.notifyItemChanged(
                                            aiPosition
                                    );

                                    recyclerAiMessages.scrollToPosition(
                                            aiPosition
                                    );
                                }
                        );
                    }

                    @Override
                    public void onComplete(String fullResponse) {
                        runOnUiThread(
                                () -> {
                                    streamingAiMessage.setContent(
                                            fullResponse
                                    );

                                    messageAdapter.notifyItemChanged(
                                            aiPosition
                                    );

                                    conversationManager.saveMessage(
                                            streamingAiMessage
                                    );

                                    setSendingState(false);
                                    clearSelectedImage();

                                    updateAiGuideMessage(
                                            "Mình đã trả lời ở bên dưới. "
                                                    + "Bạn có muốn hỏi thêm không?"
                                    );
                                }
                        );
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(
                                () -> {
                                    streamingAiMessage.setContent(
                                            "Mình chưa trả lời được.\n"
                                                    + errorMessage
                                    );

                                    messageAdapter.notifyItemChanged(
                                            aiPosition
                                    );

                                    setSendingState(false);

                                    updateAiGuideMessage(
                                            "Có lỗi xảy ra. "
                                                    + "Bạn thử hỏi lại giúp mình nhé."
                                    );
                                }
                        );
                    }
                };

        if (hasImage
                && imageForRequest != null) {
            geminiManager.sendImageMessageStream(
                    imageForRequest,
                    cleanedQuestion,
                    callback
            );
        } else {
            geminiManager.sendMessageStream(
                    cleanedQuestion,
                    callback
            );
        }
    }

    private void addMessage(
            String role,
            String content
    ) {
        AiMessage message =
                new AiMessage(
                        UUID.randomUUID().toString(),
                        role,
                        content,
                        System.currentTimeMillis()
                );

        addMessageObject(
                message,
                true
        );
    }

    private void addMessageObject(
            AiMessage message,
            boolean saveToCloud
    ) {
        messageList.add(message);

        int newPosition =
                messageList.size() - 1;

        messageAdapter.notifyItemInserted(
                newPosition
        );

        recyclerAiMessages.scrollToPosition(
                newPosition
        );

        if (saveToCloud) {
            conversationManager.saveMessage(
                    message
            );
        }
    }

    private void showAiMenu(View anchor) {
        PopupMenu popupMenu =
                new PopupMenu(this, anchor);

        popupMenu.getMenu().add(
                "＋ Cuộc trò chuyện mới"
        );
        popupMenu.getMenu().add(
                "🕘 Lịch sử gần đây"
        );
        popupMenu.getMenu().add(
                "📚 Chọn chủ đề"
        );
        popupMenu.getMenu().add(
                "🗑 Làm trống màn hình chat"
        );

        popupMenu.setOnMenuItemClickListener(
                item -> {
                    String title =
                            item.getTitle().toString();

                    if (title.contains("Cuộc trò chuyện mới")) {
                        startNewConversation();
                        return true;
                    }

                    if (title.contains("Lịch sử gần đây")) {
                        openRecentHistory();
                        return true;
                    }

                    if (title.contains("Chọn chủ đề")) {
                        showTopicDialog();
                        return true;
                    }

                    if (title.contains("Làm trống")) {
                        confirmClearCurrentChat();
                        return true;
                    }

                    return false;
                }
        );

        popupMenu.show();
    }

    private void showTopicDialog() {
        String[] topicNames =
                AiTopicManager.getTopicNames();

        String[] topicCodes =
                AiTopicManager.getTopicCodes();

        int checkedItem =
                AiTopicManager.getTopicIndex(
                        selectedTopic
                );

        new AlertDialog.Builder(this)
                .setTitle("Bạn muốn học chủ đề gì?")
                .setSingleChoiceItems(
                        topicNames,
                        checkedItem,
                        (dialog, which) -> {
                            selectedTopic =
                                    topicCodes[which];

                            selectedTopicName =
                                    topicNames[which];

                            geminiManager.changeTopic(
                                    selectedTopic
                            );

                            updateTopicUI();
                            startEmptyConversation();

                            dialog.dismiss();

                            Toast.makeText(
                                    this,
                                    "Đã chuyển sang "
                                            + selectedTopicName,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    private void startNewConversation() {
        startEmptyConversation();

        Toast.makeText(
                this,
                "Đã tạo cuộc trò chuyện mới",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void startEmptyConversation() {
        conversationManager.startNewConversation(
                selectedTopic,
                selectedTopicName
        );

        messageList.clear();

        if (messageAdapter != null) {
            messageAdapter.notifyDataSetChanged();
        }

        if (geminiManager != null) {
            geminiManager.resetCurrentChat();
        }

        if (edtAiQuestion != null) {
            edtAiQuestion.setText("");
        }

        clearSelectedImage();

        updateAiGuideMessage(
                AiTopicManager.getWelcomeMessage(
                        selectedTopic
                )
        );
    }

    private void confirmClearCurrentChat() {
        new AlertDialog.Builder(this)
                .setTitle("Làm trống màn hình chat?")
                .setMessage(
                        "Cuộc trò chuyện hiện tại vẫn được giữ "
                                + "trong lịch sử gần đây."
                )
                .setPositiveButton(
                        "Làm trống",
                        (dialog, which) ->
                                startNewConversation()
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    private void openRecentHistory() {
        if (historyLauncher == null) {
            Toast.makeText(
                    this,
                    "Lịch sử trò chuyện chưa được khởi tạo",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        ChatHistoryActivity.class
                );

        historyLauncher.launch(intent);
    }

    private void openConversation(
            String conversationId,
            String topicCode,
            String topicName
    ) {
        selectedTopic =
                topicCode != null
                        ? topicCode
                        : AiTopicManager.GENERAL;

        selectedTopicName =
                topicName != null
                        ? topicName
                        : AiTopicManager.getTopicName(
                        selectedTopic
                );

        conversationManager.openExistingConversation(
                conversationId,
                selectedTopic,
                selectedTopicName
        );

        geminiManager.changeTopic(
                selectedTopic
        );

        updateTopicUI();

        messageList.clear();
        messageAdapter.notifyDataSetChanged();
        clearSelectedImage();

        updateAiGuideMessage(
                "Mình đang mở lại cuộc trò chuyện..."
        );

        chatRepository.loadMessages(
                conversationId,
                new ChatRepository.MessagesCallback() {
                    @Override
                    public void onSuccess(
                            QuerySnapshot snapshots
                    ) {
                        loadConversation(
                                snapshots
                        );
                    }

                    @Override
                    public void onError(
                            String errorMessage
                    ) {
                        runOnUiThread(
                                () ->
                                        updateAiGuideMessage(
                                                "Không thể tải lịch sử: "
                                                        + errorMessage
                                        )
                        );
                    }
                }
        );
    }

    private void loadConversation(
            QuerySnapshot snapshots
    ) {
        runOnUiThread(
                () -> {
                    messageList.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot document
                                : snapshots.getDocuments()) {

                            AiMessage message =
                                    document.toObject(
                                            AiMessage.class
                                    );

                            if (message != null) {
                                messageList.add(
                                        message
                                );
                            }
                        }
                    }

                    messageAdapter.notifyDataSetChanged();

                    if (!messageList.isEmpty()) {
                        recyclerAiMessages.scrollToPosition(
                                messageList.size() - 1
                        );
                    }

                    geminiManager.restoreConversation(
                            selectedTopic,
                            messageList
                    );

                    updateAiGuideMessage(
                            "Đã mở lại cuộc trò chuyện. "
                                    + "Bạn có thể tiếp tục hỏi bên dưới."
                    );
                }
        );
    }

    private void updateTopicUI() {
        tvSelectedTopic.setText(
                "Chủ đề: "
                        + selectedTopicName
        );
    }

    private void updateAiGuideMessage(
            String message
    ) {
        tvAiGuideMessage.setText(
                message
        );
    }

    private void setSendingState(
            boolean isSending
    ) {
        isSendingMessage =
                isSending;

        btnSendAi.setEnabled(
                !isSending
        );

        btnSpeakToAi.setEnabled(
                !isSending
        );

        btnPickAiImage.setEnabled(
                !isSending
        );

        btnAiMenu.setEnabled(
                !isSending
        );

        edtAiQuestion.setEnabled(
                !isSending
        );

        btnSendAi.setAlpha(
                isSending
                        ? 0.5f
                        : 1f
        );

        btnPickAiImage.setAlpha(
                isSending
                        ? 0.5f
                        : 1f
        );
    }

    private void hideKeyboard() {
        View currentView =
                getCurrentFocus();

        if (currentView == null) {
            return;
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager)
                        getSystemService(
                                INPUT_METHOD_SERVICE
                        );

        inputMethodManager.hideSoftInputFromWindow(
                currentView.getWindowToken(),
                0
        );

        currentView.clearFocus();
    }
}