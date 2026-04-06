package com.example.lotteryapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Binds event cards for the main public event feed.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    /**
     * Interface definition for a callback to be invoked when an event card's action is clicked.
     */
    public interface OnEventClickListener {
        /**
         * Called when the view button on an event card is clicked.
         * @param event The {@link EventModel} associated with the clicked item.
         */
        void onViewClick(EventModel event);
    }

    private List<EventModel> events;
    private final Context context;
    private final OnEventClickListener listener;

    /**
     * Constructs a new EventAdapter.
     * @param context The current context.
     * @param events The initial list of events to display.
     * @param listener The listener to handle event click actions.
     */
    public EventAdapter(Context context, List<EventModel> events, OnEventClickListener listener) {
        this.context = context;
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventModel event = events.get(position);

        holder.tvName.setText(safe(event.getName(), "Untitled Event") + "  |  " + safe(event.getDate(), "Date TBA"));
        holder.tvAgeGroup.setText(safe(event.getAgeGroup(), "All Age Groups"));
        holder.tvLocation.setText(safe(event.getLocation(), "Location TBA"));
        holder.tvPrice.setText("$" + (int) event.getPrice());
        holder.tvStats.setText("Total: " + event.getTotalSpots() + "  |  Waiting List: " + event.getWaitingListCount());
        bindPoster(holder.ivEventPhoto, event.getPosterImage());

        holder.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onViewClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Replaces the current list of events with a new one and refreshes the RecyclerView.
     * @param newList The new list of {@link EventModel} objects.
     */
    public void updateList(List<EventModel> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    /**
     * Decodes a base64 string into a Bitmap and sets it as the image for the provided ImageView.
     * Falls back to a default app icon if the string is empty or invalid.
     * @param imageView The target ImageView.
     * @param posterBase64 The base64 encoded string of the poster image.
     */
    private void bindPoster(ImageView imageView, String posterBase64) {
        if (posterBase64 != null && !posterBase64.trim().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(posterBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.mipmap.ic_launcher);
    }

    /**
     * Provides a fallback string if the provided value is null or empty.
     * @param value The value to check.
     * @param fallback The string to return if the value is invalid.
     * @return The original value if valid; otherwise the fallback.
     */
    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    /**
     * Holds references to the views for an individual event list item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAgeGroup, tvLocation, tvPrice, tvStats;
        ImageView ivEventPhoto;
        Button btnView;

        /**
         * Constructs a new ViewHolder.
         * @param itemView The root view of the item layout.
         */
        ViewHolder(View itemView) {
            super(itemView);
            ivEventPhoto = itemView.findViewById(R.id.ivEventPhoto);
            tvName     = itemView.findViewById(R.id.tvEventName);
            tvAgeGroup = itemView.findViewById(R.id.tvAgeGroup);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice    = itemView.findViewById(R.id.tvPrice);
            tvStats    = itemView.findViewById(R.id.tvStats);
            btnView    = itemView.findViewById(R.id.btnView);
        }
    }
}
