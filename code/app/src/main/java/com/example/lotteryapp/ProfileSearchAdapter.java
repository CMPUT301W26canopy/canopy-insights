package com.example.lotteryapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        holder.tvName.setText(profile.getName());
        holder.tvEmail.setText(profile.getEmail());
        
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

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_profile_name);
            tvEmail = view.findViewById(R.id.tv_profile_email);
            checkBox = view.findViewById(R.id.cb_invite);
        }
    }
}
