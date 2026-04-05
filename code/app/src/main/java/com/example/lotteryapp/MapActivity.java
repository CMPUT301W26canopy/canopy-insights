package com.example.lotteryapp;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private WebView mapWebView;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        eventId = getIntent().getStringExtra("EVENT_ID");

        mapWebView = findViewById(R.id.mapWebView);
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Allow cross-origin requests for tile loading
        webSettings.setDomStorageEnabled(true);
        mapWebView.setWebViewClient(new WebViewClient());

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadApplicantLocations();
    }

    private void loadApplicantLocations() {
        if (eventId == null) return;

        FirestoreHelper.getDb().collection("applications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder markersJs = new StringBuilder();
                    double avgLat = 0, avgLng = 0;
                    int count = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GeoPoint geoPoint = doc.getGeoPoint("geoPoint");
                        if (geoPoint != null) {
                            String userName = doc.getString("userName");
                            // Escape single quotes in username for JS
                            String escapedUserName = userName != null ? userName.replace("'", "\\'") : "Unknown";
                            markersJs.append(String.format("L.marker([%f, %f]).addTo(map).bindPopup('%s');",
                                    geoPoint.getLatitude(), geoPoint.getLongitude(), escapedUserName));
                            
                            avgLat += geoPoint.getLatitude();
                            avgLng += geoPoint.getLongitude();
                            count++;
                        }
                    }

                    if (count > 0) {
                        avgLat /= count;
                        avgLng /= count;
                        loadLeafletMap(avgLat, avgLng, markersJs.toString());
                    } else {
                        Toast.makeText(this, "No applicant locations to display", Toast.LENGTH_SHORT).show();
                        // Load a default view if no locations (centered on a neutral location)
                        loadLeafletMap(53.5461, -113.4938, ""); // Example: Edmonton
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load locations", Toast.LENGTH_SHORT).show());
    }

    private void loadLeafletMap(double lat, double lng, String markersJs) {
        String html = "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no' />" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>#map { height: 100%; width: 100%; margin: 0; padding: 0; } body { margin: 0; padding: 0; }</style>" +
                "</head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map = L.map('map').setView([" + lat + ", " + lng + "], 10);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                "    attribution: '&copy; OpenStreetMap contributors'" +
                "}).addTo(map);" +
                // Fix for default marker icons not loading in WebView
                "delete L.Icon.Default.prototype._getIconUrl;" +
                "L.Icon.Default.mergeOptions({" +
                "    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png'," +
                "    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png'," +
                "    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png'" +
                "});" +
                markersJs +
                "</script></body></html>";

        // Using loadDataWithBaseURL is more reliable for resolving relative URLs or handling special characters
        mapWebView.loadDataWithBaseURL("https://unpkg.com/", html, "text/html", "UTF-8", null);
    }
}
