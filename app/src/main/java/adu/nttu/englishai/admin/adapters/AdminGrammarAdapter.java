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
import adu.nttu.englishai.admin.repositories.AdminGrammarRepository.AdminGrammarExercise;

/**
 * Adapter danh sách bài Grammar Sprint.
 */
public class AdminGrammarAdapter
        extends RecyclerView.Adapter<AdminGrammarAdapter.ExerciseViewHolder> {

    public interface ExerciseClickListener {
        void onExerciseClick(AdminGrammarExercise exercise);
    }

    private final List<AdminGrammarExercise> allItems =
            new ArrayList<>();

    private final List<AdminGrammarExercise> visibleItems =
            new ArrayList<>();

    private final ExerciseClickListener listener;

    public AdminGrammarAdapter(
            ExerciseClickListener listener
    ) {
        this.listener = listener;
    }

    public void submitList(
            List<AdminGrammarExercise> items
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

            visibleItems.addAll(allItems);

        } else {

            for (AdminGrammarExercise item
                    : allItems) {

                String vi =
                        item.getVietnameseSentence()
                                .toLowerCase(Locale.ROOT);

                String en =
                        item.getCorrectSentence()
                                .toLowerCase(Locale.ROOT);

                String topic =
                        item.getTopic()
                                .toLowerCase(Locale.ROOT);

                String level =
                        item.getLevel()
                                .toLowerCase(Locale.ROOT);

                if (vi.contains(normalized)
                        || en.contains(normalized)
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
    public ExerciseViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_admin_grammar,
                                parent,
                                false
                        );

        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ExerciseViewHolder holder,
            int position
    ) {

        AdminGrammarExercise item =
                visibleItems.get(position);

        holder.tvVietnamese.setText(
                item.getVietnameseSentence().isEmpty()
                        ? "Chưa có câu tiếng Việt"
                        : item.getVietnameseSentence()
        );

        holder.tvEnglish.setText(
                item.getCorrectSentence().isEmpty()
                        ? "Chưa đọc được đáp án tiếng Anh"
                        : item.getCorrectSentence()
        );

        String topic =
                item.getTopic().isEmpty()
                        ? "Chưa phân loại"
                        : item.getTopic();

        String level =
                item.getLevel().isEmpty()
                        ? "—"
                        : item.getLevel();

        holder.tvMeta.setText(
                topic
                        + " • "
                        + level
        );

        holder.card.setOnClickListener(
                view ->
                        listener.onExerciseClick(item)
        );
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    static class ExerciseViewHolder
            extends RecyclerView.ViewHolder {

        MaterialCardView card;

        TextView tvVietnamese;
        TextView tvEnglish;
        TextView tvMeta;

        ExerciseViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            card =
                    itemView.findViewById(
                            R.id.cardAdminGrammarItem
                    );

            tvVietnamese =
                    itemView.findViewById(
                            R.id.tvAdminGrammarVietnamese
                    );

            tvEnglish =
                    itemView.findViewById(
                            R.id.tvAdminGrammarEnglish
                    );

            tvMeta =
                    itemView.findViewById(
                            R.id.tvAdminGrammarMeta
                    );
        }
    }
}
