package adu.nttu.englishai.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import adu.nttu.englishai.R;
import adu.nttu.englishai.admin.repositories.AdminUserRepository.AdminUser;

/**
 * Adapter danh sách user trong Admin.
 */
public class AdminUserAdapter
        extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    public interface UserClickListener {
        void onUserClick(AdminUser user);
    }

    private final List<AdminUser> allUsers =
            new ArrayList<>();

    private final List<AdminUser> visibleUsers =
            new ArrayList<>();

    private final UserClickListener listener;

    public AdminUserAdapter(
            UserClickListener listener
    ) {
        this.listener = listener;
    }

    public void submitList(
            List<AdminUser> users
    ) {

        allUsers.clear();
        visibleUsers.clear();

        if (users != null) {
            allUsers.addAll(users);
            visibleUsers.addAll(users);
        }

        notifyDataSetChanged();
    }

    public void filter(
            String query
    ) {

        visibleUsers.clear();

        String normalized =
                query == null
                        ? ""
                        : query.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (normalized.isEmpty()) {

            visibleUsers.addAll(
                    allUsers
            );

        } else {

            for (AdminUser user
                    : allUsers) {

                String name =
                        user.getName()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                String email =
                        user.getEmail()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                String uid =
                        user.getUid() == null
                                ? ""
                                : user.getUid()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                if (name.contains(normalized)
                        || email.contains(normalized)
                        || uid.contains(normalized)) {

                    visibleUsers.add(user);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_admin_user,
                                parent,
                                false
                        );

        return new UserViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull UserViewHolder holder,
            int position
    ) {

        AdminUser user =
                visibleUsers.get(position);

        String displayName =
                user.getName().isEmpty()
                        ? "Người dùng EnglishAI"
                        : user.getName();

        holder.tvName.setText(
                displayName
        );

        holder.tvEmail.setText(
                user.getEmail().isEmpty()
                        ? "Chưa có email"
                        : user.getEmail()
        );

        boolean isAdmin =
                "admin".equalsIgnoreCase(
                        user.getRole()
                );

        holder.tvRole.setText(
                isAdmin
                        ? "ADMIN"
                        : "USER"
        );

        holder.tvStats.setText(
                user.getScore()
                        + " XP"
                        + "  •  🔥 "
                        + user.getStreak()
                        + " ngày"
        );

        holder.card.setOnClickListener(
                view ->
                        listener.onUserClick(
                                user
                        )
        );
    }

    @Override
    public int getItemCount() {
        return visibleUsers.size();
    }

    static class UserViewHolder
            extends RecyclerView.ViewHolder {

        MaterialCardView card;
        TextView tvName;
        TextView tvEmail;
        TextView tvRole;
        TextView tvStats;

        public UserViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            card =
                    itemView.findViewById(
                            R.id.cardAdminUser
                    );

            tvName =
                    itemView.findViewById(
                            R.id.tvAdminUserName
                    );

            tvEmail =
                    itemView.findViewById(
                            R.id.tvAdminUserEmail
                    );

            tvRole =
                    itemView.findViewById(
                            R.id.tvAdminUserRole
                    );

            tvStats =
                    itemView.findViewById(
                            R.id.tvAdminUserStats
                    );
        }
    }
}
