package com.example.lotteryapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen showing the event details.
 * Loads details from Firestore and allows users to join/leave waiting list.
 */
public class EventActivity extends AppCompatActivity {

    private final List<String> eventDetailsList = new ArrayList<>();
    private SimpleTextAdapter adapter;
    private TextView costHeading;
    private TextView eventHeading;
    private ImageView qrCodeView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_event);
        RecyclerView eventDescDisplay = findViewById(R.id.event_details);
        if (eventDescDisplay != null) {
            eventDescDisplay.setLayoutManager(new LinearLayoutManager(this));
            // Initialize the adapter
            adapter = new SimpleTextAdapter(eventDetailsList);
            eventDescDisplay.setAdapter(adapter);
        }

        costHeading = findViewById(R.id.cost_view);
        eventHeading = findViewById(R.id.event_heading);
        ImageButton backBtnTop = findViewById(R.id.back_btn_top);
        if (backBtnTop != null) {
            backBtnTop.setOnClickListener(v -> finish());
        }

        ImageView eventImage = findViewById(R.id.event_image);
        if (eventImage != null) {
            eventImage.setImageResource(R.mipmap.ic_launcher);
        }

       
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
                        eventDetailsList.clear();
                        eventDetailsList.add("Event: " + event.getName());
                        eventDetailsList.add("Total Spots: " + event.getTotalSpots());
                        eventDetailsList.add("Current Waiting List: " + event.getWaitingList());
                        eventDetailsList.add(String.format(Locale.getDefault(), "Price: $%d", (int) event.getPrice()));
                        eventDetailsList.add("Age Group: " + event.getAgeGroup());
                        eventDetailsList.add("Location: " + event.getLocation());
                        eventDetailsList.add("Date: " + event.getDate());

                        if (costHeading != null) {
                            costHeading.setText(String.format(Locale.getDefault(), "$ %d", (int) event.getPrice()));
                        }
                        if (eventHeading != null) {
                            eventHeading.setText(event.getName());
                        }

                        // Refresh the RecyclerView
                        adapter.notifyDataSetChanged();
                        
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
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) navHome.setOnClickListener(v -> finish());

        View navCreate = findViewById(R.id.navCreate);
        if (navCreate != null) {
            navCreate.setOnClickListener(v -> {
                Intent intent = new Intent(this, OrganizerActivity.class);
                startActivity(intent);
            });
        }

        View navHistory = findViewById(R.id.navHistory);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> Toast.makeText(this, "History — coming soon", Toast.LENGTH_SHORT).show());
        }

        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            });
        }
    }

 
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
            final TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}