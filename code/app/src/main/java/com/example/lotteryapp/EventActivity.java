package com.example.lotteryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen showing the event details.
 * Loads details from firestore and allows users to join/leave waiting list.
 */
public class EventActivity extends AppCompatActivity {

    private final List<String> eventDetailsList = new ArrayList<>();
    private RecyclerView eventDescDisplay;
    private SimpleTextAdapter adapter;
    private Button waitingListBtn;
    private TextView statusText;
    private String eventId;
    private boolean isApplied = false; // Simple local state for demo purposes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_event);

        // Reference UI elements
        eventDescDisplay = findViewById(R.id.event_details);
        waitingListBtn = findViewById(R.id.waiting_list_btn);
        statusText = findViewById(R.id.status);
        Button backBtn = findViewById(R.id.back_button);
        Button backBtnTop = findViewById(R.id.back_btn_top);

        eventDescDisplay.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter and attach it
        adapter = new SimpleTextAdapter(eventDetailsList);
        eventDescDisplay.setAdapter(adapter);

        ImageView eventImage = findViewById(R.id.event_image);
        eventImage.setImageResource(R.mipmap.ic_launcher);

        // Receive event ID from MainActivity
        Intent intent = getIntent();
        if (intent != null) {
             eventId = intent.getStringExtra("EVENT_ID");
             if (eventId != null) {
                 loadEventDetails(eventId);
             }
        }

        // Back buttons
        View.OnClickListener backListener = v -> finish();
        backBtn.setOnClickListener(backListener);
        backBtnTop.setOnClickListener(backListener);

        // Join/Withdraw logic (R5 & R6)
        waitingListBtn.setOnClickListener(v -> handleWaitingListAction());

        setupBottomNav();
    }

    private void loadEventDetails(String eventId) {
        FirestoreHelper.getDb().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    EventModel event = documentSnapshot.toObject(EventModel.class);
                    if (event != null) {
                        eventDetailsList.clear();
                        eventDetailsList.add("Event: " + event.getName());
                        eventDetailsList.add("Location: " + event.getLocation());
                        eventDetailsList.add("Price: $" + (int)event.getPrice());
                        eventDetailsList.add("Waitlist Count: " + event.getWaitingList());

                        // Update status text based on local state (for now)
                        if (isApplied) {
                            statusText.setText("Status: Applied");
                            waitingListBtn.setText("Withdraw from list");
                        } else {
                            statusText.setText("Status: Not Applied");
                            waitingListBtn.setText("Join waiting list");
                        }

                        // Refresh the RecyclerView
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show()
                );
    }

    private void handleWaitingListAction() {
        if (eventId == null) return;

        if (!isApplied) {
            // Join logic (R5)
            FirestoreHelper.getDb().collection("events").document(eventId)
                    .update("waitingList", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> {
                        isApplied = true;
                        Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                        loadEventDetails(eventId);
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            // Withdraw logic (R6)
            FirestoreHelper.getDb().collection("events").document(eventId)
                    .update("waitingList", FieldValue.increment(-1))
                    .addOnSuccessListener(aVoid -> {
                        isApplied = false;
                        Toast.makeText(this, "Withdrawn from list", Toast.LENGTH_SHORT).show();
                        loadEventDetails(eventId);
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to withdraw: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navCreate).setOnClickListener(v -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navHistory).setOnClickListener(v -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navProfile).setOnClickListener(v -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }

    /**
     * Simple Adapter as an Inner Class to avoid creating a new file.
     */
    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final List<String> data;

        public SimpleTextAdapter(List<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
