package adu.nttu.englishai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import adu.nttu.englishai.R;
import adu.nttu.englishai.adapters.LeaderboardAdapter;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardAdapter.UserItem> userList;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        firestore = FirebaseFirestore.getInstance();

        // Xử lý nút Back
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Cài đặt danh sách (RecyclerView)
        recyclerView = findViewById(R.id.recyclerViewLeaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new LeaderboardAdapter(userList);
        recyclerView.setAdapter(adapter);

        // Tải dữ liệu
        loadLeaderboardData();
    }

    private void loadLeaderboardData() {
        Toast.makeText(this, "Đang tải Bảng xếp hạng...", Toast.LENGTH_SHORT).show();

        firestore.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            userList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                String name = document.getString("name");
                if (name == null || name.isEmpty()) name = document.getString("fullName");

                String avatarUrl = document.getString("avatarUrl");

                // Lấy điểm, nếu không có thì mặc định là 0
                Long scoreObj = document.getLong("score");
                int score = (scoreObj != null) ? scoreObj.intValue() : 0;

                // Để bảng xếp hạng hấp dẫn, nếu ai chưa có điểm thì mình cho random một chút XP ảo (Dành cho bản Demo)
                if (score == 0) {
                    score = (int) (Math.random() * 50) + 10;
                }

                userList.add(new LeaderboardAdapter.UserItem(name, avatarUrl, score));
            }

            // Sắp xếp danh sách giảm dần theo XP (Đua TOP)
            Collections.sort(userList, (u1, u2) -> Integer.compare(u2.score, u1.score));

            // Cập nhật giao diện
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}