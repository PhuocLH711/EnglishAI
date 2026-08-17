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

public class RandomLearningReminderWorker extends Worker {

    private static final String CHANNEL_ID =
            "englishai_learning_reminder";

    public RandomLearningReminderWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context context =
                getApplicationContext();

        createNotificationChannel(context);

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

                "EnglishAI có chuyện muốn nói 👀"
        };

        // =====================================================
        // NỘI DUNG NGẪU NHIÊN
        // =====================================================
        String[] messages = {

                "Hôm nay bạn đã học tiếng Anh chưa? 👀",

                "EnglishAI nhớ bạn rồi đó 🥹",

                "5 phút thôi cũng được, vào học nha 📚",

                "Streak đang đợi bạn 🔥",

                "Một từ mới hôm nay cũng là tiến bộ rồi!",

                "Có người định bỏ học tiếng Anh hôm nay kìa 👀",

                "Não bạn đang chờ một chút English 🧠✨",

                "Vào làm thử một câu TOEIC nào 😎",

                "Bạn biến mất lâu quá rồi đó 👀",

                "EnglishAI có bài đang chờ bạn nè 📖",

                "Một ngày không học cũng được... nhưng 5 ngày thì hơi lâu nha 😭",

                "Hôm nay thử học 10 từ mới nhé 🚀",

                "Đừng để streak của bạn về 0 nha 🔥",

                "5 phút EnglishAI rồi quay lại lướt điện thoại cũng được 😌",

                "Tương lai của bạn đang cảm ơn 5 phút học hôm nay đó ✨",

                "Có bài TOEIC muốn thách đấu với bạn 😎",

                "Bạn còn nhớ EnglishAI không đó 🥹",

                "Một chút tiếng Anh trước khi kết thúc ngày nhé 🌙",

                "Học một câu thôi cũng được. Quan trọng là đừng bỏ cuộc 💪",

                "EnglishAI đang kiểm tra xem bạn còn chăm học không 👀"
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
        // BẤM THÔNG BÁO → MỞ APP
        // =====================================================
        Intent intent =
                new Intent(
                        context,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
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

                        // Chỉ còn 1 icon bên trái
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
                                        .bigText(message)
                        )

                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT
                        )

                        .setAutoCancel(true)

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

                scheduleNext(context);

                return Result.success();
            }
        }

        // =====================================================
        // HIỂN THỊ
        // =====================================================
        NotificationManagerCompat
                .from(context)
                .notify(
                        (int) System.currentTimeMillis(),
                        builder.build()
                );

        // =====================================================
        // LÊN LỊCH LẦN TIẾP THEO
        // =====================================================
        scheduleNext(context);

        return Result.success();
    }

    // =========================================================
    // CHANNEL
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
                    "Thông báo nhắc người dùng quay lại học tiếng Anh"
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
    // RANDOM LẦN TIẾP THEO
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