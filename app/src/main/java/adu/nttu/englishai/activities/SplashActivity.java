package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import adu.nttu.englishai.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        firebaseAuth = FirebaseAuth.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(
                this::checkLoginStatus,
                SPLASH_DELAY
        );
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        Intent intent;

        if (currentUser != null) {
            intent = new Intent(
                    SplashActivity.this,
                    MainActivity.class
            );
        } else {
            intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );
        }

        startActivity(intent);
        finish();
    }
}