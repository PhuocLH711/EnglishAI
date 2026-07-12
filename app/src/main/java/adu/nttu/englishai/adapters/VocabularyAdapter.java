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

        // 🎨 TỐI ƯU UI: Đổi cả màu nền lẫn màu chữ cho nhãn trạng thái
        if ("LEARNING".equals(status)) {
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟡 Đang học");
            holder.tvLearnedStatus.setBackgroundColor(Color.parseColor("#FFF3CD")); // Nền vàng nhạt
            holder.tvLearnedStatus.setTextColor(Color.parseColor("#856404"));       // Chữ vàng cam đậm
        } else if ("LEARNED".equals(status)) {
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟢 Đã học");
            holder.tvLearnedStatus.setBackgroundColor(Color.parseColor("#D4EDDA")); // Nền xanh lá nhạt
            holder.tvLearnedStatus.setTextColor(Color.parseColor("#155724"));       // Chữ xanh lá đậm
        } else {
            holder.tvLearnedStatus.setVisibility(View.GONE);
        }

        // 🎨 TỐI ƯU UI: Ngôi sao yêu thích màu vàng óng
        if (vocabulary.isFavorite()) {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
            holder.btnFavorite.setColorFilter(Color.parseColor("#FFC107")); // Màu vàng óng
        } else {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
            holder.btnFavorite.setColorFilter(Color.parseColor("#B0BEC5")); // Màu xám nhạt
        }

        // Bấm ngôi sao để đổi trạng thái yêu thích
        holder.btnFavorite.setOnClickListener(view -> {
            vocabulary.setFavorite(!vocabulary.isFavorite());
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(currentPosition);
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