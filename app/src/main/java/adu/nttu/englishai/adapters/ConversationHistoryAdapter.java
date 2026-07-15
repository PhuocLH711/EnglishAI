package adu.nttu.englishai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.AiConversation;

public class ConversationHistoryAdapter
        extends RecyclerView.Adapter<
        ConversationHistoryAdapter.ViewHolder> {

    public interface ConversationListener {
        void onOpen(AiConversation conversation);

        void onDelete(AiConversation conversation);
    }

    private final List<AiConversation> conversationList;
    private final ConversationListener listener;

    public ConversationHistoryAdapter(
            List<AiConversation> conversationList,
            ConversationListener listener
    ) {
        this.conversationList = conversationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_ai_conversation,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        AiConversation conversation =
                conversationList.get(position);

        String title = conversation.getTitle();

        holder.tvTitle.setText(
                title == null || title.trim().isEmpty()
                        ? "Cuộc trò chuyện"
                        : title
        );

        holder.tvTopic.setText(
                conversation.getTopicName() == null
                        ? "Hỏi tự do"
                        : conversation.getTopicName()
        );

        holder.tvTime.setText(
                formatTime(conversation.getUpdatedAt())
        );

        holder.itemView.setOnClickListener(view ->
                listener.onOpen(conversation)
        );

        holder.btnMore.setOnClickListener(view ->
                showMoreMenu(view, conversation)
        );
    }

    private void showMoreMenu(
            View anchor,
            AiConversation conversation
    ) {
        PopupMenu popupMenu =
                new PopupMenu(anchor.getContext(), anchor);

        popupMenu.getMenu().add("Mở cuộc trò chuyện");
        popupMenu.getMenu().add("Xóa cuộc trò chuyện");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.contains("Mở")) {
                listener.onOpen(conversation);
                return true;
            }

            if (title.contains("Xóa")) {
                listener.onDelete(conversation);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private String formatTime(long time) {
        if (time <= 0) {
            return "";
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                );

        return formatter.format(new Date(time));
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvTopic;
        private final TextView tvTime;
        private final ImageButton btnMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(
                    R.id.tvConversationTitle
            );

            tvTopic = itemView.findViewById(
                    R.id.tvConversationTopic
            );

            tvTime = itemView.findViewById(
                    R.id.tvConversationTime
            );

            btnMore = itemView.findViewById(
                    R.id.btnConversationMore
            );
        }
    }
}