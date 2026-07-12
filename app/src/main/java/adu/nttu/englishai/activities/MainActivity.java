package adu.nttu.englishai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ImageView;
import adu.nttu.englishai.R;
import adu.nttu.englishai.fragments.GameFragment;
import adu.nttu.englishai.fragments.HomeFragment;
import adu.nttu.englishai.fragments.ProgressFragment;
import adu.nttu.englishai.fragments.VocabularyFragment;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View layoutAiTutor = findViewById(R.id.layoutAiTutor);

        layoutAiTutor.setOnClickListener(view -> {
            view.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(120)
                    .withEndAction(() -> {
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .withEndAction(() -> {
                                    Intent intent = new Intent(
                                            MainActivity.this,
                                            AiTutorActivity.class
                                    );

                                    startActivity(intent);
                                })
                                .start();
                    })
                    .start();
        });
        db = FirebaseFirestore.getInstance();

        BottomNavigationView bottomNav =
                findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new HomeFragment()
                        )
                        .commit();

                return true;

            } else if (itemId == R.id.nav_vocabulary) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new VocabularyFragment()
                        )
                        .commit();

                return true;

            } else if (itemId == R.id.nav_progress) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new ProgressFragment()
                        )
                        .commit();

                return true;

            } else if (itemId == R.id.nav_game) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new GameFragment()
                        )
                        .commit();

                return true;
            }

            return false;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
    private void startIdleAnimation(ImageView imageView) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                imageView,
                "translationY",
                0f,
                -12f,
                0f
        );

        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.start();
    }
}