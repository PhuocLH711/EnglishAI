package adu.nttu.englishai.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import adu.nttu.englishai.R;
import adu.nttu.englishai.fragments.FlashcardFragment;
import adu.nttu.englishai.fragments.GameFragment;
import adu.nttu.englishai.fragments.HomeFragment;
import adu.nttu.englishai.fragments.SpeakingFragment;
import adu.nttu.englishai.fragments.VocabularyFragment;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
                return true;
            } else if (itemId == R.id.nav_vocabulary) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SpeakingFragment())
                        .commit();
                return true;
            } else if (itemId == R.id.nav_game) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new GameFragment())
                        .commit();
                return true;
            }
            return false;
        });

        // Mở HomeFragment đầu tiên khi vào app
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}