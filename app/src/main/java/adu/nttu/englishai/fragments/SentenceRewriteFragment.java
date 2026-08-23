package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.models.SentenceRewriteExercise;
import adu.nttu.englishai.repositories.SentenceRewriteRepository;

public class SentenceRewriteFragment extends Fragment {

    private final SentenceRewriteRepository repository =
            new SentenceRewriteRepository();

    private final List<SentenceRewriteExercise> exercises =
            new ArrayList<>();

    private TextView tvProgress, tvXp, tvOriginalSentence,
            tvPrompt, tvResultTitle, tvCorrectAnswer, tvExplanation;

    private EditText edtAnswer;
    private Button btnBack, btnCheck, btnNext;
    private View layoutResult;
    private ProgressBar progressBar;

    private int currentIndex = 0;
    private int totalXp = 0;
    private boolean answered = false;

    public SentenceRewriteFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_sentence_rewrite,
                container,
                false
        );

        bindViews(view);
        bindEvents();
        loadExercises();

        return view;
    }

    private void bindViews(View view) {
        tvProgress = view.findViewById(R.id.tvProgress);
        tvXp = view.findViewById(R.id.tvXp);
        tvOriginalSentence = view.findViewById(R.id.tvOriginalSentence);
        tvPrompt = view.findViewById(R.id.tvPrompt);
        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvCorrectAnswer = view.findViewById(R.id.tvCorrectAnswer);
        tvExplanation = view.findViewById(R.id.tvExplanation);

        edtAnswer = view.findViewById(R.id.edtAnswer);

        btnBack = view.findViewById(R.id.btnBack);
        btnCheck = view.findViewById(R.id.btnCheck);
        btnNext = view.findViewById(R.id.btnNext);

        layoutResult = view.findViewById(R.id.layoutResult);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void bindEvents() {
        btnBack.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        btnCheck.setOnClickListener(v ->
                checkAnswer()
        );

        btnNext.setOnClickListener(v ->
                showNextQuestion()
        );
    }

    private void loadExercises() {
        progressBar.setVisibility(View.VISIBLE);
        btnCheck.setEnabled(false);

        repository.getForGame(
                new SentenceRewriteRepository.ListCallback() {

                    @Override
                    public void onSuccess(
                            List<SentenceRewriteExercise> list
                    ) {
                        progressBar.setVisibility(View.GONE);

                        exercises.clear();
                        exercises.addAll(list);

                        if (exercises.isEmpty()) {
                            Toast.makeText(
                                    requireContext(),
                                    "Chưa có dữ liệu Viết Lại Câu. Hãy thêm bài từ trang Admin.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        currentIndex = 0;
                        totalXp = 0;

                        btnCheck.setEnabled(true);
                        showQuestion();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);

                        Toast.makeText(
                                requireContext(),
                                "Không tải được bài tập: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showQuestion() {
        SentenceRewriteExercise item =
                exercises.get(currentIndex);

        answered = false;

        tvProgress.setText(
                "Câu " + (currentIndex + 1)
                        + "/" + exercises.size()
        );

        tvXp.setText(totalXp + " XP");
        tvOriginalSentence.setText(item.getOriginalSentence());
        tvPrompt.setText(item.getPrompt());

        edtAnswer.setText("");
        edtAnswer.setEnabled(true);

        layoutResult.setVisibility(View.GONE);
        btnCheck.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
    }

    private void checkAnswer() {
        if (answered || exercises.isEmpty()) {
            return;
        }

        String userAnswer =
                edtAnswer.getText()
                        .toString()
                        .trim();

        if (userAnswer.isEmpty()) {
            edtAnswer.setError(
                    "Hãy nhập câu trả lời"
            );
            return;
        }

        SentenceRewriteExercise item =
                exercises.get(currentIndex);

        boolean correct =
                normalize(userAnswer)
                        .equals(
                                normalize(item.getAnswer())
                        );

        answered = true;
        edtAnswer.setEnabled(false);

        layoutResult.setVisibility(View.VISIBLE);

        if (correct) {
            int earnedXp =
                    (int) (
                            item.getXp() > 0
                                    ? item.getXp()
                                    : 10
                    );

            totalXp += earnedXp;

            tvResultTitle.setText(
                    "✅ Chính xác! +" + earnedXp + " XP"
            );

            addXpToCurrentUser(earnedXp);

        } else {
            tvResultTitle.setText(
                    "❌ Chưa chính xác"
            );
        }

        tvCorrectAnswer.setText(
                "Đáp án: " + item.getAnswer()
        );

        String explanation =
                item.getExplanation();

        if (explanation == null
                || explanation.trim().isEmpty()) {
            explanation =
                    "Hãy xem lại cấu trúc của đáp án đúng.";
        }

        tvExplanation.setText(
                "Giải thích: " + explanation
        );

        tvXp.setText(totalXp + " XP");

        btnCheck.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
    }

    private void showNextQuestion() {
        currentIndex++;

        if (currentIndex >= exercises.size()) {
            Toast.makeText(
                    requireContext(),
                    "Hoàn thành! Bạn nhận được "
                            + totalXp
                            + " XP.",
                    Toast.LENGTH_LONG
            ).show();

            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();

            return;
        }

        showQuestion();
    }

    private void addXpToCurrentUser(int xp) {
        if (FirebaseAuth.getInstance()
                .getCurrentUser() == null) {
            return;
        }

        String uid =
                FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(
                        "score",
                        FieldValue.increment(xp)
                );
    }

    private String normalize(String value) {
        if (value == null) return "";

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[.!?]+$", "")
                .replaceAll("\\s+", " ");
    }
}
