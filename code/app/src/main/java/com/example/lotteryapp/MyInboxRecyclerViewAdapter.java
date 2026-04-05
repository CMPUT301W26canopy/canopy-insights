package com.example.lotteryapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} that can display a {@link NotificationModel}.
 */
public class MyInboxRecyclerViewAdapter extends RecyclerView.Adapter<MyInboxRecyclerViewAdapter.ViewHolder> {

    private final List<NotificationModel> items;

    public MyInboxRecyclerViewAdapter(List<NotificationModel> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_inbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
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
                Context context = v.getContext();
                Intent intent = new Intent(context, EventActivity.class);
                intent.putExtra("EVENT_ID", item.getEventId());
                context.startActivity(intent);
            });
        } else {
            holder.viewEventButton.setVisibility(View.GONE);
            holder.viewEventButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
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
        }
    }
}
