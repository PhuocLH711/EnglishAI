package adu.nttu.englishai.admin.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import adu.nttu.englishai.R;
import adu.nttu.englishai.admin.repositories.AdminDashboardRepository;
import adu.nttu.englishai.admin.utils.AdminAccessManager;
import adu.nttu.englishai.admin.utils.AdminSystemBarHelper;

public class AdminDashboardActivity extends AppCompatActivity {

    private View layoutAdminLoading;
    private View layoutAdminContent;

    private TextView btnAdminBack;
    private TextView tvAdminDashboardStatus;

    private TextView tvDashboardUsers;
    private TextView tvDashboardUsersSub;

    private TextView tvDashboardVocabulary;
    private TextView tvDashboardGrammar;

    private TextView tvDashboardToeic;
    private TextView tvDashboardToeicSub;

    private View barDashboardUsers;
    private View barDashboardVocabulary;
    private View barDashboardGrammar;
    private View barDashboardToeic;

    private MaterialCardView cardAdminUsers;
    private MaterialCardView cardAdminVocabulary;
    private MaterialCardView cardAdminGrammar;
    private MaterialCardView cardAdminToeic;

    private AdminAccessManager accessManager;
    private AdminDashboardRepository dashboardRepository;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_admin_dashboard
        );

        AdminSystemBarHelper.applyTopInset(
                this,
                findViewById(
                        R.id.rootAdminDashboard
                )
        );

        bindViews();

        accessManager =
                new AdminAccessManager();

        dashboardRepository =
                new AdminDashboardRepository();

        btnAdminBack.setOnClickListener(
                view -> finish()
        );

        checkPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (layoutAdminContent != null
                && layoutAdminContent.getVisibility()
                == View.VISIBLE
                && dashboardRepository != null) {

            loadDashboardStats();
        }
    }

    private void bindViews() {

        layoutAdminLoading =
                findViewById(
                        R.id.layoutAdminLoading
                );

        layoutAdminContent =
                findViewById(
                        R.id.layoutAdminContent
                );

        btnAdminBack =
                findViewById(
                        R.id.btnAdminBack
                );

        tvAdminDashboardStatus =
                findViewById(
                        R.id.tvAdminDashboardStatus
                );

        tvDashboardUsers =
                findViewById(
                        R.id.tvDashboardUsers
                );

        tvDashboardUsersSub =
                findViewById(
                        R.id.tvDashboardUsersSub
                );

        tvDashboardVocabulary =
                findViewById(
                        R.id.tvDashboardVocabulary
                );

        tvDashboardGrammar =
                findViewById(
                        R.id.tvDashboardGrammar
                );

        tvDashboardToeic =
                findViewById(
                        R.id.tvDashboardToeic
                );

        tvDashboardToeicSub =
                findViewById(
                        R.id.tvDashboardToeicSub
                );

        barDashboardUsers =
                findViewById(
                        R.id.barDashboardUsers
                );

        barDashboardVocabulary =
                findViewById(
                        R.id.barDashboardVocabulary
                );

        barDashboardGrammar =
                findViewById(
                        R.id.barDashboardGrammar
                );

        barDashboardToeic =
                findViewById(
                        R.id.barDashboardToeic
                );

        cardAdminUsers =
                findViewById(
                        R.id.cardAdminUsers
                );

        cardAdminVocabulary =
                findViewById(
                        R.id.cardAdminVocabulary
                );

        cardAdminGrammar =
                findViewById(
                        R.id.cardAdminGrammar
                );

        cardAdminToeic =
                findViewById(
                        R.id.cardAdminToeic
                );
    }

    private void checkPermission() {

        layoutAdminLoading.setVisibility(
                View.VISIBLE
        );

        layoutAdminContent.setVisibility(
                View.GONE
        );

        accessManager.checkAdminAccess(
                new AdminAccessManager.AdminAccessCallback() {

                    @Override
                    public void onResult(
                            boolean isAdmin
                    ) {

                        if (!isAdmin) {

                            Toast.makeText(
                                    AdminDashboardActivity.this,
                                    "Bạn không có quyền truy cập khu vực quản trị.",
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();
                            return;
                        }

                        layoutAdminLoading.setVisibility(
                                View.GONE
                        );

                        layoutAdminContent.setVisibility(
                                View.VISIBLE
                        );

                        setupAdminActions();
                        loadDashboardStats();
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        Toast.makeText(
                                AdminDashboardActivity.this,
                                "Không kiểm tra được quyền quản trị: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }
                }
        );
    }

    private void setupAdminActions() {

        cardAdminUsers.setOnClickListener(
                view ->
                        startActivity(
                                new Intent(
                                        AdminDashboardActivity.this,
                                        UserManagementActivity.class
                                )
                        )
        );

        cardAdminVocabulary.setOnClickListener(
                view ->
                        startActivity(
                                new Intent(
                                        AdminDashboardActivity.this,
                                        VocabularyManagementActivity.class
                                )
                        )
        );

        cardAdminGrammar.setOnClickListener(
                view ->
                        startActivity(
                                new Intent(
                                        AdminDashboardActivity.this,
                                        GrammarManagementActivity.class
                                )
                        )
        );

        cardAdminToeic.setOnClickListener(
                view ->
                        startActivity(
                                new Intent(
                                        AdminDashboardActivity.this,
                                        ToeicAdminActivity.class
                                )
                        )
        );
    }

    private void loadDashboardStats() {

        tvAdminDashboardStatus.setText(
                "Đang đồng bộ dữ liệu..."
        );

        dashboardRepository.loadStats(
                new AdminDashboardRepository.StatsCallback() {

                    @Override
                    public void onSuccess(
                            AdminDashboardRepository.DashboardStats stats
                    ) {

                        bindStatistics(
                                stats
                        );

                        tvAdminDashboardStatus.setText(
                                "Dữ liệu đã được cập nhật."
                        );
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        tvAdminDashboardStatus.setText(
                                "Không tải được thống kê: "
                                        + exception.getMessage()
                        );
                    }
                }
        );
    }

    private void bindStatistics(
            AdminDashboardRepository.DashboardStats stats
    ) {

        tvDashboardUsers.setText(
                String.valueOf(
                        stats.totalUsers
                )
        );

        tvDashboardUsersSub.setText(
                stats.totalAdmins
                        + " admin"
        );

        tvDashboardVocabulary.setText(
                String.valueOf(
                        stats.totalVocabulary
                )
        );

        tvDashboardGrammar.setText(
                String.valueOf(
                        stats.totalGrammar
                )
        );

        tvDashboardToeic.setText(
                String.valueOf(
                        stats.totalToeicQuestions
                )
        );

        tvDashboardToeicSub.setText(
                stats.totalToeicTests
                        + " đề"
        );

        int maxValue =
                Math.max(
                        1,
                        Math.max(
                                stats.totalUsers,
                                Math.max(
                                        stats.totalVocabulary,
                                        Math.max(
                                                stats.totalGrammar,
                                                stats.totalToeicQuestions
                                        )
                                )
                        )
                );

        animateBar(
                barDashboardUsers,
                stats.totalUsers,
                maxValue
        );

        animateBar(
                barDashboardVocabulary,
                stats.totalVocabulary,
                maxValue
        );

        animateBar(
                barDashboardGrammar,
                stats.totalGrammar,
                maxValue
        );

        animateBar(
                barDashboardToeic,
                stats.totalToeicQuestions,
                maxValue
        );
    }

    private void animateBar(
            View bar,
            int value,
            int maxValue
    ) {

        int minHeight =
                dpToPx(
                        22
                );

        int maxHeight =
                dpToPx(
                        132
                );

        float ratio =
                maxValue <= 0
                        ? 0f
                        : (float) value
                          / (float) maxValue;

        int targetHeight =
                minHeight
                        + Math.round(
                        ratio
                                * (
                                maxHeight
                                        - minHeight
                        )
                );

        ViewGroup.LayoutParams initialParams =
                bar.getLayoutParams();

        initialParams.height =
                minHeight;

        bar.setLayoutParams(
                initialParams
        );

        ValueAnimator animator =
                ValueAnimator.ofInt(
                        minHeight,
                        targetHeight
                );

        animator.setDuration(
                650L
        );

        animator.setInterpolator(
                new DecelerateInterpolator()
        );

        animator.addUpdateListener(
                animation -> {

                    ViewGroup.LayoutParams params =
                            bar.getLayoutParams();

                    params.height =
                            (int) animation
                                    .getAnimatedValue();

                    bar.setLayoutParams(
                            params
                    );
                }
        );

        animator.start();
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
}