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
                        EventModel event = null;
                        try {
                            event = doc.toObject(EventModel.class);
                        } catch (Exception e) {
                            event = new EventModel();
                            event.setWaitingList(new ArrayList<>());
                            String name = doc.getString("name");
                            String date = doc.getString("date");
                            String age  = doc.getString("ageGroup");
                            String loc  = doc.getString("location");
                            Long price  = doc.getLong("price");
                            Long spots  = doc.getLong("totalSpots");
                            if (name  != null) event.setName(name);
                            if (date  != null) event.setDate(date);
                            if (age   != null) event.setAgeGroup(age);
                            if (loc   != null) event.setLocation(loc);
                            if (price != null) event.setPrice(price.doubleValue());
                            if (spots != null) event.setTotalSpots(spots.intValue());
                        }
                        event.setId(doc.getId());
                        masterList.add(event);
                        displayList.add(event);
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
                        if (e.getName().toLowerCase().contains(query) ||
                                e.getLocation().toLowerCase().contains(query)) {
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
                                e.getAgeGroup().contains(ageGroup);
                        boolean locationOk = country.isEmpty() ||
                                e.getLocation().toLowerCase().contains(country.toLowerCase());
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
                Toast.makeText(this, "History — coming soon", Toast.LENGTH_SHORT).show());

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
}