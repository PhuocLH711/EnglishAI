package adu.nttu.englishai.admin.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.admin.utils.AdminSystemBarHelper;
import adu.nttu.englishai.admin.adapters.AdminUserAdapter;
import adu.nttu.englishai.admin.repositories.AdminUserRepository;
import adu.nttu.englishai.admin.repositories.AdminUserRepository.AdminUser;

/**
 * Quản lý người dùng EnglishAI.
 *
 * Có:
 * - Danh sách users
 * - Search tên / email / uid
 * - Đổi role user <-> admin
 * - Reset XP
 * - Reset streak
 * - Xóa profile Firestore
 *
 * Tự bảo vệ tài khoản admin đang đăng nhập:
 * không cho tự hạ role hoặc tự xóa profile.
 */
public class UserManagementActivity
        extends AppCompatActivity {

    private TextView btnBack;
    private TextView tvTotalUsers;
    private TextView tvAdminCount;
    private TextView tvEmpty;
    private TextView tvStatus;

    private EditText edtSearch;

    private MaterialButton btnRefresh;

    private RecyclerView recyclerUsers;

    private AdminUserRepository repository;
    private AdminUserAdapter adapter;

    private String currentUid = "";

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_user_management
        );

        AdminSystemBarHelper.applyTopInset(
                this,
                findViewById(R.id.rootUserManagement)
        );

        FirebaseUser currentUser =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (currentUser != null) {
            currentUid =
                    currentUser.getUid();
        }

        repository =
                new AdminUserRepository();

        bindViews();
        setupRecycler();
        setupEvents();
        loadUsers();
    }

    private void bindViews() {

        btnBack =
                findViewById(
                        R.id.btnUserAdminBack
                );

        tvTotalUsers =
                findViewById(
                        R.id.tvAdminUserTotal
                );

        tvAdminCount =
                findViewById(
                        R.id.tvAdminRoleCount
                );

        tvEmpty =
                findViewById(
                        R.id.tvAdminUserEmpty
                );

        tvStatus =
                findViewById(
                        R.id.tvAdminUserStatus
                );

        edtSearch =
                findViewById(
                        R.id.edtAdminUserSearch
                );

        btnRefresh =
                findViewById(
                        R.id.btnAdminUserRefresh
                );

        recyclerUsers =
                findViewById(
                        R.id.recyclerAdminUsers
                );
    }

    private void setupRecycler() {

        adapter =
                new AdminUserAdapter(
                        this::showUserActions
                );

        recyclerUsers.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recyclerUsers.setAdapter(
                adapter
        );
    }

    private void setupEvents() {

        btnBack.setOnClickListener(
                view -> finish()
        );

        btnRefresh.setOnClickListener(
                view -> loadUsers()
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

    private void loadUsers() {

        setLoading(
                true,
                "Đang tải danh sách người dùng..."
        );

        repository.getAllUsers(
                new AdminUserRepository.UserListCallback() {

                    @Override
                    public void onSuccess(
                            List<AdminUser> users
                    ) {

                        adapter.submitList(
                                users
                        );

                        int adminCount = 0;

                        for (AdminUser user
                                : users) {

                            if ("admin".equalsIgnoreCase(
                                    user.getRole()
                            )) {
                                adminCount++;
                            }
                        }

                        tvTotalUsers.setText(
                                String.valueOf(
                                        users.size()
                                )
                        );

                        tvAdminCount.setText(
                                String.valueOf(
                                        adminCount
                                )
                        );

                        boolean empty =
                                users.isEmpty();

                        tvEmpty.setVisibility(
                                empty
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        recyclerUsers.setVisibility(
                                empty
                                        ? View.GONE
                                        : View.VISIBLE
                        );

                        setLoading(
                                false,
                                "Đã tải "
                                        + users.size()
                                        + " tài khoản."
                        );
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        setLoading(
                                false,
                                "Không tải được users: "
                                        + exception.getMessage()
                        );
                    }
                }
        );
    }

    private void showUserActions(
            AdminUser user
    ) {

        boolean isSelf =
                user.getUid() != null
                        && user.getUid()
                        .equals(currentUid);

        String[] actions;

        if (isSelf) {

            actions =
                    new String[]{
                            "Xem thông tin",
                            "Reset XP",
                            "Reset streak"
                    };

        } else {

            actions =
                    new String[]{
                            "Xem thông tin",
                            "Đổi vai trò",
                            "Reset XP",
                            "Reset streak",
                            "Xóa profile Firestore"
                    };
        }

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        user.getName().isEmpty()
                                ? "Người dùng"
                                : user.getName()
                )
                .setItems(
                        actions,
                        (dialog, which) -> {

                            String selected =
                                    actions[which];

                            switch (selected) {

                                case "Xem thông tin":
                                    showUserInfo(
                                            user
                                    );
                                    break;

                                case "Đổi vai trò":
                                    showRoleDialog(
                                            user
                                    );
                                    break;

                                case "Reset XP":
                                    confirmResetScore(
                                            user
                                    );
                                    break;

                                case "Reset streak":
                                    confirmResetStreak(
                                            user
                                    );
                                    break;

                                case "Xóa profile Firestore":
                                    confirmDeleteProfile(
                                            user
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

    private void showUserInfo(
            AdminUser user
    ) {

        String message =
                "UID:\n"
                        + safe(user.getUid())
                        + "\n\nEmail:\n"
                        + safe(user.getEmail())
                        + "\n\nRole: "
                        + user.getRole()
                        + "\nXP: "
                        + user.getScore()
                        + "\nStreak: "
                        + user.getStreak()
                        + " ngày";

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        user.getName().isEmpty()
                                ? "Thông tin người dùng"
                                : user.getName()
                )
                .setMessage(message)
                .setPositiveButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void showRoleDialog(
            AdminUser user
    ) {

        String[] roles =
                {"user", "admin"};

        int checked =
                "admin".equalsIgnoreCase(
                        user.getRole()
                )
                        ? 1
                        : 0;

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "Đổi vai trò"
                )
                .setSingleChoiceItems(
                        roles,
                        checked,
                        null
                )
                .setPositiveButton(
                        "Lưu",
                        (dialog, which) -> {

                            AlertDialog alertDialog =
                                    (AlertDialog) dialog;

                            int selectedPosition =
                                    alertDialog
                                            .getListView()
                                            .getCheckedItemPosition();

                            String role =
                                    selectedPosition == 1
                                            ? "admin"
                                            : "user";

                            updateRole(
                                    user,
                                    role
                            );
                        }
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    private void updateRole(
            AdminUser user,
            String role
    ) {

        repository.updateRole(
                user.getUid(),
                role,
                new SimpleActionCallback(
                        "Đã đổi role thành "
                                + role
                )
        );
    }

    private void confirmResetScore(
            AdminUser user
    ) {

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "Reset XP?"
                )
                .setMessage(
                        "XP của "
                                + displayName(user)
                                + " sẽ trở về 0."
                )
                .setPositiveButton(
                        "Reset",
                        (dialog, which) ->
                                repository.resetScore(
                                        user.getUid(),
                                        new SimpleActionCallback(
                                                "Đã reset XP."
                                        )
                                )
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    private void confirmResetStreak(
            AdminUser user
    ) {

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "Reset streak?"
                )
                .setMessage(
                        "Chuỗi lửa của "
                                + displayName(user)
                                + " sẽ trở về 0."
                )
                .setPositiveButton(
                        "Reset",
                        (dialog, which) ->
                                repository.resetStreak(
                                        user.getUid(),
                                        new SimpleActionCallback(
                                                "Đã reset streak."
                                        )
                                )
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    private void confirmDeleteProfile(
            AdminUser user
    ) {

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "Xóa profile?"
                )
                .setMessage(
                        "Sẽ xóa document users/"
                                + user.getUid()
                                + " khỏi Firestore.\n\n"
                                + "Tài khoản Firebase Authentication KHÔNG bị xóa."
                )
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) ->
                                repository.deleteUserProfile(
                                        user.getUid(),
                                        new SimpleActionCallback(
                                                "Đã xóa profile Firestore."
                                        )
                                )
                )
                .setNegativeButton(
                        "Hủy",
                        null
                )
                .show();
    }

    private void setLoading(
            boolean loading,
            String message
    ) {

        btnRefresh.setEnabled(
                !loading
        );

        tvStatus.setText(
                message
        );
    }

    private String displayName(
            AdminUser user
    ) {

        if (!user.getName().isEmpty()) {
            return user.getName();
        }

        if (!user.getEmail().isEmpty()) {
            return user.getEmail();
        }

        return "người dùng này";
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

    private class SimpleActionCallback
            implements AdminUserRepository.ActionCallback {

        private final String successMessage;

        SimpleActionCallback(
                String successMessage
        ) {
            this.successMessage =
                    successMessage;
        }

        @Override
        public void onSuccess() {

            Toast.makeText(
                    UserManagementActivity.this,
                    successMessage,
                    Toast.LENGTH_SHORT
            ).show();

            loadUsers();
        }

        @Override
        public void onFailure(
                Exception exception
        ) {

            Toast.makeText(
                    UserManagementActivity.this,
                    "Thao tác thất bại: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}