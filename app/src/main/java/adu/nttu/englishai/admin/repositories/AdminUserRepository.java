package adu.nttu.englishai.admin.repositories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository quản lý collection users dành cho Admin.
 *
 * Lưu ý:
 * - deleteUserProfile() chỉ xóa document Firestore users/{uid}.
 * - Nó KHÔNG xóa tài khoản trong Firebase Authentication.
 *   Muốn xóa Auth user an toàn cần backend/Admin SDK.
 */
public class AdminUserRepository {

    private final FirebaseFirestore db;

    public AdminUserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static class AdminUser {

        private String uid;
        private String name;
        private String email;
        private String role;
        private long score;
        private long streak;
        private String avatarUrl;

        public AdminUser() {
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getName() {
            return name == null ? "" : name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email == null ? "" : email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role == null || role.trim().isEmpty()
                    ? "user"
                    : role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public long getScore() {
            return score;
        }

        public void setScore(long score) {
            this.score = score;
        }

        public long getStreak() {
            return streak;
        }

        public void setStreak(long streak) {
            this.streak = streak;
        }

        public String getAvatarUrl() {
            return avatarUrl == null ? "" : avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }

    public interface UserListCallback {
        void onSuccess(List<AdminUser> users);
        void onFailure(Exception exception);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }

    public void getAllUsers(
            UserListCallback callback
    ) {

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        snapshots -> {

                            List<AdminUser> users =
                                    new ArrayList<>();

                            for (DocumentSnapshot document
                                    : snapshots.getDocuments()) {

                                AdminUser user =
                                        new AdminUser();

                                user.setUid(
                                        firstNonEmpty(
                                                document.getString("uid"),
                                                document.getId()
                                        )
                                );

                                user.setName(
                                        firstNonEmpty(
                                                document.getString("name"),
                                                document.getString("fullName")
                                        )
                                );

                                user.setEmail(
                                        firstNonEmpty(
                                                document.getString("email")
                                        )
                                );

                                user.setRole(
                                        firstNonEmpty(
                                                document.getString("role"),
                                                "user"
                                        )
                                );

                                Long score =
                                        document.getLong("score");

                                Long streak =
                                        document.getLong("streak");

                                user.setScore(
                                        score == null ? 0L : score
                                );

                                user.setStreak(
                                        streak == null ? 0L : streak
                                );

                                user.setAvatarUrl(
                                        firstNonEmpty(
                                                document.getString("avatarUrl")
                                        )
                                );

                                users.add(user);
                            }

                            callback.onSuccess(users);
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void updateRole(
            String uid,
            String newRole,
            ActionCallback callback
    ) {

        db.collection("users")
                .document(uid)
                .update(
                        "role",
                        newRole
                )
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void resetScore(
            String uid,
            ActionCallback callback
    ) {

        db.collection("users")
                .document(uid)
                .update(
                        "score",
                        0
                )
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void resetStreak(
            String uid,
            ActionCallback callback
    ) {

        db.collection("users")
                .document(uid)
                .update(
                        "streak",
                        0
                )
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    public void deleteUserProfile(
            String uid,
            ActionCallback callback
    ) {

        db.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess()
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }

    private String firstNonEmpty(
            String... values
    ) {

        if (values == null) {
            return "";
        }

        for (String value : values) {

            if (value != null
                    && !value.trim().isEmpty()) {

                return value.trim();
            }
        }

        return "";
    }
}
