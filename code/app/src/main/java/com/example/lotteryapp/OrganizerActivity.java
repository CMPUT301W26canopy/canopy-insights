package com.example.lotteryapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerActivity extends AppCompatActivity {

    private String organizerId;
    private DeviceData deviceData;

    private final List<EventModel> myEventsList = new ArrayList<>();
    private RecyclerView.Adapter myEventsAdapter;
    private Button btnCreateEvent, btnMyEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        deviceData = DeviceData.getInstance(this);
        if (deviceData.isLoggedIn()) {
            organizerId = deviceData.getAccountID();
        }

        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnMyEvents    = findViewById(R.id.btnMyEvents);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnCreateEvent.setOnClickListener(v -> {
            setActiveTab(btnCreateEvent, btnMyEvents);
            startActivity(new Intent(this, CreateEventActivity.class));
        });

        btnMyEvents.setOnClickListener(v -> {
            setActiveTab(btnMyEvents, btnCreateEvent);
            loadMyEvents();
        });

        RecyclerView recyclerView = findViewById(R.id.myEventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        myEventsAdapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                EventModel event = myEventsList.get(position);
                ((TextView) holder.itemView.findViewById(android.R.id.text1)).setText(event.getName());
                ((TextView) holder.itemView.findViewById(android.R.id.text2))
                        .setText("Date: " + event.getDate() + "  |  Spots: " + event.getTotalSpots());
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(OrganizerActivity.this, ApplicantsActivity.class);
                    intent.putExtra("EVENT_ID", event.getId());
                    intent.putExtra("EVENT_NAME", event.getName());
                    intent.putExtra("EVENT_DATE", event.getDate());
                    intent.putExtra("TOTAL_SPOTS", event.getTotalSpots());
                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() { return myEventsList.size(); }
        };
        recyclerView.setAdapter(myEventsAdapter);

        setupBottomNav();
        setActiveTab(btnMyEvents, btnCreateEvent);
        loadMyEvents();
    }

    // reload events every time we come back from CreateEventActivity
    @Override
    protected void onResume() {
        super.onResume();
        loadMyEvents();
    }

    private void setActiveTab(Button active, Button inactive) {
        active.setBackgroundTintList(ColorStateList.valueOf(0xFF6B5FA6));
        active.setTextColor(0xFFFFFFFF);
        inactive.setBackgroundTintList(ColorStateList.valueOf(0xFFE8E4F3));
        inactive.setTextColor(0xFF6B5FA6);
    }

    private void loadMyEvents() {
        String queryId = (organizerId != null) ? organizerId : "dummy_id";

        FirestoreHelper.getDb().collection("events")
                .whereEqualTo("organizerId", queryId)
                .get()
                .addOnSuccessListener(snap -> {
                    myEventsList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        EventModel e = null;
                        try {
                            e = doc.toObject(EventModel.class);
                        } catch (Exception ex) {
                            e = new EventModel();
                            e.setWaitingList(new ArrayList<>());
                            String name = doc.getString("name");
                            String date = doc.getString("date");
                            String loc  = doc.getString("location");
                            String age  = doc.getString("ageGroup");
                            Long price  = doc.getLong("price");
                            Long spots  = doc.getLong("totalSpots");
                            if (name  != null) e.setName(name);
                            if (date  != null) e.setDate(date);
                            if (loc   != null) e.setLocation(loc);
                            if (age   != null) e.setAgeGroup(age);
                            if (price != null) e.setPrice(price.doubleValue());
                            if (spots != null) e.setTotalSpots(spots.intValue());
                        }
                        e.setId(doc.getId());
                        myEventsList.add(e);
                    }
                    myEventsAdapter.notifyDataSetChanged();
                    if (myEventsList.isEmpty()) {
                        Toast.makeText(this, "No events yet", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.navCreate).setOnClickListener(v -> {});
        findViewById(R.id.navHistory).setOnClickListener(v ->
                Toast.makeText(this, "History — coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (deviceData.isLoggedIn()) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("accountID", deviceData.getAccountID());
                startActivity(intent);
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        });
    }
}