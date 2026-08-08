package adu.nttu.englishai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.ToeicTest;

public class ToeicStudentTestAdapter
        extends RecyclerView.Adapter<ToeicStudentTestAdapter.TestViewHolder> {

    public interface TestListener {
        void onPractice(ToeicTest test);
        void onMockTest(ToeicTest test);
    }

    private final List<ToeicTest> tests =
            new ArrayList<>();

    private final TestListener listener;

    public ToeicStudentTestAdapter(
            TestListener listener
    ) {
        this.listener = listener;
    }

    public void submitList(
            List<ToeicTest> newTests
    ) {
        tests.clear();

        if (newTests != null) {
            tests.addAll(newTests);
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
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_toeic_student_test,
                                parent,
                                false
                        );

        return new TestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TestViewHolder holder,
            int position
    ) {

        ToeicTest test =
                tests.get(position);

        holder.tvTitle.setText(
                safe(
                        test.getTitle(),
                        "TOEIC Practice"
                )
        );

        String source =
                safe(
                        test.getSourceName(),
                        "Nguồn chưa ghi"
                );

        holder.tvSource.setText(
                source
        );

        holder.tvMeta.setText(
                test.getTotalQuestions()
                        + " câu • "
                        + test.getDurationMinutes()
                        + " phút • "
                        + buildParts(test)
        );

        holder.btnPractice.setOnClickListener(
                view ->
                        listener.onPractice(test)
        );

        holder.btnMock.setOnClickListener(
                view ->
                        listener.onMockTest(test)
        );

        holder.card.setOnClickListener(
                view ->
                        listener.onPractice(test)
        );
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    private String buildParts(
            ToeicTest test
    ) {

        if (test.getAvailableParts().isEmpty()) {
            return "Part 1-7";
        }

        StringBuilder builder =
                new StringBuilder(
                        "Part "
                );

        for (int i = 0;
                i < test.getAvailableParts().size();
                i++) {

            if (i > 0) {
                builder.append(", ");
            }

            builder.append(
                    test.getAvailableParts().get(i)
            );
        }

        return builder.toString();
    }

    private String safe(
            String value,
            String fallback
    ) {

        if (value == null
                || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    static class TestViewHolder
            extends RecyclerView.ViewHolder {

        MaterialCardView card;

        TextView tvTitle;
        TextView tvSource;
        TextView tvMeta;

        MaterialButton btnPractice;
        MaterialButton btnMock;

        TestViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            card =
                    itemView.findViewById(
                            R.id.cardToeicStudentTest
                    );

            tvTitle =
                    itemView.findViewById(
                            R.id.tvToeicStudentTitle
                    );

            tvSource =
                    itemView.findViewById(
                            R.id.tvToeicStudentSource
                    );

            tvMeta =
                    itemView.findViewById(
                            R.id.tvToeicStudentMeta
                    );

            btnPractice =
                    itemView.findViewById(
                            R.id.btnToeicPractice
                    );

            btnMock =
                    itemView.findViewById(
                            R.id.btnToeicMock
                    );
        }
    }
}
