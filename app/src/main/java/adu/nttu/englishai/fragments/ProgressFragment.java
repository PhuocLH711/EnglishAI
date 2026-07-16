package adu.nttu.englishai.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import adu.nttu.englishai.R;
import adu.nttu.englishai.activities.LoginActivity;

public class ProgressFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvMasteredCount, tvLearningCount, tvNotStartedCount, tvFavoriteCount;

    public ProgressFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_progress, container, false);
        } catch (Exception e) {
            Log.e("PROGRESS_ERROR", "Lỗi tải layout fragment_progress: " + e.getMessage());
            return new View(getContext()); // Trả về view rỗng để tuyệt đối không bị văng app
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            TextView tvProfileName = view.findViewById(R.id.tvProfileName);
            TextView tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
            MaterialCardView btnSettings = view.findViewById(R.id.btnSettings);

            tvMasteredCount = view.findViewById(R.id.tvMasteredCount);
            tvLearningCount = view.findViewById(R.id.tvLearningCount);
            tvNotStartedCount = view.findViewById(R.id.tvNotStartedCount);
            tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount);

            MaterialCardView cardMastered = view.findViewById(R.id.cardMastered);
            MaterialCardView cardLearning = view.findViewById(R.id.cardLearning);
            MaterialCardView cardNotStarted = view.findViewById(R.id.cardNotStarted);
            MaterialCardView cardFavorite = view.findViewById(R.id.cardFavorite);

            if (cardMastered != null) {
                cardMastered.setOnClickListener(v -> showWordListBottomSheet("🟢 Từ Vựng Đã Thuộc", "mastered"));
            }
            if (cardLearning != null) {
                cardLearning.setOnClickListener(v -> showWordListBottomSheet("🟡 Từ Vựng Đang Học", "learning"));
            }
            if (cardNotStarted != null) {
                cardNotStarted.setOnClickListener(v -> showWordListBottomSheet("⚪ Từ Vựng Chưa Học", "not_started"));
            }
            if (cardFavorite != null) {
                cardFavorite.setOnClickListener(v -> showWordListBottomSheet("❤️ Từ Vựng Yêu Thích", "favorite"));
            }

            FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
            if (currentUser != null) {
                if (tvProfileEmail != null) tvProfileEmail.setText(currentUser.getEmail());

                String displayName = currentUser.getDisplayName();
                if (displayName != null && !displayName.trim().isEmpty()) {
                    if (tvProfileName != null) tvProfileName.setText(displayName.trim());
                } else if (currentUser.getEmail() != null) {
                    String email = currentUser.getEmail();
                    String fallback = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
                    if (!fallback.isEmpty()) {
                        fallback = fallback.substring(0, 1).toUpperCase() + fallback.substring(1);
                    }
                    if (tvProfileName != null) tvProfileName.setText(fallback);
                }
            }

            if (btnSettings != null) {
                btnSettings.setOnClickListener(v -> showSettingsBottomSheet());
            }
        } catch (Exception e) {
            Log.e("PROGRESS_ERROR", "Lỗi khởi tạo onViewCreated: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            loadRealVocabularyStats();
        } catch (Exception e) {
            Log.e("PROGRESS_ERROR", "Lỗi trong onResume: " + e.getMessage());
        }
    }

    private void loadRealVocabularyStats() {
        if (db == null) return;

        db.collection("vocabulary")
                .get()
                .addOnSuccessListener(snapshots -> {
                    try {
                        int mastered = 0;
                        int learning = 0;
                        int notStarted = 0;
                        int favorite = 0;

                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots) {
                                if (doc == null || doc.getData() == null) continue;

                                // Nhận diện tim an toàn chống crash kiểu dữ liệu
                                boolean isFav = false;
                                Object favObj1 = doc.get("isFavorite");
                                Object favObj2 = doc.get("favorite");
                                Object favObj3 = doc.get("isFav");

                                if (favObj1 != null) isFav = favObj1.toString().equalsIgnoreCase("true");
                                else if (favObj2 != null) isFav = favObj2.toString().equalsIgnoreCase("true");
                                else if (favObj3 != null) isFav = favObj3.toString().equalsIgnoreCase("true");

                                if (isFav) favorite++;

                                // Nhận diện trạng thái
                                String s1 = doc.getString("status");
                                String s2 = doc.getString("learningStatus");
                                String status = s1 != null ? s1 : (s2 != null ? s2 : "");

                                if ("mastered".equalsIgnoreCase(status) || "learned".equalsIgnoreCase(status)) {
                                    mastered++;
                                } else if ("learning".equalsIgnoreCase(status)) {
                                    learning++;
                                } else {
                                    notStarted++;
                                }
                            }
                        }

                        if (tvMasteredCount != null) tvMasteredCount.setText(String.valueOf(mastered));
                        if (tvLearningCount != null) tvLearningCount.setText(String.valueOf(learning));
                        if (tvNotStartedCount != null) tvNotStartedCount.setText(String.valueOf(notStarted));
                        if (tvFavoriteCount != null) tvFavoriteCount.setText(String.valueOf(favorite));
                    } catch (Exception e) {
                        Log.e("PROGRESS_ERROR", "Lỗi tính toán số liệu: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PROGRESS_ERROR", "Lỗi tải Firestore: " + e.getMessage());
                });
    }

    private void showWordListBottomSheet(String title, String filterType) {
        if (getContext() == null) return;

        try {
            BottomSheetDialog dialog = new BottomSheetDialog(getContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_word_list, null);
            dialog.setContentView(dialogView);

            TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
            TextView btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
            LinearLayout layoutWordContainer = dialogView.findViewById(R.id.layoutWordContainer);

            if (tvDialogTitle != null) tvDialogTitle.setText(title);
            if (btnCloseDialog != null) btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

            if (layoutWordContainer != null) {
                layoutWordContainer.removeAllViews();

                if (db != null) {
                    db.collection("vocabulary")
                            .get()
                            .addOnSuccessListener(snapshots -> {
                                try {
                                    int matchCount = 0;
                                    if (snapshots != null) {
                                        for (DocumentSnapshot doc : snapshots) {
                                            if (doc == null || doc.getData() == null) continue;

                                            String w1 = doc.getString("englishWord");
                                            String w2 = doc.getString("word");
                                            String word = w1 != null ? w1 : w2;

                                            String meaning = doc.getString("vietnameseMeaning");
                                            if (meaning == null) meaning = doc.getString("meaning");

                                            String phonetic = doc.getString("pronunciation");
                                            if (phonetic == null) phonetic = doc.getString("phonetic");

                                            boolean isFav = false;
                                            Object f1 = doc.get("isFavorite");
                                            Object f2 = doc.get("favorite");
                                            if (f1 != null) isFav = f1.toString().equalsIgnoreCase("true");
                                            else if (f2 != null) isFav = f2.toString().equalsIgnoreCase("true");

                                            String s1 = doc.getString("status");
                                            String s2 = doc.getString("learningStatus");
                                            String status = s1 != null ? s1 : (s2 != null ? s2 : "");

                                            boolean match = false;
                                            if ("favorite".equals(filterType) && isFav) match = true;
                                            else if ("not_started".equals(filterType) && (status.isEmpty() || "not_started".equalsIgnoreCase(status))) match = true;
                                            else if ("mastered".equals(filterType) && ("mastered".equalsIgnoreCase(status) || "learned".equalsIgnoreCase(status))) match = true;
                                            else if ("learning".equals(filterType) && "learning".equalsIgnoreCase(status)) match = true;

                                            if (match && word != null) {
                                                addWordCardToContainer(layoutWordContainer, word,
                                                        phonetic != null ? phonetic : "",
                                                        meaning != null ? meaning : "");
                                                matchCount++;
                                            }
                                        }
                                    }

                                    if (matchCount == 0) showEmptyMessage(layoutWordContainer);
                                } catch (Exception e) {
                                    showEmptyMessage(layoutWordContainer);
                                }
                            })
                            .addOnFailureListener(e -> showEmptyMessage(layoutWordContainer));
                }
            }

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi hiển thị danh sách từ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addWordCardToContainer(LinearLayout container, String word, String phonetic, String meaning) {
        if (getContext() == null) return;

        try {
            MaterialCardView card = new MaterialCardView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);
            card.setRadius(24f);
            card.setCardElevation(3f);
            card.setCardBackgroundColor(Color.parseColor("#F8F9FA"));
            card.setStrokeWidth(2);
            card.setStrokeColor(Color.parseColor("#E9ECEF"));

            LinearLayout innerLayout = new LinearLayout(getContext());
            innerLayout.setOrientation(LinearLayout.VERTICAL);
            innerLayout.setPadding(32, 24, 32, 24);

            TextView tvWord = new TextView(getContext());
            tvWord.setText(word + "  " + phonetic);
            tvWord.setTextSize(17f);
            tvWord.setTypeface(null, android.graphics.Typeface.BOLD);
            tvWord.setTextColor(Color.parseColor("#1A73E8"));

            TextView tvMeaning = new TextView(getContext());
            tvMeaning.setText("👉 Nghĩa: " + meaning);
            tvMeaning.setTextSize(14f);
            tvMeaning.setTextColor(Color.parseColor("#37474F"));
            tvMeaning.setPadding(0, 8, 0, 0);

            innerLayout.addView(tvWord);
            innerLayout.addView(tvMeaning);
            card.addView(innerLayout);
            container.addView(card);
        } catch (Exception e) {
            Log.e("PROGRESS_ERROR", "Lỗi tạo thẻ từ vựng: " + e.getMessage());
        }
    }

    private void showEmptyMessage(LinearLayout container) {
        if (getContext() == null || container == null) return;

        try {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("📭 Bé chưa có từ vựng nào trong mục này!\nHãy qua trang Từ vựng để học và thả tim ngay nhé! ❤️");
            tvEmpty.setTextSize(15f);
            tvEmpty.setTextColor(Color.parseColor("#757575"));
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(32, 64, 32, 64);
            tvEmpty.setLineSpacing(8f, 1f);

            container.addView(tvEmpty);
        } catch (Exception e) {
            Log.e("PROGRESS_ERROR", "Lỗi hiện thông báo trống: " + e.getMessage());
        }
    }

    private void showSettingsBottomSheet() {
        if (getContext() == null) return;

        try {
            BottomSheetDialog dialog = new BottomSheetDialog(getContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
            dialog.setContentView(dialogView);

            LinearLayout itemAvatar = dialogView.findViewById(R.id.itemChangeAvatar);
            LinearLayout itemInfo = dialogView.findViewById(R.id.itemUpdateInfo);
            LinearLayout itemHelp = dialogView.findViewById(R.id.itemHelp);
            Button dialogBtnLogout = dialogView.findViewById(R.id.dialogBtnLogout);

            if (itemAvatar != null) itemAvatar.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Tính năng đổi ảnh đang được cập nhật!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            if (itemInfo != null) itemInfo.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Tính năng sửa thông tin đang được cập nhật!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            if (itemHelp != null) itemHelp.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Bé hãy chạm vào các Ải ở Trang chủ để bắt đầu học nhé!", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            });

            if (dialogBtnLogout != null) {
                dialogBtnLogout.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (mAuth != null) mAuth.signOut();
                    Toast.makeText(getContext(), "Đã đăng xuất tài khoản thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) getActivity().finish();
                });
            }

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi mở bảng cài đặt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}