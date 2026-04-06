package com.example.lotteryapp;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Organizer-facing event management screen for editing event details,
 * reviewing applicants, running lottery actions, and exporting results.
 */
public class ApplicantsActivity extends AppCompatActivity {

    private String eventId, eventName, eventDate;
    private int totalSpots;

    private final List<Map<String, String>> applicantsList = new ArrayList<>();
    private final List<Map<String, String>> displayList    = new ArrayList<>();
    private RecyclerView.Adapter adapter;

    private Button btnRunLottery, btnReplacementDraw, btnCancelNoShows, btnInvite, btnViewEvent, btnMap, btnChangePoster;
    private EditText etEventName, etLocation, etPrice, etDrawDate, etTotalSpots, etWaitingListLimit, etDescription;
    private TextView tvEventTitle, tvEventDate, tvParticipantsLabel, tvApplicantCount, tvVisibility, tvEventPhotoPlaceholder;
    private ImageView eventPhotoView;
    private View participantsContainer;
    private boolean participantsExpanded = false;
    private String selectedPosterBase64;
    private ActivityResultLauncher<String> pickImageLauncher;

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
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvEventTitle.setText(eventName);
        tvEventDate.setText(eventDate);
        tvVisibility = findViewById(R.id.tvVisibility);
        eventPhotoView = findViewById(R.id.ivEventPhoto);
        tvEventPhotoPlaceholder = findViewById(R.id.tvEventPhotoPlaceholder);
        btnChangePoster = findViewById(R.id.btnChangePoster);

        etEventName       = findViewById(R.id.etEventName);
        etLocation        = findViewById(R.id.etLocation);
        etPrice           = findViewById(R.id.etPrice);
        etDrawDate        = findViewById(R.id.etDrawDate);
        etTotalSpots      = findViewById(R.id.etTotalSpots);
        etWaitingListLimit = findViewById(R.id.etWaitingListLimit);
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


        // populate editable fields
        etEventName.setText(eventName != null ? eventName : "");
        etPrice.setText(String.valueOf((int) price));
        etDrawDate.setText(eventDate != null ? eventDate : "");
        etTotalSpots.setText(String.valueOf(totalSpots));
        etDescription.setText(description != null ? description : "");

