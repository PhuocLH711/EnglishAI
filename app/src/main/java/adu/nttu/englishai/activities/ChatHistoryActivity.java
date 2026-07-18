package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.ConversationHistoryAdapter;
import adu.nttu.englishai.ai.ChatRepository;
import adu.nttu.englishai.models.AiConversation;

// =========================================================================
// CHAT HISTORY ACTIVITY: Màn hình hiển thị danh sách lịch sử trò chuyện cũ
// =========================================================================
public class ChatHistoryActivity extends AppCompatActivity {

    // Các hằng số (Constants) dùng làm "chìa khóa" (Key) để đóng gói và gửi dữ liệu sang màn hình AiTutorActivity
    // Việc dùng public static final giúp tránh gõ nhầm chính tả tên Key giữa các file
    public static final String EXTRA_CONVERSATION_ID =
            "conversationId";

    public static final String EXTRA_TOPIC_CODE =
            "topicCode";

    public static final String EXTRA_TOPIC_NAME =
            "topicName";

    // Các biến giao diện (UI Components)
    private RecyclerView recyclerChatHistory; // Danh sách các thẻ lịch sử
    private ProgressBar progressHistory;      // Vòng xoay tải dữ liệu (Loading Spinner)
    private TextView tvEmptyHistory;          // Thông báo hiển thị khi danh sách trống

    // Danh sách lưu trữ các phiên trò chuyện tải từ Firebase về
    private final ArrayList<AiConversation> conversationList =
            new ArrayList<>();

    // Adapter kết nối dữ liệu và Repository giao tiếp với Firebase
    private ConversationHistoryAdapter adapter;
    private ChatRepository chatRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        // Khởi tạo tuần tự: Giao diện -> Thanh tiêu đề -> Danh sách -> Repository -> Tải dữ liệu từ Cloud
        initViews();
        setupToolbar();
        setupRecyclerView();

        chatRepository = new ChatRepository();

