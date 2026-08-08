package adu.nttu.englishai.admin.repositories;

import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static class DashboardStats {
        public int totalUsers;
        public int totalAdmins;
        public int totalVocabulary;
        public int totalGrammar;
        public int totalToeicTests;
        public int totalToeicQuestions;
    }

    public interface StatsCallback {
        void onSuccess(DashboardStats stats);
        void onFailure(Exception exception);
    }

    public void loadStats(StatsCallback callback) {
        DashboardStats stats = new DashboardStats();

        db.collection("users").get()
                .addOnSuccessListener(users -> {
                    stats.totalUsers = users.size();
                    int admins = 0;
                    for (var doc : users.getDocuments()) {
                        String role = doc.getString("role");
                        if (role != null && "admin".equalsIgnoreCase(role.trim())) admins++;
                    }
                    stats.totalAdmins = admins;

                    db.collection("vocabularies").get()
                            .addOnSuccessListener(vocab -> {
                                stats.totalVocabulary = vocab.size();

                                db.collection("sentenceExercises").get()
                                        .addOnSuccessListener(grammar -> {
                                            stats.totalGrammar = grammar.size();

                                            db.collection("toeicTests").get()
                                                    .addOnSuccessListener(tests -> {
                                                        stats.totalToeicTests = tests.size();

                                                        db.collection("toeicQuestions").get()
                                                                .addOnSuccessListener(questions -> {
                                                                    stats.totalToeicQuestions = questions.size();
                                                                    callback.onSuccess(stats);
                                                                })
                                                                .addOnFailureListener(callback::onFailure);
                                                    })
                                                    .addOnFailureListener(callback::onFailure);
                                        })
                                        .addOnFailureListener(callback::onFailure);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
}