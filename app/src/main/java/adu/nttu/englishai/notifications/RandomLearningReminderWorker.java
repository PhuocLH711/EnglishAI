package adu.nttu.englishai.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Random;

import adu.nttu.englishai.R;
import adu.nttu.englishai.activities.MainActivity;

public class RandomLearningReminderWorker
        extends Worker {

    // =========================================================
    // CHANNEL
    // =========================================================
    private static final String CHANNEL_ID =
            "englishai_learning_reminder";

    // Cố định ID để tránh notification cũ bị chất đống
    private static final int NOTIFICATION_ID =
            1001;

    public RandomLearningReminderWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(
                context,
                params
        );
    }

    // =========================================================
    // WORK
    // =========================================================
    @NonNull
    @Override
    public Result doWork() {

        Context context =
                getApplicationContext();

        createNotificationChannel(
                context
        );

        // =====================================================
        // TIÊU ĐỀ NGẪU NHIÊN
        // =====================================================
        String[] titles = {

                "EnglishAI nhớ bạn rồi 👀",

                "Đến giờ học một chút rồi 📚",

                "Streak đang chờ bạn 🔥",

                "Một chút tiếng Anh nhé? 🇬🇧",

                "EnglishAI gọi bạn nè 👋",

                "Bạn ơi, học bài thôi 😎",

                "Cùng học EnglishAI nhé ✨"
        };

        // =====================================================
        // NỘI DUNG NGẪU NHIÊN
        // =====================================================
        String[] messages = {

                "Hôm nay bạn đã học tiếng Anh chưa? 👀",

                "5 phút thôi cũng được, vào học nha 📚",

                "Một từ mới hôm nay cũng là một bước tiến rồi ✨",

                "Vào làm thử vài câu TOEIC nào 😎",

                "EnglishAI có bài đang chờ bạn nè 📖",

                "Hôm nay thử học 10 từ mới nhé 🚀",

                "Đừng để streak của bạn về 0 nha 🔥",

                "Dành vài phút cho tiếng Anh hôm nay nhé 💙",

                "Một chút tiếng Anh trước khi kết thúc ngày nhé 🌙",

                "Học một câu thôi cũng được. Quan trọng là duy trì mỗi ngày 💪",

                "Bạn đã sẵn sàng cho một bài học mới chưa? 🎯",

                "Mỗi ngày tiến bộ một chút cùng EnglishAI nhé 📚"
        };

        Random random =
                new Random();

        String title =
                titles[
                        random.nextInt(
                                titles.length
                        )
                        ];

        String message =
                messages[
                        random.nextInt(
                                messages.length
                        )
                        ];

        // =====================================================
        // BẤM NOTIFICATION → MỞ APP
        // =====================================================
        Intent intent =
                new Intent(
                        context,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        1001,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        // =====================================================
        // TẠO NOTIFICATION
        // =====================================================
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CHANNEL_ID
                )

                        // =================================================
                        // ICON ENGLISHAI BÊN TRÁI
                        //
                        // Android notification small icon bắt buộc
                        // là icon đơn sắc.
                        //
                        // Không setLargeIcon để KHÔNG có ảnh bên phải.
                        // =================================================
                        .setSmallIcon(
                                R.drawable.ic_stat_englishai
                        )

                        .setContentTitle(
                                title
                        )

                        .setContentText(
                                message
                        )

                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(
                                                message
                                        )
                        )

                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT
                        )

                        .setCategory(
                                NotificationCompat.CATEGORY_REMINDER
                        )

                        .setAutoCancel(
                                true
                        )

                        .setOnlyAlertOnce(
                                true
                        )

                        .setContentIntent(
                                pendingIntent
                        );

        // =====================================================
        // ANDROID 13+ KIỂM TRA QUYỀN
        // =====================================================
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                // Chưa có quyền
                // vẫn tạo lịch cho lần kế tiếp
                scheduleNext(
                        context
                );

                return Result.success();
            }
        }

        // =====================================================
        // HIỂN THỊ NOTIFICATION
        // =====================================================
        NotificationManagerCompat
                .from(
                        context
                )
                .notify(
                        NOTIFICATION_ID,
                        builder.build()
                );

        // =====================================================
        // SAU KHI GỬI XONG
        // RANDOM TIẾP 1 / 2 / 3 / 5 NGÀY
        // =====================================================
        scheduleNext(
                context
        );

        return Result.success();
    }

    // =========================================================
    // TẠO NOTIFICATION CHANNEL
    // =========================================================
    private void createNotificationChannel(
            Context context
    ) {

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Nhắc nhở học EnglishAI",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );

            channel.setDescription(
                    "Nhắc bạn quay lại EnglishAI để duy trì việc học tiếng Anh."
            );

            NotificationManager manager =
                    context.getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    // =========================================================
    // LÊN LỊCH CHO LẦN SAU
    // =========================================================
    private void scheduleNext(
            Context context
    ) {

        RandomReminderScheduler
                .scheduleNextReminder(
                        context
                );
    }
}