package com.example.lotteryapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shows the entrant-facing event detail screen and drives the join, invite,
 * accept/decline, and comments flows around a single event.
 */
public class EventActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final List<String> eventDetailsList = new ArrayList<>();
    private SimpleTextAdapter adapter;
    private TextView costHeading;
    private TextView eventHeading;
    private TextView statusHeading;
    private TextView helperMessageView;
    private TextView lotteryGuidelinesView;
    private ImageView qrCodeView;
    private ImageView eventImageView;
    private Button btnJoin;
    private Button btnLeave;
    private Button btnAccept;
    private Button btnDecline;
    private Button btnComments;
    private String eventId;
    private String userId;
    private String currentApplicationId;
    private String currentApplicationStatus = "";
    private boolean isInvitedHost;
    private boolean isInvitedParticipant;
    private boolean hasDeclinedParticipantInvite;
    private boolean isPrivateEvent;
    private boolean isOrganizer;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_event);

        userId = DeviceData.getInstance(this).getAccountID();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        RecyclerView recyclerView = findViewById(R.id.event_details);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleTextAdapter(eventDetailsList);
        recyclerView.setAdapter(adapter);

        costHeading = findViewById(R.id.cost_view);
        eventHeading = findViewById(R.id.event_heading);
        statusHeading = findViewById(R.id.tvEventStatus);
        helperMessageView = findViewById(R.id.tvEventHelperMessage);
        lotteryGuidelinesView = findViewById(R.id.tvLotteryGuidelines);
        qrCodeView = findViewById(R.id.event_qr_code);
        eventImageView = findViewById(R.id.event_image);
        btnJoin = findViewById(R.id.btnJoinWaitingList);
        btnLeave = findViewById(R.id.btnLeaveWaitingList);
        btnAccept = findViewById(R.id.btnAcceptSelection);
        btnDecline = findViewById(R.id.btnDeclineSelection);
        btnComments = findViewById(R.id.btnComments);

        ImageButton backButton = findViewById(R.id.back_btn_top);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Unable to open event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnJoin.setOnClickListener(v -> joinWaitingList());
        btnLeave.setOnClickListener(v -> leaveWaitingList());
        btnAccept.setOnClickListener(v -> handleAcceptAction());
        btnDecline.setOnClickListener(v -> handleDeclineAction());
        btnComments.setOnClickListener(v -> openComments());

        setupBottomNav();
        loadEventDetails();
        refreshApplicationState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventDetails();
        refreshApplicationState();
    }

    /**
     * Loads the event document and refreshes the screen-specific state such as
     * private invites, co-host status, and the visible actions.
     */
    private void loadEventDetails() {
        FirestoreHelper.getDb().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    EventModel event = mapEvent(documentSnapshot);
                    if (event == null) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    List<String> invitedHosts = (List<String>) documentSnapshot.get("invitedHosts");
                    List<String> invitedParticipants = (List<String>) documentSnapshot.get("invitedParticipants");
                    List<String> declinedParticipantInvites = (List<String>) documentSnapshot.get("declinedParticipantInvites");
                    isInvitedHost = invitedHosts != null && invitedHosts.contains(userId);
                    isInvitedParticipant = invitedParticipants != null && invitedParticipants.contains(userId);
                    hasDeclinedParticipantInvite = declinedParticipantInvites != null
                            && declinedParticipantInvites.contains(userId);
                    isPrivateEvent = "Private".equalsIgnoreCase(documentSnapshot.getString("visibility"));
                    isOrganizer = userId != null && userId.equals(documentSnapshot.getString("organizerId"));

                    eventDetailsList.clear();
                    if (isInvitedHost) {
                        eventDetailsList.add("YOU ARE A CO-HOST");
                    } else if (isInvitedParticipant && EventFlowRules.normalizeStatus(currentApplicationStatus).isEmpty()) {
                        eventDetailsList.add("YOU HAVE A PRIVATE WAITING LIST INVITE");
                    }
                    eventDetailsList.add("Total Spots: " + event.getTotalSpots());
                    eventDetailsList.add("Current Waiting List: " + event.getWaitingListCount());
                    if (event.getWaitingListLimit() > 0) {
                        eventDetailsList.add("Waiting List Limit: " + event.getWaitingListLimit());
                    }
                    eventDetailsList.add(String.format(Locale.getDefault(), "Price: $%d", (int) event.getPrice()));
                    eventDetailsList.add("Age Group: " + safe(event.getAgeGroup(), "All Age Groups"));
                    eventDetailsList.add("Location: " + safe(event.getLocation(), "Location TBA"));
                    eventDetailsList.add("Date: " + safe(event.getDate(), "Date TBA"));
                    adapter.notifyDataSetChanged();

                    costHeading.setText(String.format(Locale.getDefault(), "$%d", (int) event.getPrice()));
                    eventHeading.setText(safe(event.getName(), "Event"));
                    bindPoster(event.getPosterImage());
                    if (lotteryGuidelinesView != null) {
                        lotteryGuidelinesView.setText(buildLotteryGuidelines(event));
                    }

                    Bitmap qr = QRCodeHelper.generateQRCode(eventId);
                    if (!isPrivateEvent && qr != null) {
                        qrCodeView.setImageBitmap(qr);
                        qrCodeView.setVisibility(View.VISIBLE);
                    } else {
                        qrCodeView.setVisibility(View.GONE);
                    }

                    loadCurrentWaitingCount();
                    updateActionButtons();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

    private EventModel mapEvent(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) {
            return null;
        }

        try {
            EventModel mapped = documentSnapshot.toObject(EventModel.class);
            if (mapped != null) {
                return mapped;
            }
        } catch (Exception ignored) {
        }

        EventModel event = new EventModel();
        event.setWaitingList(new ArrayList<>());
        event.setName(documentSnapshot.getString("name"));
        event.setDate(documentSnapshot.getString("date"));
        event.setLocation(documentSnapshot.getString("location"));
        event.setAgeGroup(documentSnapshot.getString("ageGroup"));
        event.setPosterImage(firstNonBlank(documentSnapshot.getString("posterImage"), documentSnapshot.getString("poster")));

        Double price = documentSnapshot.getDouble("price");
        Long priceLong = documentSnapshot.getLong("price");
        if (price != null) {
            event.setPrice(price);
        } else if (priceLong != null) {
            event.setPrice(priceLong.doubleValue());
        }

        Long spots = documentSnapshot.getLong("totalSpots");
        if (spots != null) {
            event.setTotalSpots(spots.intValue());
        }
        Long waitingListLimit = documentSnapshot.getLong("waitingListLimit");
        if (waitingListLimit != null) {
            event.setWaitingListLimit(waitingListLimit.intValue());
        }
        return event;
    }

    private void bindPoster(String posterValue) {
        if (posterValue != null && !posterValue.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(posterValue, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    eventImageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        eventImageView.setImageResource(R.mipmap.ic_launcher);
    }

    private void refreshApplicationState() {
        if (userId == null || eventId == null) {
            currentApplicationId = null;
            currentApplicationStatus = "";
            updateActionButtons();
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        currentApplicationId = null;
                        currentApplicationStatus = "";
                    } else {
                        DocumentSnapshot appDoc = snap.getDocuments().get(0);
                        currentApplicationId = appDoc.getId();
                        currentApplicationStatus = appDoc.getString("status");
                    }
                    updateActionButtons();
                })
                .addOnFailureListener(e -> {
                    currentApplicationId = null;
                    currentApplicationStatus = "";
                    updateActionButtons();
                });
    }

    /**
     * Starts the entrant join flow after checking role, privacy, and capacity rules.
     */
    private void joinWaitingList() {
        if (userId == null) {
            Toast.makeText(this, "Please log in to join", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isOrganizer) {
            Toast.makeText(this, "Organizers cannot join their own waiting list.", Toast.LENGTH_LONG).show();
            return;
        }
        if (isInvitedHost) {
            Toast.makeText(this, "Co-hosts cannot join the waiting list.", Toast.LENGTH_LONG).show();
            return;
        }
        if (isPrivateEvent && !isInvitedParticipant && EventFlowRules.normalizeStatus(currentApplicationStatus).isEmpty()) {
            Toast.makeText(this, "This private event requires an invitation.", Toast.LENGTH_LONG).show();
            return;
        }

        FirestoreHelper.getDb().collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Long waitingListLimit = documentSnapshot.getLong("waitingListLimit");
                    validateWaitingListCapacity(waitingListLimit, () -> {
                        Boolean verificationRequired = documentSnapshot.getBoolean("geolocationVerification");
                        if (verificationRequired != null && verificationRequired) {
                            List<String> allowedLocations = (List<String>) documentSnapshot.get("geolocationList");
                            checkLocationAndJoin(allowedLocations);
                        } else {
                            tryGetLocationAndJoin();
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to verify event settings", Toast.LENGTH_SHORT).show());
    }

    /**
     * Enforces the optional waiting-list cap before a user is allowed to join.
     */
    private void validateWaitingListCapacity(Long waitingListLimit, Runnable onAllowed) {
        if (waitingListLimit == null || waitingListLimit <= 0) {
            onAllowed.run();
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.size() >= waitingListLimit) {
                        Toast.makeText(this, "This waiting list is full.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    onAllowed.run();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Unable to verify waiting list capacity", Toast.LENGTH_SHORT).show());
    }

    private void tryGetLocationAndJoin() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, this::performJoin);
        } else {
            performJoin(null);
        }
    }

    private void checkLocationAndJoin(List<String> allowedLocations) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location == null) {
                Toast.makeText(this, "Could not determine your location. Please ensure location is enabled.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "My location: " + getAddressString(location), Toast.LENGTH_SHORT).show();
            if (isLocationInAllowedList(location, allowedLocations)) {
                performJoin(location);
            } else {
                Toast.makeText(this, "You must be in an allowed location to join this event.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performJoin(Location location) {
        Map<String, Object> application = new HashMap<>();
        application.put("eventId", eventId);
        application.put("userId", userId);
        application.put("userName", DeviceData.getInstance(this).getUsername());
        application.put("status", "waiting");
        if (location != null) {
            application.put("geoPoint", new GeoPoint(location.getLatitude(), location.getLongitude()));
        }

        FirestoreHelper.getDb().collection("applications")
                .add(application)
                .addOnSuccessListener(ref -> {
                    FirestoreHelper.getDb().collection("events")
                            .document(eventId)
                            .update(
                                    "waitingList", FieldValue.arrayUnion(userId),
                                    "invitedParticipants", FieldValue.arrayRemove(userId),
                                    "declinedParticipantInvites", FieldValue.arrayRemove(userId)
                            )
                            .addOnSuccessListener(unused -> {
                                currentApplicationId = ref.getId();
                                currentApplicationStatus = "waiting";
                                isInvitedParticipant = false;
                                hasDeclinedParticipantInvite = false;
                                updateActionButtons();
                                loadEventDetails();
                                Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                currentApplicationId = ref.getId();
                                currentApplicationStatus = "waiting";
                                isInvitedParticipant = false;
                                hasDeclinedParticipantInvite = false;
                                updateActionButtons();
                                Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show());
    }

    private void leaveWaitingList() {
        if (!EventFlowRules.canLeave(currentApplicationStatus) || currentApplicationId == null) {
            refreshApplicationState();
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .document(currentApplicationId)
                .delete()
                .addOnSuccessListener(unused -> {
                    FirestoreHelper.getDb().collection("events")
                            .document(eventId)
                            .update("waitingList", FieldValue.arrayRemove(userId))
                            .addOnSuccessListener(ignore -> {
                                currentApplicationId = null;
                                currentApplicationStatus = "";
                                updateActionButtons();
                                loadEventDetails();
                                Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                currentApplicationId = null;
                                currentApplicationStatus = "";
                                updateActionButtons();
                                Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to leave", Toast.LENGTH_SHORT).show());
    }

    private void updateApplicationStatus(String newStatus, String successMessage) {
        if (currentApplicationId == null || currentApplicationId.trim().isEmpty()) {
            refreshApplicationState();
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .document(currentApplicationId)
                .update("status", newStatus)
                .addOnSuccessListener(unused -> {
                    currentApplicationStatus = newStatus;
                    updateActionButtons();
                    Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update response", Toast.LENGTH_SHORT).show());
    }

    /**
     * Accept handles two cases: a private waiting-list invite joins the list,
     * while a selected entrant confirms registration.
     */
    private void handleAcceptAction() {
        if ("invited".equals(getEffectiveStatus())) {
            joinWaitingList();
            return;
        }
        updateApplicationStatus("accepted", "Invitation accepted");
    }

    /**
     * Decline handles both private invites and selected-entrant responses.
     */
    private void handleDeclineAction() {
        if ("invited".equals(getEffectiveStatus())) {
            declinePrivateInvite();
            return;
        }
        updateApplicationStatus("declined", "Invitation declined");
    }

    /**
     * Records that a user turned down a private waiting-list invite.
     */
    private void declinePrivateInvite() {
        FirestoreHelper.getDb().collection("events").document(eventId)
                .update(
                        "invitedParticipants", FieldValue.arrayRemove(userId),
                        "declinedParticipantInvites", FieldValue.arrayUnion(userId)
                )
                .addOnSuccessListener(unused -> {
                    isInvitedParticipant = false;
                    hasDeclinedParticipantInvite = true;
                    updateActionButtons();
                    Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to decline invite", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows only the actions that make sense for the current role and status.
     */
    private void updateActionButtons() {
        String effectiveStatus = getEffectiveStatus();
        if (isOrganizer) {
            if (statusHeading != null) {
                statusHeading.setText("Status : ORGANIZER");
            }
            if (helperMessageView != null) {
                helperMessageView.setText("You are organizing this event.");
            }
            btnJoin.setVisibility(View.GONE);
            btnLeave.setVisibility(View.GONE);
            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            return;
        }

        if (statusHeading != null) {
            statusHeading.setText(EventFlowRules.getEventStatusLabel(effectiveStatus, isInvitedHost));
        }
        if (helperMessageView != null) {
            helperMessageView.setText(getHelperMessage());
        }

        boolean showJoin = EventFlowRules.canJoin(effectiveStatus, isInvitedHost)
                && !(isPrivateEvent && !isInvitedParticipant);
        btnJoin.setVisibility(showJoin ? View.VISIBLE : View.GONE);
        btnLeave.setVisibility(EventFlowRules.canLeave(effectiveStatus) ? View.VISIBLE : View.GONE);
        btnAccept.setVisibility(EventFlowRules.canAccept(effectiveStatus) ? View.VISIBLE : View.GONE);
        btnDecline.setVisibility(EventFlowRules.canDecline(effectiveStatus) ? View.VISIBLE : View.GONE);
    }

    private String getHelperMessage() {
        if (isInvitedHost) {
            return "You are a co-host for this event.";
        }
        if (isOrganizer) {
            return "You are organizing this event.";
        }

        switch (EventFlowRules.normalizeStatus(getEffectiveStatus())) {
            case "invited":
                return "This is a private event. Accept the invitation to join the waiting list.";
            case "waiting":
                return "You are on the waiting list.";
            case "selected":
                return "You were selected. Accept or decline your invitation.";
            case "accepted":
                return "You accepted your invitation.";
            case "declined":
                return "You declined your invitation.";
            case "cancelled":
                return "Your invitation was cancelled.";
            case "invite_declined":
                return "You declined this private waiting list invitation.";
            default:
                if (isPrivateEvent) {
                    return "This is a private event. An invitation is required to join.";
                }
                return "Join the waiting list to participate.";
        }
    }

    private String getEffectiveStatus() {
        String normalized = EventFlowRules.normalizeStatus(currentApplicationStatus);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        if (isInvitedParticipant) {
            return "invited";
        }
        if (hasDeclinedParticipantInvite) {
            return "invite_declined";
        }
        return normalized;
    }

    /**
     * Gives entrants a short explanation of how the lottery is meant to work.
     */
    private String buildLotteryGuidelines(EventModel event) {
        int spots = event != null ? event.getTotalSpots() : 0;
        int waitlistCap = event != null ? event.getWaitingListLimit() : 0;
        String capText = waitlistCap > 0
                ? " Waiting list capacity is " + waitlistCap + "."
                : "";
        return "Lottery guideline: the organizer draws randomly from entrants on the waiting list for "
                + spots + " available spot" + (spots == 1 ? "" : "s")
                + ". Selected entrants must accept in the app, and declined or cancelled spots may be filled by a replacement draw."
                + capText;
    }

    private void openComments() {
        CommentsFragment fragment = CommentsFragment.newInstance(eventId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadCurrentWaitingCount() {
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (int i = 0; i < eventDetailsList.size(); i++) {
                        if (eventDetailsList.get(i).startsWith("Current Waiting List:")) {
                            eventDetailsList.set(i, "Current Waiting List: " + snapshot.size());
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                });
    }

    private String getAddressString(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String province = address.getAdminArea();
                if (city != null && province != null) return city + ", " + province;
                if (city != null) return city;
                if (province != null) return province;
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    private boolean isLocationInAllowedList(Location location, List<String> allowedLocations) {
        if (allowedLocations == null || allowedLocations.isEmpty()) return true;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String province = address.getAdminArea();
                String country = address.getCountryName();

                for (String allowed : allowedLocations) {
                    String lowerAllowed = allowed.toLowerCase(Locale.getDefault());
                    if ((city != null && lowerAllowed.contains(city.toLowerCase(Locale.getDefault())))
                            || (province != null && lowerAllowed.contains(province.toLowerCase(Locale.getDefault())))
                            || (country != null && lowerAllowed.contains(country.toLowerCase(Locale.getDefault())))) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                joinWaitingList();
            } else {
                Toast.makeText(this, "Location permission is required to join this event.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navCreate).setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerActivity.class)));
        findViewById(R.id.navHistory).setOnClickListener(v ->
                NavigationHelper.openHistory(this));
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
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

    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final List<String> data;

        SimpleTextAdapter(List<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
