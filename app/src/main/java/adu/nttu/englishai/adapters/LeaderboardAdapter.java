package adu.nttu.englishai.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import adu.nttu.englishai.R;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    // Lớp Model lưu trữ dữ liệu người chơi ngay bên trong Adapter cho gọn
    public static class UserItem {
        public String name;
        public String avatarUrl;
        public int score;

        public UserItem(String name, String avatarUrl, int score) {
            this.name = name;
            this.avatarUrl = avatarUrl;
            this.score = score;
        }
    }

    private List<UserItem> userList;

    public LeaderboardAdapter(List<UserItem> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserItem user = userList.get(position);

        // Cài đặt thông tin cơ bản
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvName.setText(user.name != null && !user.name.isEmpty() ? user.name : "Học viên Ẩn danh");
        holder.tvXP.setText(user.score + " XP");

        // TÔ MÀU CHO TOP 1, 2, 3
        if (position == 0) {
            holder.tvRank.setTextColor(Color.parseColor("#FFD700")); // Vàng Gold
            holder.tvRank.setTextSize(22f);
            holder.tvRank.setText("👑 1");
        } else if (position == 1) {
            holder.tvRank.setTextColor(Color.parseColor("#C0C0C0")); // Bạc Silver
            holder.tvRank.setTextSize(20f);
        } else if (position == 2) {
            holder.tvRank.setTextColor(Color.parseColor("#CD7F32")); // Đồng Bronze
            holder.tvRank.setTextSize(20f);
        } else {
            holder.tvRank.setTextColor(Color.parseColor("#757575")); // Xám xịt cho phần còn lại
            holder.tvRank.setTextSize(18f);
        }

        // TẢI ẢNH ĐẠI DIỆN (Hỗ trợ cả link thường và ảnh mã hóa Base64)
        if (user.avatarUrl != null && !user.avatarUrl.isEmpty()) {
            try {
                if (user.avatarUrl.length() > 500) { // Đây là chuỗi Base64
                    byte[] decodedString = Base64.decode(user.avatarUrl, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    Glide.with(holder.itemView.getContext()).load(decodedByte).circleCrop().into(holder.imgAvatar);
                } else { // Đây là link URL bình thường
                    Glide.with(holder.itemView.getContext()).load(user.avatarUrl).circleCrop().into(holder.imgAvatar);
                }
            } catch (Exception e) {
                holder.imgAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            holder.imgAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvXP;
        ImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvName = itemView.findViewById(R.id.tvName);
            tvXP = itemView.findViewById(R.id.tvXP);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}