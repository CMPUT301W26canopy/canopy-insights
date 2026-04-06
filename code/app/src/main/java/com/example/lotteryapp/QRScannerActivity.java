package com.example.lotteryapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

/**
 * Scans QR codes and routes the user to the matching event details.
 */
public class QRScannerActivity extends AppCompatActivity {

    // New Activity Result launcher
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                decodeQRCodeFromImage(imageUri);
                            }
                        } else {
                            Toast.makeText(this, "Image selection canceled.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

    /**
     * Initializes the activity and launches the image picker.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Launch the image picker
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    /**
     * Decodes a QR code from the provided image URI and navigates to the event details.
     * @param imageUri The URI of the image to decode.
     */
    private void decodeQRCodeFromImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            String eventId = QRCodeHelper.decodeQRCode(bitmap); // your helper function
            if (eventId != null) {
                Intent intent = new Intent(QRScannerActivity.this, EventActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "No QR code found in image.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
