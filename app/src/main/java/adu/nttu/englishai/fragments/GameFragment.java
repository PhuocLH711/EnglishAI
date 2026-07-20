package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import adu.nttu.englishai.R;

// =========================================================================
// GAME FRAGMENT: Màn hình Menu điều hướng tới các trò chơi ôn luyện
// =========================================================================
public class GameFragment extends Fragment {

    public GameFragment() {}

    // =========================================================================
    // HÀM TẠO GIAO DIỆN VÀ GÁN SỰ KIỆN ĐIỀU HƯỚNG
    // =========================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm bản thiết kế XML fragment_game thành đối tượng View thực tế
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        // Ánh xạ các nút bấm mở trò chơi Quiz và Flashcard
        Button btnOpenQuiz = view.findViewById(R.id.btnOpenQuiz);
        Button btnOpenFlashcard = view.findViewById(R.id.btnOpenFlashcard);

        // =========================================================================
        // 1. SỰ KIỆN MỞ TRÒ CHƠI TRẮC NGHIỆM (QUIZ FRAGMENT)
        // =========================================================================
        btnOpenQuiz.setOnClickListener(v -> {

           // QUẢN LÝ NGĂN XẾP FRAGMENT
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new QuizFragment())
                    .addToBackStack(null) // dòng này để cho phép Back lại menu
                    .commit(); // Lệnh thực thi chuyển đổi màn hình
        });

        // =========================================================================
        // 2. SỰ KIỆN MỞ TRÒ CHƠI LẬT THẺ
        // =========================================================================
        btnOpenFlashcard.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FlashcardFragment())
                    .addToBackStack(null) // Lưu vào Backstack để đảm bảo tính liên tục của luồng UX
                    .commit();
        });

        // =========================================================================
        // 3. SỰ KIỆN MỞ TRÒ CHƠI TÌM CẶP TỪ
        // =========================================================================
        View cardMemoryMatch = view.findViewById(R.id.cardMemoryMatch);
        View btnOpenMemoryMatch = view.findViewById(R.id.btnOpenMemoryMatch);

        /*
         * TỐI ƯU HÓA CODE & TRẢI NGHIỆM NGƯỜI DÙNG
         * Khi người dùng chạm vào nút bấm HAY chạm vào bất kỳ đâu trên khung thẻ rộng lớn bên ngoài,
         * ứng dụng đều kích hoạt chuyển sang màn hình MemoryMatchFragment.
         */
        View.OnClickListener openMemory = v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MemoryMatchFragment())
                    .addToBackStack(null)
                    .commit();
        };

        // Kiểm tra khác null trước khi gán sự kiện
        if (cardMemoryMatch != null) cardMemoryMatch.setOnClickListener(openMemory);
        if (btnOpenMemoryMatch != null) btnOpenMemoryMatch.setOnClickListener(openMemory);

        return view;
    }
}