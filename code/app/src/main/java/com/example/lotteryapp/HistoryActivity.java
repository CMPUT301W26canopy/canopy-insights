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

/**
 * Displays the current user's application history across events.
 * Provides a chronological feed of events the user has interacted with.
 */
public class HistoryActivity extends AppCompatActivity {

    private final List<HistoryAdapter.HistoryItem> historyItems = new ArrayList<>();
    private HistoryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private DeviceData deviceData;
    private String accountId;

    /**
     * Static helper to launch HistoryActivity for a specific account.
     * @param activity The calling activity.
     * @param accountId The ID of the account whose history should be shown.
     */
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

    /**
     * Fetches all event applications for the current user from Firestore.
     */
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

    /**
     * Loads details for a specific event associated with a history item.
     * @param eventId The ID of the event to fetch.
     * @param rawStatus The user's application status for this event.
     * @param pending Counter to track when all items are loaded.
     */
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

    /**
     * Fetches the waiting list count for a specific history item and marks its loading as complete.
     * @param item The history item to update.
     * @param pending Counter to track when all items are loaded.
     */
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

    /**
     * Decrements the pending counter and triggers the final loading sequence if zero.
     * @param pending The counter to decrement.
     */
    private void markOneComplete(AtomicInteger pending) {
        if (pending.decrementAndGet() == 0) {
            finishLoading();
        }
    }

    /**
     * Sorts the loaded history items by date and refreshes the RecyclerView display.
     */
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

    /**
     * Displays a message in the empty state view.
     * @param message The message to show.
     */
    private void showEmptyState(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    /**
     * Wires the shared bottom navigation bar actions.
     */
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

    /**
     * Normalizes age group labels for consistent display.
     * @param value The raw age group value.
     * @return A standard age group label.
     */
    private String normalizeAgeGroup(String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty() || safeValue.equalsIgnoreCase("All Ages")) {
            return "All Age Groups";
        }
        return safeValue;
    }

    /**
     * Attempts to parse and format a date string into a standard format.
     * @param rawDate The raw date string.
     * @return The formatted date string, or the original if parsing fails.
     */
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

    /**
     * Converts a raw date string into millisecond time for sorting.
     * @param rawDate The raw date string.
     * @return The time in milliseconds, or 0 if parsing fails.
     */
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

    /**
     * Safely retrieves a double value from a document snapshot.
     * @param snapshot The document snapshot.
     * @param field The field name.
     * @return The double value, or 0 if not found.
     */
    private double getDouble(DocumentSnapshot snapshot, String field) {
        Double doubleValue = snapshot.getDouble(field);
        if (doubleValue != null) {
            return doubleValue;
        }

        Long longValue = snapshot.getLong(field);
        return longValue != null ? longValue.doubleValue() : 0d;
    }

    /**
     * Safely retrieves an integer value from a document snapshot.
     * @param snapshot The document snapshot.
     * @param field The field name.
     * @return The integer value, or 0 if not found.
     */
    private int getInt(DocumentSnapshot snapshot, String field) {
        Long longValue = snapshot.getLong(field);
        return longValue != null ? longValue.intValue() : 0;
    }

    /**
     * Trims and provides an empty string for null values.
     * @param value The candidate value.
     * @return The trimmed string or empty string.
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Provides a fallback string if the value is null or blank.
     * @param value The candidate value.
     * @param fallback The fallback value.
     * @return The resulting string.
     */
    private String valueOrDefault(String value, String fallback) {
        String safeValue = safe(value);
        return safeValue.isEmpty() ? fallback : safeValue;
    }

    /**
     * Converts an object to its string representation.
     * @param value The object value.
     * @return The string representation or null.
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Picks the first non-blank string from two options.
     * @param first The first choice.
     * @param second The second choice.
     * @return The first non-blank choice or null.
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
