package adu.nttu.englishai.notifications;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RandomReminderScheduler {

    // =========================================================
    // TÊN UNIQUE WORK
    // Chỉ có 1 chuỗi notification EnglishAI
    // =========================================================
    private static final String WORK_NAME =
            "englishai_random_learning_reminder";

    // =========================================================
    // CÁC KHOẢNG NGÀY RANDOM
    // =========================================================
    private static final int[] POSSIBLE_DAYS = {
            1,
            2,
            3,
            5
    };

    private RandomReminderScheduler() {
        // Không cho tạo object
    }

    // =========================================================
    // GỌI KHI MỞ APP
    //
    // KEEP:
    // Nếu đã có notification đang chờ
    // thì KHÔNG reset lại lịch.
    // =========================================================
    public static void scheduleInitialReminder(
            Context context
    ) {

        OneTimeWorkRequest request =
                createRandomRequest();

        WorkManager
                .getInstance(
                        context.getApplicationContext()
                )
                .enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        request
                );
    }

    // =========================================================
    // GỌI SAU KHI NOTIFICATION ĐÃ CHẠY
    //
    // APPEND_OR_REPLACE:
    // tạo lượt notification kế tiếp
    // sau lượt hiện tại.
    // =========================================================
    public static void scheduleNextReminder(
            Context context
    ) {

        OneTimeWorkRequest request =
                createRandomRequest();

        WorkManager
                .getInstance(
                        context.getApplicationContext()
                )
                .enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        request
                );
    }

    // =========================================================
    // TẠO REQUEST RANDOM
    // =========================================================
    private static OneTimeWorkRequest createRandomRequest() {

        Random random =
                new Random();

        int randomIndex =
                random.nextInt(
                        POSSIBLE_DAYS.length
                );

        int randomDays =
                POSSIBLE_DAYS[randomIndex];

        return new OneTimeWorkRequest.Builder(
                RandomLearningReminderWorker.class
        )
                .setInitialDelay(
                        randomDays,
                        TimeUnit.DAYS
                )
                .build();
    }

    // =========================================================
    // HỦY TOÀN BỘ LỊCH
    // Nếu sau này làm setting bật/tắt notification
    // =========================================================
    public static void cancelReminder(
            Context context
    ) {

        WorkManager
                .getInstance(
                        context.getApplicationContext()
                )
                .cancelUniqueWork(
                        WORK_NAME
                );
    }
}