package adu.nttu.englishai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.ToeicTest;

/**
 * Adapter hiển thị các bộ đề trong TOEIC Admin.
 */
public class ToeicAdminTestAdapter
        extends RecyclerView.Adapter<ToeicAdminTestAdapter.TestViewHolder> {

    public interface TestActionListener {

        void onViewTest(
                ToeicTest test
        );

        void onDeleteTest(
                ToeicTest test
        );
    }

    private final List<ToeicTest> tests =
            new ArrayList<>();

    private final TestActionListener listener;

    public ToeicAdminTestAdapter(
            TestActionListener listener
    ) {
        this.listener = listener;
    }

    public void submitList(
            List<ToeicTest> newTests
    ) {

        tests.clear();

        if (newTests != null) {
            tests.addAll(
                    newTests
            );
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

        ToeicTest test =
                tests.get(position);

        String title =
                test.getTitle() == null
                        || test.getTitle().trim().isEmpty()
                        ? "Bộ đề TOEIC"
                        : test.getTitle();

        holder.tvTitle.setText(
                title
        );

        holder.tvId.setText(
                test.getId() == null
                        ? ""
                        : test.getId()
        );

        holder.tvMeta.setText(
                test.getTotalQuestions()
                        + " câu"
                        + buildPartText(test)
        );

        holder.btnView.setOnClickListener(
                view ->
                        listener.onViewTest(
                                test
                        )
        );

        holder.btnDelete.setOnClickListener(
                view ->
                        listener.onDeleteTest(
                                test
                        )
        );
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    private String buildPartText(
            ToeicTest test
    ) {

        if (test.getAvailableParts() == null
                || test.getAvailableParts().isEmpty()) {

            return "";
        }

        StringBuilder builder =
                new StringBuilder(
                        " • Part "
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

    static class TestViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvTitle;
        TextView tvId;
        TextView tvMeta;

        MaterialButton btnView;
        MaterialButton btnDelete;

        public TestViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            tvTitle =
                    itemView.findViewById(
                            R.id.tvAdminTestTitle
                    );

            tvId =
                    itemView.findViewById(
                            R.id.tvAdminTestId
                    );

            tvMeta =
                    itemView.findViewById(
                            R.id.tvAdminTestMeta
                    );

            btnView =
                    itemView.findViewById(
                            R.id.btnAdminViewTest
                    );

            btnDelete =
                    itemView.findViewById(
                            R.id.btnAdminDeleteTest
                    );
        }
    }
}
