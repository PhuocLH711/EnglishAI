package adu.nttu.englishai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.content.Intent;

import adu.nttu.englishai.activities.VocabularyDetailActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.Vocabulary;

public class VocabularyAdapter extends RecyclerView.Adapter<VocabularyAdapter.ViewHolder> {

    private List<Vocabulary> vocabularyList;

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

        if (vocabulary.isLearned()) {
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvLearnedStatus.setVisibility(View.GONE);
        }
        if (vocabulary.isFavorite()) {
            holder.btnFavorite.setImageResource(
                    android.R.drawable.btn_star_big_on
            );
        } else {
            holder.btnFavorite.setImageResource(
                    android.R.drawable.btn_star_big_off
            );
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
            Intent intent = new Intent(
                    view.getContext(),
                    VocabularyDetailActivity.class
            );

            intent.putExtra("id", vocabulary.getId());
            intent.putExtra("englishWord", vocabulary.getEnglishWord());
            intent.putExtra(
                    "vietnameseMeaning",
                    vocabulary.getVietnameseMeaning()
            );
            intent.putExtra(
                    "pronunciation",
                    vocabulary.getPronunciation()
            );
            intent.putExtra("example", vocabulary.getExample());
            intent.putExtra("category", vocabulary.getCategory());
            intent.putExtra("level", vocabulary.getLevel());
            intent.putExtra("favorite", vocabulary.isFavorite());

            view.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return vocabularyList.size();
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