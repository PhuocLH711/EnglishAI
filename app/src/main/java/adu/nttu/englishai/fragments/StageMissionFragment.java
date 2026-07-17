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

public class StageMissionFragment extends Fragment {

    public StageMissionFragment() {
        // Constructor rỗng bắt buộc
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stage_mission, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnBackToHome = view.findViewById(R.id.btnBackToHome);
        if (btnBackToHome != null) {
            btnBackToHome.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // THÔNG BÁO KHI BẤM VÀO MÀN HÌNH (Để app giống thật không bị đơ)
        view.setOnClickListener(v -> {
            Toast.makeText(getContext(), "🔒 Vui lòng hoàn thành các bài học trước để mở khóa!", Toast.LENGTH_SHORT).show();
        });
    }
}