package com.example.lotteryapp;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

    private Button btnRunLottery, btnReplacementDraw, btnCancelNoShows, btnInvite, btnViewEvent, btnMap, btnSendNotif, btnSignUps;
    private EditText etPrice, etDrawDate, etTotalSpots, etDescription;
    private TextView tvParticipantsLabel, tvApplicantCount, tvVisibility;
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
        tvVisibility = findViewById(R.id.tvVisibility);

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
        btnInvite         = findViewById(R.id.btnInvite);
        btnViewEvent      = findViewById(R.id.viewEvent);
        btnMap            = findViewById(R.id.btnGeolocation);
        btnSendNotif      = findViewById(R.id.btnSendNotif);
        btnSignUps        = findViewById(R.id.btnSignUps);


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

        // view event button
        btnViewEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });

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

        // Invite button logic
        btnInvite.setOnClickListener(v -> {
            InviteFragment fragment = InviteFragment.newInstance(eventId);
            fragment.show(getSupportFragmentManager(), "InviteFragment");
        });

        // Export button logic
        findViewById(R.id.btnExport).setOnClickListener(v -> {
            downLoadCSV();
        });

        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });

        btnSendNotif.setOnClickListener(v -> {
            SendNotificationFragment fragment = SendNotificationFragment.newInstance(eventId, eventName);
            fragment.show(getSupportFragmentManager(), "SendNotificationFragment");
        });

        btnSignUps.setOnClickListener(v -> showDeclineInvitationDialog());

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

        loadEventVisibility();
        loadApplicants();
    }

    private void loadEventVisibility() {
        if (eventId == null) return;
        FirestoreHelper.getDb().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String visibility = documentSnapshot.getString("visibility");
                        if (visibility != null) {
                            tvVisibility.setText(visibility.toUpperCase());
                        } else {
                            tvVisibility.setText("PUBLIC"); // default
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvVisibility.setText("ERROR");
                });
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
        List<String> selectedUserIds = new ArrayList<>();
        List<String> remainingWaitingUserIds = new ArrayList<>();
        for (int i = 0; i < spotsToFill; i++) {
            selectedUserIds.add(waiting.get(i).get("userId"));
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(waiting.get(i).get("id")), "status", "selected");
        }
        for (int i = spotsToFill; i < waiting.size(); i++) {
            remainingWaitingUserIds.add(waiting.get(i).get("userId"));
        }
        batch.commit()
                .addOnSuccessListener(v -> {
                    completeStatusUpdate(
                            Tasks.whenAll(
                                    notifySelectedUsers(selectedUserIds),
                                    notifyWaitingUsers(remainingWaitingUserIds)
                            ),
                            spotsToFill + " selected!"
                    );
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
        List<String> selectedUserIds = new ArrayList<>();
        List<String> remainingWaitingUserIds = new ArrayList<>();
        for (int i = 0; i < replacements; i++) {
            selectedUserIds.add(waiting.get(i).get("userId"));
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(waiting.get(i).get("id")), "status", "selected");
        }
        for (int i = replacements; i < waiting.size(); i++) {
            remainingWaitingUserIds.add(waiting.get(i).get("userId"));
        }
        batch.commit()
                .addOnSuccessListener(v -> {
                    completeStatusUpdate(
                            Tasks.whenAll(
                                    notifySelectedUsers(selectedUserIds),
                                    notifyWaitingUsers(remainingWaitingUserIds)
                            ),
                            replacements + " replacements selected!"
                    );
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
        List<String> cancelledUserIds = new ArrayList<>();
        for (Map<String, String> a : noShows) {
            cancelledUserIds.add(a.get("userId"));
            batch.update(FirestoreHelper.getDb().collection("applications")
                    .document(a.get("id")), "status", "cancelled");
        }

        batch.commit()
                .addOnSuccessListener(v -> {
                    completeStatusUpdate(
                            notifyCancelledUsers(cancelledUserIds),
                            noShows.size() + " cancelled"
                    );
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
                    
                    // Also delete comments document
                    batch.delete(FirestoreHelper.getDb().collection("eventComments").document(eventId));

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

    private void downLoadCSV() {
        List<String> usernames = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> phones = new ArrayList<>();
        List<String> emails = new ArrayList<>();

        List<String> selectedUserIds = new ArrayList<>();
        for (Map<String, String> applicant : applicantsList) {
            if ("selected".equals(applicant.get("status"))) {
                selectedUserIds.add(applicant.get("userId"));
            }
        }

        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, "No selected applicants to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        final int totalToFetch = selectedUserIds.size();
        final int[] fetchedCount = {0};

        for (String userId : selectedUserIds) {
            FirestoreHelper.getDb().collection("accounts").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        fetchedCount[0]++;
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            DocumentSnapshot doc = task.getResult();
                            usernames.add(doc.getString("username"));
                            names.add(doc.getString("name"));
                            phones.add(doc.getString("phoneNumber"));
                            emails.add(doc.getString("email"));
                        }

                        if (fetchedCount[0] == totalToFetch) {
                            saveCsvToFile(usernames, names, phones, emails);
                        }
                    });
        }
    }

    private void saveCsvToFile(List<String> usernames, List<String> names, List<String> phones, List<String> emails) {
        StringBuilder csv = new StringBuilder();
        csv.append("Username,Name,Phone,Email\n");
        for (int i = 0; i < usernames.size(); i++) {
            csv.append(usernames.get(i)).append(",")
               .append(names.get(i)).append(",")
               .append(phones.get(i)).append(",")
               .append(emails.get(i)).append("\n");
        }

        String fileName = "Applicants_" + (eventName != null ? eventName.replace(" ", "_") : "Event") + ".csv";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        os.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                        Toast.makeText(this, "Downloaded CSV file of those who won.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Device version not supported for direct download", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("CSV_EXPORT", "Error saving CSV", e);
            Toast.makeText(this, "Failed to save CSV file", Toast.LENGTH_SHORT).show();
        }
    }

    private Task<Void> notifySelectedUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Tasks.forResult(null);
        }

        return NotificationHelper.notifySelected(
                getNotificationSenderId(),
                eventId,
                getNotificationEventName(),
                userIds
        );
    }

    private Task<Void> notifyCancelledUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Tasks.forResult(null);
        }

        return NotificationHelper.notifyCancelled(
                getNotificationSenderId(),
                eventId,
                getNotificationEventName(),
                userIds
        );
    }

    private Task<Void> notifyWaitingUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Tasks.forResult(null);
        }

        return NotificationHelper.notifyWaitingList(
                getNotificationSenderId(),
                eventId,
                getNotificationEventName(),
                userIds
        );
    }

    private String getNotificationSenderId() {
        String organizerId = DeviceData.getInstance(this).getAccountID();
        return organizerId != null && !organizerId.isEmpty()
                ? organizerId
                : "SYSTEM_DEFAULT";
    }

    private String getNotificationEventName() {
        return eventName != null && !eventName.isEmpty()
                ? eventName
                : "this event";
    }

    private void completeStatusUpdate(Task<Void> notificationTask, String successMessage) {
        notificationTask
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                    loadApplicants();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            successMessage + " Notification delivery failed.",
                            Toast.LENGTH_SHORT).show();
                    loadApplicants();
                });
    }

    private void showDeclineInvitationDialog() {
        List<Map<String, String>> selectedApplicants = new ArrayList<>();
        for (Map<String, String> a : applicantsList) {
            if ("selected".equals(a.get("status"))) {
                selectedApplicants.add(a);
            }
        }

        if (selectedApplicants.isEmpty()) {
            Toast.makeText(this, "No pending selected applicants to decline.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[selectedApplicants.size()];
        for (int i = 0; i < selectedApplicants.size(); i++) {
            names[i] = selectedApplicants.get(i).get("userName");
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Applicant to Decline Invitation")
                .setItems(names, (dialog, which) -> {
                    Map<String, String> applicant = selectedApplicants.get(which);
                    declineInvitation(applicant);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void declineInvitation(Map<String, String> applicant) {
        String applicationId = applicant.get("id");
        String userId = applicant.get("userId");

        FirestoreHelper.getDb().collection("applications").document(applicationId)
                .update("status", "cancelled")
                .addOnSuccessListener(v -> {
                    List<String> userIds = Collections.singletonList(userId);
                    completeStatusUpdate(
                            notifyCancelledUsers(userIds),
                            "Invitation for " + applicant.get("userName") + " cancelled."
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to decline invitation", Toast.LENGTH_SHORT).show());
    }
}
