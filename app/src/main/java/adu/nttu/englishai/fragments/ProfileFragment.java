package adu.nttu.englishai.fragments;

import adu.nttu.englishai.admin.activities.AdminDashboardActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import adu.nttu.englishai.R;
import adu.nttu.englishai.activities.LoginActivity;
import adu.nttu.englishai.activities.LeaderboardActivity;

public class ProfileFragment extends Fragment {

    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_LEARNING = "LEARNING";
    private static final String STATUS_LEARNED = "LEARNED";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private ImageView imgAvatar;
    private TextView tvProfileName;
    private TextView tvProfileEmail;

    private TextView tvTotalVocabulary;
    private TextView tvNotStartedCount;
    private TextView tvLearningCount;
    private TextView tvLearnedCount;
    private TextView tvFavoriteCount;


    private final Set<String> validVocabularyIds = new HashSet<>();
    private final Map<String, DocumentSnapshot> vocabularyMap = new HashMap<>();
    private final Map<String, DocumentSnapshot> progressMap = new HashMap<>();

    private ListenerRegistration vocabularyListener;
    private ListenerRegistration progressListener;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public ProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            processAndUploadBase64Avatar(selectedImageUri);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews(view);
        setupEvents(view);
        loadUserInformation();
        startRealtimeStatistics();
    }

    private void initViews(View view) {
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);

        tvTotalVocabulary = view.findViewById(R.id.tvTotalVocabulary);
        tvNotStartedCount = view.findViewById(R.id.tvNotStartedCount);
        tvLearningCount = view.findViewById(R.id.tvLearningCount);
        tvLearnedCount = view.findViewById(R.id.tvMasteredCount);
        tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount);
    }

    private void setupEvents(View view) {
        MaterialCardView cardMastered = view.findViewById(R.id.cardMastered);
        MaterialCardView cardLearning = view.findViewById(R.id.cardLearning);
        MaterialCardView cardNotStarted = view.findViewById(R.id.cardNotStarted);
        MaterialCardView cardFavorite = view.findViewById(R.id.cardFavorite);

        if (cardMastered != null) cardMastered.setOnClickListener(v -> showWordListBottomSheet("🟢 Từ vựng đã thuộc", "learned"));
        if (cardLearning != null) cardLearning.setOnClickListener(v -> showWordListBottomSheet("🟡 Từ vựng đang học", "learning"));
        if (cardNotStarted != null) cardNotStarted.setOnClickListener(v -> showWordListBottomSheet("⚪ Từ vựng chưa học", "not_started"));
        if (cardFavorite != null) cardFavorite.setOnClickListener(v -> showWordListBottomSheet("❤️ Từ vựng yêu thích", "favorite"));

        MaterialCardView btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showSettingsBottomSheet());
        }

        MaterialCardView cardLeaderboard = view.findViewById(R.id.cardLeaderboard);
        if (cardLeaderboard != null) {
            cardLeaderboard.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), LeaderboardActivity.class));
            });
        }
    }

    // KỸ THUẬT XỬ LÝ ẢNH BASE64 (KHÔNG CẦN FIREBASE STORAGE)
    private void processAndUploadBase64Avatar(Uri imageUri) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || getContext() == null) return;

        Toast.makeText(requireContext(), "Đang xử lý ảnh...", Toast.LENGTH_SHORT).show();

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
            int maxSize = 256;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float bitmapRatio = (float)width / (float) height;
            if (bitmapRatio > 1) {
                width = maxSize;
                height = (int) (width / bitmapRatio);
            } else {
                height = maxSize;
                width = (int) (height * bitmapRatio);
            }
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            updateAvatarUrlInDatabase(base64Image);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAvatarUrlInDatabase(String base64Data) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        firestore.collection("users").document(user.getUid())
                .update("avatarUrl", base64Data)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    if (imgAvatar != null) {
                        Glide.with(this).load(decodedByte).circleCrop().into(imgAvatar);
                    }
                    Toast.makeText(requireContext(), "Đổi ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Lỗi lưu ảnh lên mây: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserInformation() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            if (tvProfileName != null) tvProfileName.setText("Khách");
            if (tvProfileEmail != null) tvProfileEmail.setText("Chưa đăng nhập");
            resetStatistics();
            return;
        }

        String authName = currentUser.getDisplayName();
        String authEmail = currentUser.getEmail();

        if (tvProfileEmail != null) tvProfileEmail.setText(authEmail == null ? "" : authEmail);

        if (tvProfileName != null) {
            if (authName != null && !authName.trim().isEmpty()) {
                tvProfileName.setText(authName.trim());
            } else if (authEmail != null && !authEmail.trim().isEmpty()) {
                String fallbackName = authEmail.contains("@") ? authEmail.substring(0, authEmail.indexOf("@")) : authEmail;
                if (!fallbackName.isEmpty()) fallbackName = fallbackName.substring(0, 1).toUpperCase(Locale.ROOT) + fallbackName.substring(1);
                tvProfileName.setText(fallbackName);
            } else {
                tvProfileName.setText("Học viên EnglishAI");
            }
        }

        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (!isAdded()) return;

                    String firestoreName = firstNonEmpty(document.getString("name"), document.getString("fullName"));
                    if (tvProfileName != null && !firestoreName.isEmpty()) {
                        tvProfileName.setText(firestoreName);
                    }

                    String role = document.getString("role");
                    boolean isAdmin = role != null && "admin".equalsIgnoreCase(role.trim());

                    String avatarData = document.getString("avatarUrl");
                    if (imgAvatar != null) {
                        if (avatarData != null && !avatarData.trim().isEmpty()) {
                            try {
                                if (avatarData.length() > 500) {
                                    byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    Glide.with(this).load(decodedByte).circleCrop().into(imgAvatar);
                                } else {
                                    Glide.with(this).load(avatarData).circleCrop().into(imgAvatar);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (currentUser.getPhotoUrl() != null) {
                            Glide.with(this).load(currentUser.getPhotoUrl()).circleCrop().into(imgAvatar);
                        }
                    }

                    Long scoreObj = document.getLong("score");
                    long realScore = (scoreObj != null) ? scoreObj : 0;
                    TextView tvTotalScore = getView().findViewById(R.id.tvTotalScore);
                    if (tvTotalScore != null) tvTotalScore.setText(realScore + " XP");

                    Long streakObj = document.getLong("streak");
                    long realStreak = (streakObj != null) ? streakObj : 0;
                    TextView tvStreak = getView().findViewById(R.id.tvStreak);
                    if (tvStreak != null) tvStreak.setText(realStreak + " Ngày");
                });
    }

    // HỘP THOẠI CÀI ĐẶT
    private void showSettingsBottomSheet() {
        if (getContext() == null) return;

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_settings,
                        null
                );

        dialog.setContentView(dialogView);

        LinearLayout itemAvatar =
                dialogView.findViewById(
                        R.id.itemChangeAvatar
                );

        LinearLayout itemInfo =
                dialogView.findViewById(
                        R.id.itemUpdateInfo
                );

        LinearLayout itemHelp =
                dialogView.findViewById(
                        R.id.itemHelp
                );

        LinearLayout itemAdminPanel =
                dialogView.findViewById(
                        R.id.itemAdminPanel
                );

        Button dialogBtnLogout =
                dialogView.findViewById(
                        R.id.dialogBtnLogout
                );

        /*
         * Mặc định ẩn Admin.
         * Chỉ hiện sau khi xác nhận role == admin từ Firestore.
         */
        if (itemAdminPanel != null) {
            itemAdminPanel.setVisibility(
                    View.GONE
            );
        }

        FirebaseUser currentUser =
                firebaseAuth == null
                        ? null
                        : firebaseAuth.getCurrentUser();

        if (currentUser != null
                && itemAdminPanel != null) {

            firestore.collection("users")
                    .document(
                            currentUser.getUid()
                    )
                    .get()
                    .addOnSuccessListener(document -> {

                        if (!isAdded()) {
                            return;
                        }

                        String role =
                                document.getString(
                                        "role"
                                );

                        boolean isAdmin =
                                role != null
                                        && "admin".equalsIgnoreCase(
                                        role.trim()
                                );

                        itemAdminPanel.setVisibility(
                                isAdmin
                                        ? View.VISIBLE
                                        : View.GONE
                        );
                    })
                    .addOnFailureListener(exception ->
                            itemAdminPanel.setVisibility(
                                    View.GONE
                            )
                    );
        }

        if (itemAvatar != null) {
            itemAvatar.setOnClickListener(v -> {
                dialog.dismiss();

                Intent intent =
                        new Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        );

                imagePickerLauncher.launch(
                        intent
                );
            });
        }

        if (itemInfo != null) {
            itemInfo.setOnClickListener(v -> {
                dialog.dismiss();
                showEditProfileDialog();
            });
        }

        if (itemHelp != null) {
            itemHelp.setOnClickListener(v ->
                    Toast.makeText(
                            requireContext(),
                            "Tính năng trợ giúp sẽ được cập nhật.",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }

        if (itemAdminPanel != null) {
            itemAdminPanel.setOnClickListener(v -> {
                dialog.dismiss();

                Intent intent =
                        new Intent(
                                requireContext(),
                                AdminDashboardActivity.class
                        );

                startActivity(
                        intent
                );
            });
        }

        if (dialogBtnLogout != null) {
            dialogBtnLogout.setOnClickListener(v -> {
                dialog.dismiss();

                if (firebaseAuth != null) {
                    firebaseAuth.signOut();
                }

                Intent intent =
                        new Intent(
                                requireActivity(),
                                LoginActivity.class
                        );

                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                startActivity(
                        intent
                );

                requireActivity().finish();
            });
        }

        dialog.show();
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Cập nhật thông tin");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (tvProfileName != null) {
            input.setText(tvProfileName.getText().toString());
            input.setSelection(input.getText().length());
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setPadding(60, 20, 60, 0);
        layout.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton("Lưu thay đổi", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateUserName(newName);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateUserName(String newName) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(newName).build();
        user.updateProfile(profileUpdates);

        firestore.collection("users").document(user.getUid())
                .update("name", newName, "fullName", newName)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    if (tvProfileName != null) tvProfileName.setText(newName);
                    requireActivity().getSharedPreferences("EnglishAI_Prefs", Context.MODE_PRIVATE)
                            .edit().putString("USER_REAL_NAME", newName).apply();
                    Toast.makeText(getContext(), "Đã cập nhật tên!", Toast.LENGTH_SHORT).show();
                });
    }

    private void startRealtimeStatistics() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            resetStatistics(); return;
        }
        stopRealtimeListeners();
        listenToVocabularyCollection();
        listenToWordProgress(currentUser.getUid());
    }

    private void listenToVocabularyCollection() {
        vocabularyListener = firestore.collection("vocabularies").addSnapshotListener((snapshot, error) -> {
            if (!isAdded() || error != null) return;
            validVocabularyIds.clear();
            vocabularyMap.clear();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    validVocabularyIds.add(doc.getId());
                    vocabularyMap.put(doc.getId(), doc);
                }
            }
            calculateAndDisplayStatistics();
        });
    }

    private void listenToWordProgress(String userId) {
        progressListener = firestore.collection("users").document(userId).collection("wordProgress").addSnapshotListener((snapshot, error) -> {
            if (!isAdded() || error != null) return;
            progressMap.clear();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) progressMap.put(doc.getId(), doc);
            }
            calculateAndDisplayStatistics();
        });
    }

    private void calculateAndDisplayStatistics() {
        int totalCount = validVocabularyIds.size(), notStartedCount = 0, learningCount = 0, learnedCount = 0, favoriteCount = 0;
        for (String id : validVocabularyIds) {
            DocumentSnapshot progressDoc = progressMap.get(id);
            if (progressDoc == null || !progressDoc.exists()) {
                notStartedCount++; continue;
            }
            String status = normalizeLearningStatus(firstNonEmpty(progressDoc.getString("learningStatus"), progressDoc.getString("status")));
            switch (status) {
                case STATUS_LEARNING: learningCount++; break;
                case STATUS_LEARNED: learnedCount++; break;
                default: notStartedCount++; break;
            }
            Boolean favorite = progressDoc.getBoolean("favorite");
            if (favorite != null && favorite) favoriteCount++;
        }
        updateStatistics(totalCount, notStartedCount, learningCount, learnedCount, favoriteCount);
    }

    private void updateStatistics(int total, int notStarted, int learning, int learned, int fav) {
        if (!isAdded() || getView() == null) return;
        if (tvTotalVocabulary != null) tvTotalVocabulary.setText(String.valueOf(total));
        if (tvNotStartedCount != null) tvNotStartedCount.setText(String.valueOf(notStarted));
        if (tvLearningCount != null) tvLearningCount.setText(String.valueOf(learning));
        if (tvLearnedCount != null) tvLearnedCount.setText(String.valueOf(learned));
        if (tvFavoriteCount != null) tvFavoriteCount.setText(String.valueOf(fav));
    }

    private void resetStatistics() {
        validVocabularyIds.clear(); progressMap.clear(); vocabularyMap.clear();
        updateStatistics(0, 0, 0, 0, 0);
    }

    private void showWordListBottomSheet(String title, String filterType) {
        if (getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_word_list, null);
        dialog.setContentView(dialogView);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
        LinearLayout layoutWordContainer = dialogView.findViewById(R.id.layoutWordContainer);

        if (tvDialogTitle != null) tvDialogTitle.setText(title);
        if (btnCloseDialog != null) btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        if (layoutWordContainer != null) {
            layoutWordContainer.removeAllViews();
            int matchCount = 0;
            for (String vocabularyId : validVocabularyIds) {
                DocumentSnapshot vocabularyDocument = vocabularyMap.get(vocabularyId);
                DocumentSnapshot progressDocument = progressMap.get(vocabularyId);
                String status = STATUS_NOT_STARTED;
                boolean favorite = false;

                if (progressDocument != null && progressDocument.exists()) {
                    status = normalizeLearningStatus(firstNonEmpty(progressDocument.getString("learningStatus"), progressDocument.getString("status")));
                    Boolean favVal = progressDocument.getBoolean("favorite");
                    favorite = (favVal != null && favVal);
                }

                if (!matchesFilter(filterType, status, favorite) || vocabularyDocument == null) continue;
                String word = firstNonEmpty(vocabularyDocument.getString("englishWord"), vocabularyDocument.getString("word"));
                String meaning = firstNonEmpty(vocabularyDocument.getString("vietnameseMeaning"), vocabularyDocument.getString("meaning"));
                String pronunciation = firstNonEmpty(vocabularyDocument.getString("pronunciation"), vocabularyDocument.getString("phonetic"));

                if (!word.isEmpty()) {
                    addWordCardToContainer(layoutWordContainer, word, pronunciation, meaning);
                    matchCount++;
                }
            }
            if (matchCount == 0) showEmptyMessage(layoutWordContainer);
        }
        dialog.show();
    }

    private boolean matchesFilter(String filterType, String status, boolean favorite) {
        if ("favorite".equals(filterType)) return favorite;
        if ("learned".equals(filterType)) return STATUS_LEARNED.equals(status);
        if ("learning".equals(filterType)) return STATUS_LEARNING.equals(status);
        if ("not_started".equals(filterType)) return STATUS_NOT_STARTED.equals(status);
        return false;
    }

    private void addWordCardToContainer(LinearLayout container, String word, String phonetic, String meaning) {
        if (getContext() == null) return;
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(Color.parseColor("#F8F9FA"));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(Color.parseColor("#E9ECEF"));

        LinearLayout innerLayout = new LinearLayout(requireContext());
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        TextView tvWord = new TextView(requireContext());
        tvWord.setText(word + (phonetic != null && !phonetic.trim().isEmpty() ? "  " + phonetic.trim() : ""));
        tvWord.setTextSize(17f);
        tvWord.setTypeface(null, android.graphics.Typeface.BOLD);
        tvWord.setTextColor(Color.parseColor("#1A73E8"));

        TextView tvMeaning = new TextView(requireContext());
        tvMeaning.setText(meaning == null || meaning.trim().isEmpty() ? "👉 Chưa có nghĩa tiếng Việt" : "👉 Nghĩa: " + meaning.trim());
        tvMeaning.setTextSize(14f);
        tvMeaning.setTextColor(Color.parseColor("#37474F"));
        tvMeaning.setPadding(0, dpToPx(6), 0, 0);

        innerLayout.addView(tvWord);
        innerLayout.addView(tvMeaning);
        card.addView(innerLayout);
        container.addView(card);
    }

    private void showEmptyMessage(LinearLayout container) {
        if (getContext() == null || container == null) return;
        TextView tvEmpty = new TextView(requireContext());
        tvEmpty.setText("📭 Chưa có từ vựng nào trong mục này.\nHãy qua trang Từ vựng để học nhé!");
        tvEmpty.setTextSize(15f);
        tvEmpty.setTextColor(Color.parseColor("#757575"));
        tvEmpty.setGravity(Gravity.CENTER);
        tvEmpty.setPadding(dpToPx(20), dpToPx(40), dpToPx(20), dpToPx(40));
        container.addView(tvEmpty);
    }

    private String normalizeLearningStatus(String status) {
        if (status == null || status.trim().isEmpty()) return STATUS_NOT_STARTED;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("LEARNED".equals(normalized) || "MASTERED".equals(normalized)) return STATUS_LEARNED;
        if ("LEARNING".equals(normalized)) return STATUS_LEARNING;
        return STATUS_NOT_STARTED;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void stopRealtimeListeners() {
        if (vocabularyListener != null) { vocabularyListener.remove(); vocabularyListener = null; }
        if (progressListener != null) { progressListener.remove(); progressListener = null; }
    }

    @Override
    public void onDestroyView() {
        stopRealtimeListeners();
        super.onDestroyView();
    }
}