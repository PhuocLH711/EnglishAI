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

public class ChatHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID =
            "conversationId";

    public static final String EXTRA_TOPIC_CODE =
            "topicCode";

    public static final String EXTRA_TOPIC_NAME =
            "topicName";

    private RecyclerView recyclerChatHistory;
    private ProgressBar progressHistory;
    private TextView tvEmptyHistory;

    private final ArrayList<AiConversation> conversationList =
            new ArrayList<>();

    private ConversationHistoryAdapter adapter;
    private ChatRepository chatRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        initViews();
        setupToolbar();
        setupRecyclerView();

        chatRepository = new ChatRepository();

        loadHistory();
    }

    private void initViews() {
        recyclerChatHistory =
                findViewById(R.id.recyclerChatHistory);

        progressHistory =
                findViewById(R.id.progressHistory);

        tvEmptyHistory =
                findViewById(R.id.tvEmptyHistory);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar =
                findViewById(R.id.toolbarChatHistory);

        toolbar.setNavigationOnClickListener(
                view -> finish()
        );
    }

    private void setupRecyclerView() {
        adapter = new ConversationHistoryAdapter(
                conversationList,
                new ConversationHistoryAdapter
                        .ConversationListener() {
                    @Override
                    public void onOpen(
                            AiConversation conversation
                    ) {
                        returnConversationResult(
                                conversation
                        );
                    }

                    @Override
                    public void onDelete(
                            AiConversation conversation
                    ) {
                        confirmDelete(conversation);
                    }
                }
        );

        recyclerChatHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerChatHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        setLoading(true);

        chatRepository.loadRecentConversations(
                50,
                new ChatRepository.ConversationsCallback() {
                    @Override
                    public void onSuccess(
                            QuerySnapshot snapshots
                    ) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            conversationList.clear();

                            for (DocumentSnapshot document
                                    : snapshots.getDocuments()) {

                                AiConversation conversation =
                                        document.toObject(
                                                AiConversation.class
                                        );

                                if (conversation != null) {
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

                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
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

    private void returnConversationResult(
            AiConversation conversation
    ) {
        Intent resultIntent = new Intent();

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

        setResult(RESULT_OK, resultIntent);
        finish();
    }

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

    private void deleteConversation(
            AiConversation conversation
    ) {
        chatRepository.deleteConversation(
                conversation.getId(),
                new ChatRepository.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            int position =
                                    conversationList.indexOf(
                                            conversation
                                    );

                            if (position >= 0) {
                                conversationList.remove(position);
                                adapter.notifyItemRemoved(position);
                            }

                            updateEmptyState();

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

    private void setLoading(boolean isLoading) {
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