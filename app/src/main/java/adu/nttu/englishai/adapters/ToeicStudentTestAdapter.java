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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.ToeicTest;
import adu.nttu.englishai.repositories.ToeicProgressRepository;

public class ToeicStudentTestAdapter
        extends RecyclerView.Adapter<ToeicStudentTestAdapter.TestViewHolder> {

    public interface TestListener {
        void onPractice(ToeicTest test);
        void onMockTest(ToeicTest test);
    }

    private final List<ToeicTest> tests =
            new ArrayList<>();

    private final Map<String, ToeicProgressRepository.TestProgress> progressMap =
            new HashMap<>();

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

    public void setProgress(
            String testId,
            ToeicProgressRepository.TestProgress progress
    ) {

        if (testId == null
                || progress == null) {
            return;
        }

        progressMap.put(
                testId,
                progress
        );

        for (int i = 0;
             i < tests.size();
             i++) {

            ToeicTest test =
                    tests.get(i);

            if (testId.equals(
                    test.getId()
            )) {

                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public TestViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(
                                parent.getContext()
                        )
                        .inflate(
                                R.layout.item_toeic_student_test,
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

        ToeicTest test =
                tests.get(position);

        holder.tvTitle.setText(
                safe(
                        test.getTitle(),
                        "TOEIC Practice"
                )
        );

        holder.tvSource.setText(
                safe(
                        test.getSourceName(),
                        "EnglishAI TOEIC"
                )
        );

        holder.tvMeta.setText(
                test.getTotalQuestions()
                        + " câu • "
                        + test.getDurationMinutes()
                        + " phút • Part 1-7"
        );

        ToeicProgressRepository.TestProgress progress =
                progressMap.get(
                        test.getId()
                );

        if (progress == null) {

            holder.tvProgress.setText(
                    "Tiến độ: đang tải..."
            );

        } else if (progress.isCompleted()) {

            holder.tvProgress.setText(
                    "✅ ĐÃ HOÀN TẤT"
            );

        } else {

            holder.tvProgress.setText(
                    "Tiến độ: "
                            + progress.getOverallPercent()
                            + "%"
            );
        }

        holder.card.setOnClickListener(
                view ->
                        listener.onPractice(test)
        );

        holder.btnPractice.setOnClickListener(
                view ->
                        listener.onPractice(test)
        );

        holder.btnMock.setOnClickListener(
                view ->
                        listener.onMockTest(test)
        );
    }

    @Override
    public int getItemCount() {
        return tests.size();
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
        TextView tvProgress;

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

            tvProgress =
                    itemView.findViewById(
                            R.id.tvToeicStudentProgress
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