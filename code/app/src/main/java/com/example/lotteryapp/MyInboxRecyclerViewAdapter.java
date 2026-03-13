package com.example.lotteryapp;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.lotteryapp.databinding.FragmentInboxBinding;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Purpose of this class is map notificationModel objects to the view of an item
 *
 */

// This class was heavily made by AI, gemini, as a result of fixing error
// On 03-13-2026, no prompts used.
public class MyInboxRecyclerViewAdapter extends RecyclerView.Adapter<MyInboxRecyclerViewAdapter.ViewHolder> {

    private final List<NotificationModel> mValues;
    // Cache to store accountID -> username so we don't query twice for the same person
    private final Map<String, String> usernameCache = new HashMap<>();

    public MyInboxRecyclerViewAdapter(List<NotificationModel> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentInboxBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        NotificationModel item = mValues.get(position);
        holder.mItem = item;
        holder.mContentView.setText(item.getMessage());

        String senderID = item.getSenderAccountID();

        // 1. Check if we already have the username in our local cache
        if (usernameCache.containsKey(senderID)) {
            holder.mIdView.setText(usernameCache.get(senderID));
        } else {
            // 2. Placeholder while we fetch the real name
            holder.mIdView.setText("Loading...");

            // 3. Query the 'accounts' collection for the sender's current username
            FirestoreHelper.getDb().collection("accounts")
                    .document(senderID)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                usernameCache.put(senderID, username);
                                // Only update the text if this holder hasn't been recycled for another item
                                if (holder.getBindingAdapterPosition() == position) {
                                    holder.mIdView.setText(username);
                                }
                            }
                        } else {
                            // Fallback if the user no longer exists
                            if (holder.getBindingAdapterPosition() == position) {
                                holder.mIdView.setText("Unknown User");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to ID if the network/database fails
                        if (holder.getBindingAdapterPosition() == position) {
                            holder.mIdView.setText(senderID);
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public NotificationModel mItem;

        public ViewHolder(FragmentInboxBinding binding) {
            super(binding.getRoot());
            mIdView = binding.itemNumber;
            mContentView = binding.content;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
