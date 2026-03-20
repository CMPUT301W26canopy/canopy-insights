package com.example.lotteryapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

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

        eventNameInput = findViewById(R.id.eventNameInput);
        eventLocationInput = findViewById(R.id.eventLocationInput);
        eventPriceInput = findViewById(R.id.eventPriceInput);
        eventDescriptionInput = findViewById(R.id.eventDescriptionInput);
        eventDateInput = findViewById(R.id.eventDateInput);
        eventTotalSpotsInput = findViewById(R.id.eventTotalSpotsInput);
        createEventButton = findViewById(R.id.createEventButton);
        eventQRCode = findViewById(R.id.eventQRCode);

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
        String name = eventNameInput.getText().toString().trim();
        String location = eventLocationInput.getText().toString().trim();
        String priceStr = eventPriceInput.getText().toString().trim();
        String description = eventDescriptionInput.getText().toString().trim();
        String date = eventDateInput.getText().toString().trim();
        String totalSpotsStr = eventTotalSpotsInput.getText().toString().trim();

        if (name.isEmpty() || location.isEmpty() || priceStr.isEmpty() ||
                description.isEmpty() || date.isEmpty() || totalSpotsStr.isEmpty()) {
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
            totalSpots = Integer.parseInt(totalSpotsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid number of spots", Toast.LENGTH_SHORT).show();
            return;
        }

        String organizerId = deviceData.isLoggedIn() ? deviceData.getAccountID() : "anonymous";

        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("location", location);
        event.put("price", price);
        event.put("description", description);
        event.put("date", date);
        event.put("totalSpots", totalSpots);
        event.put("waitingList", 0);
        event.put("ageGroup", "All Ages");
        event.put("organizerId", organizerId);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(docRef -> {
                    String eventId = docRef.getId();   // Firestore document ID
                    Bitmap qr = QRCodeHelper.generateQRCode(eventId);
                    if (qr != null) {
                        eventQRCode.setImageBitmap(qr);
                        eventQRCode.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "QR code generation failed", Toast.LENGTH_SHORT).show();
                    }

                // Change button to Done regardless
                    eventCreated = true;
                    createEventButton.setText("Done");
                    Toast.makeText(this, "Event created! QR code generated.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
