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
import adu.nttu.englishai.admin.repositories.ToeicAdminRepository.ToeicAdminTest;

public class ToeicAdminTestAdapter
        extends RecyclerView.Adapter<ToeicAdminTestAdapter.TestViewHolder> {

    public interface TestClickListener {
        void onTestClick(ToeicAdminTest test);
    }

    private final List<ToeicAdminTest> allTests =
            new ArrayList<>();

    private final List<ToeicAdminTest> visibleTests =
            new ArrayList<>();

    private final TestClickListener listener;

    public ToeicAdminTestAdapter(
            TestClickListener listener
    ) {
        this.listener = listener;
    }

    public void submitList(
            List<ToeicAdminTest> tests
    ) {

        allTests.clear();
        visibleTests.clear();

        if (tests != null) {
            allTests.addAll(tests);
            visibleTests.addAll(tests);
        }

        notifyDataSetChanged();
    }

    public void filter(
            String query
    ) {

        visibleTests.clear();

        String normalized =
                query == null
                        ? ""
                        : query.trim()
                        .toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {

            visibleTests.addAll(
                    allTests
            );

        } else {

            for (ToeicAdminTest test
                    : allTests) {

                String title =
                        test.getTitle()
                                .toLowerCase(Locale.ROOT);

                String source =
                        test.getSource()
                                .toLowerCase(Locale.ROOT);

                String difficulty =
                        test.getDifficulty()
                                .toLowerCase(Locale.ROOT);

                String year =
                        String.valueOf(
                                test.getYear()
                        );

                if (title.contains(normalized)
                        || source.contains(normalized)
                        || difficulty.contains(normalized)
                        || year.contains(normalized)) {

                    visibleTests.add(test);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TestViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(
                                parent.getContext()
                        )
                        .inflate(
                                R.layout.item_toeic_admin_test,
                                parent,
                                false
                        );

        return new TestViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull TestViewHolder holder,
            int position
    ) {

        ToeicAdminTest test =
                visibleTests.get(position);

        holder.tvTitle.setText(
                test.getTitle()
        );

        String source =
                test.getSource().isEmpty()
                        ? "Chưa ghi nguồn"
                        : test.getSource();

        String year =
                test.getYear() <= 0
                        ? ""
                        : " • " + test.getYear();

        holder.tvSource.setText(
                source + year
        );

        holder.tvMeta.setText(
                test.getDifficulty()
                        + " • "
                        + test.getDurationMinutes()
                        + " phút • "
                        + test.getTotalQuestions()
                        + " câu"
        );

        holder.tvStatus.setText(
                test.isPublished()
                        ? "PUBLISHED"
                        : "DRAFT"
        );

        holder.card.setOnClickListener(
                view ->
                        listener.onTestClick(
                                test
                        )
        );
    }

    @Override
    public int getItemCount() {
        return visibleTests.size();
    }

    static class TestViewHolder
            extends RecyclerView.ViewHolder {

        MaterialCardView card;

        TextView tvTitle;
        TextView tvSource;
        TextView tvMeta;
        TextView tvStatus;

        TestViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            card =
                    itemView.findViewById(
                            R.id.cardToeicAdminTest
                    );

            tvTitle =
                    itemView.findViewById(
                            R.id.tvToeicAdminTitle
                    );

            tvSource =
                    itemView.findViewById(
                            R.id.tvToeicAdminSource
                    );

            tvMeta =
                    itemView.findViewById(
                            R.id.tvToeicAdminMeta
                    );

            tvStatus =
                    itemView.findViewById(
                            R.id.tvToeicAdminPublished
                    );
        }
    }
}
