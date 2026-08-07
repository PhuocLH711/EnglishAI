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

import com.google.android.material.button.MaterialButton;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.ToeicAdminTestAdapter;
import adu.nttu.englishai.models.ToeicTest;
import adu.nttu.englishai.repositories.ToeicAdminRepository;

/**
 * TOEIC Admin Panel.
 *
 * Chức năng hiện tại:
 * - Mở Import Tool.
 * - Xem danh sách các bộ đề trên Firestore.
 * - Xem thông tin bộ đề.
 * - Xóa bộ đề + toàn bộ câu hỏi của bộ đề.
 * - Hiển thị thống kê tổng số đề / tổng số câu.
 */
public class ToeicAdminActivity extends AppCompatActivity {

    private TextView tvAdminTotalTests;
    private TextView tvAdminTotalQuestions;
    private TextView tvAdminEmpty;
    private TextView tvAdminStatus;

    private RecyclerView recyclerAdminTests;

    private MaterialButton btnAdminImport;
    private MaterialButton btnAdminRefresh;

    private ToeicAdminRepository repository;
    private ToeicAdminTestAdapter adapter;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_toeic_admin
        );

        bindViews();
        setupRecyclerView();
        setupButtons();

        repository =
                new ToeicAdminRepository();

        loadAdminData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (repository != null) {
            loadAdminData();
        }
    }

    private void bindViews() {

        tvAdminTotalTests =
                findViewById(
                        R.id.tvAdminTotalTests
                );

        tvAdminTotalQuestions =
                findViewById(
                        R.id.tvAdminTotalQuestions
                );

        tvAdminEmpty =
                findViewById(
                        R.id.tvAdminEmpty
                );

        tvAdminStatus =
                findViewById(
                        R.id.tvAdminStatus
                );

        recyclerAdminTests =
                findViewById(
                        R.id.recyclerAdminTests
                );

        btnAdminImport =
                findViewById(
                        R.id.btnAdminImport
                );

        btnAdminRefresh =
                findViewById(
                        R.id.btnAdminRefresh
                );
    }

    private void setupRecyclerView() {

        adapter =
                new ToeicAdminTestAdapter(
                        new ToeicAdminTestAdapter.TestActionListener() {

                            @Override
                            public void onViewTest(
                                    ToeicTest test
                            ) {
                                showTestDetails(
                                        test
                                );
                            }

                            @Override
                            public void onDeleteTest(
                                    ToeicTest test
                            ) {
                                confirmDeleteTest(
                                        test
                                );
                            }
                        }
                );

        recyclerAdminTests.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recyclerAdminTests.setAdapter(
                adapter
        );
    }

    private void setupButtons() {

        btnAdminImport.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    ToeicAdminActivity.this,
                                    ToeicImportActivity.class
                            );

                    startActivity(
                            intent
                    );
                }
        );

        btnAdminRefresh.setOnClickListener(
                view -> loadAdminData()
        );
    }

    private void loadAdminData() {

        setLoading(
                true,
                "Đang tải dữ liệu TOEIC..."
        );

        loadStats();
        loadTests();
    }

    private void loadStats() {

        repository.getStats(
                new ToeicAdminRepository.StatsCallback() {

                    @Override
                    public void onSuccess(
                            int totalTests,
                            int totalQuestions
                    ) {

                        tvAdminTotalTests.setText(
                                String.valueOf(
                                        totalTests
                                )
                        );

                        tvAdminTotalQuestions.setText(
                                String.valueOf(
                                        totalQuestions
                                )
                        );
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        tvAdminStatus.setText(
                                "Không tải được thống kê."
                        );
                    }
                }
        );
    }

    private void loadTests() {

        repository.getAllTests(
                new ToeicAdminRepository.TestListCallback() {

                    @Override
                    public void onSuccess(
                            List<ToeicTest> tests
                    ) {

                        adapter.submitList(
                                tests
                        );

                        tvAdminEmpty.setVisibility(
                                tests.isEmpty()
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        recyclerAdminTests.setVisibility(
                                tests.isEmpty()
                                        ? View.GONE
                                        : View.VISIBLE
                        );

                        setLoading(
                                false,
                                "Đã tải "
                                        + tests.size()
                                        + " bộ đề."
                        );
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        setLoading(
                                false,
                                "Không tải được danh sách đề: "
                                        + exception.getMessage()
                        );
                    }
                }
        );
    }

    private void showTestDetails(
            ToeicTest test
    ) {

        StringBuilder details =
                new StringBuilder();

        details.append(
                "ID: "
        );
        details.append(
                safeText(
                        test.getId()
                )
        );

        details.append(
                "\n\nNguồn: "
        );
        details.append(
                safeText(
                        test.getSourceName()
                )
        );

        details.append(
                "\nNăm: "
        );
        details.append(
                test.getYear()
        );

        details.append(
                "\nSố câu: "
        );
        details.append(
                test.getTotalQuestions()
        );

        details.append(
                "\nThời gian: "
        );
        details.append(
                test.getDurationMinutes()
        );
        details.append(
                " phút"
        );

        details.append(
                "\nListening: "
        );
        details.append(
                test.isHasListening()
                        ? "Có"
                        : "Không"
        );

        details.append(
                "\nReading: "
        );
        details.append(
                test.isHasReading()
                        ? "Có"
                        : "Không"
        );

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        safeTitle(
                                test.getTitle()
                        )
                )
                .setMessage(
                        details.toString()
                )
                .setPositiveButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void confirmDeleteTest(
            ToeicTest test
    ) {

        String testId =
                test.getId();

        if (testId == null
                || testId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Không thể xác định testId.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "Xóa bộ đề?"
                )
                .setMessage(
                        "Bạn sắp xóa \""
                                + safeTitle(
                                        test.getTitle()
                                )
                                + "\" và toàn bộ câu hỏi thuộc bộ đề này.\n\n"
                                + "Hành động này không thể hoàn tác."
                )
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) ->
                                deleteTest(
                                        test
                                )
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    private void deleteTest(
            ToeicTest test
    ) {

        setLoading(
                true,
                "Đang xóa "
                        + safeTitle(
                                test.getTitle()
                        )
                        + "..."
        );

        repository.deleteTestWithQuestions(
                test.getId(),
                new ToeicAdminRepository.DeleteCallback() {

                    @Override
                    public void onSuccess() {

                        Toast.makeText(
                                ToeicAdminActivity.this,
                                "Đã xóa bộ đề.",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadAdminData();
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        setLoading(
                                false,
                                "Xóa thất bại: "
                                        + exception.getMessage()
                        );
                    }
                }
        );
    }

    private void setLoading(
            boolean loading,
            String message
    ) {

        btnAdminImport.setEnabled(
                !loading
        );

        btnAdminRefresh.setEnabled(
                !loading
        );

        tvAdminStatus.setText(
                message
        );
    }

    private String safeTitle(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return "Bộ đề TOEIC";
        }

        return value.trim();
    }

    private String safeText(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return "—";
        }

        return value.trim();
    }
}
