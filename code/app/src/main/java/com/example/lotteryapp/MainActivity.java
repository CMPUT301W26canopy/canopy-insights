package com.example.lotteryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the entrant home feed, search, and filter entry points.
 * Handles the display of public events and allows users to search and filter them.
 */
public class MainActivity extends AppCompatActivity {

    private EventAdapter adapter;
    private final List<EventModel> masterList  = new ArrayList<>();
    private final List<EventModel> displayList = new ArrayList<>();
    private DeviceData deviceData;

    // Filter and Search State
    private String currentSearchQuery = "";
    private double minPrice = 0;
    private double maxPrice = 9999;
    private int minSpots = 0;
    private int maxSpots = 999999;
    private String filterMonth = "Any";
    private String filterYear = "Any";
    private String filterAgeGroup = "All Age Groups";
    private String filterCountry = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceData = DeviceData.getInstance(this);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(this, displayList, event -> {
            Toast.makeText(this, "Opening: " + event.getName(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, EventActivity.class);
            intent.putExtra("EVENT_ID", event.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadEventsFromFirestore();
        setupSearch();
        setupFilterButton();
        setupBottomNav();

        ImageButton btnQr = findViewById(R.id.btnQrScanner);
        btnQr.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QRScannerActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Loads public events from Firestore and populates the master list.
     * Triggers the initial display by calling {@link #applyFiltersAndSearch()}.
     */
    private void loadEventsFromFirestore() {
        FirestoreHelper.getDb()
                .collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String visibility = doc.getString("visibility");
                        if ("Private".equalsIgnoreCase(visibility)) {
                            continue;
                        }
                        EventModel event = null;
                        try {
                            event = doc.toObject(EventModel.class);
                        } catch (Exception e) {
                            event = new EventModel();
                            event.setName(doc.getString("name"));
                            event.setWaitingList(new ArrayList<>());
                            String date = doc.getString("date");
                            String age  = doc.getString("ageGroup");
                            String loc  = doc.getString("location");
                            String eventVisibility = doc.getString("visibility");
                            Long price  = doc.getLong("price");
                            Long spots  = doc.getLong("totalSpots");
                            if (date  != null) event.setDate(date);
                            if (age   != null) event.setAgeGroup(age);
                            if (loc   != null) event.setLocation(loc);
                            if (eventVisibility != null) event.setVisibility(eventVisibility);
                            if (price != null) event.setPrice(price.doubleValue());
                            if (spots != null) event.setTotalSpots(spots.intValue());
                        }
                        if (event != null) {
                            hydrateEvent(event, doc);
                            if (!"Private".equalsIgnoreCase(event.getVisibility())) {
                                masterList.add(event);
                                loadWaitingCount(event);
                            }
                        }
                    }
                    applyFiltersAndSearch();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Applies the current search query and filters to the master list of events.
     * Updates the display list and notifies the adapter.
     */
    private void applyFiltersAndSearch() {
        List<EventModel> result = new ArrayList<>();
        String query = currentSearchQuery.toLowerCase();

        for (EventModel e : masterList) {
            // Keyword Search check
            boolean matchesSearch = query.isEmpty() ||
                    (e.getName() != null && e.getName().toLowerCase().contains(query)) ||
                    (e.getLocation() != null && e.getLocation().toLowerCase().contains(query));

            // Filter checks
            boolean priceOk    = e.getPrice() >= minPrice && e.getPrice() <= maxPrice;
            boolean spotsOk    = e.getTotalSpots() >= minSpots && e.getTotalSpots() <= maxSpots;
            boolean ageOk      = filterAgeGroup.equals("All Age Groups") ||
                    (e.getAgeGroup() != null && e.getAgeGroup().contains(filterAgeGroup));
            boolean locationOk = filterCountry.isEmpty() ||
                    (e.getLocation() != null && e.getLocation().toLowerCase().contains(filterCountry.toLowerCase()));

            boolean dateOk = true;
            if (!filterMonth.equals("Any") || !filterYear.equals("Any")) {
                String eventDate = e.getDate();
                if (eventDate != null) {
                    if (!filterMonth.equals("Any") && !eventDate.contains(filterMonth)) dateOk = false;
                    if (!filterYear.equals("Any") && !eventDate.contains(filterYear)) dateOk = false;
                } else {
                    dateOk = false;
                }
            }

            if (matchesSearch && priceOk && spotsOk && ageOk && locationOk && dateOk) {
                result.add(e);
            }
        }

        displayList.clear();
        displayList.addAll(result);
        adapter.updateList(displayList);
    }

    /**
     * Sets up the search bar with a text listener to filter events in real-time.
     */
    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFiltersAndSearch();
            }
        });
    }

    /**
     * Configures the filter button to open a bottom sheet with various filtering options.
     */
    private void setupFilterButton() {
        ImageButton btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> {
            FilterBottomSheet sheet = new FilterBottomSheet();
            sheet.setFilterCallback(new FilterBottomSheet.FilterCallback() {
                @Override
                public void onApply(double minP, double maxP, int minS, int maxS,
                                    String month, String year, String age, String country) {
                    minPrice = minP;
                    maxPrice = maxP;
                    minSpots = minS;
                    maxSpots = maxS;
                    filterMonth = month;
                    filterYear = year;
                    filterAgeGroup = age;
                    filterCountry = country;

                    applyFiltersAndSearch();
                }

                @Override
                public void onReset() {
                    minPrice = 0;
                    maxPrice = 9999;
                    minSpots = 0;
                    maxSpots = 999999;
                    filterMonth = "Any";
                    filterYear = "Any";
                    filterAgeGroup = "All Age Groups";
                    filterCountry = "";

                    applyFiltersAndSearch();
                }
            });
            sheet.show(getSupportFragmentManager(), "filter");
        });
    }

    /**
     * Wires the bottom navigation buttons to their respective activities or fragments.
     */
    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {});

        findViewById(R.id.navCreate).setOnClickListener(v -> {
            Log.d("DEBUG", "navCreate clicked");
            Intent intent = new Intent(MainActivity.this, OrganizerActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navHistory).setOnClickListener(v ->
                NavigationHelper.openHistory(this));

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (deviceData.isLoggedIn()) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("accountID", deviceData.getAccountID());
                startActivity(intent);
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Ensures an {@link EventModel} has its core fields populated from a Firestore document.
     * @param event The event object to populate.
     * @param doc The source Firestore document.
     */
    private void hydrateEvent(EventModel event, QueryDocumentSnapshot doc) {
        event.setId(doc.getId());

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
        if (event.getVisibility() == null) {
            event.setVisibility(doc.getString("visibility"));
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
     * Fetches the current number of entrants on the waiting list for a specific event.
     * @param event The event to update with the waiting count.
     */
    private void loadWaitingCount(EventModel event) {
        if (event == null || event.getId() == null) {
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", event.getId())
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(snapshot -> {
                    event.setWaitingCount(snapshot.size());
                    applyFiltersAndSearch();
                });
    }

    /**
     * Returns the first non-null and non-empty string between two choices.
     * @param first The primary choice.
     * @param second The secondary fallback.
     * @return The resulting non-blank string, or null if both are invalid.
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
}