        loadHistory();
    }

    // Ánh xạ các biến Java với ID trong file layout XML
    private void initViews() {
        recyclerChatHistory =
                findViewById(R.id.recyclerChatHistory);

        progressHistory =
                findViewById(R.id.progressHistory);

        tvEmptyHistory =
                findViewById(R.id.tvEmptyHistory);
    }

    // Cấu hình thanh Toolbar ở cạnh trên màn hình
    private void setupToolbar() {
        MaterialToolbar toolbar =
                findViewById(R.id.toolbarChatHistory);

        // Xử lý sự kiện khi bấm nút Mũi tên quay lại (Back Navigation) -> Đóng màn hình hiện tại
        toolbar.setNavigationOnClickListener(
                view -> finish()
        );
    }

    // Cấu hình RecyclerView và thiết lập bộ lắng nghe sự kiện từ Adapter
    private void setupRecyclerView() {
        // Khởi tạo Adapter và truyền vào một Interface Callback (ConversationListener)
        // Đây là kỹ thuật phân chia trách nhiệm: Adapter chỉ bắt sự kiện click, còn xử lý logic gì là do Activity quyết định
        adapter = new ConversationHistoryAdapter(
                conversationList,
                new ConversationHistoryAdapter
                        .ConversationListener() {
                    @Override
                    public void onOpen(
                            AiConversation conversation
                    ) {
                        // Khi người dùng chạm vào thẻ để mở -> Gửi dữ liệu ngược về màn hình AiTutor
                        returnConversationResult(
                                conversation
                        );
                    }

                    @Override
                    public void onDelete(
                            AiConversation conversation
                    ) {
                        // Khi bấm nút thùng rác -> Hiện hộp thoại xác nhận xóa
                        confirmDelete(conversation);
                    }
                }
        );

        recyclerChatHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerChatHistory.setAdapter(adapter);
    }

    // HÀM QUAN TRỌNG: Tải danh sách 50 cuộc trò chuyện gần đây nhất từ Firebase Firestore
    private void loadHistory() {
        setLoading(true); // Bật vòng xoay loading, ẩn danh sách đi

        chatRepository.loadRecentConversations(
                50, // Giới hạn lấy 50 phiên gần nhất để tối ưu hiệu năng và đường truyền mạng
                new ChatRepository.ConversationsCallback() {
                    @Override
                    public void onSuccess(
                            QuerySnapshot snapshots
                    ) {
                        // Khi có dữ liệu trả về từ Firebase -> Đưa về luồng chính (UI Thread) để xử lý
                        runOnUiThread(() -> {
                            setLoading(false); // Tắt vòng xoay loading
                            conversationList.clear(); // Xóa sạch danh sách cũ để tránh bị trùng lặp

                            // Quét qua từng tài liệu (Document) tải về từ Cloud
                            for (DocumentSnapshot document
                                    : snapshots.getDocuments()) {

                                // Tự động ánh xạ JSON của Firebase thành đối tượng Java (AiConversation)
                                AiConversation conversation =
                                        document.toObject(
                                                AiConversation.class
                                        );

                                if (conversation != null) {
                                    // BÍ KÍP: Trong Firestore, Document ID nằm ngoài data body.
                                    // Nếu object chưa có ID, ta gán thủ công ID của document cho nó để sau này biết đường mà xóa/sửa
                                    if (conversation.getId() == null) {
                                        conversation.setId(
                                                document.getId()
                                        );
                                    }

                                    conversationList.add(
                                            conversation
                                    );
                                }
                            }

                            // Báo cho Adapter biết dữ liệu đã thay đổi toàn bộ để vẽ lại danh sách
                            adapter.notifyDataSetChanged();
                            updateEmptyState(); // Kiểm tra nếu rỗng thì hiện chữ "Chưa có lịch sử"
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // Khi tải lỗi -> Tắt loading, hiện Toast báo lỗi và cập nhật giao diện rỗng
                        runOnUiThread(() -> {
                            setLoading(false);

                            Toast.makeText(
                                    ChatHistoryActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                            ).show();

                            updateEmptyState();
                        });
                    }
                }
        );
    }

    // HÀM QUAN TRỌNG: Đóng gói dữ liệu lịch sử được chọn và gửi ngược về cho AiTutorActivity
    private void returnConversationResult(
            AiConversation conversation
    ) {
        Intent resultIntent = new Intent();

        // Nhét mã ID, Mã chủ đề và Tên chủ đề vào Intent
        resultIntent.putExtra(
                EXTRA_CONVERSATION_ID,
                conversation.getId()
        );

        resultIntent.putExtra(
                EXTRA_TOPIC_CODE,
                conversation.getTopicCode()
        );

        resultIntent.putExtra(
                EXTRA_TOPIC_NAME,
                conversation.getTopicName()
        );

        // setResult(RESULT_OK, ...): Báo hiệu thành công, mang theo gói dữ liệu resultIntent
        // Khi gọi finish(), gói dữ liệu này sẽ bay thẳng vào bộ lắng nghe (historyLauncher) bên AiTutorActivity
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // Hộp thoại xác nhận trước khi xóa (Tránh trường hợp người dùng bấm nhầm tay)
    private void confirmDelete(
            AiConversation conversation
    ) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa cuộc trò chuyện?")
                .setMessage(
                        "Bạn có chắc muốn xóa \""
                                + conversation.getTitle()
                                + "\"?"
                )
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) ->
                                deleteConversation(
                                        conversation
                                )
                )
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Xóa một cuộc trò chuyện khỏi cơ sở dữ liệu Firebase
    private void deleteConversation(
            AiConversation conversation
    ) {
        chatRepository.deleteConversation(
                conversation.getId(),
                new ChatRepository.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            // Tìm vị trí (index) của thẻ vừa xóa trong danh sách bộ nhớ tạm
                            int position =
                                    conversationList.indexOf(
                                            conversation
                                    );

                            if (position >= 0) {
                                // Xóa khỏi ArrayList
                                conversationList.remove(position);

                                // TỐI ƯU HIỆU NĂNG: Chỉ thông báo xóa đúng 1 dòng tại vị trí position
                                // Giúp tạo hiệu ứng thu nhỏ (animation) mượt mà, không cần tải lại cả danh sách
                                adapter.notifyItemRemoved(position);
                            }

                            updateEmptyState(); // Check lại xem xóa xong thì danh sách đã trống trơn chưa

                            Toast.makeText(
                                    ChatHistoryActivity.this,
                                    "Đã xóa cuộc trò chuyện",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() ->
                                Toast.makeText(
                                        ChatHistoryActivity.this,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                    }
                }
        );
    }

    // Quản lý trạng thái hiển thị lúc đang tải (Loading) và lúc đã tải xong
    private void setLoading(boolean isLoading) {
        // Nếu isLoading = true -> Hiện vòng xoay progress, ẩn danh sách
        progressHistory.setVisibility(
                isLoading ? View.VISIBLE : View.GONE
        );

        recyclerChatHistory.setVisibility(
                isLoading ? View.INVISIBLE : View.VISIBLE
        );

        if (isLoading) {
            tvEmptyHistory.setVisibility(View.GONE);
        }
    }

    // Kiểm tra và hiển thị giao diện "Danh sách trống" nếu người dùng chưa từng chat với AI
    private void updateEmptyState() {
        boolean isEmpty = conversationList.isEmpty();

        tvEmptyHistory.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        recyclerChatHistory.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }
}