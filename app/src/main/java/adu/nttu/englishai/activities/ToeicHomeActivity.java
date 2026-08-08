package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.ToeicStudentTestAdapter;
import adu.nttu.englishai.models.ToeicTest;
import adu.nttu.englishai.repositories.ToeicProgressRepository;
import adu.nttu.englishai.repositories.ToeicRepository;

public class ToeicHomeActivity extends AppCompatActivity {

    public static final String EXTRA_TEST_ID =
            "toeic_test_id";

    public static final String EXTRA_TEST_TITLE =
            "toeic_test_title";

    public static final String EXTRA_DURATION =
            "toeic_duration";

    public static final String EXTRA_MODE =
            "toeic_mode";

    public static final String EXTRA_PART_FILTER =
            "toeic_part_filter";

    public static final String MODE_PRACTICE =
            "practice";

    public static final String MODE_MOCK =
            "mock";

    private TextView btnBack;
    private TextView tvStatus;
    private TextView tvEmpty;

    private RecyclerView recycler;

    private ToeicRepository repository;
    private ToeicProgressRepository progressRepository;
    private ToeicStudentTestAdapter adapter;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_toeic_home
        );

        btnBack =
                findViewById(
                        R.id.btnToeicHomeBack
                );

        tvStatus =
                findViewById(
                        R.id.tvToeicHomeStatus
                );

        tvEmpty =
                findViewById(
                        R.id.tvToeicHomeEmpty
                );

        recycler =
                findViewById(
                        R.id.recyclerToeicTests
                );

        repository =
                new ToeicRepository();

        progressRepository =
                new ToeicProgressRepository();

        adapter =
                new ToeicStudentTestAdapter(
                        new ToeicStudentTestAdapter.TestListener() {

                            @Override
                            public void onPractice(
                                    ToeicTest test
                            ) {
                                showPartProgressPicker(
                                        test
                                );
                            }

                            @Override
                            public void onMockTest(
                                    ToeicTest test
                            ) {
                                showFullTestConfirmation(
                                        test
                                );
                            }
                        }
                );

        recycler.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recycler.setAdapter(
                adapter
        );

        btnBack.setOnClickListener(
                view -> finish()
        );

        loadTests();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (repository != null) {
            loadTests();
        }
    }

    private void loadTests() {

        tvStatus.setText(
                "Đang tải bộ đề..."
        );

        repository.getAllTests(
                new ToeicRepository.TestsCallback() {

                    @Override
                    public void onSuccess(
                            List<ToeicTest> tests
                    ) {

                        adapter.submitList(
                                tests
                        );

                        boolean empty =
                                tests == null
                                        || tests.isEmpty();

                        tvEmpty.setVisibility(
                                empty
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        recycler.setVisibility(
                                empty
                                        ? View.GONE
                                        : View.VISIBLE
                        );

                        if (empty) {

                            tvStatus.setText(
                                    "Chưa có dữ liệu TOEIC trên Firestore."
                            );

                            return;
                        }

                        tvStatus.setText(
                                "Có "
                                        + tests.size()
                                        + " bộ đề để luyện."
                        );

                        for (ToeicTest test
                                : tests) {

                            loadCardProgress(
                                    test
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        tvStatus.setText(
                                "Không tải được TOEIC."
                        );

                        Toast.makeText(
                                ToeicHomeActivity.this,
                                exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void loadCardProgress(
            ToeicTest test
    ) {

        progressRepository.getProgress(
                test.getId(),
                new ToeicProgressRepository.ProgressCallback() {

                    @Override
                    public void onSuccess(
                            ToeicProgressRepository.TestProgress progress
                    ) {

                        adapter.setProgress(
                                test.getId(),
                                progress
                        );
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {
                        // Không chặn danh sách đề nếu
                        // tiến độ chưa tải được.
                    }
                }
        );
    }

    private void showPartProgressPicker(
            ToeicTest test
    ) {

        progressRepository.getProgress(
                test.getId(),
                new ToeicProgressRepository.ProgressCallback() {

                    @Override
                    public void onSuccess(
                            ToeicProgressRepository.TestProgress progress
                    ) {

                        String[] items =
                                new String[8];

                        for (int part = 1;
                             part <= 7;
                             part++) {

                            int percent =
                                    progress.getPartPercent(
                                            part
                                    );

                            items[part - 1] =
                                    getPartName(part)
                                            + "  •  "
                                            + formatProgress(
                                            percent
                                    );
                        }

                        items[7] =
                                "Luyện toàn bộ đề  •  "
                                        + formatProgress(
                                        progress.getOverallPercent()
                                );

                        String title =
                                test.getTitle();

                        if (progress.isCompleted()) {

                            title +=
                                    "\n✅ Đã hoàn tất";
                        }

                        new AlertDialog.Builder(
                                ToeicHomeActivity.this
                        )
                                .setTitle(title)
                                .setItems(
                                        items,
                                        (dialog, which) -> {

                                            int part =
                                                    which < 7
                                                            ? which + 1
                                                            : 0;

                                            openTest(
                                                    test,
                                                    MODE_PRACTICE,
                                                    part
                                            );
                                        }
                                )
                                .setNegativeButton(
                                        "Đóng",
                                        null
                                )
                                .show();
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        Toast.makeText(
                                ToeicHomeActivity.this,
                                "Không tải được tiến độ: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showFullTestConfirmation(
            ToeicTest test
    ) {

        progressRepository.getProgress(
                test.getId(),
                new ToeicProgressRepository.ProgressCallback() {

                    @Override
                    public void onSuccess(
                            ToeicProgressRepository.TestProgress progress
                    ) {

                        StringBuilder message =
                                new StringBuilder();

                        for (int part = 1;
                             part <= 7;
                             part++) {

                            message.append(
                                    "Part "
                                            + part
                                            + ": "
                                            + progress.getPartPercent(part)
                                            + "%"
                            );

                            if (part < 7) {
                                message.append('\n');
                            }
                        }

                        message.append(
                                "\n\nTổng tiến độ: "
                                        + progress.getOverallPercent()
                                        + "%"
                        );

                        if (progress.isCompleted()) {

                            message.append(
                                    "\n✅ Bộ đề này đã hoàn tất."
                            );
                        }

                        message.append(
                                "\n\nThi thử sẽ làm toàn bộ "
                                        + test.getTotalQuestions()
                                        + " câu trong "
                                        + test.getDurationMinutes()
                                        + " phút."
                        );

                        new AlertDialog.Builder(
                                ToeicHomeActivity.this
                        )
                                .setTitle(
                                        test.getTitle()
                                )
                                .setMessage(
                                        message.toString()
                                )
                                .setPositiveButton(
                                        "Bắt đầu thi",
                                        (dialog, which) ->
                                                openTest(
                                                        test,
                                                        MODE_MOCK,
                                                        0
                                                )
                                )
                                .setNegativeButton(
                                        "Hủy",
                                        null
                                )
                                .show();
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        openTest(
                                test,
                                MODE_MOCK,
                                0
                        );
                    }
                }
        );
    }

    private void openTest(
            ToeicTest test,
            String mode,
            int part
    ) {

        if (test == null
                || test.getId() == null
                || test.getId().trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Bộ đề không có testId.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        ToeicPracticeActivity.class
                );

        intent.putExtra(
                EXTRA_TEST_ID,
                test.getId()
        );

        intent.putExtra(
                EXTRA_TEST_TITLE,
                test.getTitle()
        );

        intent.putExtra(
                EXTRA_DURATION,
                test.getDurationMinutes()
        );

        intent.putExtra(
                EXTRA_MODE,
                mode
        );

        intent.putExtra(
                EXTRA_PART_FILTER,
                part
        );

        startActivity(
                intent
        );
    }

    private String getPartName(
            int part
    ) {

        switch (part) {

            case 1:
                return "Part 1 • Photographs";

            case 2:
                return "Part 2 • Question-Response";

            case 3:
                return "Part 3 • Conversations";

            case 4:
                return "Part 4 • Talks";

            case 5:
                return "Part 5 • Incomplete Sentences";

            case 6:
                return "Part 6 • Text Completion";

            case 7:
                return "Part 7 • Reading Comprehension";

            default:
                return "Part " + part;
        }
    }

    private String formatProgress(
            int percent
    ) {

        if (percent >= 100) {
            return "✅ 100%";
        }

        return percent + "%";
    }
}