package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import adu.nttu.englishai.R;

// GAME FRAGMENT: Màn hình Menu điều hướng tới các trò chơi ôn luyện
public class GameFragment extends Fragment {

    public GameFragment() {}

    // HÀM TẠO GIAO DIỆN VÀ GÁN SỰ KIỆN ĐIỀU HƯỚNG
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_game,
                container,
                false
        );

        // =========================================================
        // 1. TRẮC NGHIỆM (QUIZ)
        // =========================================================
        Button btnOpenQuiz =
                view.findViewById(R.id.btnOpenQuiz);

        btnOpenQuiz.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new QuizFragment()
                    )
                    .addToBackStack(null)
                    .commit();
        });

        // =========================================================
        // 2. FLASHCARD
        // =========================================================
        Button btnOpenFlashcard =
                view.findViewById(R.id.btnOpenFlashcard);

        btnOpenFlashcard.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new FlashcardFragment()
                    )
                    .addToBackStack(null)
                    .commit();
        });

        // =========================================================
        // 3. XẾP CÂU NGỮ PHÁP
        // =========================================================
        View cardGrammarSprint =
                view.findViewById(R.id.cardGrammarSprint);

        View btnOpenGrammar =
                view.findViewById(R.id.btnOpenGrammar);

        View.OnClickListener openGrammar = v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new SentenceArrangeFragment()
                    )
                    .addToBackStack(null)
                    .commit();
        };

        if (cardGrammarSprint != null) {
            cardGrammarSprint.setOnClickListener(openGrammar);
        }

        if (btnOpenGrammar != null) {
            btnOpenGrammar.setOnClickListener(openGrammar);
        }

        // =========================================================
        // 4. VIẾT LẠI CÂU
        // =========================================================
        View cardSentenceRewrite =
                view.findViewById(R.id.cardSentenceRewrite);

        View btnOpenSentenceRewrite =
                view.findViewById(R.id.btnOpenSentenceRewrite);

        View.OnClickListener openSentenceRewrite = v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new SentenceRewriteFragment()
                    )
                    .addToBackStack(null)
                    .commit();
        };

        if (cardSentenceRewrite != null) {
            cardSentenceRewrite.setOnClickListener(
                    openSentenceRewrite
            );
        }

        if (btnOpenSentenceRewrite != null) {
            btnOpenSentenceRewrite.setOnClickListener(
                    openSentenceRewrite
            );
        }

        // =========================================================
        // 5. LẬT THẺ TÌM CẶP
        // =========================================================
        View cardMemoryMatch =
                view.findViewById(R.id.cardMemoryMatch);

        View btnOpenMemoryMatch =
                view.findViewById(R.id.btnOpenMemoryMatch);

        View.OnClickListener openMemory = v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new MemoryMatchFragment()
                    )
                    .addToBackStack(null)
                    .commit();
        };

        if (cardMemoryMatch != null) {
            cardMemoryMatch.setOnClickListener(openMemory);
        }

        if (btnOpenMemoryMatch != null) {
            btnOpenMemoryMatch.setOnClickListener(openMemory);
        }

        // =========================================================
        // 6. LUYỆN PHÁT ÂM
        // =========================================================
        View cardSpeaking =
                view.findViewById(R.id.cardSpeaking);

        View btnOpenSpeaking =
                view.findViewById(R.id.btnOpenSpeaking);

        View.OnClickListener openSpeaking = v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new SpeakingFragment()
                    )
                    .addToBackStack(null)
                    .commit();
        };

        if (cardSpeaking != null) {
            cardSpeaking.setOnClickListener(openSpeaking);
        }

        if (btnOpenSpeaking != null) {
            btnOpenSpeaking.setOnClickListener(openSpeaking);
        }

        return view;
    }
}