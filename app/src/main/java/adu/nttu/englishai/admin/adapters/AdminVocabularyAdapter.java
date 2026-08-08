package adu.nttu.englishai.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.admin.repositories.AdminVocabularyRepository.AdminVocabulary;

/**
 * Adapter danh sách từ vựng trong Admin.
 */
public class AdminVocabularyAdapter
        extends RecyclerView.Adapter<AdminVocabularyAdapter.VocabularyViewHolder> {

    public interface VocabularyClickListener {
        void onVocabularyClick(AdminVocabulary vocabulary);
    }

    private final List<AdminVocabulary> allItems =
            new ArrayList<>();

    private final List<AdminVocabulary> visibleItems =
            new ArrayList<>();

    private final VocabularyClickListener listener;

    public AdminVocabularyAdapter(
            VocabularyClickListener listener
    ) {
        this.listener = listener;
    }

    public void submitList(
            List<AdminVocabulary> items
    ) {

        allItems.clear();
        visibleItems.clear();

        if (items != null) {
            allItems.addAll(items);
            visibleItems.addAll(items);
        }

        notifyDataSetChanged();
    }

    public void filter(
            String query
    ) {

        visibleItems.clear();

        String normalized =
                query == null
                        ? ""
                        : query.trim()
                        .toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {

            visibleItems.addAll(
                    allItems
            );

        } else {

            for (AdminVocabulary item
                    : allItems) {

                String word =
                        item.getEnglishWord()
                                .toLowerCase(Locale.ROOT);

                String meaning =
                        item.getVietnameseMeaning()
                                .toLowerCase(Locale.ROOT);

                String topic =
                        item.getTopic()
                                .toLowerCase(Locale.ROOT);

                String level =
                        item.getLevel()
                                .toLowerCase(Locale.ROOT);

                if (word.contains(normalized)
                        || meaning.contains(normalized)
                        || topic.contains(normalized)
                        || level.contains(normalized)) {

                    visibleItems.add(item);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VocabularyViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_admin_vocabulary,
                                parent,
                                false
                        );

        return new VocabularyViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull VocabularyViewHolder holder,
            int position
    ) {

        AdminVocabulary item =
                visibleItems.get(position);

        holder.tvWord.setText(
                item.getEnglishWord()
        );

        holder.tvPhonetic.setText(
                item.getPhonetic().isEmpty()
                        ? "—"
                        : item.getPhonetic()
        );

        holder.tvMeaning.setText(
                item.getVietnameseMeaning().isEmpty()
                        ? "Chưa có nghĩa tiếng Việt"
                        : item.getVietnameseMeaning()
        );

        String meta =
                buildMeta(item);

        holder.tvMeta.setText(
                meta
        );

        holder.card.setOnClickListener(
                view ->
                        listener.onVocabularyClick(
                                item
                        )
        );
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    private String buildMeta(
            AdminVocabulary item
    ) {

        StringBuilder builder =
                new StringBuilder();

        if (!item.getTopic().isEmpty()) {
            builder.append(
                    item.getTopic()
            );
        }

        if (!item.getLevel().isEmpty()) {

            if (builder.length() > 0) {
                builder.append(" • ");
            }

            builder.append(
                    item.getLevel()
            );
        }

        if (builder.length() == 0) {
            return "Chưa phân loại";
        }

        return builder.toString();
    }

    static class VocabularyViewHolder
            extends RecyclerView.ViewHolder {

        MaterialCardView card;

        TextView tvWord;
        TextView tvPhonetic;
        TextView tvMeaning;
        TextView tvMeta;

        public VocabularyViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            card =
                    itemView.findViewById(
                            R.id.cardAdminVocabularyItem
                    );

            tvWord =
                    itemView.findViewById(
                            R.id.tvAdminVocabularyWord
                    );

            tvPhonetic =
                    itemView.findViewById(
                            R.id.tvAdminVocabularyPhonetic
                    );

            tvMeaning =
                    itemView.findViewById(
                            R.id.tvAdminVocabularyMeaning
                    );

            tvMeta =
                    itemView.findViewById(
                            R.id.tvAdminVocabularyMeta
                    );
        }
    }
}
