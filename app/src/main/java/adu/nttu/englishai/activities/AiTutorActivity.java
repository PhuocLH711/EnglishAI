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

// =========================================================================
// AI TUTOR ACTIVITY: Màn hình Gia sư AI tích hợp Gemini & Nhận diện giọng nói
// =========================================================================
public class AiTutorActivity extends AppCompatActivity {

    // Bộ lắng nghe kết quả trả về từ màn hình Lịch sử và thu âm giọng nói (Thay thế cho startActivityForResult cũ)
    private ActivityResultLauncher<Intent> historyLauncher;
    private ActivityResultLauncher<Intent> speechLauncher;

    // Các thành phần hiển thị trên giao diện (UI Components)
    private TextView tvAiGuideMessage;
    private TextView tvSelectedTopic;
    private EditText edtAiQuestion;

    private ImageButton btnBackAi;
    private ImageButton btnAiMenu;
    private ImageButton btnSpeakToAi;
    private ImageButton btnSendAi;

    private RecyclerView recyclerAiMessages;

    // Danh sách lưu trữ các tin nhắn (Người dùng hỏi + AI trả lời) để hiển thị lên màn hình
    private final ArrayList<AiMessage> messageList =
            new ArrayList<>();

    // Các bộ quản lý logic nghiệp vụ (Controllers/Managers)
    private ChatMessageAdapter messageAdapter;
    private GeminiManager geminiManager;               // Xử lý gọi API Gemini AI
    private ChatRepository chatRepository;             // Giao tiếp với Firebase Firestore (Lưu/Tải tin nhắn)
    private ConversationManager conversationManager;   // Quản lý phiên hội thoại hiện tại
    private SpeechRecognitionManager speechManager;    // Xử lý nhận diện giọng nói (Speech-to-Text)

    // Biến lưu trạng thái chủ đề hiện tại (Mặc định là GENERAL - Giao tiếp chung)
    private String selectedTopic = AiTopicManager.GENERAL;
    private String selectedTopicName =
            AiTopicManager.getTopicName(
                    AiTopicManager.GENERAL
            );

