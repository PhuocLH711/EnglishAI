package adu.nttu.englishai.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import adu.nttu.englishai.R;
import adu.nttu.englishai.admin.utils.AdminSystemBarHelper;
import adu.nttu.englishai.admin.repositories.AdminDashboardRepository;
import adu.nttu.englishai.admin.utils.AdminAccessManager;

public class AdminDashboardActivity extends AppCompatActivity {

    private View layoutAdminLoading;
    private View layoutAdminContent;

    private TextView btnAdminBack;
    private TextView tvAdminDashboardStatus;
    private TextView tvDashboardUsers;
    private TextView tvDashboardVocabulary;
    private TextView tvDashboardGrammar;
    private TextView tvDashboardToeic;

    private MaterialCardView cardAdminUsers;
    private MaterialCardView cardAdminVocabulary;
    private MaterialCardView cardAdminGrammar;
    private MaterialCardView cardAdminToeic;

    private AdminAccessManager accessManager;
    private AdminDashboardRepository dashboardRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        AdminSystemBarHelper.applyTopInset(
                this,
                findViewById(R.id.rootAdminDashboard)
        );

        bindViews();

        accessManager = new AdminAccessManager();
        dashboardRepository = new AdminDashboardRepository();

        btnAdminBack.setOnClickListener(v -> finish());

        checkPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutAdminContent != null
                && layoutAdminContent.getVisibility() == View.VISIBLE
                && dashboardRepository != null) {
            loadDashboardStats();
        }
    }

    private void bindViews() {
        layoutAdminLoading = findViewById(R.id.layoutAdminLoading);
        layoutAdminContent = findViewById(R.id.layoutAdminContent);

        btnAdminBack = findViewById(R.id.btnAdminBack);
        tvAdminDashboardStatus = findViewById(R.id.tvAdminDashboardStatus);

        tvDashboardUsers = findViewById(R.id.tvDashboardUsers);
        tvDashboardVocabulary = findViewById(R.id.tvDashboardVocabulary);
        tvDashboardGrammar = findViewById(R.id.tvDashboardGrammar);
        tvDashboardToeic = findViewById(R.id.tvDashboardToeic);

        cardAdminUsers = findViewById(R.id.cardAdminUsers);
        cardAdminVocabulary = findViewById(R.id.cardAdminVocabulary);
        cardAdminGrammar = findViewById(R.id.cardAdminGrammar);
        cardAdminToeic = findViewById(R.id.cardAdminToeic);
    }

    private void checkPermission() {
        layoutAdminLoading.setVisibility(View.VISIBLE);
        layoutAdminContent.setVisibility(View.GONE);

        accessManager.checkAdminAccess(
                new AdminAccessManager.AdminAccessCallback() {
                    @Override
                    public void onResult(boolean isAdmin) {
                        if (!isAdmin) {
                            Toast.makeText(
                                    AdminDashboardActivity.this,
                                    "Bạn không có quyền truy cập khu vực quản trị.",
                                    Toast.LENGTH_LONG
                            ).show();
                            finish();
                            return;
                        }

                        layoutAdminLoading.setVisibility(View.GONE);
                        layoutAdminContent.setVisibility(View.VISIBLE);

                        setupAdminActions();
                        loadDashboardStats();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Toast.makeText(
                                AdminDashboardActivity.this,
                                "Không kiểm tra được quyền quản trị: " + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    }
                }
        );
    }

    private void setupAdminActions() {
        cardAdminUsers.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        UserManagementActivity.class
                )));

        cardAdminVocabulary.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        VocabularyManagementActivity.class
                )));

        cardAdminGrammar.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        GrammarManagementActivity.class
                )));

        cardAdminToeic.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        ToeicAdminActivity.class
                )));
    }

    private void loadDashboardStats() {
        tvAdminDashboardStatus.setText("Đang đồng bộ dữ liệu...");

        dashboardRepository.loadStats(
                new AdminDashboardRepository.StatsCallback() {
                    @Override
                    public void onSuccess(AdminDashboardRepository.DashboardStats stats) {
                        tvDashboardUsers.setText(
                                stats.totalUsers + "\n" + stats.totalAdmins + " admin"
                        );

                        tvDashboardVocabulary.setText(
                                String.valueOf(stats.totalVocabulary)
                        );

                        tvDashboardGrammar.setText(
                                String.valueOf(stats.totalGrammar)
                        );

                        tvDashboardToeic.setText(
                                stats.totalToeicTests + " đề\n"
                                        + stats.totalToeicQuestions + " câu"
                        );

                        tvAdminDashboardStatus.setText(
                                "Dữ liệu đã được cập nhật."
                        );
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        tvAdminDashboardStatus.setText(
                                "Không tải được thống kê: " + exception.getMessage()
                        );
                    }
                }
        );
    }
}