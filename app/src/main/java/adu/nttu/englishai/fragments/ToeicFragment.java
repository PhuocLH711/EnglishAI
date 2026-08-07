package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import adu.nttu.englishai.R;

/**
 * Trang chính của module TOEIC.
 *
 * Giai đoạn hiện tại:
 * - Dashboard TOEIC độc lập.
 * - Hiển thị Listening Part 1-4.
 * - Hiển thị Reading Part 5-7.
 * - Có Quick Practice và Full Test.
 *
 * Chưa gắn logic thi vào từng Part ở file này.
 * Part 5 sẽ được triển khai ở bước tiếp theo.
 */
public class ToeicFragment extends Fragment {

    public ToeicFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_toeic,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(
                view,
                savedInstanceState
        );

        setupPartButtons(view);
        setupPracticeButtons(view);
    }

    private void setupPartButtons(
            @NonNull View view
    ) {

        View cardPart1 =
                view.findViewById(
                        R.id.cardToeicPart1
                );

        View cardPart2 =
                view.findViewById(
                        R.id.cardToeicPart2
                );

        View cardPart3 =
                view.findViewById(
                        R.id.cardToeicPart3
                );

        View cardPart4 =
                view.findViewById(
                        R.id.cardToeicPart4
                );

        View cardPart5 =
                view.findViewById(
                        R.id.cardToeicPart5
                );

        View cardPart6 =
                view.findViewById(
                        R.id.cardToeicPart6
                );

        View cardPart7 =
                view.findViewById(
                        R.id.cardToeicPart7
                );

        View.OnClickListener comingSoon =
                v -> Toast.makeText(
                        requireContext(),
                        "Phần này sẽ được hoàn thiện sau Part 5.",
                        Toast.LENGTH_SHORT
                ).show();

        if (cardPart1 != null) {
            cardPart1.setOnClickListener(
                    comingSoon
            );
        }

        if (cardPart2 != null) {
            cardPart2.setOnClickListener(
                    comingSoon
            );
        }

        if (cardPart3 != null) {
            cardPart3.setOnClickListener(
                    comingSoon
            );
        }

        if (cardPart4 != null) {
            cardPart4.setOnClickListener(
                    comingSoon
            );
        }

        if (cardPart6 != null) {
            cardPart6.setOnClickListener(
                    comingSoon
            );
        }

        if (cardPart7 != null) {
            cardPart7.setOnClickListener(
                    comingSoon
            );
        }

        if (cardPart5 != null) {

            cardPart5.setOnClickListener(
                    v -> Toast.makeText(
                            requireContext(),
                            "Part 5 đã sẵn sàng để triển khai ở bước tiếp theo.",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }

    private void setupPracticeButtons(
            @NonNull View view
    ) {

        View cardQuickPractice =
                view.findViewById(
                        R.id.cardToeicQuickPractice
                );

        View cardFullTest =
                view.findViewById(
                        R.id.cardToeicFullTest
                );

        if (cardQuickPractice != null) {

            cardQuickPractice.setOnClickListener(
                    v -> Toast.makeText(
                            requireContext(),
                            "Quick Practice sẽ dùng chung bộ câu TOEIC sau khi Part 5 hoàn thiện.",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }

        if (cardFullTest != null) {

            cardFullTest.setOnClickListener(
                    v -> Toast.makeText(
                            requireContext(),
                            "Full Test sẽ được mở sau khi hoàn thiện đủ các Part.",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }
}