        // clear buttons
        findViewById(R.id.btnClearEventName).setOnClickListener(v -> etEventName.setText(""));
        findViewById(R.id.btnClearLocation).setOnClickListener(v -> etLocation.setText(""));
        findViewById(R.id.btnClearPrice).setOnClickListener(v -> etPrice.setText(""));
        findViewById(R.id.btnClearDrawDate).setOnClickListener(v -> etDrawDate.setText(""));
        findViewById(R.id.btnClearSpots).setOnClickListener(v -> etTotalSpots.setText(""));
        findViewById(R.id.btnClearWaitingListLimit).setOnClickListener(v -> etWaitingListLimit.setText(""));
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
        tvVisibility.setOnClickListener(v -> toggleVisibility());
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePosterSelection);
        btnChangePoster.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

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

        // recycler
        RecyclerView recyclerView = findViewById(R.id.applicantsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_applicant, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                Map<String, String> a = displayList.get(position);
                TextView titleView = holder.itemView.findViewById(R.id.tvApplicantName);
                TextView subtitleView = holder.itemView.findViewById(R.id.tvApplicantMeta);
                ImageView avatarView = holder.itemView.findViewById(R.id.ivApplicantAvatar);
                String displayName = firstNonBlank(a.get("name"), a.get("userName"));
                String username = firstNonBlank(a.get("userName"), a.get("email"));
                titleView.setText(firstNonBlank(displayName, "Unknown entrant"));
                subtitleView.setText(firstNonBlank(username, "No username")
                        + "  |  Status: " + formatStatus(a.get("status")));
                bindAvatar(avatarView, a.get("profileImage"));
                styleSimpleListRow(holder.itemView, titleView, subtitleView);
            }
            @Override
            public int getItemCount() { return displayList.size(); }
        };
        recyclerView.setAdapter(adapter);

        loadEventVisibility();
        loadApplicants();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventVisibility();
        loadApplicants();
    }

    private void loadEventVisibility() {
        if (eventId == null) return;
        FirestoreHelper.getDb().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = firstNonBlank(documentSnapshot.getString("name"), eventName);
                        String date = firstNonBlank(documentSnapshot.getString("date"), eventDate);
                        String location = documentSnapshot.getString("location");
                        String description = documentSnapshot.getString("description");
                        String visibility = normalizeVisibility(documentSnapshot.getString("visibility"));
                        Long spotsValue = documentSnapshot.getLong("totalSpots");
                        Long waitingListLimitValue = documentSnapshot.getLong("waitingListLimit");
                        Double priceValue = documentSnapshot.getDouble("price");
                        if (priceValue == null) {
                            Long priceLong = documentSnapshot.getLong("price");
                            if (priceLong != null) {
                                priceValue = priceLong.doubleValue();
                            }
                        }

                        selectedPosterBase64 = firstNonBlank(
                                asString(documentSnapshot.get("posterImage")),
                                asString(documentSnapshot.get("poster"))
                        );
                        bindPoster(selectedPosterBase64);

                        eventName = name;
                        eventDate = date;
                        if (spotsValue != null) {
                            totalSpots = spotsValue.intValue();
                        }

                        tvEventTitle.setText(name != null ? name : "");
                        tvEventDate.setText(date != null ? date : "");
                        tvVisibility.setText(visibility);
                        etEventName.setText(name != null ? name : "");
                        etLocation.setText(location != null ? location : "");
                        etDrawDate.setText(date != null ? date : "");
                        etDescription.setText(description != null ? description : "");
                        if (spotsValue != null) {
                            etTotalSpots.setText(String.valueOf(spotsValue.intValue()));
                        }
                        if (waitingListLimitValue != null && waitingListLimitValue > 0) {
                            etWaitingListLimit.setText(String.valueOf(waitingListLimitValue.intValue()));
                        } else {
                            etWaitingListLimit.setText("");
                        }
                        if (priceValue != null) {
                            etPrice.setText(priceValue % 1 == 0
                                    ? String.valueOf(priceValue.intValue())
                                    : String.valueOf(priceValue));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    bindPoster(null);
                    tvVisibility.setText("ERROR");
                });
    }

    private void toggleParticipants() {
        participantsExpanded = !participantsExpanded;
        participantsContainer.setVisibility(participantsExpanded ? View.VISIBLE : View.GONE);
        ((android.widget.ImageView) findViewById(R.id.ivExpandIcon))
                .setRotation(participantsExpanded ? 90 : -90);
    }

    /**
     * Loads the current event's applications and refreshes the organizer actions
     * based on the live waiting, selected, and accepted counts.
     */
    private void loadApplicants() {
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    applicantsList.clear();
                    int waitingCount = 0;
                    int selectedCount = 0;
                    int acceptedCount = 0;
                    boolean hasProgressedApplications = false;
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, String> a = new HashMap<>();
                        a.put("id",       doc.getId());
                        a.put("userName", doc.getString("userName"));
                        a.put("userId",   doc.getString("userId"));
                        a.put("status",   doc.getString("status"));
                        applicantsList.add(a);

                        String status = doc.getString("status");
                        if ("waiting".equals(status)) {
                            waitingCount++;
                        } else if ("selected".equals(status)) {
                            selectedCount++;
                            hasProgressedApplications = true;
                        } else if ("accepted".equals(status)) {
                            acceptedCount++;
                            hasProgressedApplications = true;
                        } else if (status != null && !status.trim().isEmpty()) {
                            hasProgressedApplications = true;
                        }
                    }
                    tvParticipantsLabel.setText("PARTICIPANTS | applicants: " + applicantsList.size());
                    hydrateApplicantProfiles();
                    filterList("all");
                    setActiveFilter((Button) findViewById(R.id.btnFilterAll));

                    int spots = getConfiguredTotalSpots();
                    boolean canRunReplacement = hasProgressedApplications
                            && waitingCount > 0
                            && (acceptedCount + selectedCount) < spots;

                    btnRunLottery.setVisibility(hasProgressedApplications ? View.GONE : View.VISIBLE);
                    btnReplacementDraw.setVisibility(canRunReplacement ? View.VISIBLE : View.GONE);
                    btnCancelNoShows.setVisibility(selectedCount > 0 ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load applicants", Toast.LENGTH_SHORT).show());
    }

    /**
     * Saves the organizer's event edits back to Firestore.
     */
    private void saveChanges() {
        String name     = etEventName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String drawDate = etDrawDate.getText().toString().trim();
        String spotsStr = etTotalSpots.getText().toString().trim();
        String waitingListLimitStr = etWaitingListLimit.getText().toString().trim();
        String desc     = etDescription.getText().toString().trim();
        String visibility = normalizeVisibility(tvVisibility.getText().toString());

        if (name.isEmpty() || location.isEmpty() || priceStr.isEmpty() || spotsStr.isEmpty()) {
            Toast.makeText(this, "Name, location, price and spots cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        double parsedPrice;
        int parsedSpots;
        int parsedWaitingListLimit = 0;
        try {
            parsedPrice = Double.parseDouble(priceStr);
            parsedSpots = Integer.parseInt(spotsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter valid price and spots", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!waitingListLimitStr.isEmpty()) {
            try {
                parsedWaitingListLimit = Integer.parseInt(waitingListLimitStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter a valid waiting list limit", Toast.LENGTH_SHORT).show();
                return;
            }
            if (parsedWaitingListLimit < 0) {
                Toast.makeText(this, "Waiting list limit cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",        name);
        updates.put("location",    location);
        updates.put("price",       parsedPrice);
        updates.put("date",        drawDate);
        updates.put("totalSpots",  parsedSpots);
        updates.put("waitingListLimit", parsedWaitingListLimit);
        updates.put("description", desc);
        updates.put("visibility",  visibility);
        if (selectedPosterBase64 != null && !selectedPosterBase64.trim().isEmpty()) {
            updates.put("posterImage", selectedPosterBase64);
            updates.put("poster", selectedPosterBase64);
        }

        FirestoreHelper.getDb().collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    eventName = name;
                    eventDate = drawDate;
                    totalSpots = parsedSpots;
                    tvEventTitle.setText(name);
                    tvEventDate.setText(drawDate);
                    tvVisibility.setText(visibility);
                    Toast.makeText(this, "Event updated!", Toast.LENGTH_SHORT).show();
                })
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

    /**
     * Randomly promotes waiting entrants into the selected state for the initial draw.
     */
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
                                    removeUsersFromWaitingList(selectedUserIds),
                                    notifySelectedUsers(selectedUserIds),
                                    notifyWaitingUsers(remainingWaitingUserIds)
                            ),
                            spotsToFill + " selected!"
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lottery failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Fills any newly-opened spots from the waiting list after declines or cancellations.
     */
    private void runReplacementDraw() {
        int accepted = 0;
        int selectedPending = 0;
        List<Map<String, String>> waiting = new ArrayList<>();
        for (Map<String, String> a : applicantsList) {
            if ("accepted".equals(a.get("status"))) accepted++;
            if ("selected".equals(a.get("status"))) selectedPending++;
            if ("waiting".equals(a.get("status")))  waiting.add(a);
        }
        String spotsStr = etTotalSpots.getText().toString().trim();
        int spots = spotsStr.isEmpty() ? totalSpots : Integer.parseInt(spotsStr);
        int spotsLeft = spots - accepted - selectedPending;

        if (spotsLeft <= 0) {
            Toast.makeText(this, "No open spots for replacement draw yet", Toast.LENGTH_SHORT).show();
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
                                    removeUsersFromWaitingList(selectedUserIds),
                                    notifySelectedUsers(selectedUserIds),
                                    notifyWaitingUsers(remainingWaitingUserIds)
                            ),
                            replacements + " replacements selected!"
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Replacement draw failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Cancels still-pending selected entrants who never accepted their invitation.
     */
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
        List<Map<String, String>> acceptedApplicants = new ArrayList<>();
        for (Map<String, String> applicant : applicantsList) {
            if ("accepted".equals(applicant.get("status"))) {
                acceptedApplicants.add(applicant);
            }
        }

        if (acceptedApplicants.isEmpty()) {
            Toast.makeText(this, "No accepted entrants to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, String>> csvRows = new ArrayList<>();
        final int totalToFetch = acceptedApplicants.size();
        final int[] fetchedCount = {0};

        for (Map<String, String> applicant : acceptedApplicants) {
            String userId = applicant.get("userId");
            FirestoreHelper.getDb().collection("accounts").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        fetchedCount[0]++;
                        Map<String, String> row = new HashMap<>();
                        row.put("username", applicant.get("userName"));
                        row.put("name", "");
                        row.put("phone", "");
                        row.put("email", "");
                        row.put("status", "accepted");

                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            DocumentSnapshot doc = task.getResult();
                            row.put("username", firstNonBlank(doc.getString("username"), applicant.get("userName")));
                            row.put("name", doc.getString("name"));
                            row.put("phone", doc.getString("phoneNumber"));
                            row.put("email", doc.getString("email"));
                        }
                        csvRows.add(row);

                        if (fetchedCount[0] == totalToFetch) {
                            saveCsvToFile(csvRows);
                        }
                    });
        }
    }

    private void saveCsvToFile(List<Map<String, String>> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("Username,Name,Phone,Email,Status\n");
        for (Map<String, String> row : rows) {
            csv.append(csvValue(row.get("username"))).append(",")
               .append(csvValue(row.get("name"))).append(",")
               .append(csvValue(row.get("phone"))).append(",")
               .append(csvValue(row.get("email"))).append(",")
               .append(csvValue(row.get("status"))).append("\n");
        }

        String safeEventName = eventName != null && !eventName.trim().isEmpty()
                ? eventName.trim().replaceAll("[^a-zA-Z0-9-_]+", "_")
                : "Event";
        String fileName = "Final_Entrants_" + safeEventName + ".csv";

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
                        Toast.makeText(this, "Final entrants CSV saved.", Toast.LENGTH_SHORT).show();
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

    private Task<Void> removeUsersFromWaitingList(List<String> userIds) {
        if (eventId == null || eventId.trim().isEmpty() || userIds == null || userIds.isEmpty()) {
            return Tasks.forResult(null);
        }

        return FirestoreHelper.getDb().collection("events")
                .document(eventId)
                .update("waitingList", FieldValue.arrayRemove(userIds.toArray()))
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return Tasks.forResult(null);
                    }

                    Exception exception = task.getException();
                    return Tasks.forException(exception != null
                            ? exception
                            : new IllegalStateException("Failed to update waiting list"));
                });
    }

    /**
     * Enriches applicant rows with profile details so the organizer list can
     * show names, usernames, and avatars instead of only application data.
     */
    private void hydrateApplicantProfiles() {
        for (Map<String, String> applicant : applicantsList) {
            String userId = applicant.get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                continue;
            }

            FirestoreHelper.getDb().collection("accounts").document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            return;
                        }
                        applicant.put("name", firstNonBlank(doc.getString("name"), applicant.get("userName")));
                        applicant.put("userName", firstNonBlank(doc.getString("username"), applicant.get("userName")));
                        applicant.put("email", doc.getString("email"));
                        applicant.put("profileImage", doc.getString("profileImage"));
                        adapter.notifyDataSetChanged();
                    });
        }
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

    private void bindPoster(String posterBase64) {
        if (eventPhotoView == null) {
            return;
        }

        if (posterBase64 != null && !posterBase64.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(posterBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    eventPhotoView.setImageBitmap(bitmap);
                    if (tvEventPhotoPlaceholder != null) {
                        tvEventPhotoPlaceholder.setVisibility(View.GONE);
                    }
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        eventPhotoView.setImageDrawable(null);
        if (tvEventPhotoPlaceholder != null) {
            tvEventPhotoPlaceholder.setVisibility(View.VISIBLE);
        }
    }

    private void bindAvatar(ImageView imageView, String profileImageBase64) {
        if (imageView == null) {
            return;
        }

        if (profileImageBase64 != null && !profileImageBase64.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(profileImageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        imageView.setImageResource(R.drawable.ic_person);
    }

    private void handlePosterSelection(Uri uri) {
        if (uri == null) {
            return;
        }

        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            byte[] imageBytes = readBytes(inputStream);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) {
                Toast.makeText(this, "Unable to read image", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap rotatedBitmap = applyExifRotation(bitmap, imageBytes);
            Bitmap portraitBitmap = cropToPortrait(rotatedBitmap, 4f / 5f);
            Bitmap scaledBitmap = scaleBitmap(portraitBitmap, 1400);
            selectedPosterBase64 = encodeBitmap(scaledBitmap);
            bindPoster(selectedPosterBase64);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private int getConfiguredTotalSpots() {
        String spotsStr = etTotalSpots != null ? etTotalSpots.getText().toString().trim() : "";
        if (!spotsStr.isEmpty()) {
            try {
                return Integer.parseInt(spotsStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return Math.max(totalSpots, 0);
    }

    private String formatStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Unknown";
        }

        String normalized = status.trim().toLowerCase();
        String first = normalized.substring(0, 1).toUpperCase();
        String rest = normalized.substring(1);
        return first + rest;
    }

    private String csvValue(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private void toggleVisibility() {
        tvVisibility.setText("Private".equalsIgnoreCase(tvVisibility.getText().toString())
                ? "Public"
                : "Private");
    }

    private String normalizeVisibility(String rawValue) {
        return "Private".equalsIgnoreCase(rawValue) ? "Private" : "Public";
    }

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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }

    private Bitmap applyExifRotation(Bitmap bitmap, byte[] imageBytes) {
        try {
            ExifInterface exifInterface = new ExifInterface(new ByteArrayInputStream(imageBytes));
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            return bitmap;
        }
    }

    private Bitmap cropToPortrait(Bitmap bitmap, float targetRatio) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return bitmap;
        }

        float currentRatio = (float) width / (float) height;
        if (Math.abs(currentRatio - targetRatio) < 0.02f) {
            return bitmap;
        }

        int cropWidth = width;
        int cropHeight = height;
        if (currentRatio > targetRatio) {
            cropWidth = Math.round(height * targetRatio);
        } else {
            cropHeight = Math.round(width / targetRatio);
        }

        int left = Math.max((width - cropWidth) / 2, 0);
        int top = Math.max((height - cropHeight) / 2, 0);
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight);
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxDimension) {
            return bitmap;
        }

        float ratio = (float) maxDimension / longestSide;
        int scaledWidth = Math.round(width * ratio);
        int scaledHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
    }

    private String encodeBitmap(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
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
