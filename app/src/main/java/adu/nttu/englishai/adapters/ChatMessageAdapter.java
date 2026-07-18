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

// =========================================================================
// CHAT MESSAGE ADAPTER: Bộ chuyển đổi dữ liệu vẽ màn hình chat 2 bên (User & AI)
// =========================================================================
public class ChatMessageAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Hằng số định danh loại tin nhắn (Dùng để phân biệt vẽ layout bên trái hay bên phải)
    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;

    // Danh sách lưu trữ toàn bộ lịch sử tin nhắn của cuộc trò chuyện hiện tại
    private final List<AiMessage> messageList;

    // Constructor: Nhận danh sách tin nhắn từ Activity truyền vào
    public ChatMessageAdapter(List<AiMessage> messageList) {
        this.messageList = messageList;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 1: PHÂN LOẠI GIAO DIỆN (MULTI-VIEW TYPE)
    // =========================================================================
    // Hàm này tự động quét từng tin nhắn, nếu là người dùng gửi thì gán nhãn TYPE_USER (1),
    // nếu là AI trả lời thì gán nhãn TYPE_AI (2)
    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser()
                ? TYPE_USER
                : TYPE_AI;
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 2: TẠO KHUNG GIAO DIỆN (VIEW HOLDER)
    // =========================================================================
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        // Dựa vào nhãn viewType ở hàm trên để bơm (inflate) đúng bản thiết kế XML
        if (viewType == TYPE_USER) {
            // Nếu là Người dùng -> Vẽ bong bóng chat màu xanh nằm bên PHẢI (item_message_user)
            View view = inflater.inflate(
                    R.layout.item_message_user,
                    parent,
                    false
            );

            return new UserMessageViewHolder(view);
        }

        // Nếu là AI -> Vẽ bong bóng chat màu trắng/xám nằm bên TRÁI (item_message_ai)
        View view = inflater.inflate(
                R.layout.item_message_ai,
                parent,
                false
        );

        return new AiMessageViewHolder(view);
    }

    // =========================================================================
    // HÀM QUAN TRỌNG 3: ĐỔ DỮ LIỆU VÀO KHUNG (BINDING)
    // =========================================================================
    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        // Lấy tin nhắn tại vị trí hiện tại trong danh sách
        AiMessage message = messageList.get(position);

        // Kiểm tra xem khung nhìn hiện tại thuộc lớp nào để ép kiểu (Casting) cho đúng
        if (holder instanceof UserMessageViewHolder) {
            // Tin nhắn người dùng -> Hiển thị văn bản thô bình thường
            ((UserMessageViewHolder) holder)
                    .tvMessage
                    .setText(message.getContent());

        } else if (holder instanceof AiMessageViewHolder) {
            // Tin nhắn AI -> Phải đưa qua hàm formatAiText() để lọc thẻ Markdown rồi mới hiển thị
            ((AiMessageViewHolder) holder)
                    .tvMessage
                    .setText(formatAiText(message.getContent()));
        }
    }

    // Trả về tổng số lượng tin nhắn đang có trong khung chat
    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // =========================================================================
    // HÀM TỐI ƯU UX: CHUYỂN ĐỔI MARKDOWN CỦA GEMINI THÀNH HTML ANDROID
    // =========================================================================
    // AI Gemini thường trả về văn bản có định dạng Markdown (ví dụ: **chữ đậm**, ```html, ### Tiêu đề)
    // TextView thông thường của Android không đọc được Markdown, nên hàm này sẽ lọc và chuyển thành thẻ HTML
    private Spanned formatAiText(String text) {
        if (text == null) {
            return HtmlCompat.fromHtml(
                    "",
                    HtmlCompat.FROM_HTML_MODE_COMPACT
            );
        }

        String formatted = text.trim();

        // 1. Dùng Biểu thức chính quy (Regex) tìm các chữ bọc trong dấu **chữ đậm**
        // Thay thế thành thẻ HTML <b>chữ đậm</b> để in đậm văn bản
        formatted = formatted.replaceAll(
                "\\*\\*(.+?)\\*\\*",
                "<b>$1</b>"
        );

        // 2. Dọn dẹp các ký tự thừa thãi mà AI hay sinh ra, đồng thời chuyển dấu xuống dòng (\n) thành thẻ <br>
        formatted = formatted
                .replace("```html", "")
                .replace("```", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "")
                .replace("\n", "<br>");

        // 3. HtmlCompat.fromHtml: Chuyển đổi chuỗi chứa thẻ HTML thành đối tượng Spanned
        // để TextView có thể vẽ lên màn hình với đầy đủ hiệu ứng in đậm, xuống dòng
        return HtmlCompat.fromHtml(
                formatted,
                HtmlCompat.FROM_HTML_MODE_COMPACT
        );
    }

    // =========================================================================
    // CÁC LỚP VIEW HOLDER NỘI BỘ (LƯU TRỮ THẺ TÌM KIẾM SẴN)
    // =========================================================================
    // ViewHolder cho tin nhắn Người dùng
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

    // ViewHolder cho tin nhắn AI
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