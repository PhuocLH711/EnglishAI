package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import adu.nttu.englishai.R;

// =========================================================================
// GAME FRAGMENT: Màn hình Menu điều hướng tới các trò chơi ôn luyện
// =========================================================================
public class GameFragment extends Fragment {

    public GameFragment() {}

    // =========================================================================
    // HÀM TẠO GIAO DIỆN VÀ GÁN SỰ KIỆN ĐIỀU HƯỚNG
    // =========================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        // Ánh xạ các nút bấm mở trò chơi Quiz và Flashcard
        Button btnOpenQuiz = view.findViewById(R.id.btnOpenQuiz);
        Button btnOpenFlashcard = view.findViewById(R.id.btnOpenFlashcard);

        // =========================================================================
        // 1. SỰ KIỆN MỞ TRÒ CHƠI TRẮC NGHIỆM (QUIZ FRAGMENT)
        // =========================================================================
        btnOpenQuiz.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new QuizFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // =========================================================================
        // 2. SỰ KIỆN MỞ TRÒ CHƠI LẬT THẺ
        // =========================================================================
        btnOpenFlashcard.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FlashcardFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // =========================================================================
        // 3. SỰ KIỆN MỞ TRÒ CHƠI TÌM CẶP TỪ
        // =========================================================================
        View cardMemoryMatch = view.findViewById(R.id.cardMemoryMatch);
        View btnOpenMemoryMatch = view.findViewById(R.id.btnOpenMemoryMatch);

        View.OnClickListener openMemory = v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MemoryMatchFragment())
                    .addToBackStack(null)
                    .commit();
        };

        if (cardMemoryMatch != null) cardMemoryMatch.setOnClickListener(openMemory);
        if (btnOpenMemoryMatch != null) btnOpenMemoryMatch.setOnClickListener(openMemory);

        // =========================================================================
        // 4. SỰ KIỆN MỞ TRÒ CHƠI LUYỆN PHÁT ÂM (MICROPHONE)
        // =========================================================================
        View cardSpeaking = view.findViewById(R.id.cardSpeaking);
        View btnOpenSpeaking = view.findViewById(R.id.btnOpenSpeaking);

        View.OnClickListener openSpeaking = v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SpeakingFragment())
                    .addToBackStack(null)
                    .commit();
        };

        if (cardSpeaking != null) cardSpeaking.setOnClickListener(openSpeaking);
        if (btnOpenSpeaking != null) btnOpenSpeaking.setOnClickListener(openSpeaking);

        return view;
    }
}