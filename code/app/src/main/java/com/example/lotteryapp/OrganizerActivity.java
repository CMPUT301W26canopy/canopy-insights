package com.example.lotteryapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerActivity extends AppCompatActivity {

    // TODO: We need to replace with real accountID
    private static final String ORGANIZER_ID = "dummy_id";

    private final List<EventModel> myEventsList = new ArrayList<>();
    private RecyclerView.Adapter myEventsAdapter;
    private Button btnCreateEvent;
    private Button btnMyEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnMyEvents = findViewById(R.id.btnMyEvents);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Create Event tab
        btnCreateEvent.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateEventActivity.class));
            setActiveTab(btnCreateEvent, btnMyEvents);
        });

        // My Events tab
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
                    intent.putExtra("PRICE", event.getPrice());
                    intent.putExtra("DESCRIPTION", event.getDescription());
                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() {
                return myEventsList.size();
            }
        };
        recyclerView.setAdapter(myEventsAdapter);

        setupBottomNav();
        setActiveTab(btnMyEvents, btnCreateEvent);
        loadMyEvents();
    }

<<<<<<< Updated upstream
=======
    @Override
    protected void onResume() {
        super.onResume();
        loadMyEvents();
    }

>>>>>>> Stashed changes
    private void setActiveTab(Button active, Button inactive) {
        active.setBackgroundTintList(ColorStateList.valueOf(0xFF6B5FA6));
        active.setTextColor(0xFFFFFFFF);
        inactive.setBackgroundTintList(ColorStateList.valueOf(0xFFE8E4F3));
        inactive.setTextColor(0xFF6B5FA6);
    }

    private void loadMyEvents() {
<<<<<<< Updated upstream
        FirestoreHelper.getDb().collection("events")
                .whereEqualTo("organizerId", ORGANIZER_ID)
                .get()
                .addOnSuccessListener(snap -> {
                    myEventsList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        EventModel e = doc.toObject(EventModel.class);
                        e.setId(doc.getId());
                        myEventsList.add(e);
                    }
                    myEventsAdapter.notifyDataSetChanged();
                    if (myEventsList.isEmpty())
                        Toast.makeText(this, "No events yet", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show());
=======
        if (organizerId == null) {
            Toast.makeText(this, "Please sign in to view your events", Toast.LENGTH_SHORT).show();
            return;
        }

        Task<QuerySnapshot> primaryQuery = FirestoreHelper.getDb().collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get();

        Task<QuerySnapshot> cohostQuery = FirestoreHelper.getDb().collection("events")
                .whereArrayContains("invitedHosts", organizerId)
                .get();

        Tasks.whenAllSuccess(primaryQuery, cohostQuery).addOnSuccessListener(results -> {
            Set<EventModel> mergedEvents = new HashSet<>();
            for (Object result : results) {
                QuerySnapshot snapshot = (QuerySnapshot) result;
                for (QueryDocumentSnapshot doc : snapshot) {
                    EventModel e;
                    try {
                        e = doc.toObject(EventModel.class);
                    } catch (Exception ex) {
                        e = new EventModel();
                        e.setWaitingList(new ArrayList<>());
                        String name = doc.getString("name");
                        String date = doc.getString("date");
                        String loc = doc.getString("location");
                        String age = doc.getString("ageGroup");
                        String description = doc.getString("description");
                        Long price = doc.getLong("price");
                        Long spots = doc.getLong("totalSpots");
                        if (name != null) e.setName(name);
                        if (date != null) e.setDate(date);
                        if (loc != null) e.setLocation(loc);
                        if (age != null) e.setAgeGroup(age);
                        if (description != null) e.setDescription(description);
                        if (price != null) e.setPrice(price.doubleValue());
                        if (spots != null) e.setTotalSpots(spots.intValue());
                    }
                    e.setId(doc.getId());
                    mergedEvents.add(e);
                }
            }
            myEventsList.clear();
            myEventsList.addAll(mergedEvents);
            myEventsAdapter.notifyDataSetChanged();

            if (myEventsList.isEmpty()) {
                Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("OrganizerActivity", "Failed to load events", e);
            Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
>>>>>>> Stashed changes
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });
        findViewById(R.id.navCreate).setOnClickListener(v -> {}); // already here
        findViewById(R.id.navHistory).setOnClickListener(v ->
<<<<<<< Updated upstream
<<<<<<< Updated upstream
                Toast.makeText(this, "History — coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
=======
                HistoryActivity.openFrom(this, deviceData.getAccountID()));

=======
                NavigationHelper.openHistory(this));
>>>>>>> Stashed changes
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (deviceData.isLoggedIn()) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("accountID", deviceData.getAccountID());
                startActivity(intent);
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        });
>>>>>>> Stashed changes
    }
}