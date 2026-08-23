package adu.nttu.englishai.admin.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.SentenceRewriteExercise;
import adu.nttu.englishai.repositories.SentenceRewriteRepository;

public class SentenceRewriteManagementActivity
        extends AppCompatActivity {

    private final SentenceRewriteRepository repository =
            new SentenceRewriteRepository();

    private final List<SentenceRewriteExercise> allItems =
            new ArrayList<>();

    private LinearLayout listContainer;
    private TextView tvEmpty;
    private TextView tvCount;
    private TextView tvStatus;
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_sentence_rewrite_management
        );

        bindViews();
        bindEvents();
        loadData();
    }

    private void bindViews() {
        listContainer =
                findViewById(R.id.listContainer);

        tvEmpty =
                findViewById(R.id.tvEmpty);

        tvCount =
                findViewById(R.id.tvCount);

        tvStatus =
                findViewById(R.id.tvStatus);

        edtSearch =
                findViewById(R.id.edtSearch);
    }

    private void bindEvents() {

        findViewById(R.id.btnBack)
                .setOnClickListener(v ->
                        finish()
                );

        findViewById(R.id.btnAdd)
                .setOnClickListener(v ->
                        showForm(null)
                );

        findViewById(R.id.btnImport)
                .setOnClickListener(v ->
                        startActivity(
                                new Intent(
                                        SentenceRewriteManagementActivity.this,
                                        SentenceRewriteImportActivity.class
                                )
                        )
                );

        findViewById(R.id.btnRefresh)
                .setOnClickListener(v ->
                        loadData()
                );

        edtSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        filterList(
                                s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {}
                }
        );
    }

    // =========================================================
    // LOAD
    // =========================================================
    private void loadData() {

        tvStatus.setText(
                "Đang tải dữ liệu..."
        );

        repository.getAll(
                new SentenceRewriteRepository.ListCallback() {

                    @Override
                    public void onSuccess(
                            List<SentenceRewriteExercise> list
                    ) {
                        allItems.clear();
                        allItems.addAll(list);

                        tvCount.setText(
                                String.valueOf(
                                        allItems.size()
                                )
                        );

                        tvStatus.setText(
                                "Đã tải "
                                        + allItems.size()
                                        + " bài."
                        );

                        filterList(
                                edtSearch.getText()
                                        .toString()
                        );
                    }

                    @Override
                    public void onFailure(Exception e) {
                        tvStatus.setText(
                                "Không tải được dữ liệu."
                        );

                        Toast.makeText(
                                SentenceRewriteManagementActivity.this,
                                "Lỗi tải dữ liệu: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================
    // SEARCH
    // =========================================================
    private void filterList(
            String keyword
    ) {
        String key =
                keyword == null
                        ? ""
                        : keyword.trim()
                          .toLowerCase();

        List<SentenceRewriteExercise> filtered =
                new ArrayList<>();

        for (SentenceRewriteExercise item : allItems) {

            String original =
                    safe(
                            item.getOriginalSentence()
                    ).toLowerCase();

            String prompt =
                    safe(
                            item.getPrompt()
                    ).toLowerCase();

            String topic =
                    safe(
                            item.getTopic()
                    ).toLowerCase();

            String level =
                    safe(
                            item.getLevel()
                    ).toLowerCase();

            if (key.isEmpty()
                    || original.contains(key)
                    || prompt.contains(key)
                    || topic.contains(key)
                    || level.contains(key)) {

                filtered.add(item);
            }
        }

        renderList(filtered);
    }

    // =========================================================
    // RENDER LIST
    // =========================================================
    private void renderList(
            List<SentenceRewriteExercise> list
    ) {
        listContainer.removeAllViews();

        tvEmpty.setVisibility(
                list.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );

        LayoutInflater inflater =
                LayoutInflater.from(this);

        for (SentenceRewriteExercise item : list) {

            View row =
                    inflater.inflate(
                            R.layout.item_sentence_rewrite_admin,
                            listContainer,
                            false
                    );

            TextView tvTitle =
                    row.findViewById(R.id.tvTitle);

            TextView tvDetail =
                    row.findViewById(R.id.tvDetail);

            tvTitle.setText(
                    item.getOriginalSentence()
            );

            tvDetail.setText(
                    "Bắt đầu: "
                            + safe(item.getPrompt())
                            + "\n"
                            + safe(item.getTopic())
                            + " • "
                            + safe(item.getLevel())
                            + " • "
                            + item.getXp()
                            + " XP"
            );

            row.findViewById(R.id.btnEdit)
                    .setOnClickListener(v ->
                            showForm(item)
                    );

            row.findViewById(R.id.btnDelete)
                    .setOnClickListener(v ->
                            confirmDelete(item)
                    );

            listContainer.addView(row);
        }
    }

    // =========================================================
    // NHẬP TAY / SỬA
    // =========================================================
    private void showForm(
            SentenceRewriteExercise existing
    ) {

        View form =
                getLayoutInflater()
                        .inflate(
                                R.layout.dialog_sentence_rewrite_form,
                                null
                        );

        EditText edtOriginal =
                form.findViewById(R.id.edtOriginal);

        EditText edtPrompt =
                form.findViewById(R.id.edtPrompt);

        EditText edtAnswer =
                form.findViewById(R.id.edtAnswer);

        EditText edtExplanation =
                form.findViewById(R.id.edtExplanation);

        EditText edtTopic =
                form.findViewById(R.id.edtTopic);

        EditText edtLevel =
                form.findViewById(R.id.edtLevel);

        EditText edtXp =
                form.findViewById(R.id.edtXp);

        boolean editing =
                existing != null;

        if (editing) {

            edtOriginal.setText(
                    existing.getOriginalSentence()
            );

            edtPrompt.setText(
                    existing.getPrompt()
            );

            edtAnswer.setText(
                    existing.getAnswer()
            );

            edtExplanation.setText(
                    existing.getExplanation()
            );

            edtTopic.setText(
                    existing.getTopic()
            );

            edtLevel.setText(
                    existing.getLevel()
            );

            edtXp.setText(
                    String.valueOf(
                            existing.getXp()
                    )
            );
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                editing
                                        ? "Sửa bài Viết Lại Câu"
                                        : "Thêm bài Viết Lại Câu"
                        )
                        .setView(form)
                        .setNegativeButton(
                                "Hủy",
                                null
                        )
                        .setPositiveButton(
                                editing
                                        ? "Cập nhật"
                                        : "Thêm",
                                null
                        )
                        .create();

        dialog.setOnShowListener(unused -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                SentenceRewriteExercise item =
                        readForm(
                                edtOriginal,
                                edtPrompt,
                                edtAnswer,
                                edtExplanation,
                                edtTopic,
                                edtLevel,
                                edtXp
                        );

                if (item == null) {
                    return;
                }

                SentenceRewriteRepository.ActionCallback callback =
                        new SentenceRewriteRepository.ActionCallback() {

                            @Override
                            public void onSuccess() {
                                dialog.dismiss();

                                Toast.makeText(
                                        SentenceRewriteManagementActivity.this,
                                        editing
                                                ? "Đã cập nhật bài."
                                                : "Đã thêm bài.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadData();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(
                                        SentenceRewriteManagementActivity.this,
                                        "Lỗi: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        };

                if (editing) {
                    repository.update(
                            existing.getId(),
                            item,
                            callback
                    );
                } else {
                    repository.add(
                            item,
                            callback
                    );
                }
            });
        });

        dialog.show();
    }

    private SentenceRewriteExercise readForm(
            EditText edtOriginal,
            EditText edtPrompt,
            EditText edtAnswer,
            EditText edtExplanation,
            EditText edtTopic,
            EditText edtLevel,
            EditText edtXp
    ) {

        String original =
                edtOriginal.getText()
                        .toString()
                        .trim();

        String prompt =
                edtPrompt.getText()
                        .toString()
                        .trim();

        String answer =
                edtAnswer.getText()
                        .toString()
                        .trim();

        if (original.isEmpty()
                || prompt.isEmpty()
                || answer.isEmpty()) {

            Toast.makeText(
                    this,
                    "Câu gốc, yêu cầu và đáp án không được để trống.",
                    Toast.LENGTH_SHORT
            ).show();

            return null;
        }

        SentenceRewriteExercise item =
                new SentenceRewriteExercise();

        item.setOriginalSentence(original);
        item.setPrompt(prompt);
        item.setAnswer(answer);

        item.setExplanation(
                edtExplanation.getText()
                        .toString()
                        .trim()
        );

        item.setTopic(
                edtTopic.getText()
                        .toString()
                        .trim()
        );

        item.setLevel(
                edtLevel.getText()
                        .toString()
                        .trim()
        );

        long xp = 10;

        try {
            String raw =
                    edtXp.getText()
                            .toString()
                            .trim();

            if (!raw.isEmpty()) {
                xp = Long.parseLong(raw);
            }
        } catch (Exception ignored) {}

        item.setXp(xp);

        return item;
    }

    // =========================================================
    // DELETE
    // =========================================================
    private void confirmDelete(
            SentenceRewriteExercise item
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Xóa bài viết lại câu"
                )
                .setMessage(
                        "Bạn có chắc muốn xóa bài này?"
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) ->
                                repository.delete(
                                        item.getId(),
                                        new SentenceRewriteRepository.ActionCallback() {

                                            @Override
                                            public void onSuccess() {

                                                Toast.makeText(
                                                        SentenceRewriteManagementActivity.this,
                                                        "Đã xóa.",
                                                        Toast.LENGTH_SHORT
                                                ).show();

                                                loadData();
                                            }

                                            @Override
                                            public void onFailure(Exception e) {

                                                Toast.makeText(
                                                        SentenceRewriteManagementActivity.this,
                                                        "Lỗi xóa: "
                                                                + e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                        }
                                )
                )
                .show();
    }

    private String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }
}
