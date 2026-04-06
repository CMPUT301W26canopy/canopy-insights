package com.example.lotteryapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Renders history rows and their action buttons for the history screen.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<HistoryItem> items;

    public HistoryAdapter(Context context, List<HistoryItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);

        holder.titleView.setText(buildTitle(item));
        holder.statusView.setText("Status : " + EventFlowRules.getHistoryStatusLabel(item.rawStatus));
        holder.statusView.setTextColor(getStatusColor(item.rawStatus));
        holder.ageGroupView.setText(valueOrDefault(item.ageGroup, "All Age Groups"));
        holder.locationView.setText(valueOrDefault(item.location, "Location TBA"));
        holder.priceView.setText(String.format(Locale.getDefault(), "$%d", (int) Math.round(item.price)));
        holder.statsView.setText(String.format(
                Locale.getDefault(),
                "Total: %d | Waiting List : %d",
                item.totalSpots,
                item.waitingCount
        ));
        holder.descriptionView.setText(valueOrDefault(item.description, "Event description."));
        bindPoster(holder.posterView, item.posterBase64);

        boolean canOpenEvent = item.eventId != null && !item.eventId.trim().isEmpty();
        holder.viewButton.setVisibility(canOpenEvent ? View.VISIBLE : View.GONE);
        holder.menuButton.setOnClickListener(v -> showMenu(v, item.eventId));

        if (canOpenEvent) {
            holder.viewButton.setOnClickListener(v -> openEvent(item.eventId));
        } else {
            holder.viewButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String buildTitle(HistoryItem item) {
        String name = valueOrDefault(item.eventName, "Untitled Event");
        String date = valueOrDefault(item.eventDate, "");
        return date.isEmpty() ? name : name + " | " + date;
    }

    private void showMenu(View anchor, String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(context, anchor);
        popupMenu.getMenu().add("View Event");
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            openEvent(eventId);
            return true;
        });
        popupMenu.show();
    }

    private void openEvent(String eventId) {
        Intent intent = new Intent(context, EventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        context.startActivity(intent);
    }

    private void bindPoster(ImageView imageView, String posterBase64) {
        if (posterBase64 != null && !posterBase64.trim().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(posterBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    imageView.setAlpha(1f);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        imageView.setAlpha(0.32f);
        imageView.setBackgroundColor(Color.parseColor("#F4EEF8"));
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.drawable.ic_launcher_foreground);
    }

    private int getStatusColor(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return Color.parseColor("#40C66E");
        }

        switch (rawStatus.trim().toLowerCase(Locale.getDefault())) {
            case "waiting":
                return Color.parseColor("#C58A18");
            case "selected":
                return Color.parseColor("#6B5FA6");
            case "accepted":
            case "registered":
            case "attended":
                return Color.parseColor("#3FA96A");
            case "declined":
            case "completed":
            case "cancelled":
                return Color.parseColor("#C96A58");
            default:
                return Color.parseColor("#6B5FA6");
        }
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleView;
        final TextView statusView;
        final ImageView posterView;
        final TextView ageGroupView;
        final TextView locationView;
        final TextView priceView;
        final TextView statsView;
        final TextView descriptionView;
        final Button viewButton;
        final ImageButton menuButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.tvHistoryTitle);
            statusView = itemView.findViewById(R.id.tvHistoryStatus);
            posterView = itemView.findViewById(R.id.ivHistoryImage);
            ageGroupView = itemView.findViewById(R.id.tvHistoryAgeGroup);
            locationView = itemView.findViewById(R.id.tvHistoryLocation);
            priceView = itemView.findViewById(R.id.tvHistoryPrice);
            statsView = itemView.findViewById(R.id.tvHistoryStats);
            descriptionView = itemView.findViewById(R.id.tvHistoryDescription);
            viewButton = itemView.findViewById(R.id.btnHistoryView);
            menuButton = itemView.findViewById(R.id.btnHistoryMenu);
        }
    }

    public static class HistoryItem {
        public String eventId;
        public String eventName;
        public String eventDate;
        public String rawStatus;
        public String ageGroup;
        public String location;
        public String description;
        public String posterBase64;
        public double price;
        public int totalSpots;
        public int waitingCount;
        public long sortTimeMs;
    }
}
