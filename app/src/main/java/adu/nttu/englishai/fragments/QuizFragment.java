package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import adu.nttu.englishai.R;

public class QuizFragment extends Fragment {

    private Button btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4;

    public QuizFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        btnAnswer1 = view.findViewById(R.id.btnAnswer1);
        btnAnswer2 = view.findViewById(R.id.btnAnswer2);
        btnAnswer3 = view.findViewById(R.id.btnAnswer3);
        btnAnswer4 = view.findViewById(R.id.btnAnswer4);

        btnAnswer1.setOnClickListener(v -> checkAnswer(false));
        btnAnswer2.setOnClickListener(v -> checkAnswer(false));
        btnAnswer3.setOnClickListener(v -> checkAnswer(true));
        btnAnswer4.setOnClickListener(v -> checkAnswer(false));

        return view;
    }

    private void checkAnswer(boolean isCorrect) {
        if (isCorrect) {
            Toast.makeText(getContext(), "Chính xác! 🎉 Tuyệt vời!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Sai rồi, thử lại nhé! 😅", Toast.LENGTH_SHORT).show();
        }
    }
}