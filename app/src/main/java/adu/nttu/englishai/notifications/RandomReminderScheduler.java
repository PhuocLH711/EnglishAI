package adu.nttu.englishai.notifications;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RandomReminderScheduler {

    private static final String WORK_NAME =
            "englishai_random_learning_reminder";

    public static void scheduleNextReminder(
            Context context
    ) {

        // =========================================================
        // CÁC KHOẢNG NGÀY CÓ THỂ XUẤT HIỆN THÔNG BÁO
        // =========================================================
        int[] possibleDays = {
                1,
                2,
                3,
                5
        };

        Random random = new Random();

        // Random một vị trí trong mảng
        int randomIndex =
                random.nextInt(
                        possibleDays.length
                );

        // Ví dụ kết quả có thể là:
        // 1 ngày / 2 ngày / 3 ngày / 5 ngày
        int randomDays =
                possibleDays[randomIndex];

        // =========================================================
        // TẠO LỊCH THÔNG BÁO TIẾP THEO
        // =========================================================
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(
                        RandomLearningReminderWorker.class
                )
                        .setInitialDelay(
                                20,
                                TimeUnit.DAYS
                        )
                        .build();

        // =========================================================
        // CHỈ GIỮ 1 LỊCH ĐANG CHỜ
        // =========================================================
        WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        request
                );
    }
}