    // Cờ đánh dấu AI đang trả lời hay không (Dùng để khóa nút bấm, tránh gửi liên tục gây spam API)
    private boolean isSendingMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_tutor);

        // Khởi tạo tuần tự: Giao diện -> Danh sách -> API Gemini -> Cloud Database -> Giọng nói -> Sự kiện
        initViews();
        setupRecyclerView();
        setupGemini();
        setupChatRepository();
        setupSpeechRecognition();
        setupHistoryLauncher();
        setupEvents();
        updateTopicUI();
        startEmptyConversation(); // Bắt đầu vào app là tạo sẵn một phòng chat trắng mới
    }

    // Ánh xạ các biến Java với ID các thẻ trong file giao diện XML
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

    // Cấu hình RecyclerView hiển thị đoạn chat
    private void setupRecyclerView() {
        messageAdapter = new ChatMessageAdapter(messageList);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this);

        // setStackFromEnd(true): Đẩy danh sách trượt xuống dưới cùng (Giống Zalo, Messenger, khi mở lên thấy tin mới nhất)
        layoutManager.setStackFromEnd(true);

        recyclerAiMessages.setLayoutManager(layoutManager);
        recyclerAiMessages.setAdapter(messageAdapter);
    }

    // Khởi tạo bộ xử lý AI Gemini
    private void setupGemini() {
        geminiManager = new GeminiManager();
    }

    // Khởi tạo kết nối cơ sở dữ liệu và quản lý phiên hội thoại
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
                                // Đã tạo cuộc trò chuyện trên Cloud.
                            }

                            @Override
                            public void onSaveError(
                                    String errorMessage
                            ) {
                                // runOnUiThread: Đảm bảo khi có lỗi từ luồng mạng (Background Thread),
                                // việc hiện thông báo Toast phải được chạy trên luồng giao diện (UI Thread) để không bị crash
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

    // Cấu hình tính năng nhận diện giọng nói tiếng Anh
    private void setupSpeechRecognition() {
        // Đăng ký nhận kết quả trả về từ bộ thu âm của Google
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

    // Cấu hình nhận kết quả khi người dùng chọn một cuộc trò chuyện cũ từ màn hình Lịch sử (ChatHistoryActivity)
    private void setupHistoryLauncher() {
        historyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Nếu bấm Hủy hoặc không có dữ liệu thì bỏ qua
                    if (result.getResultCode() != RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    // Lấy mã ID cuộc trò chuyện và chủ đề mà người dùng vừa chọn bên trang Lịch sử
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

                        // Tải lại toàn bộ đoạn chat cũ lên màn hình
                        openConversation(
                                conversationId,
                                topicCode,
                                topicName
                        );
                    }
                }
        );
    }

    // Gán sự kiện click cho các nút bấm trên màn hình
    private void setupEvents() {
        btnBackAi.setOnClickListener(view -> finish()); // Đóng màn hình hiện tại, quay lại trang trước
        btnAiMenu.setOnClickListener(this::showAiMenu); // Mở menu 3 chấm góc phải

        // Bấm nút Gửi tin nhắn
        btnSendAi.setOnClickListener(view -> {
            String question =
                    edtAiQuestion.getText()
                            .toString()
                            .trim();

            sendQuestionToGemini(
                    question,
                    false // false: Gửi bằng văn bản gõ phím
            );
        });

        // Bấm nút Micro thu âm giọng nói
        btnSpeakToAi.setOnClickListener(view -> {
            updateAiGuideMessage(
                    "Mình đang nghe bạn nói..."
            );

            // Bật khung nhận diện giọng nói tiếng Anh (Speech-to-Text)
            speechManager.startEnglishRecognition();
        });
    }

    // Xử lý dữ liệu văn bản sau khi Google chuyển đổi giọng nói thành chữ
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

        // Lấy danh sách các câu từ mà Google nghe và đoán được
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

        // Lấy kết quả có độ chính xác cao nhất (đứng đầu danh sách index 0)
        String spokenText = matches.get(0);

        // Hiển thị câu vừa nói lên ô nhập liệu và đưa con trỏ chuột về cuối câu
        edtAiQuestion.setText(spokenText);
        edtAiQuestion.setSelection(spokenText.length());

        // Tự động gửi câu vừa nói lên cho AI Gemini luôn mà không cần bấm nút Gửi
        sendQuestionToGemini(
                spokenText,
                true // true: Đánh dấu tin nhắn này gửi từ giọng nói
        );
    }

    // HÀM QUAN TRỌNG NHẤT: Gửi câu hỏi lên AI Gemini và xử lý phản hồi
    private void sendQuestionToGemini(
            String question,
            boolean isFromVoice
    ) {
        // Kiểm tra rỗng: Không cho gửi tin nhắn trống
        if (question == null
                || question.trim().isEmpty()) {

            edtAiQuestion.setError(
                    "Vui lòng nhập tin nhắn"
            );

            edtAiQuestion.requestFocus();
            return;
        }

        // Khóa chặn: Nếu AI đang suy nghĩ trả lời thì không cho gửi tiếp để tránh lỗi xung đột
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

        hideKeyboard();         // Ẩn bàn phím đi cho thoáng màn hình
        setSendingState(true);  // Chuyển giao diện sang trạng thái "Đang gửi" (Làm mờ nút bấm)

        // 1. Hiển thị ngay câu hỏi của NGƯỜI DÙNG lên khung chat
        addMessage(
                AiMessage.ROLE_USER,
                cleanedQuestion
        );

        edtAiQuestion.setText(""); // Xóa trắng ô nhập liệu

        updateAiGuideMessage(
                "Được rồi, để mình giải thích "
                        + "dễ hiểu cho bạn nhé!"
        );

        // 2. Gọi hàm gửi câu hỏi qua cho Gemini AI (Chạy ngầm không gây đơ giao diện)
        geminiManager.sendMessage(
                cleanedQuestion,
                new GeminiManager.GeminiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        // Khi AI trả lời thành công -> Đưa về luồng UI (runOnUiThread) để hiển thị lên màn hình
                        runOnUiThread(() -> {
                            setSendingState(false); // Mở khóa các nút bấm

                            // Hiển thị câu trả lời của AI lên khung chat
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
                        // Khi lỗi (mất mạng, hết hạn hạn ngạch API...) -> Báo lỗi cho người dùng biết
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

    // Hàm thêm 1 tin nhắn vào danh sách, vẽ lên màn hình và lưu lên Firebase Cloud
    private void addMessage(
            String role,
            String content
    ) {
        // Tạo đối tượng tin nhắn mới với mã UUID ngẫu nhiên và thời gian hiện tại
        AiMessage message = new AiMessage(
                UUID.randomUUID().toString(),
                role,
                content,
                System.currentTimeMillis()
        );

        // Thêm vào danh sách bộ nhớ tạm
        messageList.add(message);

        int newPosition =
                messageList.size() - 1;

        // Báo cho Adapter biết có 1 dòng mới vừa thêm vào để nó vẽ hiệu ứng hiện ra
        messageAdapter.notifyItemInserted(
                newPosition
        );

        // Tự động cuộn màn hình xuống tin nhắn mới nhất
        recyclerAiMessages.scrollToPosition(
                newPosition
        );

        // Lưu tin nhắn này vào cơ sở dữ liệu Firebase Firestore
        conversationManager.saveMessage(message);
    }

    // Hiển thị menu tùy chọn góc trên bên phải (PopupMenu)
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

    // Hiển thị hộp thoại (Dialog) chọn chủ đề học (Giao tiếp, Ngữ pháp, IELTS, TOEIC...)
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
                            // Cập nhật chủ đề được chọn
                            selectedTopic =
                                    topicCodes[which];

                            selectedTopicName =
                                    topicNames[which];

                            // Đổi ngữ cảnh prompt cho AI Gemini theo chủ đề mới
                            geminiManager.changeTopic(
                                    selectedTopic
                            );

                            updateTopicUI();
                            startEmptyConversation(); // Tạo đoạn chat mới cho chủ đề mới

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

    // Bắt đầu một phiên trò chuyện mới toanh
    private void startNewConversation() {
        startEmptyConversation();

        Toast.makeText(
                this,
                "Đã tạo cuộc trò chuyện mới",
                Toast.LENGTH_SHORT
        ).show();
    }

    // Xóa sạch danh sách hiện tại trên màn hình để chuẩn bị cho cuộc trò chuyện mới
    private void startEmptyConversation() {
        // Tạo ID phòng chat mới trên Firebase
        conversationManager.startNewConversation(
                selectedTopic,
                selectedTopicName
        );

        // Xóa trắng bộ nhớ tạm
        messageList.clear();

        if (messageAdapter != null) {
            messageAdapter.notifyDataSetChanged();
        }

        // Xóa bộ nhớ ngữ cảnh trò chuyện cũ của AI
        if (geminiManager != null) {
            geminiManager.resetCurrentChat();
        }

        if (edtAiQuestion != null) {
            edtAiQuestion.setText("");
        }

        // Hiển thị lời chào theo chủ đề
        updateAiGuideMessage(
                AiTopicManager.getWelcomeMessage(
                        selectedTopic
                )
        );
    }

    // Hộp thoại xác nhận trước khi làm trống màn hình chat
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

    // Mở màn hình xem Lịch sử trò chuyện gần đây
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

        // Dùng launcher để khi người dùng chọn xong 1 lịch sử, nó sẽ trả kết quả về hàm setupHistoryLauncher()
        historyLauncher.launch(intent);
    }

    // Hàm tải và mở lại một phiên hội thoại cũ từ Cloud Firestore
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

        // Cập nhật bộ quản lý sang ID của phiên trò chuyện cũ
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

        // Gọi Firebase Firestore tải toàn bộ tin nhắn cũ về
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

    // Xử lý vẽ danh sách tin nhắn cũ tải từ Firebase lên màn hình
    private void loadConversation(
            QuerySnapshot snapshots
    ) {
        runOnUiThread(() -> {
            messageList.clear();

            if (snapshots != null) {
                // Quét qua từng Document trên Firestore và ép kiểu về object AiMessage
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

            // Vẽ lại toàn bộ danh sách
            messageAdapter.notifyDataSetChanged();

            if (!messageList.isEmpty()) {
                recyclerAiMessages.scrollToPosition(
                        messageList.size() - 1
                );
            }

            // QUAN TRỌNG: Nạp lại lịch sử này vào bộ nhớ của Gemini để AI nhớ ngữ cảnh đang nói chuyện gì trước đó
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

    // Cập nhật dòng chữ hiển thị tên chủ đề ở góc trên màn hình
    private void updateTopicUI() {
        tvSelectedTopic.setText(
                "Chủ đề: " + selectedTopicName
        );
    }

    // Cập nhật lời dẫn của nhân vật hướng dẫn AI
    private void updateAiGuideMessage(
            String message
    ) {
        tvAiGuideMessage.setText(message);
    }

    // Khóa/Mở khóa giao diện trong lúc chờ AI trả lời (Tránh việc bấm liên tục gây lỗi)
    private void setSendingState(
            boolean isSending
    ) {
        isSendingMessage = isSending;

        btnSendAi.setEnabled(!isSending);
        btnSpeakToAi.setEnabled(!isSending);
        btnAiMenu.setEnabled(!isSending);
        edtAiQuestion.setEnabled(!isSending);

        // Làm mờ nút gửi (alpha 0.5) khi đang gửi, sáng lại (alpha 1) khi xong
        btnSendAi.setAlpha(
                isSending ? 0.5f : 1f
        );
    }

    // Hàm ẩn bàn phím ảo trên Android
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