package com.example.lotteryapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class HistoryActivity extends AppCompatActivity {

    private final List<HistoryAdapter.HistoryItem> historyItems = new ArrayList<>();
    private HistoryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private DeviceData deviceData;
    private String accountId;

    public static void openFrom(Activity activity, @Nullable String accountId) {
        String resolvedAccountId = accountId;
        if (resolvedAccountId == null || resolvedAccountId.trim().isEmpty()) {
            resolvedAccountId = DeviceData.getInstance(activity).getAccountID();
        }

        if (resolvedAccountId == null || resolvedAccountId.trim().isEmpty()) {
            if (activity instanceof LoginActivity) {
                Toast.makeText(activity, "Please sign in to view your history", Toast.LENGTH_SHORT).show();
            } else {
                activity.startActivity(new Intent(activity, LoginActivity.class));
            }
            return;
        }

        Intent intent = new Intent(activity, HistoryActivity.class);
        intent.putExtra("accountID", resolvedAccountId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        deviceData = DeviceData.getInstance(this);
        accountId = getIntent().getStringExtra("accountID");
        if (accountId == null || accountId.trim().isEmpty()) {
            accountId = deviceData.getAccountID();
        }

        recyclerView = findViewById(R.id.historyRecyclerView);
        emptyView = findViewById(R.id.historyEmptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(this, historyItems);
        recyclerView.setAdapter(adapter);

        setupBottomNav();

        if (accountId == null || accountId.trim().isEmpty()) {
            showEmptyState("Please log in to view your history.");
            return;
        }

        loadHistory();
    }

    private void loadHistory() {
        showEmptyState("Loading history...");

        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("userId", accountId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        showEmptyState("No event history yet.");
                        return;
                    }

                    historyItems.clear();
                    AtomicInteger pending = new AtomicInteger(snapshot.size());

                    for (QueryDocumentSnapshot applicationDoc : snapshot) {
                        String eventId = applicationDoc.getString("eventId");
                        String rawStatus = safe(applicationDoc.getString("status"));

                        if (eventId == null || eventId.trim().isEmpty()) {
                            markOneComplete(pending);
                            continue;
                        }

                        loadHistoryItem(eventId, rawStatus, pending);
                    }
                })
                .addOnFailureListener(e -> showEmptyState("Failed to load history."));
    }

    private void loadHistoryItem(String eventId, String rawStatus, AtomicInteger pending) {
        FirestoreHelper.getDb().collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    HistoryAdapter.HistoryItem item = new HistoryAdapter.HistoryItem();
                    item.eventId = eventId;
                    item.rawStatus = rawStatus;

                    if (eventDoc.exists()) {
                        String rawDate = safe(eventDoc.getString("date"));
                        item.eventName = valueOrDefault(eventDoc.getString("name"), "Untitled Event");
                        item.eventDate = formatEventDate(rawDate);
                        item.sortTimeMs = parseDateToMillis(rawDate);
                        item.ageGroup = normalizeAgeGroup(eventDoc.getString("ageGroup"));
                        item.location = valueOrDefault(eventDoc.getString("location"), "Location TBA");
                        item.description = valueOrDefault(eventDoc.getString("description"), "Event description.");
                        item.price = getDouble(eventDoc, "price");
                        item.totalSpots = getInt(eventDoc, "totalSpots");
                        item.posterBase64 = firstNonBlank(
                                asString(eventDoc.get("posterImage")),
                                asString(eventDoc.get("poster"))
                        );
                    } else {
                        item.eventName = "Deleted Event";
                        item.description = "This event is no longer available.";
                    }

                    loadWaitingCountAndFinish(item, pending);
                })
                .addOnFailureListener(e -> {
                    HistoryAdapter.HistoryItem item = new HistoryAdapter.HistoryItem();
                    item.eventId = eventId;
                    item.rawStatus = rawStatus;
                    item.eventName = "Event";
                    item.description = "Event details unavailable.";
                    historyItems.add(item);
                    markOneComplete(pending);
                });
    }

    private void loadWaitingCountAndFinish(HistoryAdapter.HistoryItem item, AtomicInteger pending) {
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", item.eventId)
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(waitingSnapshot -> {
                    item.waitingCount = waitingSnapshot.size();
                    historyItems.add(item);
                    markOneComplete(pending);
                })
                .addOnFailureListener(e -> {
                    historyItems.add(item);
                    markOneComplete(pending);
                });
    }

    private void markOneComplete(AtomicInteger pending) {
        if (pending.decrementAndGet() == 0) {
            finishLoading();
        }
    }

    private void finishLoading() {
        Collections.sort(historyItems, (left, right) ->
                Long.compare(right.sortTimeMs, left.sortTimeMs));

        adapter.notifyDataSetChanged();

        if (historyItems.isEmpty()) {
            showEmptyState("No event history yet.");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        findViewById(R.id.navCreate).setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerActivity.class)));

        findViewById(R.id.navHistory).setOnClickListener(v -> {
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            String resolvedAccountId = firstNonBlank(accountId, deviceData.getAccountID());
            if (resolvedAccountId == null) {
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }

            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("accountID", resolvedAccountId);
            startActivity(intent);
        });
    }

    private String normalizeAgeGroup(String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty() || safeValue.equalsIgnoreCase("All Ages")) {
            return "All Age Groups";
        }
        return safeValue;
    }

    private String formatEventDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "";
        }

        String[] patterns = {
                "MM-dd-yyyy",
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "yyyy/MM/dd",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                inputFormat.setLenient(false);
                Date parsed = inputFormat.parse(rawDate);
                if (parsed != null) {
                    return new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(parsed);
                }
            } catch (ParseException ignored) {
            }
        }

        return rawDate;
    }

    private long parseDateToMillis(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return 0L;
        }

        String[] patterns = {
                "MM-dd-yyyy",
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "yyyy/MM/dd",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                inputFormat.setLenient(false);
                Date parsed = inputFormat.parse(rawDate);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (ParseException ignored) {
            }
        }

        return 0L;
    }

    private double getDouble(DocumentSnapshot snapshot, String field) {
        Double doubleValue = snapshot.getDouble(field);
        if (doubleValue != null) {
            return doubleValue;
        }

        Long longValue = snapshot.getLong(field);
        return longValue != null ? longValue.doubleValue() : 0d;
    }

    private int getInt(DocumentSnapshot snapshot, String field) {
        Long longValue = snapshot.getLong(field);
        return longValue != null ? longValue.intValue() : 0;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOrDefault(String value, String fallback) {
        String safeValue = safe(value);
        return safeValue.isEmpty() ? fallback : safeValue;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
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
