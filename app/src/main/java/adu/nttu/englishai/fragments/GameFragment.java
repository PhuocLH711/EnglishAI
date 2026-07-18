package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import adu.nttu.englishai.R;

// =========================================================================
// GAME FRAGMENT: Màn hình trung tâm (Menu) điều hướng tới các trò chơi ôn luyện
// =========================================================================
public class GameFragment extends Fragment {

    public GameFragment() {}

    // =========================================================================
    // HÀM TẠO GIAO DIỆN VÀ GÁN SỰ KIỆN ĐIỀU HƯỚNG (ON CREATE VIEW)
    // =========================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Bơm (inflate) bản thiết kế XML fragment_game thành đối tượng View thực tế
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        // Ánh xạ các nút bấm mở trò chơi Quiz và Flashcard
        Button btnOpenQuiz = view.findViewById(R.id.btnOpenQuiz);
        Button btnOpenFlashcard = view.findViewById(R.id.btnOpenFlashcard);

        // =========================================================================
        // 1. SỰ KIỆN MỞ TRÒ CHƠI TRẮC NGHIỆM (QUIZ FRAGMENT)
        // =========================================================================
        btnOpenQuiz.setOnClickListener(v -> {
            /*
             * KỸ THUẬT QUAN TRỌNG: QUẢN LÝ NGĂN XẾP FRAGMENT (BACKSTACK NAVIGATION)
             * - requireActivity().getSupportFragmentManager(): Lấy bộ quản lý Fragment của MainActivity
             * - replace(R.id.fragment_container, new QuizFragment()): Thay thế màn hình Menu hiện tại bằng màn hình Quiz
             * - addToBackStack(null): LỆNH CỰC KỲ QUAN TRỌNG! Lưu giao diện GameFragment này vào ngăn xếp bộ nhớ tạm.
             *   Nhờ đó, khi đang chơi Quiz mà người dùng bấm nút Back (Trở về) trên điện thoại hoặc trên màn hình,
             *   app sẽ không bị văng ra trang chủ hay thoát app, mà trượt mượt mà về lại đúng Menu Trò chơi này.
             */
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new QuizFragment())
                    .addToBackStack(null) // Thêm dòng này để cho phép Back lại menu
                    .commit(); // Lệnh thực thi chuyển đổi màn hình
        });

        // =========================================================================
        // 2. SỰ KIỆN MỞ TRÒ CHƠI LẬT THẺ (FLASHCARD FRAGMENT)
        // =========================================================================
        btnOpenFlashcard.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FlashcardFragment())
                    .addToBackStack(null) // Lưu vào Backstack để đảm bảo tính liên tục của luồng UX
                    .commit();
        });

        // =========================================================================
        // 3. SỰ KIỆN MỞ TRÒ CHƠI TÌM CẶP TỪ (MEMORY MATCH FRAGMENT)
        // =========================================================================
        // Ánh xạ thẻ Lật Thẻ Tìm Cặp từ fragment_game.xml
        // Lấy cả khung thẻ bao ngoài (cardMemoryMatch) và nút bấm bên trong (btnOpenMemoryMatch)
        View cardMemoryMatch = view.findViewById(R.id.cardMemoryMatch);
        View btnOpenMemoryMatch = view.findViewById(R.id.btnOpenMemoryMatch);

        /*
         * TỐI ƯU HÓA CODE & TRẢI NGHIỆM NGƯỜI DÙNG (DRY Principle + UX Enhancement):
         * Thay vì viết 2 đoạn code replace Fragment giống hệt nhau cho thẻ và cho nút,
         * ta khai báo chung một biến sự kiện View.OnClickListener (openMemory).
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

        // LẬP TRÌNH PHÒNG VỆ (Defensive Programming):
        // Kiểm tra khác null trước khi gán sự kiện để chống lỗi NullPointerException nếu XML bị thiếu ID
        if (cardMemoryMatch != null) cardMemoryMatch.setOnClickListener(openMemory);
        if (btnOpenMemoryMatch != null) btnOpenMemoryMatch.setOnClickListener(openMemory);

        return view;
    }
}