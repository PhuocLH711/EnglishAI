package adu.nttu.englishai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.activities.ToeicHomeActivity;
import adu.nttu.englishai.activities.ToeicPracticeActivity;
import adu.nttu.englishai.models.ToeicTest;
import adu.nttu.englishai.repositories.ToeicProgressRepository;
import adu.nttu.englishai.repositories.ToeicRepository;

public class ToeicFragment extends Fragment {

    private ToeicRepository repository;
    private ToeicProgressRepository progressRepository;

    private final List<ToeicTest> tests =
            new ArrayList<>();

    private boolean loadingTests = false;

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

        repository =
                new ToeicRepository();

        progressRepository =
                new ToeicProgressRepository();

        setupPartButtons(
                view
        );

        setupPracticeButtons(
                view
        );

        loadTests(
                null
        );
    }

    private void setupPartButtons(
            @NonNull View view
    ) {

        bindPart(
                view,
                R.id.cardToeicPart1,
                1
        );

        bindPart(
                view,
                R.id.cardToeicPart2,
                2
        );

        bindPart(
                view,
                R.id.cardToeicPart3,
                3
        );

        bindPart(
                view,
                R.id.cardToeicPart4,
                4
        );

        bindPart(
                view,
                R.id.cardToeicPart5,
                5
        );

        bindPart(
                view,
                R.id.cardToeicPart6,
                6
        );

        bindPart(
                view,
                R.id.cardToeicPart7,
                7
        );
    }

    private void bindPart(
            View root,
            int viewId,
            int part
    ) {

        View card =
                root.findViewById(
                        viewId
                );

        if (card != null) {

            card.setOnClickListener(
                    view ->
                            showTestPicker(
                                    part,
                                    ToeicHomeActivity.MODE_PRACTICE
                            )
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
                    clicked ->
                            startActivity(
                                    new Intent(
                                            requireContext(),
                                            ToeicHomeActivity.class
                                    )
                            )
            );
        }

        if (cardFullTest != null) {

            cardFullTest.setOnClickListener(
                    clicked ->
                            showTestPicker(
                                    0,
                                    ToeicHomeActivity.MODE_MOCK
                            )
            );
        }
    }

    private void loadTests(
            @Nullable Runnable afterLoad
    ) {

        if (loadingTests) {
            return;
        }

        loadingTests = true;

        repository.getAllTests(
                new ToeicRepository.TestsCallback() {

                    @Override
                    public void onSuccess(
                            List<ToeicTest> loaded
                    ) {

                        loadingTests = false;

                        tests.clear();

                        if (loaded != null) {
                            tests.addAll(
                                    loaded
                            );
                        }

                        if (afterLoad != null) {
                            afterLoad.run();
                        }
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        loadingTests = false;

                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(
                                requireContext(),
                                "Không tải được danh sách đề TOEIC.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showTestPicker(
            int preferredPart,
            String mode
    ) {

        if (tests.isEmpty()) {

            loadTests(
                    () ->
                            showTestPicker(
                                    preferredPart,
                                    mode
                            )
            );

            return;
        }

        String[] labels =
                new String[
                        tests.size()
                        ];

        for (int i = 0;
             i < tests.size();
             i++) {

            ToeicTest test =
                    tests.get(i);

            labels[i] =
                    test.getTitle();
        }

        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle(
                        preferredPart == 0
                                ? "Chọn bộ đề thi thử"
                                : "Chọn bộ đề • Part "
                                  + preferredPart
                )
                .setItems(
                        labels,
                        (dialog, which) -> {

                            ToeicTest selected =
                                    tests.get(
                                            which
                                    );

                            showSelectedTestProgress(
                                    selected,
                                    preferredPart,
                                    mode
                            );
                        }
                )
                .setNegativeButton(
                        "Đóng",
                        null
                )
                .show();
    }

    private void showSelectedTestProgress(
            ToeicTest test,
            int preferredPart,
            String mode
    ) {

        progressRepository.getProgress(
                test.getId(),
                new ToeicProgressRepository.ProgressCallback() {

                    @Override
                    public void onSuccess(
                            ToeicProgressRepository.TestProgress progress
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        StringBuilder message =
                                new StringBuilder();

                        for (int part = 1;
                             part <= 7;
                             part++) {

                            int percent =
                                    progress.getPartPercent(
                                            part
                                    );

                            message.append(
                                    "Part "
                                            + part
                                            + ": "
                            );

                            if (percent >= 100) {

                                message.append(
                                        "✅ 100%"
                                );

                            } else {

                                message.append(
                                        percent
                                                + "%"
                                );
                            }

                            if (part < 7) {
                                message.append('\n');
                            }
                        }

                        message.append(
                                "\n\nTổng tiến độ: "
                                        + progress.getOverallPercent()
                                        + "%"
                        );

                        if (progress.isCompleted()) {

                            message.append(
                                    "\n✅ ĐÃ HOÀN TẤT"
                            );
                        }

                        String positiveText;

                        if (ToeicHomeActivity.MODE_MOCK
                                .equals(mode)) {

                            positiveText =
                                    "Bắt đầu thi";

                        } else {

                            positiveText =
                                    preferredPart == 0
                                            ? "Luyện toàn bộ"
                                            : "Mở Part "
                                              + preferredPart;
                        }

                        new AlertDialog.Builder(
                                requireContext()
                        )
                                .setTitle(
                                        test.getTitle()
                                )
                                .setMessage(
                                        message.toString()
                                )
                                .setPositiveButton(
                                        positiveText,
                                        (dialog, which) ->
                                                openTest(
                                                        test,
                                                        mode,
                                                        preferredPart
                                                )
                                )
                                .setNeutralButton(
                                        "Chọn Part khác",
                                        (dialog, which) ->
                                                openTestHome()
                                )
                                .setNegativeButton(
                                        "Hủy",
                                        null
                                )
                                .show();
                    }

                    @Override
                    public void onFailure(
                            Exception exception
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        openTest(
                                test,
                                mode,
                                preferredPart
                        );
                    }
                }
        );
    }

    private void openTestHome() {

        startActivity(
                new Intent(
                        requireContext(),
                        ToeicHomeActivity.class
                )
        );
    }

    private void openTest(
            ToeicTest test,
            String mode,
            int part
    ) {

        Intent intent =
                new Intent(
                        requireContext(),
                        ToeicPracticeActivity.class
                );

        intent.putExtra(
                ToeicHomeActivity.EXTRA_TEST_ID,
                test.getId()
        );

        intent.putExtra(
                ToeicHomeActivity.EXTRA_TEST_TITLE,
                test.getTitle()
        );

        intent.putExtra(
                ToeicHomeActivity.EXTRA_DURATION,
                test.getDurationMinutes()
        );

        intent.putExtra(
                ToeicHomeActivity.EXTRA_MODE,
                mode
        );

        intent.putExtra(
                ToeicHomeActivity.EXTRA_PART_FILTER,
                part
        );

        startActivity(
                intent
        );
    }
}