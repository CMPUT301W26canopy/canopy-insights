package com.example.lotteryapp;

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
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Shows applicants for an event, lets organizer run lottery, filter, and delete event
public class ApplicantsActivity extends AppCompatActivity {

    private String eventId, eventDate;
    private int totalSpots;
    private final List<Map<String, String>> applicantsList = new ArrayList<>();
    private final List<Map<String, String>> displayList = new ArrayList<>();
    private RecyclerView.Adapter adapter;
    private Button btnRunLottery, btnReplacementDraw, btnCancelNoShows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicants);

        eventId    = getIntent().getStringExtra("EVENT_ID");
        eventDate  = getIntent().getStringExtra("EVENT_DATE");
        totalSpots = getIntent().getIntExtra("TOTAL_SPOTS", 0);

        ((TextView) findViewById(R.id.tvEventTitle)).setText(getIntent().getStringExtra("EVENT_NAME"));

        btnRunLottery      = findViewById(R.id.btnRunLottery);
        btnReplacementDraw = findViewById(R.id.btnReplacementDraw);
        btnCancelNoShows   = findViewById(R.id.btnCancelNoShows);

        if (isEventDay()) btnRunLottery.setVisibility(View.VISIBLE);

        btnRunLottery.setOnClickListener(v -> runLottery());
        btnReplacementDraw.setOnClickListener(v -> runReplacementDraw());
        btnCancelNoShows.setOnClickListener(v -> cancelNoShows());
        findViewById(R.id.btnDeleteEvent).setOnClickListener(v -> deleteEvent());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnFilterAll).setOnClickListener(v -> filterList("all"));
        findViewById(R.id.btnFilterWaiting).setOnClickListener(v -> filterList("waiting"));
        findViewById(R.id.btnFilterSelected).setOnClickListener(v -> filterList("selected"));
        findViewById(R.id.btnFilterAccepted).setOnClickListener(v -> filterList("accepted"));

        RecyclerView recyclerView = findViewById(R.id.applicantsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                Map<String, String> a = displayList.get(position);
                ((TextView) holder.itemView.findViewById(android.R.id.text1)).setText(a.get("userName"));
                ((TextView) holder.itemView.findViewById(android.R.id.text2)).setText("Status: " + a.get("status"));
            }

            @Override
            public int getItemCount() { return displayList.size(); }
        };
        recyclerView.setAdapter(adapter);

        loadApplicants();
    }

    private void loadApplicants() {
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    applicantsList.clear();
                    boolean lotteryRan = false;
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, String> a = new HashMap<>();
                        a.put("id", doc.getId());
                        a.put("userName", doc.getString("userName"));
                        a.put("userId", doc.getString("userId"));
                        a.put("status", doc.getString("status"));
                        applicantsList.add(a);
                        if ("selected".equals(doc.getString("status"))) lotteryRan = true;
                    }
                    filterList("all");
                    if (lotteryRan) {
                        btnRunLottery.setVisibility(View.GONE);
                        btnReplacementDraw.setVisibility(View.VISIBLE);
                        btnCancelNoShows.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show());
    }

    private void filterList(String status) {
        displayList.clear();
        for (Map<String, String> a : applicantsList)
            if (status.equals("all") || status.equals(a.get("status"))) displayList.add(a);
        adapter.notifyDataSetChanged();
        ((TextView) findViewById(R.id.tvApplicantCount))
                .setText("Showing: " + displayList.size() + " / Total: " + applicantsList.size());
    }

    private void runLottery() {
        List<Map<String, String>> waiting = new ArrayList<>();
        for (Map<String, String> a : applicantsList)
            if ("waiting".equals(a.get("status"))) waiting.add(a);

        if (waiting.isEmpty()) { Toast.makeText(this, "No one waiting", Toast.LENGTH_SHORT).show(); return; }

        Collections.shuffle(waiting);
        int spotsToFill = Math.min(totalSpots, waiting.size());
        WriteBatch batch = FirestoreHelper.getDb().batch();
        for (int i = 0; i < spotsToFill; i++)
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(waiting.get(i).get("id")), "status", "selected");

        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, spotsToFill + " selected!", Toast.LENGTH_SHORT).show();
                    btnRunLottery.setVisibility(View.GONE);
                    btnReplacementDraw.setVisibility(View.VISIBLE);
                    btnCancelNoShows.setVisibility(View.VISIBLE);
                    loadApplicants();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lottery failed", Toast.LENGTH_SHORT).show());
    }

    private void runReplacementDraw() {
        int accepted = 0;
        List<Map<String, String>> waiting = new ArrayList<>();
        for (Map<String, String> a : applicantsList) {
            if ("accepted".equals(a.get("status"))) accepted++;
            if ("waiting".equals(a.get("status"))) waiting.add(a);
        }

        int spotsLeft = totalSpots - accepted;
        if (spotsLeft <= 0) { Toast.makeText(this, "All spots filled!", Toast.LENGTH_SHORT).show(); return; }
        if (waiting.isEmpty()) { Toast.makeText(this, "No more applicants", Toast.LENGTH_SHORT).show(); return; }

        Collections.shuffle(waiting);
        int replacements = Math.min(spotsLeft, waiting.size());
        WriteBatch batch = FirestoreHelper.getDb().batch();
        for (int i = 0; i < replacements; i++)
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(waiting.get(i).get("id")), "status", "selected");

        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, replacements + " replacements selected!", Toast.LENGTH_SHORT).show();
                    loadApplicants();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
    }

    // Marks selected people who never accepted as cancelled (no-shows after deadline)
    private void cancelNoShows() {
        List<Map<String, String>> noShows = new ArrayList<>();
        for (Map<String, String> a : applicantsList)
            if ("selected".equals(a.get("status"))) noShows.add(a);

        if (noShows.isEmpty()) { Toast.makeText(this, "No no-shows", Toast.LENGTH_SHORT).show(); return; }

        WriteBatch batch = FirestoreHelper.getDb().batch();
        for (Map<String, String> a : noShows)
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(a.get("id")), "status", "cancelled");

        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, noShows.size() + " cancelled", Toast.LENGTH_SHORT).show();
                    loadApplicants();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
    }

    private void deleteEvent() {
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = FirestoreHelper.getDb().batch();
                    for (QueryDocumentSnapshot doc : snap) batch.delete(doc.getReference());
                    batch.delete(FirestoreHelper.getDb().collection("events").document(eventId));
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
                });
    }

    private boolean isEventDay() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date eventDateParsed = sdf.parse(eventDate);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            return !cal.getTime().before(eventDateParsed);
        } catch (Exception e) { return false; }
    }
    private void setActiveFilter(Button active) {
        int purple = 0xFF6B5FA6;
        int light  = 0xFFE8E4F3;
        int white  = 0xFFFFFFFF;

        Button[] all = {
                findViewById(R.id.btnFilterAll),
                findViewById(R.id.btnFilterWaiting),
                findViewById(R.id.btnFilterSelected),
                findViewById(R.id.btnFilterAccepted)
        };
        for (Button b : all) {
            b.setBackgroundTintList(ColorStateList.valueOf(light));
            b.setTextColor(purple);
        }
        active.setBackgroundTintList(ColorStateList.valueOf(purple));
        active.setTextColor(white);
    }
}
