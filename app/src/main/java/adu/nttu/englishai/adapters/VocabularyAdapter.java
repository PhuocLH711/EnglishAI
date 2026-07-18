package adu.nttu.englishai.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.activities.VocabularyDetailActivity;
import adu.nttu.englishai.models.Vocabulary;

// =========================================================================
// VOCABULARY ADAPTER: Cầu nối hiển thị danh sách từ vựng & đồng bộ Firebase
// =========================================================================
public class VocabularyAdapter extends RecyclerView.Adapter<VocabularyAdapter.ViewHolder> {

    // Danh sách lưu trữ bộ từ vựng được tải từ Firestore về
    private final List<Vocabulary> vocabularyList;

    // Constructor: Nhận dữ liệu từ Fragment/Activity truyền vào
    public VocabularyAdapter(List<Vocabulary> vocabularyList) {
        this.vocabularyList = vocabularyList;
    }

    // =========================================================================
    // HÀM 1: TẠO KHUNG GIAO DIỆN (INFLATE LAYOUT)
    // =========================================================================
    // Được gọi khi RecyclerView cần tạo mới một thẻ từ vựng trên màn hình
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Bơm (inflate) bản thiết kế XML item_vocabulary thành đối tượng View thực tế
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vocabulary, parent, false);
        return new ViewHolder(view);
    }

    // =========================================================================
    // HÀM 2: ĐỔ DỮ LIỆU VÀO KHUNG (BIND DATA & UI STYLING)
    // =========================================================================
    // Được gọi liên tục mỗi khi người dùng cuộn danh sách để đổ dữ liệu tại vị trí position
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Lấy đối tượng từ vựng tại vị trí hiện tại
        Vocabulary vocabulary = vocabularyList.get(position);

        // Ánh xạ dữ liệu văn bản lên các thẻ TextView
        holder.tvEnglish.setText(vocabulary.getEnglishWord());
        holder.tvVietnamese.setText(vocabulary.getVietnameseMeaning());
        holder.tvPronunciation.setText(vocabulary.getPronunciation());
        holder.tvCategory.setText(vocabulary.getCategory());

        String status = vocabulary.getLearningStatus();

        // 🎨 TỐI ƯU UI: Xử lý màu sắc và nội dung của Nhãn trạng thái học tập
        // Dùng .equalsIgnoreCase() để không phân biệt chữ hoa/thường (ví dụ "learning" hay "LEARNING" đều nhận)
        if ("LEARNING".equalsIgnoreCase(status)) {
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟡 Đang học");
            holder.tvLearnedStatus.setBackgroundColor(Color.parseColor("#FFF3CD")); // Nền vàng nhạt
            holder.tvLearnedStatus.setTextColor(Color.parseColor("#856404"));       // Chữ vàng cam đậm
        } else if ("LEARNED".equalsIgnoreCase(status) || "mastered".equalsIgnoreCase(status)) {
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟢 Đã học");
            holder.tvLearnedStatus.setBackgroundColor(Color.parseColor("#D4EDDA")); // Nền xanh lá nhạt
            holder.tvLearnedStatus.setTextColor(Color.parseColor("#155724"));       // Chữ xanh lá đậm
        } else {
            // Nếu chưa bắt đầu học ("NOT_STARTED" hoặc null) -> Ẩn luôn nhãn đi cho thẻ từ vựng gọn gàng
            holder.tvLearnedStatus.setVisibility(View.GONE);
        }

        // 🎨 TỐI ƯU UI: Đổi màu icon Ngôi sao yêu thích
        if (vocabulary.isFavorite()) {
            // Nếu đã yêu thích: Bật ngôi sao sáng vàng óng
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
            holder.btnFavorite.setColorFilter(Color.parseColor("#FFC107"));
        } else {
            // Nếu chưa yêu thích: Bật ngôi sao rỗng màu xám nhạt
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
            holder.btnFavorite.setColorFilter(Color.parseColor("#B0BEC5"));
        }

        // =========================================================================
        // 👈 CƠ CHẾ QUÉT LƯỚI BAO DÍNH: KHÔNG PHÂN BIỆT HOA/THƯỜNG NÊN 100% LƯU ĐƯỢC
        // =========================================================================
        holder.btnFavorite.setOnClickListener(view -> {
            // 1. KỸ THUẬT OPTIMISTIC UI (Cập nhật giao diện lập tức trước khi gọi mạng)
            // Đảo ngược trạng thái yêu thích trong bộ nhớ tạm (true -> false, false -> true)
            boolean newState = !vocabulary.isFavorite();
            vocabulary.setFavorite(newState);

            // Lấy vị trí thực tế của thẻ đang bấm và ra lệnh vẽ lại đúng 1 thẻ này
            // Giúp icon ngôi sao đổi màu ngay lập tức mà không cần chờ Firebase phản hồi -> Trải nghiệm cực mượt
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(currentPosition);
            }

            // 2. CẬP NHẬT ĐỒNG BỘ LÊN CLOUD FIRESTORE
            String wordToSearch = vocabulary.getEnglishWord();
            if (wordToSearch != null && !wordToSearch.trim().isEmpty()) {
                String target = wordToSearch.trim();

                // Gửi truy vấn lấy toàn bộ bảng "vocabulary" về để đối chiếu
                FirebaseFirestore.getInstance().collection("vocabulary")
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            boolean foundAndUpdated = false;
                            for (DocumentSnapshot doc : snapshots) {
                                // BÍ KÍP BAO DUNG DỮ LIỆU (Data Tolerance):
                                // Do NoSQL có thể do nhiều người nhập hoặc nhiều phiên bản cũ để lại,
                                // ta quét kiểm tra cả 4 trường hợp tên cột phổ biến có thể chứa từ tiếng Anh
                                String w1 = doc.getString("englishWord");
                                String w2 = doc.getString("word");
                                String w3 = doc.getString("english");
                                String w4 = doc.getString("name");

                                // So sánh KHÔNG PHÂN BIỆT chữ hoa hay chữ thường (Apple == apple == APPLE)
                                if ((w1 != null && w1.equalsIgnoreCase(target)) ||
                                        (w2 != null && w2.equalsIgnoreCase(target)) ||
                                        (w3 != null && w3.equalsIgnoreCase(target)) ||
                                        (w4 != null && w4.equalsIgnoreCase(target))) {

                                    // Tìm thấy tài liệu tương ứng -> Khắc trạng thái tim lên cả 3 tên cột phổ biến
                                    // Đảm bảo dù app ở màn hình nào đọc biến nào (isFavorite, favorite hay isFav) đều chính xác 100%
                                    doc.getReference().update(
                                            "isFavorite", newState,
                                            "favorite", newState,
                                            "isFav", newState
                                    );
                                    foundAndUpdated = true;
                                }
                            }
                            if (!foundAndUpdated) {
                                Toast.makeText(view.getContext(), "⚠️ Không tìm thấy từ '" + target + "' trên Firebase!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(view.getContext(), "Lỗi mạng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // =========================================================================
        // SỰ KIỆN BẤM VÀO THẺ: CHUYỂN SANG MÀN HÌNH CHI TIẾT TỪ VỰNG
        // =========================================================================
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), VocabularyDetailActivity.class);

            // Đóng gói toàn bộ thông tin của từ vựng hiện tại vào Intent để gửi sang màn hình Chi tiết
            intent.putExtra("id", vocabulary.getId());
            intent.putExtra("englishWord", vocabulary.getEnglishWord());
            intent.putExtra("vietnameseMeaning", vocabulary.getVietnameseMeaning());
            intent.putExtra("pronunciation", vocabulary.getPronunciation());
            intent.putExtra("example", vocabulary.getExample());
            intent.putExtra("category", vocabulary.getCategory());
            intent.putExtra("level", vocabulary.getLevel());
            intent.putExtra("favorite", vocabulary.isFavorite());

            // Từ Adapter muốn chuyển màn hình phải gọi thông qua Context
            view.getContext().startActivity(intent);
        });
    }

    // Trả về tổng số lượng từ vựng có trong danh sách
    @Override
    public int getItemCount() {
        return vocabularyList != null ? vocabularyList.size() : 0;
    }

    // =========================================================================
    // LỚP VIEW HOLDER NỘI BỘ (LƯU TRỮ VÀ ÁNH XẠ CÁC THẺ UI)
    // =========================================================================
    // Dùng từ khóa static để tối ưu bộ nhớ, ngăn lớp này ngậm tham chiếu đến lớp Adapter bên ngoài
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLearnedStatus;
        TextView tvEnglish;
        TextView tvVietnamese;
        TextView tvPronunciation;
        TextView tvCategory;
        ImageButton btnFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ ID một lần duy nhất lúc khởi tạo để tái sử dụng, giúp cuộn danh sách siêu mượt
            tvLearnedStatus = itemView.findViewById(R.id.tvLearnedStatus);
            tvEnglish = itemView.findViewById(R.id.tvEnglishWord);
            tvVietnamese = itemView.findViewById(R.id.tvVietnameseMeaning);
            tvPronunciation = itemView.findViewById(R.id.tvPronunciation);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}