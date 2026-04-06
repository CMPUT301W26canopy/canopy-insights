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
 */
public class MainActivity extends AppCompatActivity {

    private EventAdapter adapter;
    private final List<EventModel> masterList  = new ArrayList<>();
    private final List<EventModel> displayList = new ArrayList<>();
    private DeviceData deviceData;

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

    private void loadEventsFromFirestore() {
        FirestoreHelper.getDb()
                .collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterList.clear();
                    displayList.clear();
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
                                displayList.add(event);
                                loadWaitingCount(event);
                            }
                        }
                    }
                    adapter.updateList(displayList);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    displayList.clear();
                    displayList.addAll(masterList);
                } else {
                    List<EventModel> result = new ArrayList<>();
                    for (EventModel e : masterList) {
                        String name = e.getName() != null ? e.getName().toLowerCase() : "";
                        String location = e.getLocation() != null ? e.getLocation().toLowerCase() : "";
                        if (name.contains(query) || location.contains(query)) {
                            result.add(e);
                        }
                    }
                    displayList.clear();
                    displayList.addAll(result);
                }
                adapter.updateList(displayList);
            }
        });
    }

    private void setupFilterButton() {
        ImageButton btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> {
            FilterBottomSheet sheet = new FilterBottomSheet();
            sheet.setFilterCallback(new FilterBottomSheet.FilterCallback() {
                @Override
                public void onApply(double minPrice, double maxPrice, String month,
                                    String year, String ageGroup, String country) {
                    List<EventModel> result = new ArrayList<>();
                    for (EventModel e : masterList) {
                        boolean priceOk    = e.getPrice() >= minPrice && e.getPrice() <= maxPrice;
                        boolean ageOk      = ageGroup.equals("All Age Groups") ||
                                (e.getAgeGroup() != null && e.getAgeGroup().contains(ageGroup));
                        boolean locationOk = country.isEmpty() ||
                                (e.getLocation() != null && e.getLocation().toLowerCase().contains(country.toLowerCase()));
                        if (priceOk && ageOk && locationOk) result.add(e);
                    }
                    displayList.clear();
                    displayList.addAll(result);
                    adapter.updateList(displayList);
                }

                @Override
                public void onReset() {
                    displayList.clear();
                    displayList.addAll(masterList);
                    adapter.updateList(displayList);
                }
            });
            sheet.show(getSupportFragmentManager(), "filter");
        });
    }

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
                    adapter.updateList(displayList);
                });
    }

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
