package com.example.lotteryapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private LinearLayout linearLayout;
    private ImageButton backBtnTop;
    private TextView costHeading;
    private TextView eventHeading;
    private ImageView qrCodeView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_event);

        // Reference the RecyclerView and set LayoutManager
        eventDescDisplay = findViewById(R.id.event_details);
        eventDescDisplay.setLayoutManager(new LinearLayoutManager(this));
        
        //Initialize the adapter
        adapter = new SimpleTextAdapter(eventDetailsList);
        eventDescDisplay.setAdapter(adapter);

        //Initialize the linear layout to fix error
        linearLayout = findViewById(R.id.event_details_container);

        // Initialize the other views so they can be accessed properly in other methods
        costHeading = findViewById(R.id.cost_view);
        eventHeading = findViewById(R.id.event_heading);

        //Connect top button to go back to main activity
        backBtnTop = findViewById(R.id.back_btn_top);   
        if (backBtnTop != null) {
            backBtnTop.setOnClickListener(v -> finish());
        }

        
        
        ImageView eventImage = findViewById(R.id.event_image);
        if (eventImage != null) {
            eventImage.setImageResource(R.mipmap.ic_launcher);
        }

        // Receive event ID from MainActivity
        Intent intent = getIntent();
        if (intent != null) {
             String eventId = intent.getStringExtra("EVENT_ID");
             if (eventId != null) {
                 loadEventDetails(eventId);
             }
        }
        
        setupBottomNav();
        qrCodeView = findViewById(R.id.event_qr_code);
    }

    private void loadEventDetails(String eventId) {
        FirestoreHelper.getDb().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    EventModel event = documentSnapshot.toObject(EventModel.class);
                    if (event != null) {
                        // Set list to view details
                        eventDetailsList.clear();
                        eventDetailsList.add("Event: " + event.getName());
                        eventDetailsList.add("Total Spots: " + event.getTotalSpots());
                        eventDetailsList.add("Current Waiting List: " + event.getWaitingList());
                        eventDetailsList.add("Price: $" + (int)event.getPrice());
                        eventDetailsList.add("Age Group: " + event.getAgeGroup());

                        eventDetailsList.add("Location: " + event.getLocation());
                        eventDetailsList.add("Date: " + event.getDate());


                        
                        // Set non-list details to display correct info
                        costHeading.setText("$ "+ (int)event.getPrice());
                        eventHeading.setText(event.getName());
                        // Refresh the RecyclerView
                        adapter.notifyDataSetChanged();
                        // After loading the event
                        // Generate QR code here
                        if (qrCodeView != null) {
                            Bitmap qr = QRCodeHelper.generateQRCode(eventId); // use eventId from Firestore
                            if (qr != null) {
                                qrCodeView.setImageBitmap(qr);
                                qrCodeView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show()
                );

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
            // Use a built-in Android layout for simple text rows
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
