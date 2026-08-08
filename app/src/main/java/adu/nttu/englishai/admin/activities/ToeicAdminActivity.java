package adu.nttu.englishai.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import adu.nttu.englishai.admin.adapters.ToeicAdminTestAdapter;
import adu.nttu.englishai.admin.repositories.ToeicAdminRepository;
import adu.nttu.englishai.admin.repositories.ToeicAdminRepository.ToeicAdminTest;
import adu.nttu.englishai.admin.utils.AdminSystemBarHelper;

public class ToeicAdminActivity
        extends AppCompatActivity {

    private TextView btnBack;
    private TextView tvTotalTests;
    private TextView tvTotalQuestions;
    private TextView tvStatus;
    private TextView tvEmpty;

    private EditText edtSearch;

    private MaterialButton btnAdd;
    private MaterialButton btnImport;
    private MaterialButton btnRefresh;

    private RecyclerView recycler;

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

        AdminSystemBarHelper.applyTopInset(
                this,
                findViewById(
                        R.id.rootToeicAdmin
                )
        );

        repository =
                new ToeicAdminRepository();

        bindViews();
        setupRecycler();
        setupEvents();
        loadTests();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (repository != null
                && adapter != null) {
            loadTests();
        }
    }

    private void bindViews() {

        btnBack =
                findViewById(
                        R.id.btnToeicAdminBack
                );

        tvTotalTests =
                findViewById(
                        R.id.tvAdminTotalTests
                );

        tvTotalQuestions =
                findViewById(
                        R.id.tvAdminTotalQuestions
                );

        tvStatus =
                findViewById(
                        R.id.tvAdminToeicStatus
                );

        tvEmpty =
                findViewById(
                        R.id.tvAdminToeicEmpty
                );

        edtSearch =
                findViewById(
                        R.id.edtAdminToeicSearch
                );

        btnAdd =
                findViewById(
                        R.id.btnAdminToeicAdd
                );

        btnImport =
                findViewById(
                        R.id.btnAdminToeicImport
                );

        btnRefresh =
                findViewById(
                        R.id.btnAdminToeicRefresh
                );

        recycler =
                findViewById(
                        R.id.recyclerAdminTests
                );
    }

    private void setupRecycler() {

        adapter =
                new ToeicAdminTestAdapter(
                        this::showTestActions
                );

        recycler.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recycler.setAdapter(
                adapter
        );
    }

    private void setupEvents() {

        btnBack.setOnClickListener(
                view -> finish()
        );

        btnAdd.setOnClickListener(
                view ->
                        showTestEditor(
                                null
                        )
        );

        btnImport.setOnClickListener(
                view ->
                        startActivity(
                                new Intent(
                                        ToeicAdminActivity.this,
                                        ToeicImportActivity.class
                                )
                        )
        );

        btnRefresh.setOnClickListener(
                view -> loadTests()
        );

        edtSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        adapter.filter(
                                s == null
                                        ? ""
                                        : s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    private void loadTests() {

        setLoading(
                true,
                "Đang tải danh sách đề TOEIC..."
        );

        repository.getAllTests(
                new ToeicAdminRepository.TestListCallback() {

                    @Override
                    public void onSuccess(
                            List<ToeicAdminTest> tests
                    ) {

                        adapter.submitList(tests);

                        tvTotalTests.setText(
                                String.valueOf(
                                        tests.size()
                                )
                        );

                        long totalQuestions = 0L;

                        for (ToeicAdminTest test
                                : tests) {

                            totalQuestions +=
                                    test.getTotalQuestions();
                        }

                        tvTotalQuestions.setText(
                                String.valueOf(
                                        totalQuestions
                                )
                        );

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

                        setLoading(
                                false,
                                "Đã tải "
                                        + tests.size()
                                        + " đề."
                        );
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        setLoading(
                                false,
                                "Không tải được TOEIC: "
                                        + exception.getMessage()
                        );
                    }
                }
        );
    }

    private void showTestActions(
            ToeicAdminTest test
    ) {

        String publishAction =
                test.isPublished()
                        ? "Chuyển về Draft"
                        : "Publish đề";

        String[] actions =
                {
                        "Xem thông tin",
                        "Sửa đề",
                        publishAction,
                        "Đếm câu hỏi",
                        "Xóa đề"
                };

        new AlertDialog.Builder(this)
                .setTitle(
                        test.getTitle()
                )
                .setItems(
                        actions,
                        (dialog, which) -> {

                            if (which == 0) {
                                showTestInfo(test);

                            } else if (which == 1) {
                                showTestEditor(test);

                            } else if (which == 2) {
                                updatePublished(
                                        test,
                                        !test.isPublished()
                                );

                            } else if (which == 3) {
                                countQuestions(test);

                            } else if (which == 4) {
                                confirmDelete(test);
                            }
                        }
                )
                .setNegativeButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void showTestInfo(
            ToeicAdminTest test
    ) {

        String message =
                "ID:\n"
                        + test.getId()
                        + "\n\nNguồn: "
                        + safe(test.getSource())
                        + "\nNăm: "
                        + (
                        test.getYear() <= 0
                                ? "—"
                                : test.getYear()
                )
                        + "\nĐộ khó: "
                        + safe(test.getDifficulty())
                        + "\nThời gian: "
                        + test.getDurationMinutes()
                        + " phút"
                        + "\nSố câu khai báo: "
                        + test.getTotalQuestions()
                        + "\nTrạng thái: "
                        + (
                        test.isPublished()
                                ? "Published"
                                : "Draft"
                );

        new AlertDialog.Builder(this)
                .setTitle(
                        test.getTitle()
                )
                .setMessage(message)
                .setPositiveButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void countQuestions(
            ToeicAdminTest test
    ) {

        repository.countQuestions(
                test.getId(),
                new ToeicAdminRepository.QuestionCountCallback() {

                    @Override
                    public void onSuccess(
                            int count
                    ) {

                        Toast.makeText(
                                ToeicAdminActivity.this,
                                test.getTitle()
                                        + ": "
                                        + count
                                        + " câu trong toeicQuestions.",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        Toast.makeText(
                                ToeicAdminActivity.this,
                                "Không đếm được câu hỏi: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showTestEditor(
            @Nullable ToeicAdminTest existing
    ) {

        boolean editing =
                existing != null;

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        int padding =
                dpToPx(20);

        container.setPadding(
                padding,
                dpToPx(8),
                padding,
                0
        );

        EditText edtTitle =
                createTextField(
                        "Tên đề",
                        editing
                                ? existing.getTitle()
                                : ""
                );

        EditText edtSource =
                createTextField(
                        "Nguồn",
                        editing
                                ? existing.getSource()
                                : ""
                );

        EditText edtYear =
                createNumberField(
                        "Năm",
                        editing
                                && existing.getYear() > 0
                                ? String.valueOf(
                                existing.getYear()
                        )
                                : ""
                );

        EditText edtDifficulty =
                createTextField(
                        "Độ khó",
                        editing
                                ? existing.getDifficulty()
                                : "Mixed"
                );

        EditText edtDuration =
                createNumberField(
                        "Thời gian (phút)",
                        editing
                                ? String.valueOf(
                                existing.getDurationMinutes()
                        )
                                : "120"
                );

        EditText edtTotal =
                createNumberField(
                        "Tổng số câu",
                        editing
                                ? String.valueOf(
                                existing.getTotalQuestions()
                        )
                                : "0"
                );

        CheckBox cbPublished =
                new CheckBox(this);

        cbPublished.setText(
                "Published"
        );

        cbPublished.setChecked(
                editing
                        && existing.isPublished()
        );

        container.addView(edtTitle);
        container.addView(edtSource);
        container.addView(edtYear);
        container.addView(edtDifficulty);
        container.addView(edtDuration);
        container.addView(edtTotal);
        container.addView(cbPublished);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                editing
                                        ? "Sửa đề TOEIC"
                                        : "Tạo đề TOEIC"
                        )
                        .setView(container)
                        .setPositiveButton(
                                editing
                                        ? "Lưu"
                                        : "Tạo",
                                null
                        )
                        .setNegativeButton(
                                "Hủy",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                ignored ->
                        dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        ).setOnClickListener(
                                view -> {

                                    String title =
                                            edtTitle.getText()
                                                    .toString()
                                                    .trim();

                                    if (title.isEmpty()) {
                                        edtTitle.setError(
                                                "Nhập tên đề."
                                        );
                                        return;
                                    }

                                    ToeicAdminTest value =
                                            new ToeicAdminTest();

                                    value.setTitle(title);

                                    value.setSource(
                                            edtSource.getText()
                                                    .toString()
                                                    .trim()
                                    );

                                    value.setDifficulty(
                                            edtDifficulty.getText()
                                                    .toString()
                                                    .trim()
                                    );

                                    value.setYear(
                                            parseLong(
                                                    edtYear,
                                                    0L
                                            )
                                    );

                                    value.setDurationMinutes(
                                            parseLong(
                                                    edtDuration,
                                                    120L
                                            )
                                    );

                                    value.setTotalQuestions(
                                            parseLong(
                                                    edtTotal,
                                                    0L
                                            )
                                    );

                                    value.setPublished(
                                            cbPublished.isChecked()
                                    );

                                    if (editing) {

                                        repository.updateTest(
                                                existing.getId(),
                                                value,
                                                new TestActionCallback(
                                                        "Đã cập nhật đề.",
                                                        dialog
                                                )
                                        );

                                    } else {

                                        repository.createTest(
                                                value,
                                                new TestActionCallback(
                                                        "Đã tạo đề TOEIC.",
                                                        dialog
                                                )
                                        );
                                    }
                                }
                        )
        );

        dialog.show();
    }

    private void updatePublished(
            ToeicAdminTest test,
            boolean published
    ) {

        repository.setPublished(
                test.getId(),
                published,
                new TestActionCallback(
                        published
                                ? "Đã publish đề."
                                : "Đã chuyển về Draft.",
                        null
                )
        );
    }

    private void confirmDelete(
            ToeicAdminTest test
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Xóa đề TOEIC?"
                )
                .setMessage(
                        "Sẽ xóa document toeicTests/"
                                + test.getId()
                                + ".\n\n"
                                + "Các câu trong toeicQuestions chưa được tự động xóa."
                )
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) ->
                                repository.deleteTest(
                                        test.getId(),
                                        new TestActionCallback(
                                                "Đã xóa đề.",
                                                null
                                        )
                                )
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    private EditText createTextField(
            String hint,
            String value
    ) {

        EditText editText =
                new EditText(this);

        editText.setHint(hint);

        editText.setText(
                value == null
                        ? ""
                        : value
        );

        editText.setSingleLine(true);

        editText.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        applyFieldMargin(editText);

        return editText;
    }

    private EditText createNumberField(
            String hint,
            String value
    ) {

        EditText editText =
                new EditText(this);

        editText.setHint(hint);

        editText.setText(
                value == null
                        ? ""
                        : value
        );

        editText.setSingleLine(true);

        editText.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        applyFieldMargin(editText);

        return editText;
    }

    private void applyFieldMargin(
            EditText editText
    ) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                0,
                0,
                dpToPx(8)
        );

        editText.setLayoutParams(params);
    }

    private long parseLong(
            EditText editText,
            long fallback
    ) {

        try {
            String text =
                    editText.getText()
                            .toString()
                            .trim();

            if (text.isEmpty()) {
                return fallback;
            }

            return Long.parseLong(text);

        } catch (Exception exception) {
            return fallback;
        }
    }

    private void setLoading(
            boolean loading,
            String message
    ) {

        btnAdd.setEnabled(!loading);
        btnImport.setEnabled(!loading);
        btnRefresh.setEnabled(!loading);

        tvStatus.setText(message);
    }

    private int dpToPx(
            int dp
    ) {

        return Math.round(
                dp
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private String safe(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {
            return "—";
        }

        return value.trim();
    }

    private class TestActionCallback
            implements ToeicAdminRepository.ActionCallback {

        private final String message;
        private final AlertDialog dialog;

        TestActionCallback(
                String message,
                @Nullable AlertDialog dialog
        ) {
            this.message = message;
            this.dialog = dialog;
        }

        @Override
        public void onSuccess() {

            if (dialog != null) {
                dialog.dismiss();
            }

            Toast.makeText(
                    ToeicAdminActivity.this,
                    message,
                    Toast.LENGTH_SHORT
            ).show();

            loadTests();
        }

        @Override
        public void onFailure(
                Exception exception
        ) {

            Toast.makeText(
                    ToeicAdminActivity.this,
                    "Thao tác thất bại: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
