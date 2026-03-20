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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventActivity extends AppCompatActivity {

    private final List<String> eventDetailsList = new ArrayList<>();
    private SimpleTextAdapter adapter;
    private TextView costHeading, eventHeading;
    private ImageView qrCodeView;
    private Button btnJoin, btnLeave;
    private String eventId, userId;
    private boolean isOnWaitingList = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_event);

        DeviceData deviceData = DeviceData.getInstance(this);
        userId = deviceData.getAccountID();

        RecyclerView eventDescDisplay = findViewById(R.id.event_details);
        if (eventDescDisplay != null) {
            eventDescDisplay.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SimpleTextAdapter(eventDetailsList);
            eventDescDisplay.setAdapter(adapter);
        }

        costHeading = findViewById(R.id.cost_view);
        eventHeading = findViewById(R.id.event_heading);
        btnJoin  = findViewById(R.id.btnJoinWaitingList);
        btnLeave = findViewById(R.id.btnLeaveWaitingList);

        ImageButton backBtnTop = findViewById(R.id.back_btn_top);
        if (backBtnTop != null) backBtnTop.setOnClickListener(v -> finish());

        ImageView eventImage = findViewById(R.id.event_image);
        if (eventImage != null) eventImage.setImageResource(R.mipmap.ic_launcher);

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId != null) {
            loadEventDetails(eventId);
            checkIfAlreadyJoined(eventId);
        }

        if (btnJoin != null) btnJoin.setOnClickListener(v -> joinWaitingList());
        if (btnLeave != null) btnLeave.setOnClickListener(v -> leaveWaitingList());

        setupBottomNav();
        qrCodeView = findViewById(R.id.event_qr_code);
    }

    private void loadEventDetails(String eventId) {
        FirestoreHelper.getDb().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    EventModel event = null;
                    try {
                        event = documentSnapshot.toObject(EventModel.class);
                    } catch (Exception e) {
                        event = new EventModel();
                        event.setWaitingList(new ArrayList<>());
                        String name = documentSnapshot.getString("name");
                        String date = documentSnapshot.getString("date");
                        String loc  = documentSnapshot.getString("location");
                        String age  = documentSnapshot.getString("ageGroup");
                        Long price  = documentSnapshot.getLong("price");
                        Long spots  = documentSnapshot.getLong("totalSpots");
                        if (name  != null) event.setName(name);
                        if (date  != null) event.setDate(date);
                        if (loc   != null) event.setLocation(loc);
                        if (age   != null) event.setAgeGroup(age);
                        if (price != null) event.setPrice(price.doubleValue());
                        if (spots != null) event.setTotalSpots(spots.intValue());
                    }
                    if (event != null) {
                        eventDetailsList.clear();
                        eventDetailsList.add("Total Spots: " + event.getTotalSpots());
                        eventDetailsList.add("Current Waiting List: " + event.getWaitingListCount());
                        eventDetailsList.add(String.format(Locale.getDefault(), "Price: $%d", (int) event.getPrice()));
                        eventDetailsList.add("Age Group: " + event.getAgeGroup());
                        eventDetailsList.add("Location: " + event.getLocation());
                        eventDetailsList.add("Date: " + event.getDate());
                        if (costHeading != null)
                            costHeading.setText(String.format(Locale.getDefault(), "$%d", (int) event.getPrice()));
                        if (eventHeading != null)
                            eventHeading.setText(event.getName());
                        adapter.notifyDataSetChanged();
                        if (qrCodeView != null) {
                            Bitmap qr = QRCodeHelper.generateQRCode(eventId);
                            if (qr != null) {
                                qrCodeView.setImageBitmap(qr);
                                qrCodeView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

    private void checkIfAlreadyJoined(String eventId) {
        if (userId == null) return;
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    isOnWaitingList = !snap.isEmpty();
                    updateJoinLeaveButtons();
                });
    }

    private void joinWaitingList() {
        if (userId == null) {
            Toast.makeText(this, "Please log in to join", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> application = new HashMap<>();
        application.put("eventId", eventId);
        application.put("userId", userId);
        application.put("userName", DeviceData.getInstance(this).getUsername());
        application.put("status", "waiting");

        FirestoreHelper.getDb().collection("applications")
                .add(application)
                .addOnSuccessListener(ref -> {
                    isOnWaitingList = true;
                    updateJoinLeaveButtons();
                    Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show());
    }

    private void leaveWaitingList() {
        if (userId == null) return;
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete();
                    }
                    isOnWaitingList = false;
                    updateJoinLeaveButtons();
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to leave", Toast.LENGTH_SHORT).show());
    }

    private void updateJoinLeaveButtons() {
        if (btnJoin != null) btnJoin.setVisibility(isOnWaitingList ? View.GONE : View.VISIBLE);
        if (btnLeave != null) btnLeave.setVisibility(isOnWaitingList ? View.VISIBLE : View.GONE);
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) navHome.setOnClickListener(v -> finish());
        View navCreate = findViewById(R.id.navCreate);
        if (navCreate != null) navCreate.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerActivity.class)));
        View navHistory = findViewById(R.id.navHistory);
        if (navHistory != null) navHistory.setOnClickListener(v ->
                Toast.makeText(this, "History — coming soon", Toast.LENGTH_SHORT).show());
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final List<String> data;
        SimpleTextAdapter(List<String> data) { this.data = data; }

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
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}