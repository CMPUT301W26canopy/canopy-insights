package com.example.lotteryapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shows the organizer dashboard and event list.
 * This activity handles displaying events that the user is organizing or co-hosting.
 */
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
                TextView titleView = holder.itemView.findViewById(android.R.id.text1);
                TextView subtitleView = holder.itemView.findViewById(android.R.id.text2);
                titleView.setText(event.getName());
                subtitleView.setText("Location: " + safe(event.getLocation(), "TBA")
                        + "  |  Date: " + safe(event.getDate(), "TBA")
                        + "  |  Spots: " + event.getTotalSpots()
                        + "  |  Waiting: " + event.getWaitingListCount());
                styleSimpleListRow(holder.itemView, titleView, subtitleView);
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
            public int getItemCount() { return myEventsList.size(); }
        };
        recyclerView.setAdapter(myEventsAdapter);

        setupBottomNav();
        setActiveTab(btnMyEvents, btnCreateEvent);
        loadMyEvents();
    }

    // reload events every time we come back from CreateEventActivity
    /**
     * Called when the activity is resuming.
     * Reloads the organizer's events to ensure the list is up to date.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadMyEvents();
    }

    /**
     * Updates the UI to reflect which tab is currently active.
     * @param active The button that should be styled as active.
     * @param inactive The button that should be styled as inactive.
     */
    private void setActiveTab(Button active, Button inactive) {
        active.setBackgroundTintList(ColorStateList.valueOf(0xFF6B5FA6));
        active.setTextColor(0xFFFFFFFF);
        inactive.setBackgroundTintList(ColorStateList.valueOf(0xFFE8E4F3));
        inactive.setTextColor(0xFF6B5FA6);
    }

    /**
     * Fetches events from Firestore where the user is either the primary organizer or a co-host.
     * Merges results and updates the list display.
     */
    private void loadMyEvents() {
        if (organizerId == null) {
            Toast.makeText(this, "Please sign in to view your events", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query 1: Events where the user is the primary organizer
        Task<QuerySnapshot> primaryQuery = FirestoreHelper.getDb().collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get();

        // Query 2: Events where the user is in the invitedHosts list (co-hosts)
        Task<QuerySnapshot> cohostQuery = FirestoreHelper.getDb().collection("events")
                .whereArrayContains("invitedHosts", organizerId)
                .get();

        Tasks.whenAllSuccess(primaryQuery, cohostQuery).addOnSuccessListener(results -> {
            Set<EventModel> mergedEvents = new HashSet<>();
            for (Object result : results) {
                QuerySnapshot snapshot = (QuerySnapshot) result;
                for (QueryDocumentSnapshot doc : snapshot) {
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
                    hydrateEvent(e, doc);
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
    }

    /**
     * Configures the click listeners for the bottom navigation bar.
     */
    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.navCreate).setOnClickListener(v -> {});
        findViewById(R.id.navHistory).setOnClickListener(v ->
                NavigationHelper.openHistory(this));
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

    /**
     * Manually populates an EventModel with fields from a Firestore document snapshot.
     * @param event The EventModel instance to populate.
     * @param doc The source QueryDocumentSnapshot from Firestore.
     */
    private void hydrateEvent(EventModel event, QueryDocumentSnapshot doc) {
        event.setId(doc.getId());
        if (event.getDescription() == null) {
            event.setDescription(doc.getString("description"));
        }
        if (event.getName() == null) {
            event.setName(doc.getString("name"));
        }
        if (event.getDate() == null) {
            event.setDate(doc.getString("date"));
        }
        if (event.getAgeGroup() == null) {
            event.setAgeGroup(doc.getString("ageGroup"));
        }
        if (event.getLocation() == null) {
            event.setLocation(doc.getString("location"));
        }
        if (event.getPosterImage() == null || event.getPosterImage().trim().isEmpty()) {
            event.setPosterImage(firstNonBlank(
                    doc.getString("posterImage"),
                    doc.getString("poster")
            ));
        }
        if (event.getWaitingList() == null) {
            Object waitingList = doc.get("waitingList");
            if (waitingList instanceof List) {
                event.setWaitingList((List<String>) waitingList);
            } else {
                event.setWaitingList(new ArrayList<>());
            }
        }
    }

    /**
     * Picks the first string that is not null or empty.
     * @param first The primary string choice.
     * @param second The secondary string choice.
     * @return The first non-blank string, or null if both are blank.
     */
    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }

    /**
     * Returns the given value or a fallback string if the value is blank.
     * @param value The candidate string.
     * @param fallback The string to return if value is null or empty.
     * @return The original value or fallback.
     */
    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    /**
     * Applies custom styling to a standard list item row.
     * @param itemView The root view of the row.
     * @param titleView The primary text view in the row.
     * @param subtitleView The secondary text view in the row.
     */
    private void styleSimpleListRow(View itemView, TextView titleView, TextView subtitleView) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFFF7F3FC);
        background.setCornerRadius(dpToPx(16));
        background.setStroke(dpToPx(1), 0xFFE2DAF0);
        itemView.setBackground(background);
        itemView.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        if (params instanceof RecyclerView.LayoutParams) {
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) params;
            layoutParams.bottomMargin = dpToPx(10);
            itemView.setLayoutParams(layoutParams);
        }

        titleView.setTextColor(0xFF221A35);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        subtitleView.setTextColor(0xFF6E647E);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    }

    /**
     * Converts density-independent pixels (dp) to screen pixels (px).
     * @param dp The value in dp.
     * @return The corresponding value in px.
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
