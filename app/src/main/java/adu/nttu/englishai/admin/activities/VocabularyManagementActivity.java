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
import adu.nttu.englishai.activities.ImportVocabularyActivity;
import adu.nttu.englishai.admin.adapters.AdminVocabularyAdapter;
import adu.nttu.englishai.admin.repositories.AdminVocabularyRepository;
import adu.nttu.englishai.admin.repositories.AdminVocabularyRepository.AdminVocabulary;

/**
 * Quản lý từ vựng trong Admin.
 *
 * Có:
 * - Danh sách toàn bộ vocabularies
 * - Search từ / nghĩa / topic / level
 * - Thêm từ
 * - Sửa từ
 * - Xóa từ
 * - Mở ImportVocabularyActivity cũ để nhập hàng loạt
 */
public class VocabularyManagementActivity
        extends AppCompatActivity {

    private TextView btnBack;
    private TextView tvTotal;
    private TextView tvStatus;
    private TextView tvEmpty;

    private EditText edtSearch;

    private MaterialButton btnAdd;
    private MaterialButton btnImport;
    private MaterialButton btnRefresh;

    private RecyclerView recycler;

    private AdminVocabularyRepository repository;
    private AdminVocabularyAdapter adapter;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_vocabulary_management
        );

        AdminSystemBarHelper.applyTopInset(
                this,
                findViewById(R.id.rootVocabularyManagement)
        );

        repository =
                new AdminVocabularyRepository();

        bindViews();
        setupRecycler();
        setupEvents();
        loadVocabulary();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (repository != null
                && adapter != null) {

            loadVocabulary();
        }
    }

    private void bindViews() {

        btnBack =
                findViewById(
                        R.id.btnVocabularyAdminBack
                );

        tvTotal =
                findViewById(
                        R.id.tvAdminVocabularyTotal
                );

        tvStatus =
                findViewById(
                        R.id.tvAdminVocabularyStatus
                );

        tvEmpty =
                findViewById(
                        R.id.tvAdminVocabularyEmpty
                );

        edtSearch =
                findViewById(
                        R.id.edtAdminVocabularySearch
                );

        btnAdd =
                findViewById(
                        R.id.btnAdminVocabularyAdd
                );

        btnImport =
                findViewById(
                        R.id.btnAdminVocabularyImport
                );

        btnRefresh =
                findViewById(
                        R.id.btnAdminVocabularyRefresh
                );

        recycler =
                findViewById(
                        R.id.recyclerAdminVocabulary
                );
    }

    private void setupRecycler() {

        adapter =
                new AdminVocabularyAdapter(
                        this::showVocabularyActions
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

        btnRefresh.setOnClickListener(
                view -> loadVocabulary()
        );

        btnAdd.setOnClickListener(
                view ->
                        showVocabularyEditor(
                                null
                        )
        );

        btnImport.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    VocabularyManagementActivity.this,
                                    ImportVocabularyActivity.class
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

    private void loadVocabulary() {

        setLoading(
                true,
                "Đang tải kho từ vựng..."
        );

        repository.getAllVocabulary(
                new AdminVocabularyRepository.VocabularyListCallback() {

                    @Override
                    public void onSuccess(
                            List<AdminVocabulary> vocabularies
                    ) {

                        adapter.submitList(
                                vocabularies
                        );

                        tvTotal.setText(
                                String.valueOf(
                                        vocabularies.size()
                                )
                        );

                        boolean empty =
                                vocabularies.isEmpty();

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
                                        + vocabularies.size()
                                        + " từ."
                        );
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        setLoading(
                                false,
                                "Không tải được vocabularies: "
                                        + exception.getMessage()
                        );
                    }
                }
        );
    }

    private void showVocabularyActions(
            AdminVocabulary vocabulary
    ) {

        String[] actions =
                {
                        "Xem",
                        "Sửa",
                        "Xóa"
                };

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        vocabulary.getEnglishWord()
                )
                .setItems(
                        actions,
                        (dialog, which) -> {

                            switch (which) {

                                case 0:
                                    showVocabularyInfo(
                                            vocabulary
                                    );
                                    break;

                                case 1:
                                    showVocabularyEditor(
                                            vocabulary
                                    );
                                    break;

                                case 2:
                                    confirmDelete(
                                            vocabulary
                                    );
                                    break;
                            }
                        }
                )
                .setNegativeButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void showVocabularyInfo(
            AdminVocabulary vocabulary
    ) {

        String message =
                "Document ID:\n"
                        + safe(vocabulary.getId())
                        + "\n\nPhiên âm: "
                        + safe(vocabulary.getPhonetic())
                        + "\n\nNghĩa:\n"
                        + safe(vocabulary.getVietnameseMeaning())
                        + "\n\nChủ đề: "
                        + safe(vocabulary.getTopic())
                        + "\nLevel: "
                        + safe(vocabulary.getLevel());

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        vocabulary.getEnglishWord()
                )
                .setMessage(message)
                .setPositiveButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void showVocabularyEditor(
            @Nullable AdminVocabulary existing
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

        EditText edtWord =
                createField(
                        "Từ tiếng Anh",
                        editing
                                ? existing.getEnglishWord()
                                : ""
                );

        EditText edtPhonetic =
                createField(
                        "Phiên âm",
                        editing
                                ? existing.getPhonetic()
                                : ""
                );

        EditText edtMeaning =
                createField(
                        "Nghĩa tiếng Việt",
                        editing
                                ? existing.getVietnameseMeaning()
                                : ""
                );

        EditText edtTopic =
                createField(
                        "Chủ đề (không bắt buộc)",
                        editing
                                ? existing.getTopic()
                                : ""
                );

        EditText edtLevel =
                createField(
                        "Level (không bắt buộc)",
                        editing
                                ? existing.getLevel()
                                : ""
                );

        container.addView(edtWord);
        container.addView(edtPhonetic);
        container.addView(edtMeaning);
        container.addView(edtTopic);
        container.addView(edtLevel);

        AlertDialog dialog =
                new AlertDialog.Builder(
                        this
                )
                        .setTitle(
                                editing
                                        ? "Sửa từ vựng"
                                        : "Thêm từ vựng"
                        )
                        .setView(container)
                        .setPositiveButton(
                                editing
                                        ? "Lưu"
                                        : "Thêm",
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

                                    String word =
                                            edtWord
                                                    .getText()
                                                    .toString()
                                                    .trim();

                                    String meaning =
                                            edtMeaning
                                                    .getText()
                                                    .toString()
                                                    .trim();

                                    if (word.isEmpty()) {

                                        edtWord.setError(
                                                "Nhập từ tiếng Anh."
                                        );

                                        return;
                                    }

                                    if (meaning.isEmpty()) {

                                        edtMeaning.setError(
                                                "Nhập nghĩa tiếng Việt."
                                        );

                                        return;
                                    }

                                    AdminVocabulary value =
                                            new AdminVocabulary();

                                    value.setEnglishWord(
                                            word
                                    );

                                    value.setPhonetic(
                                            edtPhonetic
                                                    .getText()
                                                    .toString()
                                                    .trim()
                                    );

                                    value.setVietnameseMeaning(
                                            meaning
                                    );

                                    value.setTopic(
                                            edtTopic
                                                    .getText()
                                                    .toString()
                                                    .trim()
                                    );

                                    value.setLevel(
                                            edtLevel
                                                    .getText()
                                                    .toString()
                                                    .trim()
                                    );

                                    if (editing) {

                                        repository.updateVocabulary(
                                                existing.getId(),
                                                value,
                                                new VocabularyActionCallback(
                                                        "Đã cập nhật từ vựng.",
                                                        dialog
                                                )
                                        );

                                    } else {

                                        repository.addVocabulary(
                                                value,
                                                new VocabularyActionCallback(
                                                        "Đã thêm từ vựng.",
                                                        dialog
                                                )
                                        );
                                    }
                                }
                        )
        );

        dialog.show();
    }

    private void confirmDelete(
            AdminVocabulary vocabulary
    ) {

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "Xóa từ vựng?"
                )
                .setMessage(
                        "Bạn sắp xóa \""
                                + vocabulary.getEnglishWord()
                                + "\" khỏi kho từ vựng.\n\n"
                                + "Lưu ý: progress/favorite cũ của người dùng có thể vẫn còn document tham chiếu tới từ này."
                )
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) ->
                                repository.deleteVocabulary(
                                        vocabulary.getId(),
                                        new VocabularyActionCallback(
                                                "Đã xóa từ vựng.",
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

        editText.setHint(
                hint
        );

        editText.setText(
                value == null
                        ? ""
                        : value
        );

        editText.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

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

        editText.setLayoutParams(
                params
        );

        return editText;
    }

    private void setLoading(
            boolean loading,
            String message
    ) {

        btnAdd.setEnabled(
                !loading
        );

        btnImport.setEnabled(
                !loading
        );

        btnRefresh.setEnabled(
                !loading
        );

        tvStatus.setText(
                message
        );
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

    private class VocabularyActionCallback
            implements AdminVocabularyRepository.ActionCallback {

        private final String message;
        private final AlertDialog dialog;

        VocabularyActionCallback(
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
                    VocabularyManagementActivity.this,
                    message,
                    Toast.LENGTH_SHORT
            ).show();

            loadVocabulary();
        }

        @Override
        public void onFailure(
                Exception exception
        ) {

            Toast.makeText(
                    VocabularyManagementActivity.this,
                    "Thao tác thất bại: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}