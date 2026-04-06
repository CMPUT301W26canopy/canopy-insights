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

    private final List<String> eventDetailsList = new ArrayList<>();
    private SimpleTextAdapter adapter;
    private TextView costHeading, eventHeading;
    private ImageView qrCodeView, eventImageView;
    private Button btnJoin, btnLeave, btnComments;
    private String eventId, userId;
    private boolean isOnWaitingList = false;
    private boolean isInvitedHost = false;
    
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_event);

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
        }

        setupBottomNav();
        qrCodeView = findViewById(R.id.event_qr_code);
    }

    private void loadEventDetails(String eventId) {
        FirestoreHelper.getDb().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
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
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

    private void checkIfAlreadyJoined(String eventId) {
        if (userId == null) return;
        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    isOnWaitingList = !snap.isEmpty();
                    updateJoinLeaveButtons();
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
        return false;
    }

    private void performJoin(Location location) {
        Map<String, Object> application = new HashMap<>();
        application.put("eventId", eventId);
        application.put("userId", userId);
        application.put("userName", DeviceData.getInstance(this).getUsername());
        application.put("status", "waiting");
        
        if (location != null) {
            // Storing as a GeoPoint for map display
            application.put("geoPoint", new GeoPoint(location.getLatitude(), location.getLongitude()));
        }

        FirestoreHelper.getDb().collection("applications")
                .add(application)
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
        }
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) navHome.setOnClickListener(v -> finish());
        View navCreate = findViewById(R.id.navCreate);
        if (navCreate != null) navCreate.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerActivity.class)));
        View navHistory = findViewById(R.id.navHistory);
        if (navHistory != null) navHistory.setOnClickListener(v ->
                NavigationHelper.openHistory(this));
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final List<String> data;
        SimpleTextAdapter(List<String> data) { this.data = data; }

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
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
