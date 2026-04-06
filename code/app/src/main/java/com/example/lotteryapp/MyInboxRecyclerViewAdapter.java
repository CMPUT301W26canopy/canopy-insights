package com.example.lotteryapp;

<<<<<<< HEAD
=======
import androidx.recyclerview.widget.RecyclerView;

>>>>>>> main
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

<<<<<<< HEAD
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
=======
import java.util.List;
>>>>>>> main

/**
 * {@link RecyclerView.Adapter} that can display a {@link NotificationModel}.
 */
public class MyInboxRecyclerViewAdapter extends RecyclerView.Adapter<MyInboxRecyclerViewAdapter.ViewHolder> {

<<<<<<< HEAD
    private final List<NotificationModel> items;

    public MyInboxRecyclerViewAdapter(List<NotificationModel> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
=======
    private final List<NotificationModel> mValues;

    public MyInboxRecyclerViewAdapter(List<NotificationModel> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
>>>>>>> main
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_inbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
<<<<<<< HEAD
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel item = items.get(position);

        String message = item.getMessage();
        holder.titleView.setText(message == null || message.trim().isEmpty()
                ? "Notification"
                : message);

        holder.contentView.setText(item.getEventId() != null && !item.getEventId().isEmpty()
                ? "Related to one of your events."
                : "General update.");

        String formattedTimestamp = formatTimestamp(item.getTimestamp());
        if (formattedTimestamp.isEmpty()) {
            holder.timestampView.setVisibility(View.GONE);
        } else {
            holder.timestampView.setVisibility(View.VISIBLE);
            holder.timestampView.setText(formattedTimestamp);
        }

        if (item.getEventId() != null && !item.getEventId().isEmpty()) {
            holder.viewEventButton.setVisibility(View.VISIBLE);
            holder.viewEventButton.setOnClickListener(v -> {
=======
    public void onBindViewHolder(final ViewHolder holder, int position) {
        NotificationModel item = mValues.get(position);
        holder.mItem = item;
        holder.mIdView.setText(item.getSenderAccountID());
        holder.mContentView.setText(item.getMessage());

        if (item.getEventId() != null && !item.getEventId().isEmpty()) {
            holder.mViewEventBtn.setVisibility(View.VISIBLE);
            holder.mViewEventBtn.setOnClickListener(v -> {
>>>>>>> main
                Context context = v.getContext();
                Intent intent = new Intent(context, EventActivity.class);
                intent.putExtra("EVENT_ID", item.getEventId());
                context.startActivity(intent);
            });
        } else {
<<<<<<< HEAD
            holder.viewEventButton.setVisibility(View.GONE);
            holder.viewEventButton.setOnClickListener(null);
=======
            holder.mViewEventBtn.setVisibility(View.GONE);
>>>>>>> main
        }
    }

    @Override
    public int getItemCount() {
<<<<<<< HEAD
        return items.size();
    }

    private String formatTimestamp(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "";
        }

        if (rawValue.matches("\\d{10}")) {
            return rawValue.substring(0, 2) + "-" + rawValue.substring(2, 4)
                    + "  " + rawValue.substring(4, 6) + ":" + rawValue.substring(6, 8);
        }

        String[] inputPatterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "MM-dd-yyyy"
        };

        String[] outputPatterns = {
                "MMM dd, yyyy  h:mm a",
                "MMM dd, yyyy",
                "MMM dd, yyyy"
        };

        for (int i = 0; i < inputPatterns.length; i++) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(inputPatterns[i], Locale.getDefault());
                inputFormat.setLenient(false);
                Date parsed = inputFormat.parse(rawValue);
                if (parsed != null) {
                    return new SimpleDateFormat(outputPatterns[i], Locale.getDefault()).format(parsed);
                }
            } catch (ParseException ignored) {
            }
        }

        return rawValue;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleView;
        final TextView contentView;
        final TextView timestampView;
        final Button viewEventButton;

        ViewHolder(@NonNull View view) {
            super(view);
            titleView = view.findViewById(R.id.item_number);
            contentView = view.findViewById(R.id.content);
            timestampView = view.findViewById(R.id.timestamp);
            viewEventButton = view.findViewById(R.id.btn_view_event);
=======
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public final Button mViewEventBtn;
        public NotificationModel mItem;

        public ViewHolder(View view) {
            super(view);
            mIdView = view.findViewById(R.id.item_number);
            mContentView = view.findViewById(R.id.content);
            mViewEventBtn = view.findViewById(R.id.btn_view_event);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
>>>>>>> main
        }
    }
}
