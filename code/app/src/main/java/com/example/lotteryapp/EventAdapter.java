package com.example.lotteryapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for the event feed RecyclerView.
 * Binds EventModel data to each card in the list.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    // callback so MainActivity knows when View is clicked
    public interface OnEventClickListener {
        void onViewClick(EventModel event);
    }

    private List<EventModel> events;
    private final Context context;
    private final OnEventClickListener listener;

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

        holder.tvName.setText(event.getName() + "  |  " + event.getDate());
        holder.tvAgeGroup.setText(event.getAgeGroup());
        holder.tvLocation.setText(event.getLocation());
        holder.tvPrice.setText("$" + (int) event.getPrice());
        holder.tvStats.setText("Total: " + event.getTotalSpots() + "  |  Waiting List: " + event.getWaitingList());

        holder.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onViewClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    // replaces the list and refreshes the RecyclerView
    public void updateList(List<EventModel> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAgeGroup, tvLocation, tvPrice, tvStats;
        Button btnView;

        ViewHolder(View itemView) {
            super(itemView);
            tvName     = itemView.findViewById(R.id.tvEventName);
            tvAgeGroup = itemView.findViewById(R.id.tvAgeGroup);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice    = itemView.findViewById(R.id.tvPrice);
            tvStats    = itemView.findViewById(R.id.tvStats);
            btnView    = itemView.findViewById(R.id.btnView);
        }
    }
}