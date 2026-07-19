package adu.nttu.englishai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import adu.nttu.englishai.R;

// =========================================================================
// STAGE MISSION FRAGMENT: Màn hình Danh sách Nhiệm vụ trong Ải (Từ vựng, Quiz, Nói...)
// =========================================================================
public class StageMissionFragment extends Fragment {

    /*
     * CONSTRUCTOR RỖNG BẮT BUỘC (DEFAULT EMPTY CONSTRUCTOR):
     * Theo quy chuẩn kiến trúc Android, tất cả các Fragment BẮT BUỘC phải có constructor rỗng.
     * Khi hệ điều hành Android cần tái tạo lại Fragment (ví dụ khi xoay điện thoại hoặc
     * khi hệ thống thu hồi bộ nhớ tạm thời), nó sẽ gọi constructor rỗng này bằng reflection.
     * Nếu không có, ứng dụng sẽ bị crash ngay lập tức với lỗi InstantiationException!
     */
    public StageMissionFragment() {
        // Constructor rỗng bắt buộc
    }

    // =========================================================================
    // HÀM 1: BƠM GIAO DIỆN TỪ XML THÀNH VIEW OBJECT (ON CREATE VIEW)
    // =========================================================================
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Trách nhiệm duy nhất của onCreateView là bơm (inflate) bản thiết kế XML thành đối tượng View trong bộ nhớ
        return inflater.inflate(R.layout.fragment_stage_mission, container, false);
    }

    // =========================================================================
    // HÀM 2: CẤU HÌNH LOGIC & GÁN SỰ KIỆN SAU KHI VIEW ĐÃ TẠO XONG (ON VIEW CREATED)
    // =========================================================================
    /*
     * KỸ THUẬT CHUẨN ANDROID JETPACK:
     * Việc tìm kiếm ID (findViewById) và gán sự kiện (setOnClickListener) nên được viết ở onViewCreated
     * để đảm bảo cấu trúc giao diện đã được khởi tạo hoàn tất 100% và an toàn tuyệt đối,
     * tránh nguy cơ bị lỗi NullPointerException khi thao tác trên một View chưa sẵn sàng.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút Back quay lại màn hình Trang chủ (HomeFragment)
        View btnBackToHome = view.findViewById(R.id.btnBackToHome);
        if (btnBackToHome != null) {
            btnBackToHome.setOnClickListener(v -> {
                /*
                 * QUẢN LÝ NGĂN XẾP FRAGMENT (BACKSTACK POPPING):
                 * popBackStack(): Đóng màn hình Nhiệm vụ ải hiện tại và lùi lại màn hình Trang chủ cũ
                 * đang lưu trong ngăn xếp. Kỹ thuật này giúp chuyển trang mượt mà tức thì,
                 * không tốn tài nguyên tải lại dữ liệu Trang chủ từ Firebase!
                 */
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // =========================================================================
        // THÔNG BÁO KHI BẤM VÀO MÀN HÌNH (UX TACTILE FEEDBACK)
        // =========================================================================
        /*
         * KỸ THUẬT PHÒNG VỆ TRẢI NGHIỆM NGƯỜI DÙNG (UX Gamification & Placeholder Pattern):
         * Đối với các ải thử thách hoặc tính năng đang bị khóa (chưa đủ điều kiện mở),
         * nếu người dùng chạm vào màn hình mà không có phản hồi gì, họ sẽ tưởng app bị treo/đơ (frozen).
         * Việc gán sự kiện click lên toàn bộ khung nền view gốc (root view) để hiện Toast thông báo giải thích
         * giúp người học hiểu rõ quy luật của game ("Vui lòng hoàn thành bài trước để mở khóa"),
         * mang lại cảm giác ứng dụng phản hồi cực kỳ thông minh và mượt mà!
         */
        view.setOnClickListener(v -> {
            Toast.makeText(getContext(), "🔒 Vui lòng hoàn thành các bài học trước để mở khóa!", Toast.LENGTH_SHORT).show();
        });
    }
}