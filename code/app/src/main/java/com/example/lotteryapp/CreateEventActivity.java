package com.example.lotteryapp;

import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    EditText eventNameInput, eventLocationInput, eventPriceInput, eventDescriptionInput,
            eventDateInput, eventTotalSpotsInput;
    Button createEventButton;
    ImageView eventQRCode;
    private boolean eventCreated = false;
    private DeviceData deviceData;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_event);

        db = FirebaseFirestore.getInstance();
        deviceData = DeviceData.getInstance(this);

        eventNameInput        = findViewById(R.id.eventNameInput);
        eventLocationInput    = findViewById(R.id.eventLocationInput);
        eventPriceInput       = findViewById(R.id.eventPriceInput);
        eventDescriptionInput = findViewById(R.id.eventDescriptionInput);
        eventDateInput        = findViewById(R.id.eventDateInput);
        eventTotalSpotsInput  = findViewById(R.id.eventTotalSpotsInput);
        createEventButton     = findViewById(R.id.createEventButton);
        eventQRCode           = findViewById(R.id.eventQRCode);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        createEventButton.setOnClickListener(v -> {
            if (!eventCreated) {
                createEvent();
            } else {
                finish();
            }
        });
    }

    private void createEvent() {
        String name        = eventNameInput.getText().toString().trim();
        String location    = eventLocationInput.getText().toString().trim();
        String priceStr    = eventPriceInput.getText().toString().trim();
        String description = eventDescriptionInput.getText().toString().trim();
        String date        = eventDateInput.getText().toString().trim();
        String spotsStr    = eventTotalSpotsInput.getText().toString().trim();

        if (name.isEmpty() || location.isEmpty() || priceStr.isEmpty() ||
                description.isEmpty() || date.isEmpty() || spotsStr.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int totalSpots;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid price", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            totalSpots = Integer.parseInt(spotsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid number of spots", Toast.LENGTH_SHORT).show();
            return;
        }

        // use the logged-in accountID so OrganizerActivity can find it
        String organizerId = deviceData.getAccountID();
        if (organizerId == null) {
            Toast.makeText(this, "Please log in before creating an event", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("location", location);
        event.put("price", price);
        event.put("description", description);
        event.put("date", date);
        event.put("totalSpots", totalSpots);
        event.put("waitingList", new ArrayList<>());
        event.put("ageGroup", "All Ages");
        event.put("organizerId", organizerId);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(docRef -> {
                    String eventId = docRef.getId();
                    Bitmap qr = QRCodeHelper.generateQRCode(eventId);
                    if (qr != null) {
                        eventQRCode.setImageBitmap(qr);
                        eventQRCode.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "QR code generation failed", Toast.LENGTH_SHORT).show();
                    }
                    eventCreated = true;
                    createEventButton.setText("Done");
                    Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}