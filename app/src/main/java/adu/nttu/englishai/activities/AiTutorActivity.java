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

import java.util.ArrayList;
import java.util.UUID;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.ChatMessageAdapter;
import adu.nttu.englishai.ai.AiTopicManager;
import adu.nttu.englishai.ai.GeminiManager;
import adu.nttu.englishai.models.AiMessage;
import adu.nttu.englishai.utils.SpeechRecognitionManager;

public class AiTutorActivity extends AppCompatActivity {

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
    private SpeechRecognitionManager speechManager;

    private ActivityResultLauncher<Intent> speechLauncher;

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
        setupSpeechRecognition();
        setupEvents();
        updateTopicUI();

        updateAiGuideMessage(
                "Xin chào! Hôm nay bạn muốn học gì?"
        );
    }

    private void initViews() {
        tvAiGuideMessage =
                findViewById(R.id.tvAiGuideMessage);

        tvSelectedTopic =
                findViewById(R.id.tvSelectedTopic);

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

        recyclerAiMessages =
                findViewById(R.id.recyclerAiMessages);
    }

    private void setupRecyclerView() {
        messageAdapter =
                new ChatMessageAdapter(messageList);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this);

        layoutManager.setStackFromEnd(true);

        recyclerAiMessages.setLayoutManager(
                layoutManager
        );

        recyclerAiMessages.setAdapter(
                messageAdapter
        );
    }

    private void setupGemini() {
        geminiManager = new GeminiManager();
    }

    private void setupSpeechRecognition() {
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts
                        .StartActivityForResult(),
                result -> handleSpeechResult(
                        result.getResultCode(),
                        result.getData()
                )
        );

        speechManager = new SpeechRecognitionManager(
                this,
                speechLauncher
        );
    }

    private void setupEvents() {
        btnBackAi.setOnClickListener(
                view -> finish()
        );

        btnAiMenu.setOnClickListener(
                this::showAiMenu
        );

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
        edtAiQuestion.setSelection(
                spokenText.length()
        );

        sendQuestionToGemini(
                spokenText,
                true
        );
    }

    private void sendQuestionToGemini(
            String question,
            boolean isFromVoice
    ) {
        if (question == null || question.trim().isEmpty()) {
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

        hideKeyboard();
        setSendingState(true);

        addMessage(
                AiMessage.ROLE_USER,
                question.trim()
        );

        edtAiQuestion.setText("");

        updateAiGuideMessage(
                "Được rồi, để mình giải thích "
                        + "dễ hiểu cho bạn nhé!"
        );

        geminiManager.sendMessage(
                question.trim(),
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
                    public void onError(String errorMessage) {
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
                "🗑 Xóa nội dung hiện tại"
        );

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

            if (title.contains("Xóa nội dung")) {
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

                            updateAiGuideMessage(
                                    AiTopicManager
                                            .getWelcomeMessage(
                                                    selectedTopic
                                            )
                            );

                            dialog.dismiss();
                        }
                )
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void startNewConversation() {
        geminiManager.resetCurrentChat();

        messageList.clear();
        messageAdapter.notifyDataSetChanged();

        edtAiQuestion.setText("");

        updateAiGuideMessage(
                AiTopicManager.getWelcomeMessage(
                        selectedTopic
                )
        );

        Toast.makeText(
                this,
                "Đã tạo cuộc trò chuyện mới",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void confirmClearCurrentChat() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa nội dung hiện tại?")
                .setMessage(
                        "Các tin nhắn trên màn hình sẽ bị xóa."
                )
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) ->
                                startNewConversation()
                )
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openRecentHistory() {
        Toast.makeText(
                this,
                "Màn hình lịch sử sẽ được làm "
                        + "ở bước tiếp theo",
                Toast.LENGTH_SHORT
        ).show();
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
        View currentView = getCurrentFocus();

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