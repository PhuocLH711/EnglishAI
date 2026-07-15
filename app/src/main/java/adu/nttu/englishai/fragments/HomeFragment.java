package adu.nttu.englishai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import adu.nttu.englishai.R;
// Nếu file LoginActivity của bạn nằm ở gói activities thì import vào, ví dụ:
// import adu.nttu.englishai.activities.LoginActivity;

public class HomeFragment extends Fragment {

    private Button btnStartDaily, btnLogout;
    private MaterialCardView cardStage1, cardStage2, cardStage3;

    public HomeFragment() {
        // Constructor rỗng bắt buộc cho Fragment
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Ánh xạ toàn bộ ID từ file XML mới
        btnStartDaily = view.findViewById(R.id.btnStartDaily);
        cardStage1 = view.findViewById(R.id.cardStage1);
        cardStage2 = view.findViewById(R.id.cardStage2);
        cardStage3 = view.findViewById(R.id.cardStage3);

        // 2. Nút "Học ngay 🚀" & Chặng 3 -> Mở thẳng màn chơi Trắc nghiệm (Quiz)
        View.OnClickListener openQuiz = v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new QuizFragment())
                    .addToBackStack(null)
                    .commit();
        };
        if (btnStartDaily != null) btnStartDaily.setOnClickListener(openQuiz);
        if (cardStage3 != null) cardStage3.setOnClickListener(openQuiz);

        // 3. Chặng 1: Từ vựng -> Tự động chuyển sáng Tab Từ vựng bên dưới BottomNav
        if (cardStage1 != null) {
            cardStage1.setOnClickListener(v -> {
                BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_vocabulary); // Tự động nhảy tab
                } else {
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new VocabularyFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // 4. Chặng 2: Luyện phát âm -> Mở thẳng màn Luyện nói (Speaking)
        if (cardStage2 != null) {
            cardStage2.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SpeakingFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // 5. Nút Đăng xuất tài khoản
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(requireContext(), "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show();

                // Đóng Activity hiện tại để quay về màn hình Đăng nhập / Splash
                requireActivity().finish();

                // (Tùy chọn) Nếu muốn mở trang LoginActivity cụ thể thì dùng 2 dòng dưới:
                // Intent intent = new Intent(requireActivity(), LoginActivity.class);
                // startActivity(intent);
            });
        }

        return view;
    }
}