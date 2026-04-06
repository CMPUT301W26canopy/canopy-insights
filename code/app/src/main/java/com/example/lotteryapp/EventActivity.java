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
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final List<String> eventDetailsList = new ArrayList<>();
    private SimpleTextAdapter adapter;
    private TextView costHeading;
    private TextView eventHeading;
    private TextView statusHeading;
    private TextView helperMessageView;
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
        btnAccept.setOnClickListener(v -> updateApplicationStatus("accepted", "Invitation accepted"));
        btnDecline.setOnClickListener(v -> updateApplicationStatus("declined", "Invitation declined"));
        btnComments.setOnClickListener(v -> openComments());

        setupBottomNav();
        loadEventDetails();
        refreshApplicationState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshApplicationState();
    }

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
                    isInvitedHost = invitedHosts != null && invitedHosts.contains(userId);

                    eventDetailsList.clear();
                    if (isInvitedHost) {
                        eventDetailsList.add("YOU ARE A CO-HOST");
                    }
                    eventDetailsList.add("Total Spots: " + event.getTotalSpots());
                    eventDetailsList.add("Current Waiting List: " + event.getWaitingListCount());
                    eventDetailsList.add(String.format(Locale.getDefault(), "Price: $%d", (int) event.getPrice()));
                    eventDetailsList.add("Age Group: " + safe(event.getAgeGroup(), "All Age Groups"));
                    eventDetailsList.add("Location: " + safe(event.getLocation(), "Location TBA"));
                    eventDetailsList.add("Date: " + safe(event.getDate(), "Date TBA"));
                    adapter.notifyDataSetChanged();

                    costHeading.setText(String.format(Locale.getDefault(), "$%d", (int) event.getPrice()));
                    eventHeading.setText(safe(event.getName(), "Event"));
                    bindPoster(event.getPosterImage());

                    Bitmap qr = QRCodeHelper.generateQRCode(eventId);
                    if (qr != null) {
                        qrCodeView.setImageBitmap(qr);
                        qrCodeView.setVisibility(View.VISIBLE);
                    }

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

    private void joinWaitingList() {
        if (userId == null) {
            Toast.makeText(this, "Please log in to join", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isInvitedHost) {
            Toast.makeText(this, "Co-hosts cannot join the waiting list.", Toast.LENGTH_LONG).show();
            return;
        }

        FirestoreHelper.getDb().collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean verificationRequired = documentSnapshot.getBoolean("geolocationVerification");
                    if (verificationRequired != null && verificationRequired) {
                        List<String> allowedLocations = (List<String>) documentSnapshot.get("geolocationList");
                        checkLocationAndJoin(allowedLocations);
                    } else {
                        tryGetLocationAndJoin();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to verify event settings", Toast.LENGTH_SHORT).show());
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
                    currentApplicationId = ref.getId();
                    currentApplicationStatus = "waiting";
                    updateActionButtons();
                    Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
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
                    currentApplicationId = null;
                    currentApplicationStatus = "";
                    updateActionButtons();
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
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

    private void updateActionButtons() {
        if (statusHeading != null) {
            statusHeading.setText(EventFlowRules.getEventStatusLabel(currentApplicationStatus, isInvitedHost));
        }
        if (helperMessageView != null) {
            helperMessageView.setText(getHelperMessage());
        }

        btnJoin.setVisibility(EventFlowRules.canJoin(currentApplicationStatus, isInvitedHost) ? View.VISIBLE : View.GONE);
        btnLeave.setVisibility(EventFlowRules.canLeave(currentApplicationStatus) ? View.VISIBLE : View.GONE);
        btnAccept.setVisibility(EventFlowRules.canAccept(currentApplicationStatus) ? View.VISIBLE : View.GONE);
        btnDecline.setVisibility(EventFlowRules.canDecline(currentApplicationStatus) ? View.VISIBLE : View.GONE);
    }

    private String getHelperMessage() {
        if (isInvitedHost) {
            return "You are a co-host for this event.";
        }

        switch (EventFlowRules.normalizeStatus(currentApplicationStatus)) {
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
            default:
                return "Join the waiting list to participate.";
        }
    }

    private void openComments() {
        CommentsFragment fragment = CommentsFragment.newInstance(eventId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
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
