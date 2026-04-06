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
<<<<<<< HEAD
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
=======
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
>>>>>>> main
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
<<<<<<< HEAD
import java.lang.StringBuilder;
=======
>>>>>>> main

public class EventActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final List<String> eventDetailsList = new ArrayList<>();

    private SimpleTextAdapter adapter;
<<<<<<< HEAD
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

    private DeviceData deviceData;
    private FusedLocationProviderClient fusedLocationClient;

    private String eventId;
    private String userId;
    private String currentApplicationId;
    private String currentApplicationStatus = "";
    private boolean isInvitedHost;

    private EventModel currentEvent;
    private int waitingCount;
    private int selectedCount;
    private int acceptedCount;

    @Override
    protected void onResume() {
        super.onResume();
        if (deviceData != null) {
            userId = deviceData.getAccountID();
        }
        if (eventId != null && !eventId.trim().isEmpty()) {
            loadEventDetails(eventId);
        }
    }
=======
    private TextView costHeading, eventHeading;
    private ImageView qrCodeView, eventImageView;
    private Button btnJoin, btnLeave, btnComments;
    private String eventId, userId;
    private boolean isOnWaitingList = false;
    private boolean isInvitedHost = false;
    
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
>>>>>>> main

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_event);

<<<<<<< HEAD
        deviceData = DeviceData.getInstance(this);
        userId = deviceData.getAccountID();
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

        if (btnJoin != null) {
            btnJoin.setOnClickListener(v -> joinWaitingList());
        }
        if (btnLeave != null) {
            btnLeave.setOnClickListener(v -> leaveWaitingList());
        }
        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> acceptSelection());
        }
        if (btnDecline != null) {
            btnDecline.setOnClickListener(v -> declineSelection());
        }
        if (btnComments != null) {
            btnComments.setOnClickListener(v -> openComments());
=======
        DeviceData deviceData = DeviceData.getInstance(this);
        userId = deviceData.getAccountID();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        RecyclerView eventDescDisplay = findViewById(R.id.event_details);
        if (eventDescDisplay != null) {
            eventDescDisplay.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SimpleTextAdapter(eventDetailsList);
            eventDescDisplay.setAdapter(adapter);
        }

        costHeading = findViewById(R.id.cost_view);
        eventHeading = findViewById(R.id.event_heading);
        btnJoin  = findViewById(R.id.btnJoinWaitingList);
        btnLeave = findViewById(R.id.btnLeaveWaitingList);
        btnComments = findViewById(R.id.btnComments);
        eventImageView = findViewById(R.id.event_image);

        ImageButton backBtnTop = findViewById(R.id.back_btn_top);
        if (backBtnTop != null) backBtnTop.setOnClickListener(v -> finish());

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId != null) {
            loadEventDetails(eventId);
            checkIfAlreadyJoined(eventId);
        }

        if (btnJoin != null) btnJoin.setOnClickListener(v -> joinWaitingList());
        if (btnLeave != null) btnLeave.setOnClickListener(v -> leaveWaitingList());
        
        if (btnComments != null) {
            btnComments.setOnClickListener(v -> {
                if (eventId != null) {
                    CommentsFragment fragment = CommentsFragment.newInstance(eventId);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
>>>>>>> main
        }

        setupBottomNav();

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Unable to open event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventDetails(eventId);
    }

    private void openComments() {
        if (eventId == null || eventId.trim().isEmpty()) {
            return;
        }

        CommentsFragment fragment = CommentsFragment.newInstance(eventId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadEventDetails(String targetEventId) {
        FirestoreHelper.getDb().collection("events")
                .document(targetEventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
<<<<<<< HEAD
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Event no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
=======
                    EventModel event = null;
                    try {
                        event = documentSnapshot.toObject(EventModel.class);
                    } catch (Exception e) {
                        event = new EventModel();
                        event.setWaitingList(new ArrayList<>());
                        String name = documentSnapshot.getString("name");
                        String date = documentSnapshot.getString("date");
                        String loc  = documentSnapshot.getString("location");
                        String age  = documentSnapshot.getString("ageGroup");
                        String poster = documentSnapshot.getString("posterImage");
                        Long price  = documentSnapshot.getLong("price");
                        Long spots  = documentSnapshot.getLong("totalSpots");
                        if (name  != null) event.setName(name);
                        if (date  != null) event.setDate(date);
                        if (loc   != null) event.setLocation(loc);
                        if (age   != null) event.setAgeGroup(age);
                        if (poster != null) event.setPosterImage(poster);
                        if (price != null) event.setPrice(price.doubleValue());
                        if (spots != null) event.setTotalSpots(spots.intValue());
                    }
                    if (event != null) {
                        // Check if current user is an invited host
                        List<String> invitedHosts = (List<String>) documentSnapshot.get("invitedHosts");
                        isInvitedHost = invitedHosts != null && invitedHosts.contains(userId);
                        
                        eventDetailsList.clear();
                        if (isInvitedHost) {
                            eventDetailsList.add("★ YOU ARE A CO-HOST ★");
                        }
                        eventDetailsList.add("Total Spots: " + event.getTotalSpots());
                        eventDetailsList.add("Current Waiting List: " + event.getWaitingListCount());
                        eventDetailsList.add(String.format(Locale.getDefault(), "Price: $%d", (int) event.getPrice()));
                        eventDetailsList.add("Age Group: " + event.getAgeGroup());
                        eventDetailsList.add("Location: " + event.getLocation());
                        eventDetailsList.add("Date: " + event.getDate());
                        
                        if (costHeading != null)
                            costHeading.setText(String.format(Locale.getDefault(), "$%d", (int) event.getPrice()));
                        if (eventHeading != null)
                            eventHeading.setText(event.getName());
                        
                        // Load poster image
                        if (eventImageView != null && event.getPosterImage() != null && !event.getPosterImage().isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(event.getPosterImage(), Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                eventImageView.setImageBitmap(decodedByte);
                            } catch (Exception e) {
                                eventImageView.setImageResource(R.mipmap.ic_launcher);
                            }
                        } else if (eventImageView != null) {
                            eventImageView.setImageResource(R.mipmap.ic_launcher);
                        }

                        adapter.notifyDataSetChanged();
                        updateJoinLeaveButtons();

                        if (qrCodeView != null) {
                            Bitmap qr = QRCodeHelper.generateQRCode(eventId);
                            if (qr != null) {
                                qrCodeView.setImageBitmap(qr);
                                qrCodeView.setVisibility(View.VISIBLE);
                            }
                        }
>>>>>>> main
                    }

                    currentEvent = buildEventModel(documentSnapshot);
                    isInvitedHost = isCurrentUserHost(documentSnapshot, currentEvent);

                    renderEventBasics();
                    refreshApplicationState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

<<<<<<< HEAD
    private EventModel buildEventModel(DocumentSnapshot snapshot) {
        try {
            EventModel model = snapshot.toObject(EventModel.class);
            if (model != null) {
                return model;
            }
        } catch (Exception ignored) {
        }

        EventModel fallback = new EventModel();
        fallback.setWaitingList(new ArrayList<>());

        String name = snapshot.getString("name");
        String date = snapshot.getString("date");
        String location = snapshot.getString("location");
        String ageGroup = snapshot.getString("ageGroup");
        String posterImage = snapshot.getString("posterImage");
        String description = snapshot.getString("description");
        String organizerId = snapshot.getString("organizerId");
        String visibility = snapshot.getString("visibility");

        Double price = snapshot.getDouble("price");
        Long longPrice = snapshot.getLong("price");
        Long totalSpotsValue = snapshot.getLong("totalSpots");

        if (name != null) fallback.setName(name);
        if (date != null) fallback.setDate(date);
        if (location != null) fallback.setLocation(location);
        if (ageGroup != null) fallback.setAgeGroup(ageGroup);
        if (posterImage != null) fallback.setPosterImage(posterImage);
        if (description != null) fallback.setDescription(description);
        if (organizerId != null) fallback.setOrganizerId(organizerId);
        if (visibility != null) fallback.setVisibility(visibility);

        if (price != null) {
            fallback.setPrice(price);
        } else if (longPrice != null) {
            fallback.setPrice(longPrice.doubleValue());
        }

        if (totalSpotsValue != null) {
            fallback.setTotalSpots(totalSpotsValue.intValue());
        }

        List<String> invitedHosts = castStringList(snapshot.get("invitedHosts"));
        if (invitedHosts != null) {
            fallback.setInvitedHosts(invitedHosts);
        }

        ArrayList<String> geoList = new ArrayList<>();
        List<String> storedGeoList = castStringList(snapshot.get("geolocationList"));
        if (storedGeoList != null) {
            geoList.addAll(storedGeoList);
        }
        fallback.setGeolocationList(geoList);

        Boolean geolocationVerification = snapshot.getBoolean("geolocationVerification");
        fallback.setGeolocationVerification(geolocationVerification != null && geolocationVerification);

        return fallback;
    }

    private boolean isCurrentUserHost(DocumentSnapshot snapshot, EventModel eventModel) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        String organizerId = firstNonBlank(
                eventModel != null ? eventModel.getOrganizerId() : null,
                snapshot.getString("organizerId")
        );

        if (userId.equals(organizerId)) {
            return true;
        }

        List<String> invitedHosts = castStringList(snapshot.get("invitedHosts"));
        return invitedHosts != null && invitedHosts.contains(userId);
    }

    private void renderEventBasics() {
        if (currentEvent == null) {
            return;
        }

        if (eventHeading != null) {
            eventHeading.setText(valueOrDefault(currentEvent.getName(), "Event"));
        }

        if (costHeading != null) {
            costHeading.setText(String.format(Locale.getDefault(), "$%d", (int) Math.round(currentEvent.getPrice())));
        }

        bindPoster(currentEvent.getPosterImage());
        bindQrCode();
    }

    private void refreshApplicationState() {
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    waitingCount = 0;
                    selectedCount = 0;
                    acceptedCount = 0;
                    currentApplicationId = null;
                    currentApplicationStatus = "";

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = normalizeStatus(doc.getString("status"));
                        if ("waiting".equals(status)) {
                            waitingCount++;
                        } else if ("selected".equals(status)) {
                            selectedCount++;
                        } else if ("accepted".equals(status)) {
                            acceptedCount++;
                        }

                        String applicantUserId = doc.getString("userId");
                        if (userId != null && userId.equals(applicantUserId)) {
                            currentApplicationId = doc.getId();
                            currentApplicationStatus = status;
                        }
                    }

                    renderEventDetails();
                    updateJoinLeaveButtons();
                })
                .addOnFailureListener(e -> {
                    renderEventDetails();
=======
    private void checkIfAlreadyJoined(String eventId) {
        if (userId == null) return;
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    isOnWaitingList = !snap.isEmpty();
>>>>>>> main
                    updateJoinLeaveButtons();
                });
    }

<<<<<<< HEAD
    private void renderEventDetails() {
        eventDetailsList.clear();

        if (currentEvent == null) {
            adapter.notifyDataSetChanged();
            return;
        }

        if (isInvitedHost) {
            eventDetailsList.add("You are a co-host for this event.");
        }

        eventDetailsList.add("Total Spots : " + currentEvent.getTotalSpots());
        eventDetailsList.add("Current Waiting List : " + waitingCount);
        eventDetailsList.add("Selected Pending : " + selectedCount);
        eventDetailsList.add("Registered : " + acceptedCount);
        eventDetailsList.add("Allowed : " + valueOrDefault(currentEvent.getAgeGroup(), "all age groups"));
        eventDetailsList.add("Description : " + valueOrDefault(currentEvent.getDescription(), "No description provided."));
        eventDetailsList.add("Venue : " + valueOrDefault(currentEvent.getLocation(), "Location TBA"));
        eventDetailsList.add("Date of the Event : " + valueOrDefault(currentEvent.getDate(), "Date TBA"));

        adapter.notifyDataSetChanged();
        updateStatusCopy();
    }

    private void updateStatusCopy() {
        if (statusHeading == null || helperMessageView == null) {
            return;
        }

        String normalized = EventFlowRules.normalizeStatus(currentApplicationStatus);
        String label = EventFlowRules.getEventStatusLabel(currentApplicationStatus, isInvitedHost);
        String helper;
        int color;

        if (isInvitedHost) {
            helper = "Co-hosts can manage this event but cannot join the waiting list.";
            color = 0xFF6B5FA6;
        } else {
            switch (normalized) {
                case "waiting":
                    helper = "Participants are randomly selected after registration closes.";
                    color = 0xFFE4A900;
                    break;
                case "selected":
                    helper = "You were chosen. Please accept or decline from this screen.";
                    color = 0xFF7C74FF;
                    break;
                case "accepted":
                    helper = "You accepted your invitation and are registered for this event.";
                    color = 0xFF40C66E;
                    break;
                case "declined":
                    helper = "You declined this invitation.";
                    color = 0xFFC96A58;
                    break;
                case "cancelled":
                    helper = "Your invitation expired or was cancelled by the organizer.";
                    color = 0xFFC96A58;
                    break;
                default:
                    helper = "Join the waiting list to enter the draw.";
                    color = 0xFF6B5FA6;
                    break;
            }
        }

        statusHeading.setText(label);
        statusHeading.setTextColor(color);
        helperMessageView.setText(helper);
        helperMessageView.setTextColor(color == 0xFF40C66E ? 0xFF3F8E57 : 0xFF8A5570);
    }

    private void joinWaitingList() {
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Please log in to join", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        if (isInvitedHost) {
            Toast.makeText(this, "Hosts cannot join the waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = normalizeStatus(currentApplicationStatus);
        if (!status.isEmpty()) {
            Toast.makeText(this, "You already have a status for this event.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEvent == null) {
            Toast.makeText(this, "Event details are still loading.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> allowedLocations = currentEvent.getGeolocationList();
        if (currentEvent.isGeolocationVerification() && allowedLocations != null && !allowedLocations.isEmpty()) {
            checkLocationAndJoin(allowedLocations);
        } else {
            tryGetLocationAndJoin();
        }
    }

    private void tryGetLocationAndJoin() {
        boolean hasFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (hasFine || hasCoarse) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, this::performJoin);
=======
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
                        // Even if verification is off, try to capture location for the map
                        tryGetLocationAndJoin();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to verify event settings", Toast.LENGTH_SHORT).show());
    }

    private void tryGetLocationAndJoin() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                performJoin(location);
            });
>>>>>>> main
        } else {
            performJoin(null);
        }
    }

    private void checkLocationAndJoin(List<String> allowedLocations) {
<<<<<<< HEAD
        boolean hasFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!hasFine && !hasCoarse) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location == null) {
                        Toast.makeText(this, "Could not verify your location.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isLocationInAllowedList(location, allowedLocations)) {
                        performJoin(location);
                    } else {
                        Toast.makeText(this, "Your location is not eligible for this event.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getAddressString(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    String line = address.getAddressLine(i);
                    if (line != null && !line.trim().isEmpty()) {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(line);
                    }
                }
                return builder.toString();
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    private boolean isLocationInAllowedList(Location location, List<String> allowedLocations) {
        if (allowedLocations == null || allowedLocations.isEmpty()) {
            return true;
        }

        String address = getAddressString(location).toLowerCase(Locale.getDefault());
        if (address.isEmpty()) {
            return false;
        }

        for (String allowed : allowedLocations) {
            if (allowed != null && !allowed.trim().isEmpty()
                    && address.contains(allowed.trim().toLowerCase(Locale.getDefault()))) {
                return true;
            }
        }

=======
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                String myLocation = getAddressString(location);
                Toast.makeText(this, "My location: " + myLocation, Toast.LENGTH_SHORT).show();
                
                if (isLocationInAllowedList(location, allowedLocations)) {
                    performJoin(location);
                } else {
                    Toast.makeText(this, "You must be in an allowed location to join this event.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Could not determine your location. Please ensure location is enabled.", Toast.LENGTH_SHORT).show();
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
                    String lowerAllowed = allowed.toLowerCase();
                    if ((city != null && lowerAllowed.contains(city.toLowerCase())) ||
                        (province != null && lowerAllowed.contains(province.toLowerCase())) ||
                        (country != null && lowerAllowed.contains(country.toLowerCase()))) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
>>>>>>> main
        return false;
    }

    private void performJoin(Location location) {
        Map<String, Object> application = new HashMap<>();
        application.put("eventId", eventId);
        application.put("userId", userId);
<<<<<<< HEAD
        application.put("userName", valueOrDefault(deviceData.getUsername(), "Entrant"));
        application.put("status", "waiting");

        if (location != null) {
=======
        application.put("userName", DeviceData.getInstance(this).getUsername());
        application.put("status", "waiting");
        
        if (location != null) {
            // Storing as a GeoPoint for map display
>>>>>>> main
            application.put("geoPoint", new GeoPoint(location.getLatitude(), location.getLongitude()));
        }

        FirestoreHelper.getDb().collection("applications")
                .add(application)
<<<<<<< HEAD
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Joined waiting list", Toast.LENGTH_SHORT).show();
                    refreshApplicationState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to join waiting list", Toast.LENGTH_SHORT).show());
    }

    private void acceptSelection() {
        if (!"selected".equals(normalizeStatus(currentApplicationStatus)) || currentApplicationId == null) {
            Toast.makeText(this, "No invitation to accept.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .document(currentApplicationId)
                .update("status", "accepted")
                .addOnSuccessListener(unused -> {
                    sendOrganizerNotification("accepted the invitation");
                    Toast.makeText(this, "You are registered for this event.", Toast.LENGTH_SHORT).show();
                    refreshApplicationState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to accept invitation", Toast.LENGTH_SHORT).show());
    }

    private void declineSelection() {
        if (!"selected".equals(normalizeStatus(currentApplicationStatus)) || currentApplicationId == null) {
            Toast.makeText(this, "No invitation to decline.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .document(currentApplicationId)
                .update("status", "declined")
                .addOnSuccessListener(unused -> {
                    sendOrganizerNotification("declined the invitation");
                    fillReplacementSpotsAfterDecline();
                    Toast.makeText(this, "Invitation declined.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to decline invitation", Toast.LENGTH_SHORT).show());
    }

    private void fillReplacementSpotsAfterDecline() {
        if (currentEvent == null) {
            refreshApplicationState();
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int occupiedSpots = 0;
                    List<QueryDocumentSnapshot> waitingApplicants = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = normalizeStatus(doc.getString("status"));
                        if ("accepted".equals(status) || "selected".equals(status)) {
                            occupiedSpots++;
                        } else if ("waiting".equals(status)) {
                            waitingApplicants.add(doc);
                        }
                    }

                    int openSpots = currentEvent.getTotalSpots() - occupiedSpots;
                    if (openSpots <= 0 || waitingApplicants.isEmpty()) {
                        refreshApplicationState();
                        return;
                    }

                    Collections.shuffle(waitingApplicants);
                    int replacements = Math.min(openSpots, waitingApplicants.size());
                    WriteBatch batch = FirestoreHelper.getDb().batch();

                    for (int i = 0; i < replacements; i++) {
                        QueryDocumentSnapshot waitingDoc = waitingApplicants.get(i);
                        batch.update(waitingDoc.getReference(), "status", "selected");
                        queueNotification(
                                batch,
                                waitingDoc.getString("userId"),
                                "A spot opened up for " + safeEventName()
                                        + ". You were selected and can now accept or decline."
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> refreshApplicationState())
                            .addOnFailureListener(e -> refreshApplicationState());
                })
                .addOnFailureListener(e -> refreshApplicationState());
    }

    private void leaveWaitingList() {
        if (!"waiting".equals(normalizeStatus(currentApplicationStatus)) || currentApplicationId == null) {
            Toast.makeText(this, "You are not on the waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreHelper.getDb().collection("applications")
                .document(currentApplicationId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                    refreshApplicationState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to leave waiting list", Toast.LENGTH_SHORT).show());
    }

    private void updateJoinLeaveButtons() {
        if (btnJoin == null || btnLeave == null || btnAccept == null || btnDecline == null) {
            return;
        }

        btnJoin.setVisibility(EventFlowRules.canJoin(currentApplicationStatus, isInvitedHost) ? View.VISIBLE : View.GONE);
        btnLeave.setVisibility(EventFlowRules.canLeave(currentApplicationStatus) ? View.VISIBLE : View.GONE);
        btnAccept.setVisibility(EventFlowRules.canAccept(currentApplicationStatus) ? View.VISIBLE : View.GONE);
        btnDecline.setVisibility(EventFlowRules.canDecline(currentApplicationStatus) ? View.VISIBLE : View.GONE);
    }

    private void bindPoster(String posterBase64) {
        if (eventImageView == null) {
            return;
        }

        if (posterBase64 != null && !posterBase64.trim().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(posterBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    eventImageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        eventImageView.setImageResource(R.mipmap.ic_launcher);
    }

    private void bindQrCode() {
        if (qrCodeView == null || eventId == null || eventId.trim().isEmpty()) {
            return;
        }

        Bitmap qrCode = QRCodeHelper.generateQRCode(eventId);
        if (qrCode != null) {
            qrCodeView.setImageBitmap(qrCode);
            qrCodeView.setVisibility(View.VISIBLE);
        } else {
            qrCodeView.setVisibility(View.GONE);
        }
    }

    private void sendOrganizerNotification(String action) {
        if (currentEvent == null) {
            return;
        }

        String organizerId = currentEvent.getOrganizerId();
        if (organizerId == null || organizerId.trim().isEmpty() || organizerId.equals(userId)) {
            return;
        }

        String actorName = valueOrDefault(deviceData.getUsername(), "An entrant");
        String message = actorName + " " + action + " for " + safeEventName() + ".";
        writeNotification(organizerId, message);
    }

    private void writeNotification(String receiverId, String message) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("notificationList", FieldValue.arrayUnion(buildNotificationMap(receiverId, message)));

        FirestoreHelper.getDb().collection("notifications")
                .document(receiverId)
                .set(updateData, SetOptions.merge());
    }

    private void queueNotification(WriteBatch batch, String receiverId, String message) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("notificationList", FieldValue.arrayUnion(buildNotificationMap(receiverId, message)));
        batch.set(
                FirestoreHelper.getDb().collection("notifications").document(receiverId),
                updateData,
                SetOptions.merge()
        );
    }

    private Map<String, Object> buildNotificationMap(String receiverId, String message) {
        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("senderAccountID", userId);
        notificationMap.put("receiverAccountID", receiverId);
        notificationMap.put("message", message);
        notificationMap.put("timestamp", buildTimestamp());
        notificationMap.put("eventID", eventId);
        return notificationMap;
    }

    private String buildTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String safeEventName() {
        return currentEvent != null ? valueOrDefault(currentEvent.getName(), "this event") : "this event";
    }

    private List<String> castStringList(Object value) {
        if (!(value instanceof List)) {
            return null;
        }

        List<?> rawList = (List<?>) value;
        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private String normalizeStatus(String status) {
        return EventFlowRules.normalizeStatus(status);
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ArrayList<String> allowedLocations = currentEvent != null ? currentEvent.getGeolocationList() : null;
            if (currentEvent != null && currentEvent.isGeolocationVerification()
                    && allowedLocations != null && !allowedLocations.isEmpty()) {
                checkLocationAndJoin(allowedLocations);
            } else {
                tryGetLocationAndJoin();
            }
        } else {
            Toast.makeText(this, "Location permission is required for this event.", Toast.LENGTH_SHORT).show();
=======
                .addOnSuccessListener(ref -> {
                    isOnWaitingList = true;
                    updateJoinLeaveButtons();
                    Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show());
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

    private void leaveWaitingList() {
        if (userId == null) return;
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (userId.equals(doc.getString("userId"))) {
                            doc.getReference().delete();
                        }
                    }
                    isOnWaitingList = false;
                    updateJoinLeaveButtons();
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to leave", Toast.LENGTH_SHORT).show());
    }

    private void updateJoinLeaveButtons() {
        if (isInvitedHost) {
            if (btnJoin != null) btnJoin.setVisibility(View.GONE);
            if (btnLeave != null) btnLeave.setVisibility(View.GONE);
        } else {
            if (btnJoin != null) btnJoin.setVisibility(isOnWaitingList ? View.GONE : View.VISIBLE);
            if (btnLeave != null) btnLeave.setVisibility(isOnWaitingList ? View.VISIBLE : View.GONE);
>>>>>>> main
        }
    }

    private void setupBottomNav() {
<<<<<<< HEAD
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        findViewById(R.id.navCreate).setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerActivity.class)));
<<<<<<< Updated upstream

        findViewById(R.id.navHistory).setOnClickListener(v ->
                HistoryActivity.openFrom(this, userId));

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (userId != null && !userId.trim().isEmpty()) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("accountID", userId);
                startActivity(intent);
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        });
=======
        View navHistory = findViewById(R.id.navHistory);
        if (navHistory != null) navHistory.setOnClickListener(v ->
                NavigationHelper.openHistory(this));
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
>>>>>>> Stashed changes
    }

    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {

        private final List<String> items;

        SimpleTextAdapter(List<String> items) {
            this.items = items;
        }
=======
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) navHome.setOnClickListener(v -> finish());
        View navCreate = findViewById(R.id.navCreate);
        if (navCreate != null) navCreate.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerActivity.class)));
        View navHistory = findViewById(R.id.navHistory);
        if (navHistory != null) navHistory.setOnClickListener(v ->
                Toast.makeText(this, "History — coming soon", Toast.LENGTH_SHORT).show());
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final List<String> data;
        SimpleTextAdapter(List<String> data) { this.data = data; }
>>>>>>> main

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(items.get(position));
            holder.textView.setTextSize(16f);
            holder.textView.setSingleLine(false);
            int horizontal = (int) (12 * holder.textView.getResources().getDisplayMetrics().density);
            int vertical = (int) (6 * holder.textView.getResources().getDisplayMetrics().density);
            holder.textView.setPadding(horizontal, vertical, horizontal, vertical);
        }

        @Override
<<<<<<< HEAD
        public int getItemCount() {
            return items.size();
        }
=======
        public int getItemCount() { return data.size(); }
>>>>>>> main

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
