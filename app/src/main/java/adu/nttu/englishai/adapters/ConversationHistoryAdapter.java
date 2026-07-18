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

// =========================================================================
// CONVERSATION HISTORY ADAPTER: Bộ chuyển đổi dữ liệu vẽ danh sách lịch sử chat
// =========================================================================
public class ConversationHistoryAdapter
        extends RecyclerView.Adapter<
        ConversationHistoryAdapter.ViewHolder> {

    // =========================================================================
    // INTERFACE CALLBACK (GIAO TIẾP VỚI ACTIVITY)
    // =========================================================================
    // Định nghĩa một giao diện lắng nghe sự kiện. Khi người dùng bấm Mở hoặc Xóa một thẻ,
    // Adapter sẽ không tự xử lý mà gọi callback báo ngược lại cho ChatHistoryActivity giải quyết.
    public interface ConversationListener {
        void onOpen(AiConversation conversation);

        void onDelete(AiConversation conversation);
    }

    // Danh sách các cuộc hội thoại được truyền vào và đối tượng lắng nghe sự kiện
    private final List<AiConversation> conversationList;
    private final ConversationListener listener;

    // Constructor: Khởi tạo Adapter với danh sách dữ liệu và bộ lắng nghe từ Activity
    public ConversationHistoryAdapter(
            List<AiConversation> conversationList,
            ConversationListener listener
    ) {
        this.conversationList = conversationList;
        this.listener = listener;
    }

    // =========================================================================
    // HÀM 1: TẠO KHUNG GIAO DIỆN (INFLATE LAYOUT)
    // =========================================================================
    // Được gọi khi RecyclerView cần tạo một thẻ hiển thị mới trên màn hình
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        // Bơm (inflate) bản thiết kế XML của từng dòng lịch sử (item_ai_conversation) thành đối tượng View
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_ai_conversation,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    // =========================================================================
    // HÀM 2: ĐỔ DỮ LIỆU VÀO KHUNG (BIND DATA)
    // =========================================================================
    // Được gọi liên tục mỗi khi cuộn màn hình để lấy dữ liệu tại vị trí (position) đổ vào các thẻ UI
    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        // Lấy đối tượng cuộc hội thoại tại vị trí hiện tại
        AiConversation conversation =
                conversationList.get(position);

        String title = conversation.getTitle();

        // 1. Gán tiêu đề: Kiểm tra null hoặc rỗng, nếu không có tên thì để mặc định là "Cuộc trò chuyện"
        holder.tvTitle.setText(
                title == null || title.trim().isEmpty()
                        ? "Cuộc trò chuyện"
                        : title
        );

        // 2. Gán tên chủ đề: Nếu không có chủ đề thì gán mặc định là "Hỏi tự do"
        holder.tvTopic.setText(
                conversation.getTopicName() == null
                        ? "Hỏi tự do"
                        : conversation.getTopicName()
        );

        // 3. Gán thời gian: Đưa mili-giây qua hàm formatTime để đổi thành định dạng Ngày/Tháng/Năm
        holder.tvTime.setText(
                formatTime(conversation.getUpdatedAt())
        );

        // 4. Xử lý sự kiện CHẠM VÀO THẺ: Gọi hàm onOpen của listener để gửi dữ liệu về Activity
        holder.itemView.setOnClickListener(view ->
                listener.onOpen(conversation)
        );

        // 5. Xử lý sự kiện BẤM NÚT 3 CHẤM (MORE): Mở menu tùy chọn popup
        holder.btnMore.setOnClickListener(view ->
                showMoreMenu(view, conversation)
        );
    }

    // =========================================================================
    // HÀM TẠO VÀ XỬ LÝ MENU TÙY CHỌN (POPUP MENU)
    // =========================================================================
    // Hiển thị một menu nhỏ ngay bên cạnh nút 3 chấm với 2 tùy chọn: Mở và Xóa
    private void showMoreMenu(
            View anchor,
            AiConversation conversation
    ) {
        PopupMenu popupMenu =
                new PopupMenu(anchor.getContext(), anchor);

        // Thêm các mục vào menu
        popupMenu.getMenu().add("Mở cuộc trò chuyện");
        popupMenu.getMenu().add("Xóa cuộc trò chuyện");

        // Lắng nghe sự kiện người dùng chọn mục nào trong menu
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.contains("Mở")) {
                listener.onOpen(conversation); // Báo về Activity để mở
                return true;
            }

            if (title.contains("Xóa")) {
                listener.onDelete(conversation); // Báo về Activity để xóa khỏi Firebase
                return true;
            }

            return false;
        });

        popupMenu.show(); // Hiển thị menu lên màn hình
    }

    // =========================================================================
    // HÀM CHUYỂN ĐỔI THỜI GIAN (DATE FORMATTER)
    // =========================================================================
    // Chuyển đổi con số thời gian (mili-giây tính từ năm 1970) sang chuỗi Ngày/Tháng/Năm Giờ:Phút dễ đọc
    private String formatTime(long time) {
        if (time <= 0) {
            return ""; // Tránh hiển thị ngày lỗi (1/1/1970) nếu thời gian chưa được tạo
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                );

        return formatter.format(new Date(time));
    }

    // Trả về tổng số lượng cuộc hội thoại đang có trong danh sách
    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    // =========================================================================
    // LỚP VIEW HOLDER NỘI BỘ (LƯU TRỮ VÀ ÁNH XẠ CÁC THẺ UI)
    // =========================================================================
    // Dùng từ khóa static để tối ưu bộ nhớ, ngăn lớp này ngậm tham chiếu đến lớp Adapter bên ngoài
    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvTopic;
        private final TextView tvTime;
        private final ImageButton btnMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Tìm và ánh xạ các ID thẻ giao diện chỉ 1 lần duy nhất lúc tạo View
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