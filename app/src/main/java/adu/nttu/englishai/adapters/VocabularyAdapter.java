package adu.nttu.englishai.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.activities.VocabularyDetailActivity;
import adu.nttu.englishai.models.Vocabulary;

// =========================================================================
// VOCABULARY ADAPTER: Cầu nối hiển thị danh sách từ vựng lên RecyclerView
// =========================================================================
public class VocabularyAdapter
        extends RecyclerView.Adapter<VocabularyAdapter.ViewHolder> {

    //INTERFACE GIAO TIẾP (CALLBACK PATTERN):
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Vocabulary vocabulary, boolean newState);
    }

    private final List<Vocabulary> vocabularyList;
    private final OnFavoriteClickListener favoriteClickListener;

    public VocabularyAdapter(
            List<Vocabulary> vocabularyList,
            OnFavoriteClickListener favoriteClickListener
    ) {
        this.vocabularyList = vocabularyList;
        this.favoriteClickListener = favoriteClickListener;
    }

    // =========================================================================
    // HÀM 1: TẠO KHUNG GIAO DIỆN THẺ TỪ VỰNG (ON CREATE VIEW HOLDER)
    // =========================================================================
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        // Bơm (inflate) bản thiết kế XML item_vocabulary thành một đối tượng View trong bộ nhớ
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_vocabulary,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    // =========================================================================
    // HÀM 2: ĐỔ DỮ LIỆU VÀO KHUNG THẺ (ON BIND VIEW HOLDER)
    // =========================================================================
    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        // Lấy đối tượng từ vựng tại vị trí position đang được cuộn tới
        Vocabulary vocabulary = vocabularyList.get(position);

        // Gán các thông tin văn bản lên giao diện (Sử dụng safeText để chống hiển thị chữ null hoặc crash)
        holder.tvEnglish.setText(
                safeText(vocabulary.getEnglishWord())
        );

        holder.tvVietnamese.setText(
                safeText(vocabulary.getVietnameseMeaning())
        );

        holder.tvPronunciation.setText(
                safeText(vocabulary.getPronunciation())
        );

        holder.tvCategory.setText(
                safeText(vocabulary.getCategory())
        );

        bindLearningStatus(holder, vocabulary);
        bindFavoriteButton(holder, vocabulary);

        // =========================================================================
        // SỰ KIỆN 1: BẤM NÚT THẢ TIM (OPTIMISTIC UI UPDATE)
        // =========================================================================
        holder.btnFavorite.setOnClickListener(view -> {
            /*
             * KỸ THUẬT LẬP TRÌNH PHÒNG VỆ (DEFENSIVE INDEX CHECK):
             * Sử dụng getBindingAdapterPosition() thay cho position cố định trong hàm.
             * Nếu người dùng vừa bấm thả tim đúng lúc họ cuộn tay thật nhanh hoặc thẻ đang bị xóa,
             * vị trí trả về có thể là NO_POSITION (-1). Lệnh kiểm tra này chặn đứng lỗi IndexOutOfBoundsException!
             */
            int currentPosition =
                    holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            boolean oldState = vocabulary.isFavorite();
            boolean newState = !oldState;

            /*
             * CẬP NHẬT GIAO DIỆN LẠC QUAN (OPTIMISTIC UI UPDATE):
             * Nếu kết nối mạng thất bại, bên VocabularyFragment sẽ tự động hoàn tác lại màu ngôi sao này sau.
             */
            vocabulary.setFavorite(newState);
            notifyItemChanged(currentPosition);

            if (favoriteClickListener != null) {
                favoriteClickListener.onFavoriteClick(
                        vocabulary,
                        newState
                );
            }
        });

        // =========================================================================
        // SỰ KIỆN 2: BẤM VÀO THẺ TỪ VỰNG -> MỞ TRANG CHI TIẾT (INTENT BUNDLING)
        // =========================================================================
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(
                    view.getContext(),
                    VocabularyDetailActivity.class
            );

            // Đóng gói toàn bộ thông tin của từ vựng vào Intent để gửi sang màn hình Chi tiết
            intent.putExtra("id", vocabulary.getId());
            intent.putExtra(
                    "englishWord",
                    vocabulary.getEnglishWord()
            );
            intent.putExtra(
                    "vietnameseMeaning",
                    vocabulary.getVietnameseMeaning()
            );
            intent.putExtra(
                    "pronunciation",
                    vocabulary.getPronunciation()
            );
            intent.putExtra(
                    "wordType",
                    vocabulary.getWordType()
            );
            intent.putExtra(
                    "example",
                    vocabulary.getExample()
            );
            intent.putExtra(
                    "exampleMeaning",
                    vocabulary.getExampleMeaning()
            );
            intent.putExtra(
                    "category",
                    vocabulary.getCategory()
            );
            intent.putExtra(
                    "level",
                    vocabulary.getLevel()
            );
            intent.putExtra(
                    "imageUrl",
                    vocabulary.getImageUrl()
            );
            intent.putExtra(
                    "favorite",
                    vocabulary.isFavorite()
            );
            intent.putExtra(
                    "learningStatus",
                    vocabulary.getLearningStatus()
            );

            view.getContext().startActivity(intent);
        });
    }

    // =========================================================================
    // HÀM PHỤ 1: HIỂN THỊ NHÃN TRẠNG THÁI HỌC TẬP (DYNAMIC STATUS STYLING)
    // =========================================================================
    private void bindLearningStatus(
            ViewHolder holder,
            Vocabulary vocabulary
    ) {
        String status = vocabulary.getLearningStatus();

        if (Vocabulary.STATUS_LEARNING.equalsIgnoreCase(status)) {
            // TRƯỜNG HỢP 1: ĐANG HỌC
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟡 Đang học");
            holder.tvLearnedStatus.setBackgroundColor(
                    Color.parseColor("#FFF3CD")
            );
            holder.tvLearnedStatus.setTextColor(
                    Color.parseColor("#856404")
            );

        } else if (
                Vocabulary.STATUS_LEARNED.equalsIgnoreCase(status)
                        || "MASTERED".equalsIgnoreCase(status)
        ) {
            // TRƯỜNG HỢP 2: ĐÃ HỌC
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟢 Đã học");
            holder.tvLearnedStatus.setBackgroundColor(
                    Color.parseColor("#D4EDDA")
            );
            holder.tvLearnedStatus.setTextColor(
                    Color.parseColor("#155724")
            );

        } else {
            // TRƯỜNG HỢP 3: CHƯA HỌC
            holder.tvLearnedStatus.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // HÀM PHỤ 2: HIỂN THỊ MÀU NGÔI SAO YÊU THÍCH
    // =========================================================================
    private void bindFavoriteButton(
            ViewHolder holder,
            Vocabulary vocabulary
    ) {
        if (vocabulary.isFavorite()) {
            // Đã yêu thích
            holder.btnFavorite.setImageResource(
                    android.R.drawable.btn_star_big_on
            );
            holder.btnFavorite.setColorFilter(
                    Color.parseColor("#FFC107")
            );
        } else {
            // Chưa yêu thích
            holder.btnFavorite.setImageResource(
                    android.R.drawable.btn_star_big_off
            );
            holder.btnFavorite.setColorFilter(
                    Color.parseColor("#B0BEC5")
            );
        }
    }

    // Hàm tiện ích: Đảm bảo chuỗi trả về không bao giờ bị null gây vỡ layout hay crash
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    @Override
    public int getItemCount() {
        return vocabularyList == null
                ? 0
                : vocabularyList.size();
    }

    // =========================================================================
    // LỚP VIEW HOLDER: TỐI ƯU HÓA BỘ NHỚ RAM (VIEW HOLDER PATTERN)
    // =========================================================================
    /*
     * TỐI ƯU HIỆU NĂNG:
     * Lớp ViewHolder đóng vai trò cache các tham chiếu đến thẻ TextView, ImageButton trong XML.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvLearnedStatus;
        TextView tvEnglish;
        TextView tvVietnamese;
        TextView tvPronunciation;
        TextView tvCategory;
        ImageButton btnFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvLearnedStatus =
                    itemView.findViewById(R.id.tvLearnedStatus);

            tvEnglish =
                    itemView.findViewById(R.id.tvEnglishWord);

            tvVietnamese =
                    itemView.findViewById(
                            R.id.tvVietnameseMeaning
                    );

            tvPronunciation =
                    itemView.findViewById(R.id.tvPronunciation);

            tvCategory =
                    itemView.findViewById(R.id.tvCategory);

            btnFavorite =
                    itemView.findViewById(R.id.btnFavorite);
        }
    }
}