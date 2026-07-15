package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import adu.nttu.englishai.R;

public class GameFragment extends Fragment {

    public GameFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        Button btnOpenQuiz = view.findViewById(R.id.btnOpenQuiz);
        Button btnOpenFlashcard = view.findViewById(R.id.btnOpenFlashcard);

        // Nút mở Quiz
        btnOpenQuiz.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new QuizFragment())
                    .addToBackStack(null) // Thêm dòng này để cho phép Back lại menu
                    .commit();
        });

        // Nút mở Flashcard
        btnOpenFlashcard.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FlashcardFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Ánh xạ thẻ Lật Thẻ Tìm Cặp từ fragment_game.xml
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

        return view;
    }
}