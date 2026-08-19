package adu.nttu.englishai.admin.repositories;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicInteger;

public class AdminDashboardRepository {

    private final FirebaseFirestore db =
            FirebaseFirestore.getInstance();

    public static class DashboardStats {

        public int totalUsers = 0;
        public int totalAdmins = 0;
        public int totalVocabulary = 0;
        public int totalGrammar = 0;
        public int totalToeicTests = 0;
        public int totalToeicQuestions = 0;
    }

    public interface StatsCallback {

        void onSuccess(DashboardStats stats);

        void onFailure(Exception exception);
    }

    public void loadStats(
            StatsCallback callback
    ) {

        DashboardStats stats =
                new DashboardStats();

        // Có 5 nhóm dữ liệu cần tải
        AtomicInteger completed =
                new AtomicInteger(0);

        AtomicInteger failed =
                new AtomicInteger(0);

        final Exception[] lastException =
                new Exception[1];

        // =========================================================
        // HÀM KIỂM TRA KHI TẤT CẢ REQUEST ĐÃ XONG
        // =========================================================
        Runnable checkComplete = () -> {

            if (completed.incrementAndGet() == 5) {

                // Nếu tất cả đều lỗi thì báo failure
                if (failed.get() == 5
                        && lastException[0] != null) {

                    callback.onFailure(
                            lastException[0]
                    );

                } else {

                    // Chỉ cần một phần tải được
                    // vẫn trả dữ liệu để dashboard hiển thị
                    callback.onSuccess(
                            stats
                    );
                }
            }
        };

        // =========================================================
        // USERS + ADMIN
        // =========================================================
        db.collection("users")
                .get()
                .addOnSuccessListener(users -> {

                    stats.totalUsers =
                            users.size();

                    int admins = 0;

                    for (var doc :
                            users.getDocuments()) {

                        String role =
                                doc.getString("role");

                        if (role != null
                                && "admin"
                                .equalsIgnoreCase(
                                        role.trim()
                                )) {

                            admins++;
                        }
                    }

                    stats.totalAdmins =
                            admins;

                    checkComplete.run();
                })
                .addOnFailureListener(exception -> {

                    failed.incrementAndGet();
                    lastException[0] = exception;

                    checkComplete.run();
                });

        // =========================================================
        // VOCABULARY
        // =========================================================
        db.collection("vocabularies")
                .get()
                .addOnSuccessListener(vocab -> {

                    stats.totalVocabulary =
                            vocab.size();

                    checkComplete.run();
                })
                .addOnFailureListener(exception -> {

                    failed.incrementAndGet();
                    lastException[0] = exception;

                    checkComplete.run();
                });

        // =========================================================
        // GRAMMAR
        // =========================================================
        db.collection("sentenceExercises")
                .get()
                .addOnSuccessListener(grammar -> {

                    stats.totalGrammar =
                            grammar.size();

                    checkComplete.run();
                })
                .addOnFailureListener(exception -> {

                    failed.incrementAndGet();
                    lastException[0] = exception;

                    checkComplete.run();
                });

        // =========================================================
        // TOEIC TESTS
        // =========================================================
        db.collection("toeicTests")
                .get()
                .addOnSuccessListener(tests -> {

                    stats.totalToeicTests =
                            tests.size();

                    checkComplete.run();
                })
                .addOnFailureListener(exception -> {

                    failed.incrementAndGet();
                    lastException[0] = exception;

                    checkComplete.run();
                });

        // =========================================================
        // TOEIC QUESTIONS
        // =========================================================
        db.collection("toeicQuestions")
                .get()
                .addOnSuccessListener(questions -> {

                    stats.totalToeicQuestions =
                            questions.size();

                    checkComplete.run();
                })
                .addOnFailureListener(exception -> {

                    failed.incrementAndGet();
                    lastException[0] = exception;

                    checkComplete.run();
                });
    }
}