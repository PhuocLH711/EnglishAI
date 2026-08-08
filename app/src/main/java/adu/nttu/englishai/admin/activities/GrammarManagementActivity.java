package adu.nttu.englishai.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
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
import adu.nttu.englishai.admin.utils.AdminSystemBarHelper;
import adu.nttu.englishai.activities.ImportSentenceExerciseActivity;
import adu.nttu.englishai.admin.adapters.AdminGrammarAdapter;
import adu.nttu.englishai.admin.repositories.AdminGrammarRepository;
import adu.nttu.englishai.admin.repositories.AdminGrammarRepository.AdminGrammarExercise;

/**
 * Quản lý Grammar Sprint / sentenceExercises.
 *
 * Chức năng:
 * - List toàn bộ câu
 * - Search
 * - Xem
 * - Sửa
 * - Xóa
 * - Mở ImportSentenceExerciseActivity để nhập hàng loạt
 *
 * Không đổi schema hiện tại của sentenceExercises.
 */
public class GrammarManagementActivity
        extends AppCompatActivity {

    private TextView btnBack;
    private TextView tvTotal;
    private TextView tvStatus;
    private TextView tvEmpty;

    private EditText edtSearch;

    private MaterialButton btnImport;
    private MaterialButton btnRefresh;

    private RecyclerView recycler;

    private AdminGrammarRepository repository;
    private AdminGrammarAdapter adapter;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_grammar_management
        );

        AdminSystemBarHelper.applyTopInset(
                this,
                findViewById(R.id.rootGrammarManagement)
        );

        repository =
                new AdminGrammarRepository();

        bindViews();
        setupRecycler();
        setupEvents();
        loadExercises();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (repository != null
                && adapter != null) {

            loadExercises();
        }
    }

    private void bindViews() {

        btnBack =
                findViewById(
                        R.id.btnGrammarAdminBack
                );

        tvTotal =
                findViewById(
                        R.id.tvAdminGrammarTotal
                );

        tvStatus =
                findViewById(
                        R.id.tvAdminGrammarStatus
                );

        tvEmpty =
                findViewById(
                        R.id.tvAdminGrammarEmpty
                );

        edtSearch =
                findViewById(
                        R.id.edtAdminGrammarSearch
                );

        btnImport =
                findViewById(
                        R.id.btnAdminGrammarImport
                );

        btnRefresh =
                findViewById(
                        R.id.btnAdminGrammarRefresh
                );

        recycler =
                findViewById(
                        R.id.recyclerAdminGrammar
                );
    }

    private void setupRecycler() {

        adapter =
                new AdminGrammarAdapter(
                        this::showExerciseActions
                );

        recycler.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recycler.setAdapter(adapter);
    }

    private void setupEvents() {

        btnBack.setOnClickListener(
                view -> finish()
        );

        btnRefresh.setOnClickListener(
                view -> loadExercises()
        );

        btnImport.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    GrammarManagementActivity.this,
                                    ImportSentenceExerciseActivity.class
                            );

                    startActivity(intent);
                }
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

    private void loadExercises() {

        setLoading(
                true,
                "Đang tải sentenceExercises..."
        );

        repository.getAllExercises(
                new AdminGrammarRepository.ListCallback() {

                    @Override
                    public void onSuccess(
                            List<AdminGrammarExercise> exercises
                    ) {

                        adapter.submitList(exercises);

                        tvTotal.setText(
                                String.valueOf(
                                        exercises.size()
                                )
                        );

                        boolean empty =
                                exercises.isEmpty();

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
                                        + exercises.size()
                                        + " câu."
                        );
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        setLoading(
                                false,
                                "Không tải được sentenceExercises: "
                                        + exception.getMessage()
                        );
                    }
                }
        );
    }

    private void showExerciseActions(
            AdminGrammarExercise exercise
    ) {

        String[] actions =
                {
                        "Xem",
                        "Sửa",
                        "Xóa"
                };

        new AlertDialog.Builder(this)
                .setTitle(
                        exercise.getTopic().isEmpty()
                                ? "Grammar Sprint"
                                : exercise.getTopic()
                )
                .setItems(
                        actions,
                        (dialog, which) -> {

                            if (which == 0) {
                                showExerciseInfo(exercise);
                            } else if (which == 1) {
                                showExerciseEditor(exercise);
                            } else if (which == 2) {
                                confirmDelete(exercise);
                            }
                        }
                )
                .setNegativeButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void showExerciseInfo(
            AdminGrammarExercise exercise
    ) {

        String message =
                "Document ID:\n"
                        + safe(exercise.getId())
                        + "\n\nTiếng Việt:\n"
                        + safe(exercise.getVietnameseSentence())
                        + "\n\nĐáp án:\n"
                        + safe(exercise.getCorrectSentence())
                        + "\n\nChủ điểm: "
                        + safe(exercise.getTopic())
                        + "\nMức độ: "
                        + safe(exercise.getLevel())
                        + "\n\nGiải thích:\n"
                        + safe(exercise.getExplanation());

        new AlertDialog.Builder(this)
                .setTitle("Chi tiết câu")
                .setMessage(message)
                .setPositiveButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void showExerciseEditor(
            AdminGrammarExercise original
    ) {

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

        EditText edtVietnamese =
                createField(
                        "Câu tiếng Việt",
                        original.getVietnameseSentence()
                );

        EditText edtEnglish =
                createField(
                        "Câu tiếng Anh đúng",
                        original.getCorrectSentence()
                );

        EditText edtTopic =
                createField(
                        "Chủ điểm ngữ pháp",
                        original.getTopic()
                );

        EditText edtLevel =
                createField(
                        "Mức độ",
                        original.getLevel()
                );

        EditText edtExplanation =
                createField(
                        "Giải thích",
                        original.getExplanation()
                );

        container.addView(edtVietnamese);
        container.addView(edtEnglish);
        container.addView(edtTopic);
        container.addView(edtLevel);
        container.addView(edtExplanation);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Sửa bài Grammar Sprint"
                        )
                        .setView(container)
                        .setPositiveButton(
                                "Lưu",
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

                                    String vietnamese =
                                            edtVietnamese
                                                    .getText()
                                                    .toString()
                                                    .trim();

                                    String english =
                                            edtEnglish
                                                    .getText()
                                                    .toString()
                                                    .trim();

                                    if (vietnamese.isEmpty()) {
                                        edtVietnamese.setError(
                                                "Không được để trống."
                                        );
                                        return;
                                    }

                                    if (english.isEmpty()) {
                                        edtEnglish.setError(
                                                "Không được để trống."
                                        );
                                        return;
                                    }

                                    AdminGrammarExercise edited =
                                            new AdminGrammarExercise();

                                    edited.setVietnameseSentence(
                                            vietnamese
                                    );

                                    edited.setCorrectSentence(
                                            english
                                    );

                                    edited.setTopic(
                                            edtTopic.getText()
                                                    .toString()
                                                    .trim()
                                    );

                                    edited.setLevel(
                                            edtLevel.getText()
                                                    .toString()
                                                    .trim()
                                    );

                                    edited.setExplanation(
                                            edtExplanation.getText()
                                                    .toString()
                                                    .trim()
                                    );

                                    repository.updateExercise(
                                            original,
                                            edited,
                                            new GrammarActionCallback(
                                                    "Đã cập nhật bài tập.",
                                                    dialog
                                            )
                                    );
                                }
                        )
        );

        dialog.show();
    }

    private void confirmDelete(
            AdminGrammarExercise exercise
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Xóa bài tập?"
                )
                .setMessage(
                        "Câu này sẽ bị xóa khỏi sentenceExercises.\n\n"
                                + safe(exercise.getVietnameseSentence())
                )
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) ->
                                repository.deleteExercise(
                                        exercise.getId(),
                                        new GrammarActionCallback(
                                                "Đã xóa bài tập.",
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

    private EditText createField(
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

        editText.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        editText.setMinLines(1);
        editText.setMaxLines(4);

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

        return editText;
    }

    private void setLoading(
            boolean loading,
            String message
    ) {

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

    private class GrammarActionCallback
            implements AdminGrammarRepository.ActionCallback {

        private final String message;
        private final AlertDialog dialog;

        GrammarActionCallback(
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
                    GrammarManagementActivity.this,
                    message,
                    Toast.LENGTH_SHORT
            ).show();

            loadExercises();
        }

        @Override
        public void onFailure(
                Exception exception
        ) {

            Toast.makeText(
                    GrammarManagementActivity.this,
                    "Thao tác thất bại: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}