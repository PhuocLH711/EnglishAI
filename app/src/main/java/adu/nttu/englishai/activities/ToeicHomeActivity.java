package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.ToeicStudentTestAdapter;
import adu.nttu.englishai.models.ToeicTest;
import adu.nttu.englishai.repositories.ToeicRepository;

public class ToeicHomeActivity
        extends AppCompatActivity {

    public static final String EXTRA_TEST_ID =
            "toeic_test_id";

    public static final String EXTRA_TEST_TITLE =
            "toeic_test_title";

    public static final String EXTRA_DURATION =
            "toeic_duration";

    public static final String EXTRA_MODE =
            "toeic_mode";

    public static final String MODE_PRACTICE =
            "practice";

    public static final String MODE_MOCK =
            "mock";

    private TextView btnBack;
    private TextView tvStatus;
    private TextView tvEmpty;

    private RecyclerView recycler;

    private ToeicRepository repository;
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

        adapter =
                new ToeicStudentTestAdapter(
                        new ToeicStudentTestAdapter.TestListener() {

                            @Override
                            public void onPractice(
                                    ToeicTest test
                            ) {
                                openTest(
                                        test,
                                        MODE_PRACTICE
                                );
                            }

                            @Override
                            public void onMockTest(
                                    ToeicTest test
                            ) {
                                openTest(
                                        test,
                                        MODE_MOCK
                                );
                            }
                        }
                );

        recycler.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recycler.setAdapter(adapter);

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

                        adapter.submitList(tests);

                        boolean empty =
                                tests.isEmpty();

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
                        } else {
                            tvStatus.setText(
                                    "Có "
                                            + tests.size()
                                            + " bộ đề để luyện."
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

    private void openTest(
            ToeicTest test,
            String mode
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

        startActivity(intent);
    }
}
