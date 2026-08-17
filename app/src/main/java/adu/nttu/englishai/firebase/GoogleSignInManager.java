package adu.nttu.englishai.firebase;

import android.app.Activity;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import adu.nttu.englishai.R;

// =========================================================================
// GOOGLE SIGN-IN MANAGER
// Dùng BUTTON FLOW của Credential Manager
// =========================================================================
public class GoogleSignInManager {

    private final Activity activity;
    private final CredentialManager credentialManager;

    // =========================================================================
    // CALLBACK
    // =========================================================================
    public interface GoogleSignInCallback {

        void onSuccess(String idToken);

        void onError(String message);
    }

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public GoogleSignInManager(Activity activity) {

        this.activity = activity;

        this.credentialManager =
                CredentialManager.create(activity);
    }

    // =========================================================================
    // ĐĂNG NHẬP GOOGLE
    // =========================================================================
    public void signIn(
            GoogleSignInCallback callback
    ) {

        // =========================================================
        // BUTTON FLOW
        //
        // Quan trọng:
        // Không dùng GetGoogleIdOption ở đây nữa.
        //
        // Vì app có nút "Tiếp tục với Google" riêng.
        // =========================================================
        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(
                        activity.getString(
                                R.string.default_web_client_id
                        )
                )
                        .build();

        // =========================================================
        // TẠO REQUEST
        // =========================================================
        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(
                                googleOption
                        )
                        .build();

        // =========================================================
        // MỞ GOOGLE SIGN-IN
        // =========================================================
        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                activity.getMainExecutor(),

                new CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException>() {

                    // =================================================
                    // GOOGLE TRẢ CREDENTIAL
                    // =================================================
                    @Override
                    public void onResult(
                            GetCredentialResponse result
                    ) {

                        handleCredential(
                                result.getCredential(),
                                callback
                        );
                    }

                    // =================================================
                    // LỖI
                    // =================================================
                    @Override
                    public void onError(
                            @NonNull GetCredentialException e
                    ) {

                        String message =
                                e.getMessage();

                        if (message == null
                                || message.trim().isEmpty()) {

                            message =
                                    "Không thể mở đăng nhập Google";
                        }

                        callback.onError(
                                message
                        );
                    }
                }
        );
    }

    // =========================================================================
    // XỬ LÝ GOOGLE CREDENTIAL
    // =========================================================================
    private void handleCredential(
            Credential credential,
            GoogleSignInCallback callback
    ) {

        // =========================================================
        // GOOGLE TRẢ CUSTOM CREDENTIAL
        // =========================================================
        if (!(credential instanceof CustomCredential)) {

            callback.onError(
                    "Thông tin đăng nhập Google không hợp lệ"
            );

            return;
        }

        CustomCredential customCredential =
                (CustomCredential) credential;

        // =========================================================
        // KIỂM TRA GOOGLE ID TOKEN
        // =========================================================
        if (!GoogleIdTokenCredential
                .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(
                        customCredential.getType()
                )) {

            callback.onError(
                    "Không nhận được Google ID Token"
            );

            return;
        }

        try {

            // =====================================================
            // CHUYỂN DATA THÀNH GOOGLE CREDENTIAL
            // =====================================================
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential
                            .createFrom(
                                    customCredential.getData()
                            );

            String idToken =
                    googleCredential.getIdToken();

            if (idToken == null
                    || idToken.trim().isEmpty()) {

                callback.onError(
                        "Google không trả về ID Token"
                );

                return;
            }

            // =====================================================
            // TRẢ TOKEN VỀ LOGIN ACTIVITY
            // =====================================================
            callback.onSuccess(
                    idToken
            );

        } catch (Exception e) {

            callback.onError(
                    "Không thể đọc tài khoản Google: "
                            + e.getMessage()
            );
        }
    }
}