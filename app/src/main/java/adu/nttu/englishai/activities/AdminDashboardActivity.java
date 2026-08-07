package adu.nttu.englishai.activities;
import adu.nttu.englishai.activities.ToeicAdminActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import adu.nttu.englishai.R;
import adu.nttu.englishai.utils.AdminAccessManager;

/**
 * Dashboard quản trị chung của EnglishAI.
 *
 * Không xuất hiện trong Bottom Navigation của người học.
 * Chỉ tài khoản có users/{uid}.role = "admin" mới được vào.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private View layoutAdminLoading;
    private View layoutAdminContent;

    private TextView btnAdminBack;

    private MaterialCardView cardAdminUsers;
    private MaterialCardView cardAdminVocabulary;
    private MaterialCardView cardAdminGrammar;
    private MaterialCardView cardAdminToeic;

    private AdminAccessManager adminAccessManager;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_admin_dashboard
        );

        bindViews();

        adminAccessManager =
                new AdminAccessManager();

        checkPermission();
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

        btnAdminBack.setOnClickListener(
                view -> finish()
        );
    }

    private void checkPermission() {

        layoutAdminLoading.setVisibility(
                View.VISIBLE
        );

        layoutAdminContent.setVisibility(
                View.GONE
        );

        adminAccessManager.checkAdminAccess(
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

        cardAdminToeic.setOnClickListener(
                view -> startActivity(
                        new Intent(
                                AdminDashboardActivity.this,
                                ToeicAdminActivity.class
                        )
                )
        );

        cardAdminUsers.setOnClickListener(
                view -> showComingSoon(
                        "Quản lý người dùng"
                )
        );

        cardAdminVocabulary.setOnClickListener(
                view -> showComingSoon(
                        "Quản lý từ vựng"
                )
        );

        cardAdminGrammar.setOnClickListener(
                view -> showComingSoon(
                        "Quản lý ngữ pháp"
                )
        );
    }

    private void showComingSoon(
            String feature
    ) {

        Toast.makeText(
                this,
                feature
                        + " sẽ được triển khai tiếp theo.",
                Toast.LENGTH_SHORT
        ).show();
    }
}
