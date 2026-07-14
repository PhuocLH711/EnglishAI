package adu.nttu.englishai.adapters;

import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.AiMessage;

public class ChatMessageAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;

    private final List<AiMessage> messageList;

    public ChatMessageAdapter(List<AiMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser()
                ? TYPE_USER
                : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_USER) {
            View view = inflater.inflate(
                    R.layout.item_message_user,
                    parent,
                    false
            );

            return new UserMessageViewHolder(view);
        }

        View view = inflater.inflate(
                R.layout.item_message_ai,
                parent,
                false
        );

        return new AiMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        AiMessage message = messageList.get(position);

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder)
                    .tvMessage
                    .setText(message.getContent());

        } else if (holder instanceof AiMessageViewHolder) {
            ((AiMessageViewHolder) holder)
                    .tvMessage
                    .setText(formatAiText(message.getContent()));
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private Spanned formatAiText(String text) {
        if (text == null) {
            return HtmlCompat.fromHtml(
                    "",
                    HtmlCompat.FROM_HTML_MODE_COMPACT
            );
        }

        String formatted = text.trim();

        formatted = formatted.replaceAll(
                "\\*\\*(.+?)\\*\\*",
                "<b>$1</b>"
        );

        formatted = formatted
                .replace("```html", "")
                .replace("```", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "")
                .replace("\n", "<br>");

        return HtmlCompat.fromHtml(
                formatted,
                HtmlCompat.FROM_HTML_MODE_COMPACT
        );
    }

    static class UserMessageViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView tvMessage;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(
                    R.id.tvUserMessage
            );
        }
    }

    static class AiMessageViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView tvMessage;

        public AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(
                    R.id.tvAiMessage
            );
        }
    }
}