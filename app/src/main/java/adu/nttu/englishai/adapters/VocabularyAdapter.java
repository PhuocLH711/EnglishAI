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

public class VocabularyAdapter extends RecyclerView.Adapter<VocabularyAdapter.ViewHolder> {

    private final List<Vocabulary> vocabularyList;

    public VocabularyAdapter(List<Vocabulary> vocabularyList) {
        this.vocabularyList = vocabularyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vocabulary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vocabulary vocabulary = vocabularyList.get(position);

        holder.tvEnglish.setText(vocabulary.getEnglishWord());
        holder.tvVietnamese.setText(vocabulary.getVietnameseMeaning());
        holder.tvPronunciation.setText(vocabulary.getPronunciation());
        holder.tvCategory.setText(vocabulary.getCategory());

        String status = vocabulary.getLearningStatus();

        // 🎨 TỐI ƯU UI: Nhãn trạng thái
        if ("LEARNING".equalsIgnoreCase(status)) {
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟡 Đang học");
            holder.tvLearnedStatus.setBackgroundColor(Color.parseColor("#FFF3CD"));
            holder.tvLearnedStatus.setTextColor(Color.parseColor("#856404"));
        } else if ("LEARNED".equalsIgnoreCase(status) || "mastered".equalsIgnoreCase(status)) {
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟢 Đã học");
            holder.tvLearnedStatus.setBackgroundColor(Color.parseColor("#D4EDDA"));
            holder.tvLearnedStatus.setTextColor(Color.parseColor("#155724"));
        } else {
            holder.tvLearnedStatus.setVisibility(View.GONE);
        }

        // 🎨 TỐI ƯU UI: Ngôi sao yêu thích
        if (vocabulary.isFavorite()) {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
            holder.btnFavorite.setColorFilter(Color.parseColor("#FFC107"));
        } else {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
            holder.btnFavorite.setColorFilter(Color.parseColor("#B0BEC5"));
        }

        // =========================================================================
        // 👈 CƠ CHẾ QUÉT LƯỚI BAO DÍNH: KHÔNG PHÂN BIỆT HOA/THƯỜNG NÊN 100% LƯU ĐƯỢC
        // =========================================================================
        holder.btnFavorite.setOnClickListener(view -> {
            boolean newState = !vocabulary.isFavorite();
            vocabulary.setFavorite(newState);

            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(currentPosition);
            }

            String wordToSearch = vocabulary.getEnglishWord();
            if (wordToSearch != null && !wordToSearch.trim().isEmpty()) {
                String target = wordToSearch.trim();
                FirebaseFirestore.getInstance().collection("vocabulary")
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            boolean foundAndUpdated = false;
                            for (DocumentSnapshot doc : snapshots) {
                                // Lấy tất cả các cột có khả năng chứa tiếng Anh trên Firebase của bạn
                                String w1 = doc.getString("englishWord");
                                String w2 = doc.getString("word");
                                String w3 = doc.getString("english");
                                String w4 = doc.getString("name");

                                // So sánh KHÔNG PHÂN BIỆT chữ hoa hay chữ thường (Apple == apple == APPLE)
                                if ((w1 != null && w1.equalsIgnoreCase(target)) ||
                                        (w2 != null && w2.equalsIgnoreCase(target)) ||
                                        (w3 != null && w3.equalsIgnoreCase(target)) ||
                                        (w4 != null && w4.equalsIgnoreCase(target))) {

                                    // Tìm thấy -> Khắc tim lên cả 3 tên cột phổ biến để không bao giờ trượt
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

        // Bấm vào thẻ từ vựng để mở trang chi tiết
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), VocabularyDetailActivity.class);
            intent.putExtra("id", vocabulary.getId());
            intent.putExtra("englishWord", vocabulary.getEnglishWord());
            intent.putExtra("vietnameseMeaning", vocabulary.getVietnameseMeaning());
            intent.putExtra("pronunciation", vocabulary.getPronunciation());
            intent.putExtra("example", vocabulary.getExample());
            intent.putExtra("category", vocabulary.getCategory());
            intent.putExtra("level", vocabulary.getLevel());
            intent.putExtra("favorite", vocabulary.isFavorite());

            view.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return vocabularyList != null ? vocabularyList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLearnedStatus;
        TextView tvEnglish;
        TextView tvVietnamese;
        TextView tvPronunciation;
        TextView tvCategory;
        ImageButton btnFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLearnedStatus = itemView.findViewById(R.id.tvLearnedStatus);
            tvEnglish = itemView.findViewById(R.id.tvEnglishWord);
            tvVietnamese = itemView.findViewById(R.id.tvVietnameseMeaning);
            tvPronunciation = itemView.findViewById(R.id.tvPronunciation);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}