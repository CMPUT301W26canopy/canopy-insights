package com.example.lotteryapp;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link NotificationModel}.
 * It manages the display of individual notification items in the inbox list,
 * including event navigation and conditional action button labels.
 */
public class MyInboxRecyclerViewAdapter extends RecyclerView.Adapter<MyInboxRecyclerViewAdapter.ViewHolder> {

    private final List<NotificationModel> mValues;

    /**
     * Constructs a new MyInboxRecyclerViewAdapter.
     * @param items The list of {@link NotificationModel} objects to display.
     */
    public MyInboxRecyclerViewAdapter(List<NotificationModel> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_inbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        NotificationModel item = mValues.get(position);
        holder.mItem = item;
        holder.mIdView.setText(getNotificationTitle(item));
        holder.mContentView.setText(item.getMessage());

        if (item.getEventId() != null && !item.getEventId().isEmpty()) {
            holder.mViewEventBtn.setVisibility(View.VISIBLE);
            holder.mViewEventBtn.setText(item.getMessage() != null && item.getMessage().startsWith("Invitation:")
                    ? "View / Respond"
                    : "View Event");
            holder.mViewEventBtn.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, EventActivity.class);
                intent.putExtra("EVENT_ID", item.getEventId());
                context.startActivity(intent);
            });
        } else {
            holder.mViewEventBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    /**
     * Categorizes a notification to provide a readable title label based on its content or sender.
     * @param item The notification model to analyze.
     * @return A string title (e.g., "Admin", "Invitation", "App", or the sender's ID).
     */
    private String getNotificationTitle(NotificationModel item) {
        String message = item.getMessage() != null ? item.getMessage() : "";
        String sender = item.getSenderAccountID() != null ? item.getSenderAccountID() : "";

        if (message.startsWith("Admin update:")) {
            return "Admin";
        }
        if (message.startsWith("Invitation:")) {
            return "Invitation";
        }
        if (sender.equalsIgnoreCase("SYSTEM_DEFAULT") || sender.equalsIgnoreCase("system")) {
            return "App";
        }
        return sender.isEmpty() ? "Notification" : sender;
    }

    /**
     * View holder for individual notification list items.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public final Button mViewEventBtn;
        public NotificationModel mItem;

        /**
         * Constructs a new ViewHolder.
         * @param view The view associated with the list item.
         */
        public ViewHolder(View view) {
            super(view);
            mIdView = view.findViewById(R.id.item_number);
            mContentView = view.findViewById(R.id.content);
            mViewEventBtn = view.findViewById(R.id.btn_view_event);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
