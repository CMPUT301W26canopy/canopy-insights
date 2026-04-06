package com.example.lotteryapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Displays profile search results for invites and organizer actions.
 */
public class ProfileSearchAdapter extends RecyclerView.Adapter<ProfileSearchAdapter.ViewHolder> {

    private List<ProfileModel> profiles = new ArrayList<>();
    private final Set<String> selectedProfileIds = new HashSet<>();

    public void setProfiles(List<ProfileModel> profiles) {
        this.profiles = profiles;
        notifyDataSetChanged();
    }

    public Set<String> getSelectedProfileIds() {
        return selectedProfileIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfileModel profile = profiles.get(position);
        String displayName = firstNonBlank(profile.getName(), profile.getUsername());
        holder.tvName.setText(displayName != null ? displayName : "Unnamed profile");
        holder.tvEmail.setText(firstNonBlank(profile.getEmail(), profile.getPhoneNumber()));
        bindProfileImage(holder.profileImageView, profile.getProfileImage());
        
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedProfileIds.contains(profile.getAccountID()));
        
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedProfileIds.add(profile.getAccountID());
            } else {
                selectedProfileIds.remove(profile.getAccountID());
            }
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        CheckBox checkBox;
        ImageView profileImageView;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_profile_name);
            tvEmail = view.findViewById(R.id.tv_profile_email);
            checkBox = view.findViewById(R.id.cb_invite);
            profileImageView = view.findViewById(R.id.iv_profile_pic);
        }
    }

    private void bindProfileImage(ImageView imageView, String profileImage) {
        if (profileImage != null && !profileImage.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(profileImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        imageView.setImageResource(R.drawable.ic_person);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }
}
