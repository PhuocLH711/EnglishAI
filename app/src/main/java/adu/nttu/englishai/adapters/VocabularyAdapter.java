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

public class VocabularyAdapter
        extends RecyclerView.Adapter<VocabularyAdapter.ViewHolder> {

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_vocabulary,
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
        Vocabulary vocabulary = vocabularyList.get(position);

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

        holder.btnFavorite.setOnClickListener(view -> {
            int currentPosition =
                    holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            boolean oldState = vocabulary.isFavorite();
            boolean newState = !oldState;

            vocabulary.setFavorite(newState);
            notifyItemChanged(currentPosition);

            if (favoriteClickListener != null) {
                favoriteClickListener.onFavoriteClick(
                        vocabulary,
                        newState
                );
            }
        });

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(
                    view.getContext(),
                    VocabularyDetailActivity.class
            );

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

    private void bindLearningStatus(
            ViewHolder holder,
            Vocabulary vocabulary
    ) {
        String status = vocabulary.getLearningStatus();

        if (Vocabulary.STATUS_LEARNING.equalsIgnoreCase(status)) {
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
            holder.tvLearnedStatus.setVisibility(View.VISIBLE);
            holder.tvLearnedStatus.setText("🟢 Đã học");
            holder.tvLearnedStatus.setBackgroundColor(
                    Color.parseColor("#D4EDDA")
            );
            holder.tvLearnedStatus.setTextColor(
                    Color.parseColor("#155724")
            );

        } else {
            holder.tvLearnedStatus.setVisibility(View.GONE);
        }
    }

    private void bindFavoriteButton(
            ViewHolder holder,
            Vocabulary vocabulary
    ) {
        if (vocabulary.isFavorite()) {
            holder.btnFavorite.setImageResource(
                    android.R.drawable.btn_star_big_on
            );
            holder.btnFavorite.setColorFilter(
                    Color.parseColor("#FFC107")
            );
        } else {
            holder.btnFavorite.setImageResource(
                    android.R.drawable.btn_star_big_off
            );
            holder.btnFavorite.setColorFilter(
                    Color.parseColor("#B0BEC5")
            );
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    @Override
    public int getItemCount() {
        return vocabularyList == null
                ? 0
                : vocabularyList.size();
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