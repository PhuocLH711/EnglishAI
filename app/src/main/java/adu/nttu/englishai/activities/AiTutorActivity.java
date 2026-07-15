package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
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

    private TextView tvAiGuideMessage;
    private TextView tvSelectedTopic;
    private EditText edtAiQuestion;

    private ImageButton btnBackAi;
    private ImageButton btnAiMenu;
    private ImageButton btnSpeakToAi;
    private ImageButton btnSendAi;

    private RecyclerView recyclerAiMessages;

    private final ArrayList<AiMessage> messageList =
            new ArrayList<>();

    private ChatMessageAdapter messageAdapter;
    private GeminiManager geminiManager;
    private ChatRepository chatRepository;
    private ConversationManager conversationManager;
    private SpeechRecognitionManager speechManager;

    private String selectedTopic = AiTopicManager.GENERAL;
    private String selectedTopicName =
            AiTopicManager.getTopicName(
                    AiTopicManager.GENERAL
            );

    private boolean isSendingMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_tutor);

        initViews();
        setupRecyclerView();
        setupGemini();
        setupChatRepository();
        setupSpeechRecognition();
        setupHistoryLauncher();
        setupEvents();
        updateTopicUI();
        startEmptyConversation();
    }

    private void initViews() {
        tvAiGuideMessage = findViewById(R.id.tvAiGuideMessage);
        tvSelectedTopic = findViewById(R.id.tvSelectedTopic);
        edtAiQuestion = findViewById(R.id.edtAiQuestion);

        btnBackAi = findViewById(R.id.btnBackAi);
        btnAiMenu = findViewById(R.id.btnAiMenu);
        btnSpeakToAi = findViewById(R.id.btnSpeakToAi);
        btnSendAi = findViewById(R.id.btnSendAi);

        recyclerAiMessages = findViewById(R.id.recyclerAiMessages);
    }

    private void setupRecyclerView() {
        messageAdapter = new ChatMessageAdapter(messageList);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this);

        layoutManager.setStackFromEnd(true);

        recyclerAiMessages.setLayoutManager(layoutManager);
        recyclerAiMessages.setAdapter(messageAdapter);
    }

    private void setupGemini() {
        geminiManager = new GeminiManager();
    }

    private void setupChatRepository() {
        chatRepository = new ChatRepository();

        conversationManager =
                new ConversationManager(
                        chatRepository,
                        new ConversationManager.ConversationListener() {
                            @Override
                            public void onConversationCreated(
                                    String conversationId
                            ) {
                                // Đã tạo cuộc trò chuyện.
                            }

                            @Override
                            public void onSaveError(
                                    String errorMessage
                            ) {
                                runOnUiThread(() ->
                                        Toast.makeText(
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
        speechLauncher = registerForActivityResult(
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

    private void setupHistoryLauncher() {
        historyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

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
        btnBackAi.setOnClickListener(view -> finish());
        btnAiMenu.setOnClickListener(this::showAiMenu);

        btnSendAi.setOnClickListener(view -> {
            String question =
                    edtAiQuestion.getText()
                            .toString()
                            .trim();

            sendQuestionToGemini(
                    question,
                    false
            );
        });

        btnSpeakToAi.setOnClickListener(view -> {
            updateAiGuideMessage(
                    "Mình đang nghe bạn nói..."
            );

            speechManager.startEnglishRecognition();
        });
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

        if (matches == null || matches.isEmpty()) {
            updateAiGuideMessage(
                    "Mình chưa nhận diện được câu nói."
            );
            return;
        }

        String spokenText = matches.get(0);

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
        if (question == null
                || question.trim().isEmpty()) {

            edtAiQuestion.setError(
                    "Vui lòng nhập tin nhắn"
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
                question.trim();

        hideKeyboard();
        setSendingState(true);

        addMessage(
                AiMessage.ROLE_USER,
                cleanedQuestion
        );

        edtAiQuestion.setText("");

        updateAiGuideMessage(
                "Được rồi, để mình giải thích "
                        + "dễ hiểu cho bạn nhé!"
        );

        geminiManager.sendMessage(
                cleanedQuestion,
                new GeminiManager.GeminiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            setSendingState(false);

                            addMessage(
                                    AiMessage.ROLE_AI,
                                    response
                            );

                            updateAiGuideMessage(
                                    "Mình đã trả lời ở bên dưới. "
                                            + "Bạn có muốn hỏi thêm không?"
                            );
                        });
                    }

                    @Override
                    public void onError(
                            String errorMessage
                    ) {
                        runOnUiThread(() -> {
                            setSendingState(false);

                            addMessage(
                                    AiMessage.ROLE_AI,
                                    "Mình chưa trả lời được.\n"
                                            + errorMessage
                            );

                            updateAiGuideMessage(
                                    "Có lỗi xảy ra. "
                                            + "Bạn thử hỏi lại giúp mình nhé."
                            );
                        });
                    }
                }
        );
    }

    private void addMessage(
            String role,
            String content
    ) {
        AiMessage message = new AiMessage(
                UUID.randomUUID().toString(),
                role,
                content,
                System.currentTimeMillis()
        );

        messageList.add(message);

        int newPosition =
                messageList.size() - 1;

        messageAdapter.notifyItemInserted(
                newPosition
        );

        recyclerAiMessages.scrollToPosition(
                newPosition
        );

        conversationManager.saveMessage(message);
    }

    private void showAiMenu(View anchor) {
        PopupMenu popupMenu =
                new PopupMenu(this, anchor);

        popupMenu.getMenu().add("＋ Cuộc trò chuyện mới");
        popupMenu.getMenu().add("🕘 Lịch sử gần đây");
        popupMenu.getMenu().add("📚 Chọn chủ đề");
        popupMenu.getMenu().add("🗑 Làm trống màn hình chat");

        popupMenu.setOnMenuItemClickListener(item -> {
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
        });

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
                .setNegativeButton("Hủy", null)
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
                .setNegativeButton("Hủy", null)
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

        Intent intent = new Intent(
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

        geminiManager.changeTopic(selectedTopic);
        updateTopicUI();

        messageList.clear();
        messageAdapter.notifyDataSetChanged();

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
                        loadConversation(snapshots);
                    }

                    @Override
                    public void onError(
                            String errorMessage
                    ) {
                        runOnUiThread(() ->
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
        runOnUiThread(() -> {
            messageList.clear();

            if (snapshots != null) {
                for (DocumentSnapshot document
                        : snapshots.getDocuments()) {

                    AiMessage message =
                            document.toObject(
                                    AiMessage.class
                            );

                    if (message != null) {
                        messageList.add(message);
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
        });
    }

    private void updateTopicUI() {
        tvSelectedTopic.setText(
                "Chủ đề: " + selectedTopicName
        );
    }

    private void updateAiGuideMessage(
            String message
    ) {
        tvAiGuideMessage.setText(message);
    }

    private void setSendingState(
            boolean isSending
    ) {
        isSendingMessage = isSending;

        btnSendAi.setEnabled(!isSending);
        btnSpeakToAi.setEnabled(!isSending);
        btnAiMenu.setEnabled(!isSending);
        edtAiQuestion.setEnabled(!isSending);

        btnSendAi.setAlpha(
                isSending ? 0.5f : 1f
        );
    }

    private void hideKeyboard() {
        View currentView =
                getCurrentFocus();

        if (currentView == null) {
            return;
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(
                        INPUT_METHOD_SERVICE
                );

        inputMethodManager.hideSoftInputFromWindow(
                currentView.getWindowToken(),
                0
        );

        currentView.clearFocus();
    }
}