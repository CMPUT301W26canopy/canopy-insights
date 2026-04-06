package com.example.lotteryapp;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets organizers create events, upload a poster, and optionally configure
 * privacy, geolocation checks, and a waiting-list cap.
 */
public class CreateEventActivity extends AppCompatActivity {

    EditText eventNameInput, eventLocationInput, eventPriceInput, eventDescriptionInput,
            eventDateInput, eventTotalSpotsInput, eventWaitlistLimitInput;
    Button createEventButton, btnPrivate, btnGeolocation, btnUploadPoster;
    ImageView eventQRCode, eventPosterPreview;
    private boolean eventCreated = false;
    private final ArrayList<String> addedLocations = new ArrayList<>();
    private boolean geolocationVerification = false;
    private String selectedPosterBase64;

    private DeviceData deviceData;
    private ActivityResultLauncher<String> pickImageLauncher;

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
        eventWaitlistLimitInput = findViewById(R.id.eventWaitlistLimitInput);
        createEventButton     = findViewById(R.id.createEventButton);
        btnPrivate            = findViewById(R.id.btnPrivate);
        btnGeolocation        = findViewById(R.id.btnGeolocation);
        btnUploadPoster       = findViewById(R.id.btnUploadPoster);
        eventQRCode           = findViewById(R.id.eventQRCode);
        eventPosterPreview    = findViewById(R.id.eventPosterPreview);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePosterSelection);

        btnPrivate.setOnClickListener(v -> {
            if (btnPrivate.getText().toString().equalsIgnoreCase("Public")) {
                btnPrivate.setText("Private");
            } else {
                btnPrivate.setText("Public");
            }
        });

        createEventButton.setOnClickListener(v -> {
            if (!eventCreated) {
                createEvent();
            } else {
                finish();
            }
        });

        btnGeolocation.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.geolocationContainer, new GeolocationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnUploadPoster.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    public ArrayList<String> getAddedLocations() {
        return addedLocations;
    }

    public boolean isGeolocationVerification() {
        return geolocationVerification;
    }

    public void setGeolocationVerification(boolean geolocationVerification) {
        this.geolocationVerification = geolocationVerification;
    }

    /**
     * Validates the form, builds the Firestore event payload, and saves it.
     */
    private void createEvent() {
        String name        = eventNameInput.getText().toString().trim();
        String location    = eventLocationInput.getText().toString().trim();
        String priceStr    = eventPriceInput.getText().toString().trim();
        String description = eventDescriptionInput.getText().toString().trim();
        String date        = eventDateInput.getText().toString().trim();
        String spotsStr    = eventTotalSpotsInput.getText().toString().trim();
        String waitlistLimitStr = eventWaitlistLimitInput.getText().toString().trim();
        String visibility  = btnPrivate.getText().toString();

        if (name.isEmpty() || location.isEmpty() || priceStr.isEmpty() ||
                description.isEmpty() || date.isEmpty() || spotsStr.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int totalSpots;
        int waitingListLimit = 0;
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
        if (!waitlistLimitStr.isEmpty()) {
            try {
                waitingListLimit = Integer.parseInt(waitlistLimitStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter a valid waiting list limit", Toast.LENGTH_SHORT).show();
                return;
            }
            if (waitingListLimit < 0) {
                Toast.makeText(this, "Waiting list limit cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }
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
        event.put("waitingListLimit", waitingListLimit);
        event.put("waitingList", new ArrayList<>());
        event.put("invitedParticipants", new ArrayList<>());
        event.put("declinedParticipantInvites", new ArrayList<>());
        event.put("invitedHosts", new ArrayList<>());
        event.put("ageGroup", "All Ages");
        event.put("organizerId", organizerId);
        event.put("visibility", visibility);
        event.put("geolocationList", addedLocations);
        event.put("geolocationVerification", geolocationVerification);
        if (selectedPosterBase64 != null && !selectedPosterBase64.isEmpty()) {
            event.put("posterImage", selectedPosterBase64);
            event.put("poster", selectedPosterBase64);
        }

        db.collection("events")
                .add(event)
                .addOnSuccessListener(docRef -> {
                    String eventId = docRef.getId();
                    if (visibility.equalsIgnoreCase("Public")) {
                        Bitmap qr = QRCodeHelper.generateQRCode(eventId);
                        if (qr != null) {
                            eventQRCode.setImageBitmap(qr);
                            eventQRCode.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(this, "QR code generation failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        eventQRCode.setVisibility(View.GONE);
                    }

                    eventCreated = true;
                    createEventButton.setText("Done");
                    Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads the chosen image, normalizes orientation, and stores a portrait
     * poster version for the event.
     */
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
            if (selectedPosterBase64 == null || selectedPosterBase64.isEmpty()) {
                Toast.makeText(this, "Unable to save image", Toast.LENGTH_SHORT).show();
                return;
            }

            eventPosterPreview.setImageBitmap(scaledBitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
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
}
