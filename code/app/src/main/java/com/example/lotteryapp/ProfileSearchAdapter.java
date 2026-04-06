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

    /**
     * Sets the list of profiles to display.
     * @param profiles The list of profiles to display.
     */

    public void setProfiles(List<ProfileModel> profiles) {
        this.profiles = profiles;
        notifyDataSetChanged();
    }

    /**
     * Gets the set of selected profile IDs.
     * @return A set of selected profile account IDs.
     */
    public Set<String> getSelectedProfileIds() {
        return selectedProfileIds;
    }

    /**
     * Inflates the layout for a profile search item and creates a ViewHolder.
     * @param parent The parent view group.
     * @param viewType The type of view.
     * @return A new ViewHolder instance.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_search, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds profile data to the views in the ViewHolder.
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the list.
     */
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

    /**
     * Gets the number of items in the profile list.
     * @return The size of the profiles list.
     */
    @Override
    public int getItemCount() {
        return profiles.size();
    }

    /**
     * ViewHolder for profile search items, holding references to UI components.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        CheckBox checkBox;
        ImageView profileImageView;

        /**
         * Initializes UI components from the inflated view.
         * @param view The root view of the item layout.
         */
        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_profile_name);
            tvEmail = view.findViewById(R.id.tv_profile_email);
            checkBox = view.findViewById(R.id.cb_invite);
            profileImageView = view.findViewById(R.id.iv_profile_pic);
        }
    }

    /**
     * Decodes and binds a Base64 encoded image string to an ImageView, or sets a default if invalid.
     * @param imageView The ImageView to update.
     * @param profileImage The Base64 encoded string representing the profile image.
     */
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

    /**
     * Returns the first provided string that is not null or empty.
     * @param first The primary string to check.
     * @param second The fallback string to check.
     * @return The first non-blank string, or null if both are blank.
     */
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
