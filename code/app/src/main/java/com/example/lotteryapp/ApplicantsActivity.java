package com.example.lotteryapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicantsActivity extends AppCompatActivity {

    private String eventId, eventName, eventDate;
    private int totalSpots;

    private final List<Map<String, String>> applicantsList = new ArrayList<>();
    private final List<Map<String, String>> displayList    = new ArrayList<>();
    private RecyclerView.Adapter adapter;

    private Button btnRunLottery, btnReplacementDraw, btnCancelNoShows;
    private EditText etPrice, etDrawDate, etTotalSpots, etDescription;
    private TextView tvParticipantsLabel, tvApplicantCount;
    private View participantsContainer;
    private boolean participantsExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicants);

        eventId    = getIntent().getStringExtra("EVENT_ID");
        eventName  = getIntent().getStringExtra("EVENT_NAME");
        eventDate  = getIntent().getStringExtra("EVENT_DATE");
        totalSpots = getIntent().getIntExtra("TOTAL_SPOTS", 0);
        double price = getIntent().getDoubleExtra("PRICE", 0);
        String description = getIntent().getStringExtra("DESCRIPTION");

        // bind views
        ((TextView) findViewById(R.id.tvEventTitle)).setText(eventName);
        ((TextView) findViewById(R.id.tvEventDate)).setText(eventDate);

        etPrice           = findViewById(R.id.etPrice);
        etDrawDate        = findViewById(R.id.etDrawDate);
        etTotalSpots      = findViewById(R.id.etTotalSpots);
        etDescription     = findViewById(R.id.etDescription);
        tvParticipantsLabel  = findViewById(R.id.tvParticipantsLabel);
        tvApplicantCount     = findViewById(R.id.tvApplicantCount);
        participantsContainer = findViewById(R.id.participantsContainer);
        btnRunLottery     = findViewById(R.id.btnRunLottery);
        btnReplacementDraw = findViewById(R.id.btnReplacementDraw);
        btnCancelNoShows  = findViewById(R.id.btnCancelNoShows);

        // populate editable fields
        etPrice.setText(String.valueOf((int) price));
        etDrawDate.setText(eventDate != null ? eventDate : "");
        etTotalSpots.setText(String.valueOf(totalSpots));
        etDescription.setText(description != null ? description : "");

        // clear buttons
        findViewById(R.id.btnClearPrice).setOnClickListener(v -> etPrice.setText(""));
        findViewById(R.id.btnClearDrawDate).setOnClickListener(v -> etDrawDate.setText(""));
        findViewById(R.id.btnClearSpots).setOnClickListener(v -> etTotalSpots.setText(""));
        findViewById(R.id.btnClearDescription).setOnClickListener(v -> etDescription.setText(""));

        // back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // three dot menu — save changes
        findViewById(R.id.btnMenu).setOnClickListener(v -> saveChanges());

        // participants expand/collapse
        findViewById(R.id.rowParticipants).setOnClickListener(v -> toggleParticipants());

        // filter buttons
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> {
            filterList("all");
            setActiveFilter((Button) findViewById(R.id.btnFilterAll));
        });
        findViewById(R.id.btnFilterWaiting).setOnClickListener(v -> {
            filterList("waiting");
            setActiveFilter((Button) findViewById(R.id.btnFilterWaiting));
        });
        findViewById(R.id.btnFilterSelected).setOnClickListener(v -> {
            filterList("selected");
            setActiveFilter((Button) findViewById(R.id.btnFilterSelected));
        });
        findViewById(R.id.btnFilterAccepted).setOnClickListener(v -> {
            filterList("accepted");
            setActiveFilter((Button) findViewById(R.id.btnFilterAccepted));
        });

        // action buttons
        btnRunLottery.setOnClickListener(v -> runLottery());
        btnReplacementDraw.setOnClickListener(v -> runReplacementDraw());
        btnCancelNoShows.setOnClickListener(v -> cancelNoShows());
        findViewById(R.id.btnDeleteEvent).setOnClickListener(v -> deleteEvent());

        // recycler
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
                ((TextView) holder.itemView.findViewById(android.R.id.text2))
                        .setText("Status: " + a.get("status"));
            }
            @Override
            public int getItemCount() { return displayList.size(); }
        };
        recyclerView.setAdapter(adapter);

        loadApplicants();
    }

    private void toggleParticipants() {
        participantsExpanded = !participantsExpanded;
        participantsContainer.setVisibility(participantsExpanded ? View.VISIBLE : View.GONE);
        ((android.widget.ImageView) findViewById(R.id.ivExpandIcon))
                .setRotation(participantsExpanded ? 90 : -90);
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
                        a.put("id",       doc.getId());
                        a.put("userName", doc.getString("userName"));
                        a.put("userId",   doc.getString("userId"));
                        a.put("status",   doc.getString("status"));
                        applicantsList.add(a);
                        if ("selected".equals(doc.getString("status"))) lotteryRan = true;
                    }
                    tvParticipantsLabel.setText("PARTICIPANTS | applicants: " + applicantsList.size());
                    filterList("all");
                    setActiveFilter((Button) findViewById(R.id.btnFilterAll));

                    if (lotteryRan) {
                        btnRunLottery.setVisibility(View.GONE);
                        btnReplacementDraw.setVisibility(View.VISIBLE);
                        btnCancelNoShows.setVisibility(View.VISIBLE);
                    } else {
                        btnRunLottery.setVisibility(View.VISIBLE);
                        btnReplacementDraw.setVisibility(View.GONE);
                        btnCancelNoShows.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load applicants", Toast.LENGTH_SHORT).show());
    }

    private void saveChanges() {
        String priceStr = etPrice.getText().toString().trim();
        String drawDate = etDrawDate.getText().toString().trim();
        String spotsStr = etTotalSpots.getText().toString().trim();
        String desc     = etDescription.getText().toString().trim();

        if (priceStr.isEmpty() || spotsStr.isEmpty()) {
            Toast.makeText(this, "Price and spots cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("price",       Double.parseDouble(priceStr));
        updates.put("date",        drawDate);
        updates.put("totalSpots",  Integer.parseInt(spotsStr));
        updates.put("description", desc);

        FirestoreHelper.getDb().collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Event updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
    }

    private void filterList(String status) {
        displayList.clear();
        for (Map<String, String> a : applicantsList)
            if (status.equals("all") || status.equals(a.get("status"))) displayList.add(a);
        adapter.notifyDataSetChanged();
        tvApplicantCount.setText("Showing: " + displayList.size() + " / Total: " + applicantsList.size());
    }

    private void runLottery() {
        String spotsStr = etTotalSpots.getText().toString().trim();
        int spots = spotsStr.isEmpty() ? totalSpots : Integer.parseInt(spotsStr);

        List<Map<String, String>> waiting = new ArrayList<>();
        for (Map<String, String> a : applicantsList)
            if ("waiting".equals(a.get("status"))) waiting.add(a);

        if (waiting.isEmpty()) {
            Toast.makeText(this, "No one on the waiting list", Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.shuffle(waiting);
        int spotsToFill = Math.min(spots, waiting.size());
        WriteBatch batch = FirestoreHelper.getDb().batch();
        for (int i = 0; i < spotsToFill; i++) {
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(waiting.get(i).get("id")), "status", "selected");
        }
        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, spotsToFill + " selected!", Toast.LENGTH_SHORT).show();
                    loadApplicants();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lottery failed", Toast.LENGTH_SHORT).show());
    }

    private void runReplacementDraw() {
        int accepted = 0;
        List<Map<String, String>> waiting = new ArrayList<>();
        for (Map<String, String> a : applicantsList) {
            if ("accepted".equals(a.get("status"))) accepted++;
            if ("waiting".equals(a.get("status")))  waiting.add(a);
        }
        String spotsStr = etTotalSpots.getText().toString().trim();
        int spots = spotsStr.isEmpty() ? totalSpots : Integer.parseInt(spotsStr);
        int spotsLeft = spots - accepted;

        if (spotsLeft <= 0) {
            Toast.makeText(this, "All spots filled!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (waiting.isEmpty()) {
            Toast.makeText(this, "No more applicants", Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.shuffle(waiting);
        int replacements = Math.min(spotsLeft, waiting.size());
        WriteBatch batch = FirestoreHelper.getDb().batch();
        for (int i = 0; i < replacements; i++) {
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(waiting.get(i).get("id")), "status", "selected");
        }
        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, replacements + " replacements selected!", Toast.LENGTH_SHORT).show();
                    loadApplicants();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Replacement draw failed", Toast.LENGTH_SHORT).show());
    }

    private void cancelNoShows() {
        List<Map<String, String>> noShows = new ArrayList<>();
        for (Map<String, String> a : applicantsList)
            if ("selected".equals(a.get("status"))) noShows.add(a);

        if (noShows.isEmpty()) {
            Toast.makeText(this, "No pending selected applicants", Toast.LENGTH_SHORT).show();
            return;
        }
        WriteBatch batch = FirestoreHelper.getDb().batch();
        for (Map<String, String> a : noShows)
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(a.get("id")), "status", "cancelled");

        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, noShows.size() + " cancelled", Toast.LENGTH_SHORT).show();
                    loadApplicants();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to cancel", Toast.LENGTH_SHORT).show());
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
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
                });
    }

    private void setActiveFilter(Button active) {
        Button[] all = {
                findViewById(R.id.btnFilterAll),
                findViewById(R.id.btnFilterWaiting),
                findViewById(R.id.btnFilterSelected),
                findViewById(R.id.btnFilterAccepted)
        };
        for (Button b : all) {
            b.setBackgroundTintList(ColorStateList.valueOf(0xFFE8E4F3));
            b.setTextColor(0xFF6B5FA6);
        }
        active.setBackgroundTintList(ColorStateList.valueOf(0xFF6B5FA6));
        active.setTextColor(0xFFFFFFFF);
    }
}