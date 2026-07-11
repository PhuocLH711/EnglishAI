package adu.nttu.englishai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

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
        if (vocabulary.isFavorite()) {
            holder.btnFavorite.setImageResource(
                    android.R.drawable.btn_star_big_on
            );
        } else {
            holder.btnFavorite.setImageResource(
                    android.R.drawable.btn_star_big_off
            );
        }

        holder.btnFavorite.setOnClickListener(view -> {
            vocabulary.setFavorite(!vocabulary.isFavorite());

            notifyItemChanged(holder.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return vocabularyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvEnglish;
        TextView tvVietnamese;
        TextView tvPronunciation;
        TextView tvCategory;
        ImageButton btnFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvEnglish = itemView.findViewById(R.id.tvEnglishWord);
            tvVietnamese = itemView.findViewById(R.id.tvVietnameseMeaning);
            tvPronunciation = itemView.findViewById(R.id.tvPronunciation);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);

        }
    }

}