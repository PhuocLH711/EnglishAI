package adu.nttu.englishai.admin.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Kiểm tra quyền truy cập khu vực quản trị.
 *
 * Quy ước:
 * users/{uid}/role = "admin"
 *
 * Lưu ý:
 * Đây chỉ là lớp kiểm tra quyền ở UI.
 * Firestore Security Rules vẫn phải chặn quyền ghi/xóa đối với user thường.
 */
public class AdminAccessManager {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public AdminAccessManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public interface AdminAccessCallback {
        void onResult(boolean isAdmin);
        void onFailure(Exception exception);
    }

    public void checkAdminAccess(
            AdminAccessCallback callback
    ) {

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            callback.onResult(false);
            return;
        }

        firestore
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(
                        document -> {

                            String role =
                                    document.getString(
                                            "role"
                                    );

                            callback.onResult(
                                    role != null
                                            && "admin".equalsIgnoreCase(
                                            role.trim()
                                    )
                            );
                        }
                )
                .addOnFailureListener(
                        callback::onFailure
                );
    }
}
